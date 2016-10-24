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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.mule.api.annotations.MetaDataKeyRetriever;
import org.mule.api.annotations.MetaDataRetriever;
import org.mule.api.annotations.components.MetaDataCategory;
import org.mule.common.metadata.DefaultMetaData;
import org.mule.common.metadata.DefaultMetaDataKey;
import org.mule.common.metadata.DefinedMapMetaDataModel;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataKey;
import org.mule.common.metadata.builder.DefaultMetaDataBuilder;
import org.mule.common.metadata.builder.DynamicObjectBuilder;
import org.mule.common.metadata.datatype.DataType;
import org.mule.modules.vantiq.error.VantiqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.vantiq.client.Vantiq;
import io.vantiq.client.VantiqResponse;

/**
 * DataSense resolver that pulls data type information from a Vantiq system.
 * 
 *  @author Vantiq
 */
@MetaDataCategory
public class VantiqDataSenseResolver {

    private static Logger log = LoggerFactory.getLogger(VantiqDataSenseResolver.class);
    
    @Inject
    private VantiqConnector connector;

    private Vantiq vantiq;
    
    /**
     * Retrieves the list of keys based on the Vantiq data types
     * 
     * @return The list of data types from Vantiq
     * @throws Exception If anything fails
     */
    @SuppressWarnings("unchecked")
    @MetaDataKeyRetriever
    public List<MetaDataKey> getMetaDataKeys() throws Exception {
        List<String> props = new ArrayList<String>();
        props.add("name");
        VantiqResponse response = vantiq.select(Vantiq.SystemResources.TYPES.value(), 
                                                props, null, null);
        if(!response.isSuccess()) {
            throw new VantiqException(response);
        }
        List<MetaDataKey> keys = new ArrayList<MetaDataKey>();

        for(JsonObject entry : (List<JsonObject>) response.getBody()) {
            String typeName = ((JsonObject) entry).get("name").getAsString();
            keys.add(new DefaultMetaDataKey(typeName, typeName));
        }

        return keys;        
    }

    /**
     * Get metadata for the given data type from Vantiq
     * 
     * @param key The key selected from the list of valid keys
     * @return The MetaData model of that corresponds to the key
     * @throws Exception If anything fails
     */
    @MetaDataRetriever
    public MetaData getMetaData(MetaDataKey key) throws Exception {
        VantiqResponse response = vantiq.selectOne(Vantiq.SystemResources.TYPES.value(), key.getId());
        if(!response.isSuccess()) {
            log.error("ERROR", response);
            throw new VantiqException(response);
        }
        
        DynamicObjectBuilder<?> doDef = new DefaultMetaDataBuilder().createDynamicObject(key.getId());
        JsonObject obj = (JsonObject) response.getBody();
        JsonObject properties = obj.getAsJsonObject("properties");
        for(Map.Entry<String, JsonElement> entry : properties.entrySet()) {
            String        prop = entry.getKey();
            JsonObject propDef = entry.getValue().getAsJsonObject(); 
            String        type = propDef.get("type").getAsString(); 

            // Skip system fields
            if(propDef.get("system") != null && propDef.get("system").getAsBoolean()) continue;
            
            switch(type) {
            case "String":
                doDef.addSimpleField(prop, DataType.STRING);
                break;
            case "DateTime":
                doDef.addSimpleField(prop, DataType.DATE_TIME);
                break;
            case "Boolean":
                doDef.addSimpleField(prop, DataType.BOOLEAN);
                break;
            case "Real":
                doDef.addSimpleField(prop, DataType.DOUBLE);
                break;
            case "Integer":
                doDef.addSimpleField(prop, DataType.LONG);
                break;
            case "Decimal":
                doDef.addSimpleField(prop, DataType.DECIMAL);
                break;
            case "Currency":
                doDef.addSimpleField(prop, DataType.STRING);
                break;
            case "Object":
            case "GeoJSON":
                doDef.addDynamicObjectField(prop).endDynamicObject();
                break;
            default:
                // Skip unknown attributes
                log.warn("Unknown data type: " + prop + " in " + key.getDisplayName());    
            }
        }
        
        DefinedMapMetaDataModel model = doDef.build();
        MetaData metaData = new DefaultMetaData(model);

        return metaData;
    }

    public VantiqConnector getConnector() {
        return connector;
    }

    public void setConnector(VantiqConnector connector) {
        this.connector = connector;
        this.vantiq = this.connector.getConnectionManagement().getVantiq();
    }
}
