/*
 * Copyright 2012 - 2018 the original author or authors.
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
package org.springframework.data.solr.core;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.solr.core.query.SolrDataQuery;

/**
 * The QueryParser takes a spring-data-solr Query and returns a SolrQuery. All Query parameters are translated into the
 * according SolrQuery fields. <b>Example:</b> <code>
 *  Query query = new SimpleQuery(new Criteria("field_1").is("value_1").and("field_2").startsWith("value_2")).addProjection("field_3").setPageRequest(new PageRequest(0, 10));
 * </code> Will be parsed to a SolrQuery that outputs the following <code>
 *  q=field_1%3Avalue_1+AND+field_2%3Avalue_2*&fl=field_3&start=0&rows=10
 * </code>
 *
 * @author Christoph Strobl
 */
public interface QueryParser {

	/**
	 * Convert given Query into a SolrQuery executable via {@link SolrClient}
	 *
	 * @param query
	 * @return
	 */
	SolrQuery constructSolrQuery(SolrDataQuery query);

	/**
	 * Get the queryString to use withSolrQuery.setParam(CommonParams.Q, "queryString"}
	 *
	 * @param query
	 * @return String representation of query without faceting, pagination, projection...
	 */
	String getQueryString(SolrDataQuery query);

	/**
	 * Register an additional converter for transforming object values to solr readable format
	 *
	 * @param converter
	 */
	void registerConverter(Converter<?, ?> converter);

}
