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

import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.TestConnectivity;
import org.mule.api.annotations.ValidateConnection;
import org.mule.api.annotations.components.ConnectionManagement;
import org.mule.api.annotations.display.Password;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Default;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vantiq.client.Vantiq;
import io.vantiq.client.VantiqResponse;

/**
 * Provides the connectivity to a Vantiq system using the Vantiq Java SDK.
 * 
 * @author Vantiq
 */
@ConnectionManagement(friendlyName = "Configuration")
public class VantiqConnectionManagement {
	
    private static Logger log = LoggerFactory.getLogger(VantiqConnectionManagement.class);

    private Vantiq vantiq;
    
    @Configurable
    @Default("https://dev.vantiq.com")
    private String server;
    
    @Configurable
    @Default("1")
    private int apiVersion;

    @Configurable
    @Default("/system/adapter/MuleSoft/inbound")
    private String topic;
    
    @Configurable
    @Default("30000")
    private int timeout;
    
    //--------------------------------------------------------------------------
    // Connection Management Methods
    //--------------------------------------------------------------------------    
    
    @Connect
    @TestConnectivity
    public void connect(@ConnectionKey String username, @Password String password) 
        throws ConnectionException {

        this.vantiq = new Vantiq(this.server, this.apiVersion);

        VantiqResponse response = this.vantiq.authenticate(username, password);
        int code = response.getStatusCode();
        if(code == 401) {
            throw new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS, null, "Invalid Credentials");            
        } else if(code == 404) {
            throw new ConnectionException(ConnectionExceptionCode.UNKNOWN_HOST, null, "Invalid Vantiq Server");         
        } else if(code >= 500) {
            throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, null, "Vantiq Server Error");                
        } else if(code >= 400 || !response.isSuccess()) {
            throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, null, "Vantiq Authentication Request");                
        } else {
            log.info("User '" + username + "' authenticated");
        }
    }
    
    @Disconnect
    public void disconnect() {
        this.vantiq = null;
    }
    
    @ConnectionIdentifier
    public String getAccessToken() {
        return (this.vantiq != null ? this.vantiq.getAccessToken() : null);
    }
    
    @ValidateConnection
    public boolean validateConnection() {
        return (this.vantiq != null && this.vantiq.isAuthenticated());
    }
    
    public Vantiq getVantiq() {
        return this.vantiq;
    }
    
    //--------------------------------------------------------------------------
    // Getters/Setters
    //--------------------------------------------------------------------------
        
    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

	public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(int apiVersion) {
        this.apiVersion = apiVersion;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
	
}