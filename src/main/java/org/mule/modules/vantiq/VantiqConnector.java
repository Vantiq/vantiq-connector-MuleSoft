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
import java.lang.reflect.Type;
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
import org.mule.api.annotations.lifecycle.OnException;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.MetaDataKeyParam;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.SourceCallback;
import org.mule.modules.vantiq.error.VantiqErrorHandler;
import org.mule.modules.vantiq.error.VantiqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import io.vantiq.client.ResponseHandler;
import io.vantiq.client.SubscriptionCallback;
import io.vantiq.client.SubscriptionMessage;
import io.vantiq.client.Vantiq;
import io.vantiq.client.Vantiq.TypeOperation;
import io.vantiq.client.VantiqError;
import io.vantiq.client.VantiqResponse;
import okhttp3.Response;

/**
 * Vantiq Mule Connector that supports integration between Vantiq systems and Anypoint
 * systems.
 * 
 * @author Vantiq
 */
@MetaDataScope(VantiqDataSenseResolver.class)
@Connector(name="vantiq", friendlyName="Vantiq", schemaVersion="1.0", minMuleVersion="3.5.0")
@OnException(handler=VantiqErrorHandler.class)
public class VantiqConnector {

    static Gson  gson = new Gson();
    
    private static Logger log = LoggerFactory.getLogger(VantiqConnector.class); 
    
    @NotNull
    @Config
    private VantiqConnectionManagement connectionManagement;
    
    private Vantiq vantiq;

    //--------------------------------------------------------------------------
    // DataSense
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    // Sources
    //--------------------------------------------------------------------------

    private void subscribe(String resource, 
                           String id,
                           TypeOperation op, 
                           final SourceCallback callback) {
        final String path;
        if(op == null) {
            path = resource + "/" + id;            
        } else {
            path = resource + "/" + id + "/" + op.toString().toLowerCase();
        }
        this.vantiq.subscribe(resource, id, op, new SubscriptionCallback() {

            @Override public void onConnect() {                
                log.info("Subscription successful: " + path);
            }

            @Override public void onMessage(SubscriptionMessage message) {
                try {
                    callback.process(message.getBody());
                } catch(Exception ex) {
                    log.error("Subscription Error", ex);
                }
            }

            @Override public void onError(String error) {
                log.error(error);
            }

            @Override public void onFailure(Throwable t) {
                log.error("Failure", t);
            }
            
        });
    }
    
    /**
     * Creates a source that generates messages from PUBLISH events in Vantiq on 
     * a specific topic. 
     * 
     * @param topic The publish topic to subscribe to (e.g. "/test/topic")
     */
    @Source(sourceStrategy = SourceStrategy.NONE)
    public void subscribeTopic(String topic,
                               SourceCallback callback) {
        this.subscribe(Vantiq.SystemResources.TOPICS.value(), topic, null, callback);
    }

    /**
     * Creates a source that generates messages from TYPE events in Vantiq on 
     * a specific type with a specific operation. 
     * 
     * @param dataType The Vantiq data type that will generate the events
     * @param operation The type operation to listen to (i.e. "insert", "update", "delete")
     */
    @Source(sourceStrategy = SourceStrategy.NONE)
    @UserDefinedMetaData
    public void subscribeType(@MetaDataKeyParam String dataType, 
                              TypeOperation operation,
                              SourceCallback callback) {
        this.subscribe(Vantiq.SystemResources.TYPES.value(), dataType, operation, callback);
    }
    
    /**
     * Creates a source that generates messages from SOURCE events in Vantiq on 
     * a specific Vantiq Source.
     * 
     * @param source The Vantiq source that will generate the events
     * @param operation The type operation to listen to (i.e. "insert", "update", "delete")
     */
    @Source(sourceStrategy = SourceStrategy.NONE)
    public void subscribeSource(String source,
                                SourceCallback callback) {
        this.subscribe(Vantiq.SystemResources.SOURCES.value(), source, null, callback);
    }
        
    /**
     * Performs a query into the Vantiq system and returns the results as
     * messages.  The query has a default polling period of 30 seconds.
     * 
     * @param dataType The data type to query
     * @param selectList The optional list of properties to return for each record
     * @param where The optional where clause to filter the data
     * @param callback Callback for the messages
     */
    @Source(sourceStrategy = SourceStrategy.POLLING, pollingPeriod = 30000)
    @UserDefinedMetaData
    public void selectData(@MetaDataKeyParam String dataType, 
                           @Optional List<String> selectList,
                           @Optional Map<String,Object> where,
                           final SourceCallback callback) {
        this.vantiq.select(dataType, selectList, where, null, new ResponseHandler() {

            @Override
            public void onSuccess(Object body, Response response) {
                try {
                    callback.process(body);
                } catch(Exception ex) {
                    log.error("select: callback error", ex);
                }
            }

            @Override
            public void onError(List<VantiqError> errors, Response response) {
                log.error("select: Vantiq server errors", errors);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("select: error", t);
            }
            
        });
        
    }
    
    //--------------------------------------------------------------------------
    // Public Connector API
    //--------------------------------------------------------------------------

    /**
     * Returns the list of actions that are supported within Vantiq and can be 
     * the source for data within MuleSoft.
     * 
     * @return List of supported actions
     * @throws VantiqException Thrown if an error occurs
     * @throws IOException If an network error occurs
     */
    @Processor
    public List<Map<String,Object>> getSupportedActions() throws IOException {
        VantiqResponse response = this.vantiq.execute("SystemAdapterControlActions", Collections.EMPTY_MAP);
        if(response.isSuccess()) {
            Type resultType = new TypeToken<List<Map<String,Object>>>(){}.getType();
            return gson.fromJson((JsonArray) response.getBody(), resultType);
        } else {
            throw new VantiqException(response);
        }
    }
    
    /**
     * Publishes data into the Vantiq system using the MuleSoft adapter
     * 
     * @param dataType The name of the Vantiq data type to publish to
     * @param payload The data to send to Vantiq
     * @return if the publish was successful
     * @throws VantiqException Thrown if an error occurs
     * @throws IOException If an network error occurs
     */
    @Processor
    @UserDefinedMetaData
    public boolean publishData(final @MetaDataKeyParam String dataType, 
                               final @Default("#[payload]") List<Map<String,Object>> payload) throws IOException {
        String topic = this.connectionManagement.getTopic();
        
        //
        // Build message of the form
        //
        // {
        //    "type": dataType,
        //    "content": payload
        // }
        //
        Map<String,Object> message = new HashMap<String,Object>();
        message.put("type", dataType);
        message.put("content", payload);
        
        return this.publishTopic(topic, message);
    }
    
    /**
     * Publishes to a specific topic in Vantiq.
     * 
     * @param topic The name of the topic to publish to (e.g. "/test/topic")
     * @param payload The content of the publish event.  Note this will be transformed to JSON using GSON.
     * 
     * @return if the publish was successful
     */
    @Processor
    public boolean publishTopic(String topic,
                                final @Default("#[payload]") Object payload) throws IOException {
        VantiqResponse response = this.vantiq.publish(Vantiq.SystemResources.TOPICS.value(), topic, payload);
        if(response.isSuccess()) {
            return true;
        } else {
            throw new VantiqException(response);
        }
    }

    
    //--------------------------------------------------------------------------
    // Getters/Setters
    //--------------------------------------------------------------------------
    
    public VantiqConnectionManagement getConnectionManagement() {
        return connectionManagement;
    }

    public void setConnectionManagement(VantiqConnectionManagement connectionManagement) {
        this.connectionManagement = connectionManagement;
        this.vantiq = this.connectionManagement.getVantiq();
    }


}