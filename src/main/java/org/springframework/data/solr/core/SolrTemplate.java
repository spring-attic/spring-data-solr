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
package org.springframework.data.solr.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.core.convert.MappingSolrConverter;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.core.query.Update;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.server.SolrServerFactory;
import org.springframework.data.solr.server.support.HttpSolrServerFactory;
import org.springframework.util.Assert;

/**
 * Implementation of SolrOperations
 * 
 * @author Christoph Strobl
 */
public class SolrTemplate implements SolrOperations, InitializingBean, ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrTemplate.class);
	private static final QueryParser DEFAULT_QUERY_PARSER = new QueryParser();
	private static final PersistenceExceptionTranslator EXCEPTION_TRANSLATOR = new SolrExceptionTranslator();

	@SuppressWarnings("serial")
	private static final List<String> ITERABLE_CLASSES = new ArrayList<String>() {
		{
			add(List.class.getName());
			add(Collection.class.getName());
			add(Iterator.class.getName());
		}
	};

	private SolrServerFactory solrServerFactory;
	private QueryParser queryParser = DEFAULT_QUERY_PARSER;
	private final SolrConverter solrConverter;

	public SolrTemplate(SolrServer solrServer) {
		this(solrServer, null);
	}

	public SolrTemplate(SolrServer solrServer, String core) {
		this(new HttpSolrServerFactory(solrServer, core));
	}

	public SolrTemplate(SolrServerFactory solrServerFactory) {
		this(solrServerFactory, null);
	}

	public SolrTemplate(SolrServerFactory solrServerFactory, SolrConverter solrConverter) {
		Assert.notNull(solrServerFactory, "SolrServerFactory must not be 'null'.");
		Assert.notNull(solrServerFactory.getSolrServer(), "SolrServerFactory has to return a SolrServer.");

		this.solrServerFactory = solrServerFactory;
		this.solrConverter = solrConverter == null ? getDefaultSolrConverter(solrServerFactory) : solrConverter;
	}

	@Override
	public <T> T execute(SolrCallback<T> action) {
		Assert.notNull(action);

		try {
			SolrServer solrServer = this.getSolrServer();
			return action.doInSolr(solrServer);
		} catch (Exception e) {
			DataAccessException resolved = getExceptionTranslator().translateExceptionIfPossible(
					new RuntimeException(e.getMessage(), e));
			throw resolved == null ? new UncategorizedSolrException(e.getMessage(), e) : resolved;
		}
	}

	@Override
	public SolrPingResponse ping() {
		return execute(new SolrCallback<SolrPingResponse>() {
			@Override
			public SolrPingResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
				return solrServer.ping();
			}
		});
	}

	@Override
	public long count(final SolrDataQuery query) {
		Assert.notNull(query, "Query must not be 'null'.");

		return execute(new SolrCallback<Long>() {

			@Override
			public Long doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
				SolrQuery solrQuery = queryParser.constructSolrQuery(query);
				solrQuery.setStart(0);
				solrQuery.setRows(0);

				return solrServer.query(solrQuery).getResults().getNumFound();
			}
		});
	}

	@Override
	public UpdateResponse saveBean(final Object objectToAdd) {
		assertNoCollection(objectToAdd);
		return execute(new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
				return solrServer.add(convertBeanToSolrInputDocument(objectToAdd));
			}
		});
	}

	@Override
	public UpdateResponse saveBeans(final Collection<?> beansToAdd) {
		return execute(new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
				return solrServer.add(convertBeansToSolrInputDocuments(beansToAdd));
			}
		});
	}

	@Override
	public UpdateResponse saveDocument(final SolrInputDocument documentToAdd) {
		return execute(new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
				return solrServer.add(documentToAdd);
			}
		});
	}

	@Override
	public UpdateResponse saveDocuments(final Collection<SolrInputDocument> documentsToAdd) {
		return execute(new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
				return solrServer.add(documentsToAdd);
			}
		});
	}
        
        @Override
        public UpdateResponse updatePartialDocument(Update update) {
            final SolrInputDocument solrInputDocument = update.getSolrInputDocument();
            Assert.notNull(solrInputDocument, "Query must not be 'null'.");
            return execute(new SolrCallback<UpdateResponse>() {
                @Override
                public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
                    return solrServer.add(solrInputDocument);
                }
            });
        }

	@Override
	public UpdateResponse delete(SolrDataQuery query) {
		Assert.notNull(query, "Query must not be 'null'.");

		final String queryString = this.queryParser.getQueryString(query);

		return execute(new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
				return solrServer.deleteByQuery(queryString);
			}
		});
	}

	@Override
	public UpdateResponse deleteById(final String id) {
		Assert.notNull(id, "Cannot delete 'null' id.");

		return execute(new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
				return solrServer.deleteById(id);
			}
		});
	}

	@Override
	public UpdateResponse deleteById(Collection<String> ids) {
		Assert.notNull(ids, "Cannot delete 'null' collection.");

		final List<String> toBeDeleted = new ArrayList<String>(ids);
		return execute(new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
				return solrServer.deleteById(toBeDeleted);
			}
		});
	}

	@Override
	public <T> T queryForObject(Query query, Class<T> clazz) {
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		query.setPageRequest(new PageRequest(0, 1));
		QueryResponse response = query(query);

		if (response.getResults().size() > 0) {
			if (response.getResults().size() > 1) {
				LOGGER.warn("More than 1 result found for singe result query ('{}'), returning first entry in list");
			}
			return response.getBeans(clazz).get(0);
		}
		return null;
	}

	public <T> Page<T> queryForPage(Query query, Class<T> clazz) {
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		QueryResponse response = query(query);
		return new PageImpl<T>(response.getBeans(clazz), query.getPageRequest(), response.getResults().getNumFound());
	}

	@Override
	public <T> FacetPage<T> queryForFacetPage(FacetQuery query, Class<T> clazz) {
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		QueryResponse response = query(query);

		FacetPage<T> page = new FacetPage<T>(response.getBeans(clazz), query.getPageRequest(), response.getResults()
				.getNumFound());
		page.addAllFacetResultPages(ResultHelper.convertFacetQueryResponseToFacetPageMap(query, response));

		return page;
	}

	final QueryResponse query(SolrDataQuery query) {
		Assert.notNull(query, "Query must not be 'null'");

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		LOGGER.debug("Executing query '" + solrQuery + "' against solr.");

		return executeSolrQuery(solrQuery);
	}

	final QueryResponse executeSolrQuery(final SolrQuery solrQuery) {
		return execute(new SolrCallback<QueryResponse>() {
			@Override
			public QueryResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
				return solrServer.query(solrQuery);
			}
		});
	}

	@Override
	public void commit() {
		execute(new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
				return solrServer.commit();
			}
		});
	}

	@Override
	public void rollback() {
		execute(new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
				return solrServer.rollback();
			}
		});
	}

	@Override
	public SolrInputDocument convertBeanToSolrInputDocument(Object bean) {
		if (bean instanceof SolrInputDocument) {
			return (SolrInputDocument) bean;
		}

		SolrInputDocument document = new SolrInputDocument();
		getConverter().write(bean, document);
		return document;
	}

	private Collection<SolrInputDocument> convertBeansToSolrInputDocuments(Iterable<?> beans) {
		if (beans == null) {
			return Collections.emptyList();
		}
		List<SolrInputDocument> resultList = new ArrayList<SolrInputDocument>();
		for (Object bean : beans) {
			resultList.add(convertBeanToSolrInputDocument(bean));
		}
		return resultList;
	}

	protected void assertNoCollection(Object o) {
		if (null != o && (o.getClass().isArray() || ITERABLE_CLASSES.contains(o.getClass().getName()))) {
			throw new IllegalArgumentException("Collections are not supported for this operation");
		}
	}

	private final SolrConverter getDefaultSolrConverter(SolrServerFactory factory) {
		MappingSolrConverter converter = new MappingSolrConverter(factory, new SimpleSolrMappingContext());
		converter.afterPropertiesSet(); // have to call this one to initialize default converters
		return converter;
	}

	@Override
	public final SolrServer getSolrServer() {
		return solrServerFactory.getSolrServer();
	}

	@Override
	public SolrConverter getConverter() {
		return this.solrConverter;
	}

	public static PersistenceExceptionTranslator getExceptionTranslator() {
		return EXCEPTION_TRANSLATOR;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		// future use
	}

	@Override
	public void afterPropertiesSet() {
		if (this.queryParser == null) {
			LOGGER.warn("QueryParser not set, using default one.");
			queryParser = DEFAULT_QUERY_PARSER;
		}
	}



}
