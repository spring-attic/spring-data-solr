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

/**
 * Common interface for any Query
 * 
 * @author Christoph Strobl
 */
public interface SolrDataQuery {

	/**
	 * Append criteria to query. Criteria must not be null, nor point to a field with null value.
	 * 
	 * @param criteria
	 * @return
	 */
	<T extends SolrDataQuery> T addCriteria(Criteria criteria);

	/**
	 * 
	 * @return
	 */
	Criteria getCriteria();

	/**
	 * Specifies the default operator for query expressions, overriding the default operator specified in the schema.xml
	 * file.
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

}
