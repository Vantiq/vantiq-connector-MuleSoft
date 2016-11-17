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
 * Provides the connectivity to a Vantiq system using the 
 * <a href="https://github.com/Vantiq/vantiq-sdk-java">Vantiq Java SDK</a>.
 * 
 * Basic (i.e. username/password) authentication is used to connect to Vantiq.
 * 
 * @author Vantiq
 */
@ConnectionManagement(friendlyName = "Configuration")
public class VantiqConnectionManagement {
	
    private static final Logger log = LoggerFactory.getLogger(VantiqConnectionManagement.class);

    private Vantiq vantiq;
    
    /**
     * The server URL of the Vantiq server.  The Vantiq cloud servers are: 
     * <ul>
     *   <li>Production: https://api.vantiq.com</li>
     *   <li>Development: https://dev.vantiq.com</li>
     * </ul>
     */
    @Configurable
    @Default("https://api.vantiq.com")
    private String server;
    
    /**
     * The Vantiq topic that is used on the Vantiq side to handle 
     * requests from Anypoint to import data into Vantiq.
     */
    @Configurable
    @Default("/system/connector/MuleSoft/inbound")
    private String topic;
    
    //--------------------------------------------------------------------------
    // Connection Management Methods
    //--------------------------------------------------------------------------    
    
    /**
     * Connects to Vantiq using the provided credentials and the configured
     * server URL.
     * 
     * If HTTP 401 is returned, then an INCORRECT_CREDENTIALS exception is thrown.
     * If HTTP 404 is returned, then an UNKNOWN_HOST exception is thrown.
     * If HTTP 5xx or another 4xx is return, then an UNKNOWN exception is thrown. 
     * 
     * @param username The username on the Vantiq server
     * @param password The password on the Vantiq server
     * @throws ConnectionException If the connection fails
     */
    @Connect
    @TestConnectivity
    public void connect(@ConnectionKey String username, @Password String password) throws ConnectionException {
        this.vantiq = new Vantiq(this.server);
        checkAuthenticateResponse(this.vantiq.authenticate(username, password));
        log.info("User '" + username + "' authenticated.");
    }
    
    /**
     * Checks the HTTP status codes to ensure the response was successful
     * 
     * @param response
     * @throws ConnectionException
     */
    private void checkAuthenticateResponse(VantiqResponse response) throws ConnectionException {
        int code = response.getStatusCode();
        if(code == 401) {
            throw new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS, null, "Invalid Credentials");            
        } else if(code == 404) {
            throw new ConnectionException(ConnectionExceptionCode.UNKNOWN_HOST, null, "Invalid Vantiq Server URL");         
        } else if(code >= 400 || !response.isSuccess()) {
            throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, null, "Authentication Error");                
        }        
    }
    
    /**
     * Disconnects from Vantiq, closing down all active subscriptions.
     */
    @Disconnect
    public void disconnect() {
        this.vantiq.unsubscribeAll();
        this.vantiq = null;
    }
    
    /**
     * Returns the unique identifier for the connection which is the access token
     * created during the authentication process.
     * 
     * @return accessToken The access token for the authenticated session
     */
    @ConnectionIdentifier
    public String getAccessToken() {
        return this.vantiq != null ? this.vantiq.getAccessToken() : null;
    }

    /**
     * Returns if the connection is connected and authenticated.
     * 
     * @return true if the connection is valid and the authentication is valid.
     */
    @ValidateConnection
    public boolean validateConnection() {
        return this.vantiq != null && this.vantiq.isAuthenticated();
    }
    
    /**
     * Returns the Vantiq SDK instace.
     * 
     * @return Vantiq SDK instance
     */
    public Vantiq getVantiq() {
        return this.vantiq;
    }
    
    //--------------------------------------------------------------------------
    // Getters/Setters
    //--------------------------------------------------------------------------
        
    /**
     * Returns the server URL for the Vantiq system
     * 
     * @return The Vantiq server URL
     */
    public String getServer() {
        return server;
    }

    /**
     * Sets the server URL for the Vantiq system
     * 
     * @param server The Vantiq server URL
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * Returns the Vantiq topic that is used in Vantiq to handle 
     * requests from Anypoint to import data into Vantiq.
     *
     * @return Vantiq topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Sets the Vantiq topic that is used in Vantiq to handle 
     * requests from Anypoint to import data into Vantiq.
     *
     * @param topic Vantiq topic
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

}