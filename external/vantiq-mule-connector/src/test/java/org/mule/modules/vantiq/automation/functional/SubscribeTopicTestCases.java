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

public class SubscribeTopicTestCases extends AbstractTestCase<VantiqConnector> {

    private final static String TEST_TOPIC = "/test/topic";
    
    public SubscribeTopicTestCases() {
        super(VantiqConnector.class);
    }
    
    @Before
    public void setUp() throws Throwable {
        getDispatcher().initializeSource("subscribeTopic", new Object[] { TEST_TOPIC, null } );
        
        // Sleep for a couple seconds to ensure the subscription has been completed
        Thread.sleep(2000);
    }
    
    @After
    public void tearDown() throws Throwable {
        getDispatcher().shutDownSource("subscribeTopic");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSubscribeTopic() throws Throwable {
        //
        // Publish a number of messages to the topic and see that the subscription was able to read it
        //
        Map<String,Object> payload = new HashMap<String,Object>();
        payload.put("id", "subscribe-topic-test");
        payload.put("x",  "a-value");
        
        // Note that failure would throw an exception
        getConnector().publishTopic(TEST_TOPIC, payload);

        //
        // We loop waiting for data to be received.
        //
        long startTime = System.currentTimeMillis();
        boolean done = false;
        while(!done) {
            List<Object> msgs = getDispatcher().getSourceMessages("subscribeTopic");
            if(msgs != null && msgs.size() > 0) {
                done = true;
                assert(msgs.size() == 1);
                
                Map<String,Object> msg = (Map<String,Object>) msgs.get(0);
                Map<String,Object> value = (Map<String,Object>) msg.get("value");
                
                assertThat("Correct id", value.get("id").toString(), is("subscribe-topic-test"));
                assertThat("Correct x",  value.get("x").toString(),  is("a-value"));
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