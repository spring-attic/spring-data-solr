/*
 * Copyright 2012 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.query;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;

/**
 *
 * @author Nihed
 */
public class Update {
    public static final String ACTION = "set";

    private String idFieldName;
    private Object idFieldValue;
    private Map<String, Object> updates = new LinkedHashMap<String, Object>();

    public Update(String idFieldName, Object idFieldValue) {
        this.idFieldName=idFieldName;
        this.idFieldValue=idFieldValue;
        
        
    }
    
    public void put(String key, Object value){
        updates.put(key, value);
    }
    
    public SolrInputDocument getSolrInputDocument(){
        if(updates.size()==0){
            return null;
        }
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(idFieldName, idFieldValue);
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            HashMap<String,Object> mapValue= new HashMap<String, Object>();
            mapValue.put(ACTION, value);
            solrInputDocument.addField(key, mapValue);
        }
        
        return solrInputDocument;
    }
}
