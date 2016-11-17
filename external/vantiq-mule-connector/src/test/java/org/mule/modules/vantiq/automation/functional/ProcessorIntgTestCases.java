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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mule.common.metadata.DefaultMetaDataField;
import org.mule.common.metadata.DefinedMapMetaDataModel;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataKey;
import org.mule.common.metadata.datatype.DataType;
import org.mule.modules.vantiq.VantiqConnector;
import org.mule.modules.vantiq.VantiqDataSenseResolver;
import org.mule.tools.devkit.ctf.junit.AbstractTestCase;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ProcessorIntgTestCases extends AbstractTestCase<VantiqConnector> {
    
    private static final Gson gson = new Gson();

    public ProcessorIntgTestCases() {
        super(VantiqConnector.class);
    }
    
    private void verifyFieldType(DefinedMapMetaDataModel model, String fieldName, DataType expectedType) {
        DefaultMetaDataField field = (DefaultMetaDataField) model.getFieldByName(fieldName);
        assertThat(fieldName + ": name", field.getMetaDataModel().getDataType(), is(expectedType));        
    }
    
    @Test
    public void verifyBuildMetaData() throws Exception {
        InputStream stream = this.getClass().getResourceAsStream("/TestType.json");
        JsonObject dataType = (JsonObject) gson.fromJson(new InputStreamReader(stream), JsonObject.class);
        MetaData metadata = VantiqDataSenseResolver.buildMetaData(dataType.get("name").getAsString(), dataType);

        DefinedMapMetaDataModel model = (DefinedMapMetaDataModel) metadata.getPayload();
        assertThat("Non-empty set of metadata properties", model.getKeys(), not(empty()));

        // Check valid fields
        verifyFieldType(model, "aString",   DataType.STRING);
        verifyFieldType(model, "aDateTime", DataType.DATE_TIME);
        verifyFieldType(model, "aInteger",  DataType.LONG);
        verifyFieldType(model, "aReal",     DataType.DOUBLE);
        verifyFieldType(model, "aDecimal",  DataType.DECIMAL);
        verifyFieldType(model, "aBoolean",  DataType.BOOLEAN);
        verifyFieldType(model, "aCurrency", DataType.STRING);
        verifyFieldType(model, "anObject",  DataType.MAP);
        verifyFieldType(model, "aGeoJSON",  DataType.MAP);
        
        // Check that system and unknown fields are handled
        assertThat("System field", model.getFieldByName("sysField"), is(nullValue()));
        assertThat("Unkonwn type", model.getFieldByName("aWrongType"), is(nullValue()));
    }
    
    @Test
    public void verifyGetSupportedActions() throws Exception {
        List<String> actions = getConnector().getSupportedActions();
        
        // We don't expect any actions, we just want to make sure there was
        // error querying for the list of supported actions
        assertThat("Got actions", actions, not(nullValue()));
    }
    
    @Test
    public void verifyPublishData() throws Exception {
        String dataType = "TestType";
        Map<String,Object> payload = new HashMap<String,Object>();
        payload.put("id", "abc");
        payload.put("x", "def");
        List<Map<String,Object>> payloadList = new ArrayList<Map<String,Object>>();
        payloadList.add(payload);

        // Note that failure would throw an exception
        getConnector().publishData(dataType, payloadList);
    }
    
    @Test
    public void verifyMetaDataKeys() throws Exception {
        List<MetaDataKey> result = getDispatcher().fetchMetaDataKeys().get();
        assertThat("At least one metadata type", result, not(empty()));
        
        for(MetaDataKey key : result) {
            if("TestType".equals(key.getId())) {
                MetaData md = getDispatcher().fetchMetaData(key).get();
                DefinedMapMetaDataModel model = (DefinedMapMetaDataModel) md.getPayload();
                assertThat("Non-empty set of metadata properties", model.getKeys(), not(empty()));
                break;
            }
        }
    }
    
    @Test
    public void verifyPublishTopic() throws Exception {
        Map<String,Object> payload = new HashMap<String,Object>();
        payload.put("id", "abc");
        payload.put("x", "def");

        // Note that failure would throw an exception
        getConnector().publishTopic("/test/topic", payload);
    }
    
}