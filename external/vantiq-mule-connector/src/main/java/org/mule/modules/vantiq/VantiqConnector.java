/**
 * Copyright (c) 2016 Vantiq, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.mule.modules.vantiq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.mule.api.annotations.Config;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.MetaDataScope;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.SourceStrategy;
import org.mule.api.annotations.display.UserDefinedMetaData;
import org.mule.api.annotations.licensing.RequiresEnterpriseLicense;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.MetaDataKeyParam;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.SourceCallback;
import org.mule.modules.vantiq.error.VantiqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.vantiq.client.ResponseHandler;
import io.vantiq.client.SubscriptionCallback;
import io.vantiq.client.SubscriptionMessage;
import io.vantiq.client.Vantiq;
import io.vantiq.client.Vantiq.TypeOperation;
import io.vantiq.client.VantiqError;
import io.vantiq.client.VantiqResponse;
import okhttp3.Response;

/**
 * This connector that supports bi-directional integration between 
 * Vantiq systems and MuleSoft Anypoint systems.
 * 
 * The connector uses the <a href="https://github.com/Vantiq/vantiq-sdk-java">Vantiq Java SDK</a>.
 * Details of the SDK are documented in the 
 * <a href="https://github.com/Vantiq/vantiq-sdk-java/blob/master/docs/api.md">API docs</a>.
 * 
 * @author Vantiq
 */
@MetaDataScope(VantiqDataSenseResolver.class)
@Connector(name="vantiq", friendlyName="Vantiq", schemaVersion="1.0", minMuleVersion="3.6.0")
@RequiresEnterpriseLicense(allowEval = true)
public class VantiqConnector {

    private static final Logger log = LoggerFactory.getLogger(VantiqConnector.class); 
    
    @NotNull
    @Config
    private VantiqConnectionManagement connectionManagement;
    
    /**
     * The Vantiq SDK client instance that is the interface with the Vantiq
     * system.
     */
    private Vantiq vantiq;

    //--------------------------------------------------------------------------
    // Sources
    //--------------------------------------------------------------------------

    /**
     * Handler class for both subscriptions and responses
     */
    private static class SourceCallbackHandler implements SubscriptionCallback, ResponseHandler {
        
        private String path;
        private SourceCallback callback;
        
        public SourceCallbackHandler(SourceCallback callback, String path) {
            this.callback = callback;
            this.path = path;
        }
        
        @Override public void onConnect() {                
            log.info("Subscription successful: " + path);
        }

        @Override public void onMessage(SubscriptionMessage message) {
            processEvent(message.getBody());
        }

        @Override public void onError(String error) {
            log.error(error);
        }

        @Override public void onFailure(Throwable t) {
            log.error("Failure", t);
        }

        @Override
        public void onSuccess(Object body, Response response) {
            processEvent(body);
        }

        @Override
        public void onError(List<VantiqError> errors, Response response) {
            log.error("Error", errors);
        }
        
        private void processEvent(Object payload) {
            try {
                this.callback.process(payload);
            } catch(Exception ex) {
                log.error("Callback Error", ex);
            }
        }
            
    }
    
    /**
     * A single entry point that creates a subscription to a specific resource
     * in the currently connected Vantiq system.  The subscription is created
     * by creating a WebSocket through the Vantiq SDK that listens for 
     * event messages.
     * 
     * @param resource The Vantiq resource to listen, either TOPICS or TYPES. 
     * @param id The unique ID for the specific resource instance to listen to
     * @param op Only for TYPE events, the specific event type to subscribe to
     * @param callback The callback to use on the arrival of the event
     */
    private void subscribe(Vantiq.SystemResources resource, 
                           String id,
                           TypeOperation op, 
                           SourceCallback callback) {
        final String path;
        if(op == null) {
            path = resource + "/" + id;            
        } else {
            path = resource + "/" + id + "/" + op.toString().toLowerCase();
        } 
        getVantiq().subscribe(resource.value(), id, op, new SourceCallbackHandler(callback, path));
    }
    
    /**
     * Creates a source for messages that arise from topic (i.e. publish) events
     * occurring on the given Vantiq system.
     * 
     * A single message occurs for each event that occurs in Vantiq on the
     * given topic.  The message will be the payload of the Vantiq event.
     * 
     * @param topic The topic of interest
     */
    @Source(sourceStrategy = SourceStrategy.NONE)
    public void subscribeTopic(String topic,
                               SourceCallback callback) {
        this.subscribe(Vantiq.SystemResources.TOPICS, topic, null, callback);
    }

    /**
     * Creates a source for messages that arise from type events occurring on the
     * given Vantiq system.  Type events occur when changes occur on a specific
     * Vantiq data type.  This method creates a subscription for all type
     * events for a given data type and type operation.  The possible operations
     * are:
     * <ul>
     *   <li>INSERT: When a new record of the given type is created</li>
     *   <li>UPDATE: When an existing record of the given type is changed</li>
     *   <li>DELETE: When an existing record of the given type is removed</li>
     * </ul> 
     * 
     * DataSense is used to query Vantiq for the available data types that
     * are defined in the Vantiq system.
     * 
     * @param dataType The Vantiq data type
     * @param operation The specific type operation
     */
    @Source(sourceStrategy = SourceStrategy.NONE)
    @UserDefinedMetaData
    public void subscribeType(@MetaDataKeyParam String dataType, 
                              TypeOperation operation,
                              SourceCallback callback) {
        this.subscribe(Vantiq.SystemResources.TYPES, dataType, operation, callback);
    }
  
