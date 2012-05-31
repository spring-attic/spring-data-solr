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
package at.pagu.soldockr.core.query.result;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import at.pagu.soldockr.core.query.Field;

public class FacetPage<T> extends PageImpl<T> {

  private static final long serialVersionUID = 9024455741261109788L;

  private Map<String, Page<FacetEntry>> facetResultPages = new HashMap<String, Page<FacetEntry>>(1);

  public FacetPage(List<T> content) {
    super(content);
  }

  public FacetPage(List<T> content, Pageable pageable, long total) {
    super(content, pageable, total);
  }

  public Page<FacetEntry> getFacetResult(Field field) {
    Page<FacetEntry> page = facetResultPages.get(field.getName());
    return page != null ? page : new PageImpl<FacetEntry>(Collections.<FacetEntry> emptyList());
  }

  public void addFacetResultPage(Page<FacetEntry> page, Field field) {
    facetResultPages.put(field.getName(), page);
  }

  public void addAllFacetResultPages(Map<Field, Page<FacetEntry>> pageMap) {
    for (Map.Entry<Field, Page<FacetEntry>> entry : pageMap.entrySet()) {
      addFacetResultPage(entry.getValue(), entry.getKey());
    }
  }

}
