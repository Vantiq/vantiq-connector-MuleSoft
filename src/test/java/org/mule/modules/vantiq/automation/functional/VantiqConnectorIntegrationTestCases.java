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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mule.common.Result;
import org.mule.common.metadata.DefinedMapMetaDataModel;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataKey;
import org.mule.modules.vantiq.VantiqConnector;
import org.mule.tools.devkit.ctf.junit.AbstractTestCase;

public class VantiqConnectorIntegrationTestCases extends AbstractTestCase<VantiqConnector> {
    
//    private Logger log = LoggerFactory.getLogger(this.getClass());

    public VantiqConnectorIntegrationTestCases() {
        super(VantiqConnector.class);
    }
    
    @Test
    public void verifyGetSupportedActions() throws Exception {
        List<Map<String,Object>> actions = getConnector().getSupportedActions();
        assert(actions.size() > 0);
    }
    
    @Test
    public void verifyPublishData() throws Exception {
        String dataType = "TestType";
        Map<String,Object> payload = new HashMap<String,Object>();
        payload.put("id", "abc");
        payload.put("x", "def");
        List<Map<String,Object>> payloadList = new ArrayList<Map<String,Object>>();
        payloadList.add(payload);

        boolean status = getConnector().publishData(dataType, payloadList);
        assert(status);
    }
    
    @Test
    public void verifyMetaDataKeys() throws Exception {
        Result<List<MetaDataKey>> result = getDispatcher().fetchMetaDataKeys();
        assert(result.get().size() > 0);
        
        for(MetaDataKey key : result.get()) {
            if("TestType".equals(key.getId())) {
                MetaData md = getDispatcher().fetchMetaData(key).get();
                DefinedMapMetaDataModel model = (DefinedMapMetaDataModel) md.getPayload();
                assert(model.getKeys().size() > 0);
                break;
            }
        }
    }

}