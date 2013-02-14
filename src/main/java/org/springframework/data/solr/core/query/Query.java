/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.solr.core.query;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * A Query that can be translated into a solr understandable Query.
 * 
 * @author Christoph Strobl
 */
public interface Query extends SolrDataQuery {

	int DEFAULT_PAGE_SIZE = 10;

	/**
	 * add given Field to those included in result. Corresponds to the 'fl' parameter in solr.
	 * 
	 * @param field
	 * @return
	 */
	<T extends Query> T addProjectionOnField(Field field);

	/**
	 * restrict result to entries on given page. Corresponds to the 'start' and 'rows' parameter in solr
	 * 
	 * @param pageable
	 * @return
	 */
	<T extends Query> T setPageRequest(Pageable pageable);

	/**
	 * add the given field to those used for grouping result Corresponds to '' in solr
	 * 
	 * @param field
	 * @return
	 */
	<T extends Query> T addGroupByField(Field field);

	/**
	 * add query to filter results Corresponds to 'fq' in solr
	 * 
	 * @param query
	 * @return
	 */
	<T extends Query> T addFilterQuery(FilterQuery query);

	/**
	 * Get filter queries if defined
	 * 
	 * @return
	 */
	List<FilterQuery> getFilterQueries();

	/**
	 * Get page settings if defined
	 * 
	 * @return
	 */
	Pageable getPageRequest();

	/**
	 * Get group by fields if defined
	 * 
	 * @return
	 */
	List<Field> getGroupByFields();

	/**
	 * Get projection fields if defined
	 * 
	 * @return
	 */
	List<Field> getProjectionOnFields();

	/**
	 * Add {@link Sort} to query
	 * 
	 * @param sort
	 * @return
	 */
	<T extends Query> T addSort(Sort sort);

	/**
	 * @return null if not set
	 */
	Sort getSort();

	/**
	 * Set the default operator {@code q.op} for query expressions
	 * 
	 * @return
	 */
	void setDefaultOperator(Operator operator);

	/**
	 * Get the specified default operator for query expressions, overriding the default operator specified in the
	 * schema.xml file.
	 * 
	 * @return
	 */
	Operator getDefaultOperator();

	/**
	 * Operator to be used for {@code q.op}
	 */
	enum Operator {
		AND("AND"), OR("OR"), NONE("");

		private String operator;

		private Operator(String operator) {
			this.operator = operator;
		}

		public String asQueryStringRepresentation() {
			return this.operator;
		}

		@Override
		public String toString() {
			return asQueryStringRepresentation();
		}
	}
	
    boolean isQuoteVaule();
    
    void setQuoteValues(boolean quoteValues);


}
