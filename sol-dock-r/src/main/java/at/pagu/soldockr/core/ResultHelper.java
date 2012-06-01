/*
 * Copyright (C) 2012 sol-dock-r authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.pagu.soldockr.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.data.domain.Page;
import org.springframework.util.Assert;

import at.pagu.soldockr.core.query.FacetQuery;
import at.pagu.soldockr.core.query.Field;
import at.pagu.soldockr.core.query.SimpleField;
import at.pagu.soldockr.core.query.result.FacetEntry;
import at.pagu.soldockr.core.query.result.FacetPage;
import at.pagu.soldockr.core.query.result.SimpleFacetEntry;

final class ResultHelper {

  private ResultHelper() {}

  static Map<Field, Page<FacetEntry>> convertFacetQueryResponseToFacetPageMap(FacetQuery query, QueryResponse response) {
    Assert.notNull(query, "Cannot convert response for 'null', query");
    
    if (!query.hasFacetOptions() || response == null) {
      return Collections.emptyMap();
    }
    Map<Field, Page<FacetEntry>> facetResult = new HashMap<Field, Page<FacetEntry>>();

    if (CollectionUtils.isNotEmpty(response.getFacetFields())) {
      int initalPageSize = query.getFacetOptions().getPageable().getPageSize();
      for (FacetField facetField : response.getFacetFields()) {
        if (facetField != null && StringUtils.isNotBlank(facetField.getName())) {
          Field field = new SimpleField(facetField.getName());
          if (CollectionUtils.isNotEmpty(facetField.getValues())) {
            List<FacetEntry> pageEntries = new ArrayList<FacetEntry>(initalPageSize);
            for (Count count : facetField.getValues()) {
              if (count != null) {
                pageEntries.add(new SimpleFacetEntry(field, count.getName(), count.getCount()));
              }
            }
            facetResult.put(field,
                new FacetPage<FacetEntry>(pageEntries, query.getFacetOptions().getPageable(), facetField.getValueCount()));
          } else {
            facetResult.put(field,
                new FacetPage<FacetEntry>(Collections.<FacetEntry> emptyList(), query.getFacetOptions().getPageable(), 0));
          }
        }
      }
    }
    return facetResult;
  }

}
