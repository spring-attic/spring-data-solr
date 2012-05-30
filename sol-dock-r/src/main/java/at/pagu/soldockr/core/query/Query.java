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
package at.pagu.soldockr.core.query;

import java.util.List;

import org.springframework.data.domain.Pageable;

public interface Query extends FilterQuery {
  
  int DEFAULT_PAGE_SIZE = 10;
   
  <T extends Query> T addProjectionOnField(Field field);
  
  <T extends Query> T setPageRequest(Pageable pageable);
  
  <T extends Query> T addGroupByField(Field field);
  
  <T extends Query> T setFacetOptions(FacetOptions facetOptions);
  
  <T extends Query> T addFilterQuery(FilterQuery query);
  
  List<FilterQuery> getFilterQueries();
  
  Pageable getPageRequest();
  
  List<Field> getGroupByFields();
  
  List<Field> getProjectionOnFields();
  
  FacetOptions getFacetOptions();

}
