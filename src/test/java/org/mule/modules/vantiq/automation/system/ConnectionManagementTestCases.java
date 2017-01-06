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
package org.mule.modules.vantiq.automation.system;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.mule.api.ConnectionException;
import org.mule.modules.vantiq.VantiqConnectionManagement;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ConnectionManagementTestCases {
    
    private static final String DEFAULT_SPRING_CONFIG = "AutomationSpringBeans.xml";

    private static ApplicationContext testContext;

    private static HashMap<String, Object> vantiqCredentials;

    @Before
    public void setUp() throws Exception {
        ArrayList<String> springConfigs = new ArrayList<String>();
        springConfigs.add(DEFAULT_SPRING_CONFIG);
        testContext = new ClassPathXmlApplicationContext(springConfigs.toArray(new String[springConfigs.size()]));
        vantiqCredentials = (HashMap<String, Object>) testContext.getBean("vantiqCredentials");
    }

    private void runTestWithFailure(String url, 
                                    String username, 
                                    String password,
                                    String expectedError) throws Exception {
        VantiqConnectionManagement cm = new VantiqConnectionManagement();
        
        // Verify failure
        try {
            cm.setServer(url);
            cm.connect(username, password);
            fail("Expected connection failure: " + expectedError);
        } catch(ConnectionException ex) {
            assertThat(expectedError, ex.getMessage(), is(expectedError));
        }
        
        // Ensure connection is not valid
        assertThat("Connection should be invalid", cm.validateConnection(), is(false));
    }
    
    private void runTestWithoutFailure(String url,
            String username,
            String password) throws Exception {
        VantiqConnectionManagement cm = new VantiqConnectionManagement();

        // Verify failure
        try {
            cm.setServer(url);
            cm.connect(username, password);
        } catch(ConnectionException ex) {
            fail("Expected successful connection but failed with error: " + ex.getMessage());
        }

        // Ensure connection is valid
        assertThat("Connection should be valid", cm.validateConnection(), is(true));
    }

    private <T> void runTestWithRuntimeError(String url, 
                                             String username, 
                                             String password, 
                                             Class<T> expectedException) throws Exception {        
        VantiqConnectionManagement cm = new VantiqConnectionManagement();

        // Verify failure
        try {
            cm.setServer(url);
            cm.connect(username, password);
            fail("Expected connection failure: " + expectedException.getName());
        } catch(RuntimeException ex) {
            String msg = cm.getServer() + " expected exception: " + expectedException.getName();
            assertThat(msg, ex.getCause(), instanceOf(expectedException));
        }
    }

    // For this test to work automation-credentials.properties must be supplied
    // in src/test/resources and contain a valid username and password
    @Test
    public void testValidConnection() throws Exception {
        runTestWithoutFailure("https://dev.vantiq.com",
                (String)vantiqCredentials.get("validUsername"),
                (String)vantiqCredentials.get("validPassword"));
    }

    @Test
    public void testInvalidServer() throws Exception {
        runTestWithRuntimeError("http://this-is.a-dummy.server-url", "someuser", "somepassword", UnknownHostException.class);
    }

    @Test
    public void testInvalidServerURL() throws Exception {
        runTestWithFailure("https://dev.vantiq.com/foo/bar", "someuser", "somepassword", "Invalid Vantiq Server URL");
    }

    @Test
    public void testInvalidCredentials() throws Exception {
        runTestWithFailure("https://dev.vantiq.com", "system", "somepassword", "Invalid Credentials");
    }

    @Test
    public void testBlankCredentials() throws Exception {
        runTestWithFailure("https://dev.vantiq.com", "", "", "Invalid Credentials");
    }

}