    /**
     * Creates a source that polls the Vantiq system for messages that 
     * result from a query.  The query matching criteria is defined
     * by the where clause.  The syntax for the where is defined 
     * in the <a href="https://dev.vantiq.com/docs/system/api/developer.html#-where-parameter">Vantiq developer documentation</a>.
     * In addition, the select list may be provided to restrict the
     * fields returned in each record.
     * 
     * If no where clause is provided, then all records are returned.
     * 
     * If no selectList is provided, then all fields are returned.
     *
     * @param dataType The Vantiq data type to query
     * @param selectList The optional list of properties to return for each record
     * @param where The optional where clause to filter the data
     * @param callback Callback for the messages
     */
    @Source(sourceStrategy = SourceStrategy.POLLING, pollingPeriod = 30000)
    @UserDefinedMetaData
    public void selectData(@MetaDataKeyParam String dataType, 
                           @Optional List<String> selectList,
                           @Optional Map<String,Object> where,
                           SourceCallback callback) {
        getVantiq().select(dataType, selectList, where, null, new SourceCallbackHandler(callback, null));
    }
    
    //--------------------------------------------------------------------------
    // Public Connector API
    //--------------------------------------------------------------------------

    /**
     * Returns the list of control actions from the Vantiq system.  Control
     * actions are messages that are triggered from within Vantiq that are
     * targeted for a specific purpose on a 3rd party system.
     * 
     * @return List of supported actions
     * @throws VantiqException Thrown if an error occurs
     * @throws IOException If an network error occurs
     */
    @Processor
    public List<String> getSupportedActions() throws IOException {                
        VantiqResponse resp = checkResponse(getVantiq().execute("Connector_GetControlActions", Collections.emptyMap()));

        List<String> result = new ArrayList<String>();
        for(JsonElement element : (JsonArray) resp.getBody()) {
            result.add(element.getAsJsonObject().get("action").getAsString());
        }
        return result;
    }
    
    /**
     * Publishes data targeted for a specific Vantiq data type that
     * is handled by a Vantiq adapter to import the data into the
     * Vantiq system.
     * 
     * The payload is a list of records where each record should 
     * match the fields in the Vantiq data type.
     * 
     * DataSense is used to query Vantiq for the available data types that
     * are defined in the Vantiq system.
     * 
     * @param dataType The target Vantiq data type
     * @param payload The list of records to send
     *
     * @throws VantiqException Thrown if an error occurs
     * @throws IOException If an network error occurs
     */
    @Processor
    @UserDefinedMetaData
    public void publishData(@MetaDataKeyParam      final String dataType, 
                            @Default("#[payload]") final List<Map<String,Object>> payload) throws IOException {
        String topic = this.connectionManagement.getTopic();
        
        //
        // Build message with the fields
        //
        //  type:    dataType,
        //  content: payload
        //
        Map<String,Object> message = new HashMap<String,Object>();
        message.put("type", dataType);
        message.put("content", payload);
        
        this.publishTopic(topic, message);
    }
    
    /**
     * Publishes data to Vantiq on a specific Vantiq topic as an ad-hoc means for pushing
     * data into Vantiq.  To handle the data in Vantiq, a rule should exist that listens on
     * the given topic.  The payload is transformed to JSON using GSON. 
     * 
     * @param topic The name of the topic (e.g. "/my/topic")
     * @param payload The content of the publish event.
     * 
     * @throws VantiqException Thrown if an error occurs
     * @throws IOException If an network error occurs
     */
    @Processor
    public void publishTopic(String topic,
                             @Default("#[payload]") final Object payload) throws IOException {
        checkResponse(getVantiq().publish(Vantiq.SystemResources.TOPICS.value(), topic, payload)); 
    }

    /**
     * Inserts data into Vantiq of a specific type.
     * 
     * @param dataType The target Vantiq data type
     * @param payload The record to insert
     * 
     * @throws VantiqException Thrown if an error occurs
     * @throws IOException If an network error occurs
     */
    @Processor 
    public void insertData(@MetaDataKeyParam final String dataType,
                           @Default("#[payload]") final Map<String,Object> payload) throws IOException {
        checkResponse(getVantiq().insert(dataType, payload));
    }
    
    /**
     * Throws an exception if the response was not successful.  Otherwise, this is a no-op.
     * 
     * @param response
     * @throws VantiqException if the response failed
     */
    private VantiqResponse checkResponse(VantiqResponse response) {
        if(!response.isSuccess()) {
            throw new VantiqException(response);
        }
        return response;
    }
    
    //--------------------------------------------------------------------------
    // Getters/Setters
    //--------------------------------------------------------------------------
    
    /**
     * Returns the connection management instance that is responsible for establishing 
     * and authenticating the connection
     * 
     * @return The connection management instance
     */
    public VantiqConnectionManagement getConnectionManagement() {
        return connectionManagement;
    }

    /**
     * Sets the connection management instance that is responsible for establishing 
     * and authenticating the connection
     * 
     * @param connectionManagement The connection management instance
     */
    public void setConnectionManagement(VantiqConnectionManagement connectionManagement) {
        this.connectionManagement = connectionManagement;
        this.vantiq = this.connectionManagement.getVantiq();
    }
    
    /**
     * Returns the Vantiq SDK instance
     * 
     * @return Vantiq instance
     */
    public Vantiq getVantiq() {
        return this.vantiq;
    }

}