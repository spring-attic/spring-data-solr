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
package org.springframework.data.solr.repository.query;

import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;

/**
 * Solr specific implementation of {@link RepositoryQuery} that can handle string based queries
 * 
 * @author Christoph Strobl
 */
public class StringBasedSolrQuery extends AbstractSolrQuery {

	private final String rawQueryString;

	public StringBasedSolrQuery(SolrQueryMethod method, SolrOperations solrOperations) {
		this(method.getAnnotatedQuery(), method, solrOperations);
	}

	public StringBasedSolrQuery(String query, SolrQueryMethod queryMethod, SolrOperations solrOperations) {
		super(solrOperations, queryMethod);
		this.rawQueryString = query;
	}

	@Override
	protected Query createQuery(SolrParameterAccessor parameterAccessor) {
		SimpleQuery query = createQueryFromString(this.rawQueryString, parameterAccessor);
		appendProjection(query);
		query.addSort(parameterAccessor.getSort());
		return query;
	}

}
