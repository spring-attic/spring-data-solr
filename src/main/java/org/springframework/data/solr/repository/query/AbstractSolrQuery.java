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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.query.Query;
import org.springframework.util.Assert;

/**
 * Base implementation of a solr specific {@link RepositoryQuery}
 * 
 * @author Christoph Strobl
 */
public abstract class AbstractSolrQuery implements RepositoryQuery {

	private final SolrOperations solrOperations;
	private final SolrQueryMethod solrQueryMethod;

	public AbstractSolrQuery(SolrOperations solrOperations, SolrQueryMethod solrQueryMethod) {
		Assert.notNull(solrOperations);
		Assert.notNull(solrQueryMethod);
		this.solrOperations = solrOperations;
		this.solrQueryMethod = solrQueryMethod;
	}

	@Override
	public Object execute(Object[] parameters) {
		SolrParameterAccessor accessor = new SolrParametersParameterAccessor(solrQueryMethod, parameters);

		Query query = createQuery(accessor);

		if (solrQueryMethod.isPageQuery()) {
			return new PagedExecution(accessor.getPageable()).execute(query);
		} else if (solrQueryMethod.isCollectionQuery()) {
			return new CollectionExecution(accessor.getPageable()).execute(query);
		}

		return new SingleEntityExecution().execute(query);
	}

	protected abstract Query createQuery(SolrParameterAccessor parameterAccessor);

	@Override
	public SolrQueryMethod getQueryMethod() {
		return this.solrQueryMethod;
	}

	private interface QueryExecution {
		Object execute(Query query);
	}

	abstract class AbstractQueryExecution implements QueryExecution {

		protected Page<?> executeFind(Query query) {
			SolrEntityInformation<?, ?> metadata = solrQueryMethod.getEntityInformation();
			return solrOperations.queryForPage(query, metadata.getJavaType());
		}

	}

	class CollectionExecution extends AbstractQueryExecution {
		private final Pageable pageable;

		public CollectionExecution(Pageable pageable) {
			this.pageable = pageable;
		}

		@Override
		public Object execute(Query query) {
			query.setPageRequest(pageable != null ? pageable : new PageRequest(0, Math.max(1, (int) count(query))));
			return executeFind(query).getContent();
		}

		private long count(Query query) {
			return solrOperations.count(query);
		}

	}

	class PagedExecution extends AbstractQueryExecution {
		private final Pageable pageable;

		public PagedExecution(Pageable pageable) {
			Assert.notNull(pageable);
			this.pageable = pageable;
		}

		@Override
		public Object execute(Query query) {
			query.setPageRequest(pageable);
			return executeFind(query);
		}
	}

	class SingleEntityExecution implements QueryExecution {

		@Override
		public Object execute(Query query) {
			SolrEntityInformation<?, ?> metadata = solrQueryMethod.getEntityInformation();
			return solrOperations.queryForObject(query, metadata.getJavaType());
		}
	}

}
