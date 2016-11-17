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
package org.mule.modules.vantiq.automation.functional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.modules.vantiq.VantiqConnector;
import org.mule.tools.devkit.ctf.junit.AbstractTestCase;

import io.vantiq.client.Vantiq;

public class SubscribeTypeTestCases extends AbstractTestCase<VantiqConnector> {

    public SubscribeTypeTestCases() {
        super(VantiqConnector.class);
    }
    
    @Before
    public void setUp() throws Throwable {
        // Subscribe to any new log messages
        getDispatcher().initializeSource("subscribeType", 
                                         new Object[] { "ArsLogMessage", 
                                                        Vantiq.TypeOperation.INSERT, 
                                                        null } );
        
        // Sleep for a couple seconds to ensure the subscription has been completed
        Thread.sleep(2000);
    }
    
    @After
    public void tearDown() throws Throwable {
        getDispatcher().shutDownSource("subscribeType");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSubscribeType() throws Throwable {
        //
        // Insert a log message
        //
        Map<String,Object> logRecord = new HashMap<String,Object>();
        logRecord.put("invocationId", "testSubscribeType-" + System.currentTimeMillis());
        logRecord.put("timestamp", "2016-11-16T23:31:22.222Z");
        logRecord.put("sequenceId", 0);
        logRecord.put("level", "INFO");
        logRecord.put("message", "testSubscribeType log message");
                
        // Note that failure would throw an exception
        getConnector().insertData("ArsLogMessage", logRecord);

        //
        // We loop waiting for data to be received.
        //
        long startTime = System.currentTimeMillis();
        boolean done = false;
        while(!done) {
            List<Object> msgs = getDispatcher().getSourceMessages("subscribeType");
            if(msgs != null && msgs.size() > 0) {
                done = true;
                assert(msgs.size() == 1);
                
                Map<String,Object> msg = (Map<String,Object>) msgs.get(0);
                Map<String,Object> value = (Map<String,Object>) msg.get("value");
                
                assertThat("Correct sequenceId", ((Number) value.get("sequenceId")).intValue(), is(0));
                assertThat("Correct level", value.get("level").toString(), is("INFO"));
                assertThat("Correct message", value.get("message").toString(), is("testSubscribeType log message"));
            } else {
                long waited = (System.currentTimeMillis() - startTime);
                if(waited > 5000) {
                    fail("Timeout.  Reached 5 seconds without receiving a message.");
                } else {
                    // If we haven't received a message yet, wait 100ms, then check again
                    Thread.sleep(100);                    
                }
            }
        }
    }
}