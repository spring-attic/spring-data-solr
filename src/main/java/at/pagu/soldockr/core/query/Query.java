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

/**
 * A Query that can be translated into a solr understandable Query. 
 *
 * @author Christoph Strobl
 */
public interface Query extends SolDockRQuery {
  
  int DEFAULT_PAGE_SIZE = 10;
   
  /**
   * add given Field to those included in result.
   * Corresponds to the 'fl' parameter in solr.
   * 
   * @param field
   * @return
   */
  <T extends Query> T addProjectionOnField(Field field);
  
  /**
   * restrict result to entries on given page.
   * Corresponds to the 'start' and 'rows' parameter in solr
   * 
   * @param pageable
   * @return
   */
  <T extends Query> T setPageRequest(Pageable pageable);
  
  /**
   * add the given field to those used for grouping result
   * Corresponds to '' in solr
   * 
   * @param field
   * @return
   */
  <T extends Query> T addGroupByField(Field field);
   
  /**
   * add query to filter results
   * Corresponds to 'fq' in solr
   * 
   * @param query
   * @return
   */
  <T extends Query> T addFilterQuery(FilterQuery query);
  
  /**
   * Get filter queries if defined
   * @return
   */
  List<FilterQuery> getFilterQueries();
  
  /**
   * Get page settings if defined
   * @return
   */
  Pageable getPageRequest();
  
  /**
   * Get group by fields if defined
   * @return
   */
  List<Field> getGroupByFields();
  
  /**
   * Get projection fields if defined
   * @return
   */
  List<Field> getProjectionOnFields();

}
