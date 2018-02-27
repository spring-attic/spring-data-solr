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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.core.QueryParserBase.NamedObjectsFacetAndHighlightQuery;
import org.springframework.data.solr.core.QueryParserBase.NamedObjectsFacetQuery;
import org.springframework.data.solr.core.QueryParserBase.NamedObjectsHighlightQuery;
import org.springframework.data.solr.core.QueryParserBase.NamedObjectsQuery;
import org.springframework.data.solr.core.convert.MappingSolrConverter;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.query.AbstractQueryDecorator;
import org.springframework.data.solr.core.query.FacetAndHighlightQuery;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.core.query.TermsQuery;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.data.solr.core.query.result.DelegatingCursor;
import org.springframework.data.solr.core.query.result.FacetAndHighlightPage;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.core.query.result.ScoredPage;
import org.springframework.data.solr.core.query.result.SolrResultPage;
import org.springframework.data.solr.core.query.result.SpellcheckQueryResult.Alternative;
import org.springframework.data.solr.core.query.result.StatsPage;
import org.springframework.data.solr.core.query.result.TermsPage;
import org.springframework.data.solr.core.query.result.TermsResultPage;
import org.springframework.data.solr.core.schema.DefaultSchemaOperations;
import org.springframework.data.solr.core.schema.SchemaOperations;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator.Feature;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.data.solr.server.support.HttpSolrClientFactory;
import org.springframework.data.solr.server.support.MulticoreSolrClientFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link SolrOperations}
 *
 * @author Christoph Strobl
 * @author Joachim Uhrlass
 * @author Francisco Spaeth
 * @author Shiradwade Sateesh Krishna
 * @author David Webb
 * @author Petar Tahchiev
 * @author Juan Manuel de Blas
 */
public class SolrTemplate implements SolrOperations, InitializingBean, ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrTemplate.class);
	private static final PersistenceExceptionTranslator EXCEPTION_TRANSLATOR = new SolrExceptionTranslator();
	private final QueryParsers queryParsers = new QueryParsers();
	private MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext;

	private ApplicationContext applicationContext;
	private String solrCore;
	private final RequestMethod defaultRequestMethod;

	@SuppressWarnings("serial") //
	private static final List<String> ITERABLE_CLASSES = new ArrayList<String>() {
		{
			add(List.class.getName());
			add(Collection.class.getName());
			add(Iterator.class.getName());
		}
	};

	private SolrClientFactory solrClientFactory;

	private SolrConverter solrConverter;

	private Set<Feature> schemaCreationFeatures;

	public SolrTemplate(SolrClient solrClient) {
		this(solrClient, null);
	}

	public SolrTemplate(SolrClient solrClient, String core) {
		this(new HttpSolrClientFactory(solrClient, core));
		this.solrCore = core;
	}

	public SolrTemplate(SolrClient solrClient, String core, RequestMethod requestMethod) {
		this(new HttpSolrClientFactory(solrClient, core), requestMethod);
		this.solrCore = core;

	}

	public SolrTemplate(SolrClientFactory solrClientFactory) {
		this(solrClientFactory, (SolrConverter) null);
	}

	/**
	 * @param solrClientFactory must not be {@literal null}.
	 * @param defaultCore can be {@literal null}.
	 * @since 2.1
	 */
	public SolrTemplate(SolrClientFactory solrClientFactory, String defaultCore) {
		this(solrClientFactory, (SolrConverter) null);
		this.solrCore = defaultCore;
	}

	public SolrTemplate(SolrClientFactory solrClientFactory, RequestMethod requestMethod) {
		this(solrClientFactory, null, requestMethod);
	}

	public SolrTemplate(SolrClientFactory solrClientFactory, SolrConverter solrConverter) {
		this(solrClientFactory, solrConverter, RequestMethod.GET);
	}

	/**
	 * @param solrClientFactory must not be {@literal null}.
	 * @param solrConverter must not be {@literal null}.
	 * @param defaultRequestMethod can be {@literal null}. Will be defaulted to {@link RequestMethod#GET}
	 * @since 2.0
	 */
	public SolrTemplate(SolrClientFactory solrClientFactory, SolrConverter solrConverter,
			RequestMethod defaultRequestMethod) {

		Assert.notNull(solrClientFactory, "SolrClientFactory must not be 'null'.");

		this.solrClientFactory = solrClientFactory;
		this.defaultRequestMethod = defaultRequestMethod != null ? defaultRequestMethod : RequestMethod.GET;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#execute(org.springframework.data.solr.core.SolrCallback)
	 */
	@Override
	public <T> T execute(SolrCallback<T> action) {
		Assert.notNull(action, "SolrCallback must not be null!");

		try {
			SolrClient solrClient = this.getSolrClient();
			return action.doInSolr(solrClient);
		} catch (Exception e) {
			DataAccessException resolved = getExceptionTranslator()
					.translateExceptionIfPossible(new RuntimeException(e.getMessage(), e));
			throw resolved == null ? new UncategorizedSolrException(e.getMessage(), e) : resolved;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#execute(java.lang.String, org.springframework.data.solr.core.CollectionCallback)
	 */
	@Override
	public <T> T execute(String collection, CollectionCallback<T> action) {

		Assert.notNull(action, "Action must not be null!");

		try {

			SolrClient solrClient = null;
			if(StringUtils.hasText(collection)) {
				if(this.solrClientFactory instanceof MulticoreSolrClientFactory) {
					solrClient = this.solrClientFactory.getSolrClient();
				} else {
					solrClient = this.solrClientFactory.getSolrClient(collection);
				}
			} else {
				solrClient = this.getSolrClient();
			}
			return action.doInSolr(solrClient, collection);
		} catch (Exception e) {
			DataAccessException resolved = getExceptionTranslator().translateExceptionIfPossible(
					e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e.getMessage(), e));
			throw resolved == null ? new UncategorizedSolrException(e.getMessage(), e) : resolved;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#ping()
	 */
	@Override
	public SolrPingResponse ping() {
		return execute(new SolrCallback<SolrPingResponse>() {
			@Override
			public SolrPingResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.ping();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#count(org.springframework.data.solr.core.query.SolrDataQuery)
	 */
	@Override
	public long count(SolrDataQuery query) {
		return count(query, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#count(java.lang.String, org.springframework.data.solr.core.query.SolrDataQuery)
	 */
	public long count(String collectionName, SolrDataQuery query) {
		return count(collectionName, query, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#count(org.springframework.data.solr.core.query.SolrDataQuery, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public long count(final SolrDataQuery query, final RequestMethod method) {
		return count(null, query, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#count(java.lang.String, org.springframework.data.solr.core.query.SolrDataQuery, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public long count(String collectionName, final SolrDataQuery query, final RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(method, "Method must not be 'null'.");

		return execute(collectionName, new CollectionCallback<Long>() {

			@Override
			public Long doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {

				SolrQuery solrQuery = queryParsers.getForClass(query.getClass()).constructSolrQuery(query);
				solrQuery.setStart(0);
				solrQuery.setRows(0);

				return StringUtils.hasText(collection)
						? solrClient.query(collection, solrQuery, getSolrRequestMethod(method)).getResults().getNumFound()
						: solrClient.query(solrQuery, getSolrRequestMethod(method)).getResults().getNumFound();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveBean(java.lang.Object)
	 */
	@Override
	public UpdateResponse saveBean(Object obj) {
		return saveBean(obj, -1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveBean(java.lang.String, java.lang.Object)
	 */
	@Override
	public UpdateResponse saveBean(String collectionName, Object obj) {
		return saveBean(collectionName, obj, -1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveBean(java.lang.Object, int)
	 */
	@Override
	public UpdateResponse saveBean(final Object objectToAdd, final int commitWithinMs) {
		return saveBean(getSolrCoreOrBeanCollection(ClassUtils.getUserClass(objectToAdd)), objectToAdd, commitWithinMs);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveBean(java.lang.String, java.lang.Object, int)
	 */
	@Override
	public UpdateResponse saveBean(String collectionName, final Object objectToAdd, final int commitWithinMs) {

		assertNoCollection(objectToAdd);

		return execute(collectionName, new CollectionCallback<UpdateResponse>() {

			@Override
			public UpdateResponse doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return StringUtils.hasText(collection)
						? solrClient.add(collection, convertBeanToSolrInputDocument(objectToAdd), commitWithinMs)
						: solrClient.add(convertBeanToSolrInputDocument(objectToAdd), commitWithinMs);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveBeans(java.util.Collection)
	 */
	@Override
	public UpdateResponse saveBeans(Collection<?> beans) {
		return saveBeans(beans, -1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveBeans(java.lang.String, java.util.Collection)
	 */
	@Override
	public UpdateResponse saveBeans(String collectionName, Collection<?> beans) {
		return saveBeans(collectionName, beans, -1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveBeans(java.util.Collection, int)
	 */
	@Override
	public UpdateResponse saveBeans(final Collection<?> beansToAdd, final int commitWithinMs) {
		return saveBeans(getSolrCoreOrBeanCollection(ClassUtils.getUserClass(beansToAdd.iterator().next())), beansToAdd,
				commitWithinMs);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveBeans(java.lang.String, java.util.Collection, int)
	 */
	@Override
	public UpdateResponse saveBeans(String collectionName, final Collection<?> beansToAdd, final int commitWithinMs) {

		return execute(collectionName, new CollectionCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return StringUtils.hasText(collection)
						? solrClient.add(collection, convertBeansToSolrInputDocuments(beansToAdd), commitWithinMs)
						: solrClient.add(convertBeansToSolrInputDocuments(beansToAdd), commitWithinMs);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveDocument(org.apache.solr.common.SolrInputDocument)
	 */
	@Override
	public UpdateResponse saveDocument(SolrInputDocument document) {
		return saveDocument(document, -1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveDocument(java.lang.String, org.apache.solr.common.SolrInputDocument)
	 */
	@Override
	public UpdateResponse saveDocument(String collectionName, SolrInputDocument document) {
		return saveDocument(collectionName, document, -1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveDocument(org.apache.solr.common.SolrInputDocument, int)
	 */
	@Override
	public UpdateResponse saveDocument(final SolrInputDocument documentToAdd, final int commitWithinMs) {
		return saveDocument(null, documentToAdd, commitWithinMs);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveDocument(java.lang.String, org.apache.solr.common.SolrInputDocument, int)
	 */
	@Override
	public UpdateResponse saveDocument(String collectionName, final SolrInputDocument documentToAdd,
			final int commitWithinMs) {

		return execute(collectionName, new CollectionCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return StringUtils.hasText(collection) ? solrClient.add(collection, documentToAdd, commitWithinMs)
						: solrClient.add(documentToAdd, commitWithinMs);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveDocuments(java.util.Collection)
	 */
	@Override
	public UpdateResponse saveDocuments(Collection<SolrInputDocument> documents) {
		return saveDocuments(documents, -1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveDocuments(java.lang.String, java.util.Collection)
	 */
	@Override
	public UpdateResponse saveDocuments(String collectionName, Collection<SolrInputDocument> documents) {
		return saveDocuments(collectionName, documents, -1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveDocuments(java.util.Collection, int)
	 */
	@Override
	public UpdateResponse saveDocuments(final Collection<SolrInputDocument> documentsToAdd, final int commitWithinMs) {
		return execute(new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.add(documentsToAdd, commitWithinMs);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveDocuments(java.lang.String, java.util.Collection, int)
	 */
	@Override
	public UpdateResponse saveDocuments(String collectionName, final Collection<SolrInputDocument> documentsToAdd,
			final int commitWithinMs) {

		return execute(collectionName, new CollectionCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return StringUtils.hasText(collection) ? solrClient.add(collection, documentsToAdd, commitWithinMs)
						: solrClient.add(documentsToAdd, commitWithinMs);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#delete(org.springframework.data.solr.core.query.SolrDataQuery)
	 */
	@Override
	public UpdateResponse delete(SolrDataQuery query) {
		return delete(null, query);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#delete(java.lang.String, org.springframework.data.solr.core.query.SolrDataQuery)
	 */
	@Override
	public UpdateResponse delete(String collectionName, SolrDataQuery query) {

		Assert.notNull(query, "Query must not be 'null'.");

		final String queryString = this.queryParsers.getForClass(query.getClass()).getQueryString(query);

		return execute(collectionName, new CollectionCallback<UpdateResponse>() {

			@Override
			public UpdateResponse doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return StringUtils.hasText(collection) ? solrClient.deleteByQuery(collection, queryString)
						: solrClient.deleteByQuery(queryString);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#deleteById(java.lang.String)
	 */
	@Override
	public UpdateResponse deleteById(final String id) {
		return deleteById(null, id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#deleteById(java.lang.String, java.lang.String)
	 */
	@Override
	public UpdateResponse deleteById(String collectionName, final String id) {

		Assert.notNull(id, "Cannot delete 'null' id.");

		return execute(collectionName, new CollectionCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return StringUtils.hasText(collection) ? solrClient.deleteById(collection, id) : solrClient.deleteById(id);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#deleteById(java.util.Collection)
	 */
	@Override
	public UpdateResponse deleteById(Collection<String> ids) {
		return deleteById(null, ids);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#deleteById(java.lang.String, java.util.Collection)
	 */
	@Override
	public UpdateResponse deleteById(String collectionName, Collection<String> ids) {

		Assert.notNull(ids, "Cannot delete 'null' collection.");

		final List<String> toBeDeleted = new ArrayList<String>(ids);

		return execute(collectionName, new CollectionCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return StringUtils.hasText(collection) ? solrClient.deleteById(collection, toBeDeleted)
						: solrClient.deleteById(toBeDeleted);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForObject(org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	@Override
	public <T> T queryForObject(Query query, Class<T> clazz) {
		return queryForObject(query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForObject(java.lang.String, org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	public <T> T queryForObject(String collectionName, Query query, Class<T> clazz) {
		return queryForObject(collectionName, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForObject(org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> T queryForObject(Query query, Class<T> clazz, RequestMethod method) {
		return queryForObject(getSolrCoreOrBeanCollection(clazz), query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForObject(java.lang.String, org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	public <T> T queryForObject(String collectionName, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		query.setPageRequest(new PageRequest(0, 1));
		QueryResponse response = querySolr(collectionName, query, clazz, method);

		if (response.getResults().size() > 0) {
			if (response.getResults().size() > 1) {
				LOGGER.warn("More than 1 result found for singe result query ('{}'), returning first entry in list", query);
			}
			return convertSolrDocumentListToBeans(response.getResults(), clazz).get(0);
		}
		return null;

	}

	private <T> SolrResultPage<T> doQueryForPage(String collectionName, Query query, Class<T> clazz,
			RequestMethod requestMethod) {

		QueryResponse response = null;
		NamedObjectsQuery namedObjectsQuery = new NamedObjectsQuery(query);
		response = querySolr(collectionName, namedObjectsQuery, clazz,
				requestMethod != null ? requestMethod : getDefaultRequestMethod());
		Map<String, Object> objectsName = namedObjectsQuery.getNamesAssociation();

		return createSolrResultPage(query, clazz, response, objectsName);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForPage(org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	@Override
	public <T> ScoredPage<T> queryForPage(Query query, Class<T> clazz) {
		return queryForPage(getSolrCoreOrBeanCollection(clazz), query, clazz);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForPage(java.lang.String, org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	public <T> ScoredPage<T> queryForPage(String collectionName, Query query, Class<T> clazz) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		return doQueryForPage(collectionName, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryFor(org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	@Override
	public <T, S extends Page<T>> S query(Query query, Class<T> clazz) {
		return query(query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T, S extends Page<T>> S query(String collectionName, Query query, Class<T> clazz) {
		return query(collectionName, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#query(org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T, S extends Page<T>> S query(Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(clazz, "Method must not be 'null'.");

		return (S) doQueryForPage(getSolrCoreOrBeanCollection(clazz), query, clazz, method);
	}

	public <T, S extends Page<T>> S query(String collectionName, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(clazz, "Method must not be 'null'.");

		return (S) doQueryForPage(collectionName, query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForPage(org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> ScoredPage<T> queryForPage(Query query, Class<T> clazz, RequestMethod method) {
		return queryForPage(getSolrCoreOrBeanCollection(clazz), query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForPage(java.lang.String, org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	public <T> ScoredPage<T> queryForPage(String collectionName, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");

		return doQueryForPage(collectionName, query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForGroupPage(org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	@Override
	public <T> GroupPage<T> queryForGroupPage(Query query, Class<T> clazz) {
		return queryForGroupPage(query, clazz, RequestMethod.GET);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForGroupPage(java.lang.String, org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	public <T> GroupPage<T> queryForGroupPage(String collectionName, Query query, Class<T> clazz) {
		return queryForGroupPage(collectionName, query, clazz, RequestMethod.GET);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForGroupPage(org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> GroupPage<T> queryForGroupPage(Query query, Class<T> clazz, RequestMethod method) {
		return queryForGroupPage(getSolrCoreOrBeanCollection(clazz), query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForGroupPage(java.lang.String, org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	public <T> GroupPage<T> queryForGroupPage(String collectionName, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");

		return doQueryForPage(collectionName, query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForStatsPage(org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	@Override
	public <T> StatsPage<T> queryForStatsPage(Query query, Class<T> clazz) {
		return queryForStatsPage(query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForStatsPage(java.lang.String, org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	public <T> StatsPage<T> queryForStatsPage(String collectionName, Query query, Class<T> clazz) {
		return queryForStatsPage(collectionName, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForStatsPage(org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> StatsPage<T> queryForStatsPage(Query query, Class<T> clazz, RequestMethod method) {
		return queryForStatsPage(getSolrCoreOrBeanCollection(clazz), query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForStatsPage(java.lang.String, org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	public <T> StatsPage<T> queryForStatsPage(String collectionName, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");

		return doQueryForPage(collectionName, query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForFacetPage(org.springframework.data.solr.core.query.FacetQuery, java.lang.Class)
	 */
	@Override
	public <T> FacetPage<T> queryForFacetPage(FacetQuery query, Class<T> clazz) {
		return queryForFacetPage(query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForFacetPage(java.lang.String, org.springframework.data.solr.core.query.FacetQuery, java.lang.Class)
	 */
	public <T> FacetPage<T> queryForFacetPage(String collectionName, FacetQuery query, Class<T> clazz) {
		return queryForFacetPage(collectionName, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForFacetPage(org.springframework.data.solr.core.query.FacetQuery, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> FacetPage<T> queryForFacetPage(FacetQuery query, Class<T> clazz, RequestMethod method) {
		return queryForFacetPage(getSolrCoreOrBeanCollection(clazz), query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForFacetPage(java.lang.String, org.springframework.data.solr.core.query.FacetQuery, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	public <T> FacetPage<T> queryForFacetPage(String collectionName, FacetQuery query, Class<T> clazz,
			RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		NamedObjectsFacetQuery namedObjectsQuery = new NamedObjectsFacetQuery(query);

		return createSolrResultPage(query, clazz, querySolr(collectionName, namedObjectsQuery, clazz, method),
				namedObjectsQuery.getNamesAssociation());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForHighlightPage(org.springframework.data.solr.core.query.HighlightQuery, java.lang.Class)
	 */
	@Override
	public <T> HighlightPage<T> queryForHighlightPage(HighlightQuery query, Class<T> clazz) {
		return queryForHighlightPage(query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForHighlightPage(java.lang.String, org.springframework.data.solr.core.query.HighlightQuery, java.lang.Class)
	 */
	public <T> HighlightPage<T> queryForHighlightPage(String collectionName, HighlightQuery query, Class<T> clazz) {
		return queryForHighlightPage(collectionName, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForHighlightPage(org.springframework.data.solr.core.query.HighlightQuery, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> HighlightPage<T> queryForHighlightPage(HighlightQuery query, Class<T> clazz, RequestMethod method) {
		return queryForHighlightPage(getSolrCoreOrBeanCollection(clazz), query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForHighlightPage(java.lang.String, org.springframework.data.solr.core.query.HighlightQuery, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	public <T> HighlightPage<T> queryForHighlightPage(String collectionName, HighlightQuery query, Class<T> clazz,
			RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		NamedObjectsHighlightQuery namedObjectsQuery = new NamedObjectsHighlightQuery(query);
		QueryResponse response = querySolr(collectionName, namedObjectsQuery, clazz, getDefaultRequestMethod());

		return createSolrResultPage(query, clazz, response, namedObjectsQuery.getNamesAssociation());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForFacetAndHighlightPage(org.springframework.data.solr.core.query.FacetAndHighlightQuery, java.lang.Class)
	 */
	@Override
	public <T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(FacetAndHighlightQuery query, Class<T> clazz) {
		return queryForFacetAndHighlightPage(query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForFacetAndHighlightPage(java.lang.String, org.springframework.data.solr.core.query.FacetAndHighlightQuery, java.lang.Class)
	 */
	public <T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(String collectionName, FacetAndHighlightQuery query,
			Class<T> clazz) {
		return queryForFacetAndHighlightPage(collectionName, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForFacetAndHighlightPage(org.springframework.data.solr.core.query.FacetAndHighlightQuery, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(FacetAndHighlightQuery query, Class<T> clazz,
			RequestMethod method) {
		return queryForFacetAndHighlightPage(getSolrCoreOrBeanCollection(clazz), query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForFacetAndHighlightPage(java.lang.String, org.springframework.data.solr.core.query.FacetAndHighlightQuery, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	public <T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(String collectionName, FacetAndHighlightQuery query,
			Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		NamedObjectsFacetAndHighlightQuery namedObjectsFacetAndHighlightQuery = new NamedObjectsFacetAndHighlightQuery(
				query);

		QueryResponse response = querySolr(collectionName, namedObjectsFacetAndHighlightQuery, clazz, method);
		Map<String, Object> objectsName = namedObjectsFacetAndHighlightQuery.getNamesAssociation();

		return createSolrResultPage(query, clazz, response, objectsName);
	}

	private <T> SolrResultPage<T> createSolrResultPage(Query query, Class<T> clazz, QueryResponse response,
			Map<String, Object> objectsName) {

		List<T> beans = convertQueryResponseToBeans(response, clazz);
		SolrDocumentList results = response.getResults();
		long numFound = results == null ? 0 : results.getNumFound();
		Float maxScore = results == null ? null : results.getMaxScore();

		Pageable pageRequest = query.getPageRequest();

		SolrResultPage<T> page = new SolrResultPage<T>(beans, pageRequest, numFound, maxScore);

		page.setFieldStatsResults(ResultHelper.convertFieldStatsInfoToFieldStatsResultMap(response.getFieldStatsInfo()));
		page.setGroupResults(
				ResultHelper.convertGroupQueryResponseToGroupResultMap(query, objectsName, response, this, clazz));

		if (query instanceof HighlightQuery) {
			ResultHelper.convertAndAddHighlightQueryResponseToResultPage(response, page);
		}

		if (query instanceof FacetQuery) {

			page.setFacetQueryResultPage(
					ResultHelper.convertFacetQueryResponseToFacetQueryResult((FacetQuery) query, response));
			page.addAllFacetFieldResultPages(
					ResultHelper.convertFacetQueryResponseToFacetPageMap((FacetQuery) query, response));
			page.addAllFacetPivotFieldResult(
					ResultHelper.convertFacetQueryResponseToFacetPivotMap((FacetQuery) query, response));
			page.addAllRangeFacetFieldResultPages(
					ResultHelper.convertFacetQueryResponseToRangeFacetPageMap((FacetQuery) query, response));
		}

		if (query.getSpellcheckOptions() != null) {
			Map<String, List<Alternative>> suggestions = ResultHelper.extreactSuggestions(response);
			for (Entry<String, List<Alternative>> entry : suggestions.entrySet()) {
				page.addSuggestions(entry.getKey(), entry.getValue());
			}
		}

		return page;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForTermsPage(org.springframework.data.solr.core.query.TermsQuery)
	 */
	@Override
	public TermsPage queryForTermsPage(TermsQuery query) {
		return queryForTermsPage(query, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForTermsPage(java.lang.String, org.springframework.data.solr.core.query.TermsQuery)
	 */
	public TermsPage queryForTermsPage(String collectionName, TermsQuery query) {
		return queryForTermsPage(collectionName, query, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForTermsPage(org.springframework.data.solr.core.query.TermsQuery, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public TermsPage queryForTermsPage(TermsQuery query, RequestMethod method) {
		return queryForTermsPage(null, query, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForTermsPage(java.lang.String, org.springframework.data.solr.core.query.TermsQuery, org.springframework.data.solr.core.RequestMethod)
	 */
	public TermsPage queryForTermsPage(String collectionName, TermsQuery query, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");

		QueryResponse response = querySolr(collectionName, query, null, method);

		TermsResultPage page = new TermsResultPage();
		page.addAllTerms(ResultHelper.convertTermsQueryResponseToTermsMap(response));
		return page;
	}

	final QueryResponse querySolr(String collectionName, SolrDataQuery query, Class<?> clazz) {
		return querySolr(collectionName, query, clazz, defaultRequestMethod);
	}

	final QueryResponse querySolr(String collectionName, SolrDataQuery query, Class<?> clazz, RequestMethod requestMethod) {

		Assert.notNull(query, "Query must not be 'null'");
		Assert.notNull(requestMethod, "RequestMethod must not be 'null'");

		QueryParser parser = null;
		if(query instanceof AbstractQueryDecorator) {
			parser = queryParsers.getForClass((Class) ((AbstractQueryDecorator) query).getQueryType());
		} else {
			parser = queryParsers.getForClass(query.getClass());
		}

		SolrQuery solrQuery =parser.constructSolrQuery(query);

		if (clazz != null) {
			SolrPersistentEntity<?> persistedEntity = mappingContext.getPersistentEntity(clazz);
			if (persistedEntity.hasScoreProperty()) {
				solrQuery.setIncludeScore(true);
			}
		}

		LOGGER.debug("Executing query '{}' against solr.", solrQuery);

		return executeSolrQuery(collectionName, solrQuery, getSolrRequestMethod(requestMethod));
	}

	final QueryResponse executeSolrQuery(final SolrQuery solrQuery, final SolrRequest.METHOD method) {

		return execute(new SolrCallback<QueryResponse>() {
			@Override
			public QueryResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.query(solrQuery, method);
			}
		});
	}

	final QueryResponse executeSolrQuery(String collectionName, final SolrQuery solrQuery,
			final SolrRequest.METHOD method) {

		return execute(collectionName, new CollectionCallback<QueryResponse>() {

			@Override
			public QueryResponse doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return solrClient.query(collection, solrQuery, method);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#commit()
	 */
	@Override
	public void commit() {
		commit(null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#commit(java.lang.String)
	 */
	public void commit(String collectionName) {

		execute(collectionName, new CollectionCallback<UpdateResponse>() {

			@Override
			public UpdateResponse doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return StringUtils.hasText(collection) ? solrClient.commit(collection) : solrClient.commit();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#softCommit()
	 */
	@Override
	public void softCommit() {
		softCommit(null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#softCommit(java.lang.String)
	 */
	public void softCommit(String collectionName) {

		execute(collectionName, new CollectionCallback<UpdateResponse>() {

			@Override
			public UpdateResponse doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return StringUtils.hasText(collection) ? solrClient.commit(collection, true, true, true)
						: solrClient.commit(true, true, true);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#rollback()
	 */
	@Override
	public void rollback() {
		rollback(null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#rollback(java.lang.String)
	 */
	public void rollback(String collectionName) {

		execute(collectionName, new CollectionCallback<UpdateResponse>() {

			@Override
			public UpdateResponse doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return StringUtils.hasText(collection) ? solrClient.rollback(collection) : solrClient.rollback();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#convertBeanToSolrInputDocument(java.lang.Object)
	 */
	@Override
	public SolrInputDocument convertBeanToSolrInputDocument(Object bean) {
		if (bean instanceof SolrInputDocument) {
			return (SolrInputDocument) bean;
		}

		SolrInputDocument document = new SolrInputDocument();
		getConverter().write(bean, document);
		return document;
	}

	/**
	 * @param collectionName
	 * @return
	 * @since 1.3
	 */
	public String getSchemaName(final String collectionName) {
		return getSchemaOperations(collectionName).getSchemaName();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForCursor(org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	@Override
	@SuppressWarnings("resource")
	public <T> Cursor<T> queryForCursor(Query query, final Class<T> clazz) {

		return new DelegatingCursor<T>(queryParsers.getForClass(query.getClass()).constructSolrQuery(query)) {

			@Override
			protected org.springframework.data.solr.core.query.result.DelegatingCursor.PartialResult<T> doLoad(
					SolrQuery nativeQuery) {

				QueryResponse response = executeSolrQuery(nativeQuery, getSolrRequestMethod(getDefaultRequestMethod()));
				if (response == null) {
					return new PartialResult<T>("", Collections.<T> emptyList());
				}

				return new PartialResult<T>(response.getNextCursorMark(), convertQueryResponseToBeans(response, clazz));
			}

		}.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#getById(java.util.Collection, java.lang.Class)
	 */
	@Override
	public <T> Collection<T> getById(final Collection<? extends Serializable> ids, final Class<T> clazz) {
		return getById(getSolrCoreOrBeanCollection(clazz), ids, clazz);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#getById(java.lang.String, java.util.Collection, java.lang.Class)
	 */
	public <T> Collection<T> getById(String collectionName, final Collection<? extends Serializable> ids,
			final Class<T> clazz) {

		if (CollectionUtils.isEmpty(ids)) {
			return Collections.emptyList();
		}

		final List<String> stringIds = new ArrayList<String>(ids.size());
		for (Serializable id : ids) {
			stringIds.add(id.toString());
		}

		return execute(collectionName, new CollectionCallback<Collection<T>>() {

			@Override
			public Collection<T> doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return convertSolrDocumentListToBeans(solrClient.getById(collection, stringIds), clazz);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#getById(java.io.Serializable, java.lang.Class)
	 */
	@Override
	public <T> T getById(Serializable id, Class<T> clazz) {
		return getById(getSolrCoreOrBeanCollection(clazz), id, clazz);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#getById(java.lang.String, java.io.Serializable, java.lang.Class)
	 */
	public <T> T getById(String collectionName, Serializable id, Class<T> clazz) {

		Assert.notNull(id, "Id must not be 'null'.");

		Collection<T> result = getById(collectionName, Collections.singletonList(id), clazz);
		if (result.isEmpty()) {
			return null;
		}
		return result.iterator().next();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#getSchemaOperations(java.lang.String)
	 */
	@Override
	public SchemaOperations getSchemaOperations(String collection) {
		return new DefaultSchemaOperations(collection, this);
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

	public <T> List<T> convertQueryResponseToBeans(QueryResponse response, Class<T> targetClass) {
		return response != null ? convertSolrDocumentListToBeans(response.getResults(), targetClass)
				: Collections.<T> emptyList();
	}

	public <T> List<T> convertSolrDocumentListToBeans(SolrDocumentList documents, Class<T> targetClass) {
		if (documents == null) {
			return Collections.<T> emptyList();
		}
		return getConverter().read(documents, targetClass);
	}

	public <T> T convertSolrDocumentToBean(SolrDocument document, Class<T> targetClass) {
		return getConverter().read(targetClass, document);
	}

	protected void assertNoCollection(Object o) {
		if (null != o && (o.getClass().isArray() || ITERABLE_CLASSES.contains(o.getClass().getName()))) {
			throw new IllegalArgumentException("Collections are not supported for this operation");
		}
	}

	private final SolrConverter getDefaultSolrConverter() {
		MappingSolrConverter converter = new MappingSolrConverter(this.mappingContext);
		converter.afterPropertiesSet(); // have to call this one to initialize default converters
		return converter;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#getSolrClient()
	 */
	@Override
	public final SolrClient getSolrClient() {
		return solrClientFactory.getSolrClient(this.solrCore);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#getConverter()
	 */
	@Override
	public SolrConverter getConverter() {
		return this.solrConverter;
	}

	public static PersistenceExceptionTranslator getExceptionTranslator() {
		return EXCEPTION_TRANSLATOR;
	}

	private String getSolrCoreOrBeanCollection(Class<?> clazz) {
		return StringUtils.hasText(solrCore) ? solrCore : mappingContext.getPersistentEntity(clazz).getSolrCoreName();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public void registerQueryParser(Class<? extends SolrDataQuery> clazz, QueryParser queryParser) {
		this.queryParsers.registerParser(clazz, queryParser);
	}

	public void setSolrConverter(SolrConverter solrConverter) {
		this.solrConverter = solrConverter;
	}

	public String getSolrCore() {
		return solrCore;
	}

	public void setSolrCore(String solrCore) {
		this.solrCore = solrCore;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {

		if (this.mappingContext == null) {
			this.mappingContext = new SimpleSolrMappingContext(
					new SolrPersistentEntitySchemaCreator(this.solrClientFactory).enable(this.schemaCreationFeatures));
		}

		if (this.solrConverter == null) {
			this.solrConverter = getDefaultSolrConverter();
		}
		registerPersistenceExceptionTranslator();
	}

	private void registerPersistenceExceptionTranslator() {
		if (this.applicationContext != null
				&& this.applicationContext.getBeansOfType(PersistenceExceptionTranslator.class).isEmpty()) {
			if (this.applicationContext instanceof ConfigurableApplicationContext) {
				((ConfigurableApplicationContext) this.applicationContext).getBeanFactory()
						.registerSingleton("solrExceptionTranslator", EXCEPTION_TRANSLATOR);
			}
		}
	}

	private SolrRequest.METHOD getSolrRequestMethod(RequestMethod requestMethod) {

		RequestMethod rm = requestMethod != null ? requestMethod : getDefaultRequestMethod();

		switch (rm) {
			case GET:
				return SolrRequest.METHOD.GET;
			case POST:
				return SolrRequest.METHOD.POST;
			case PUT:
				return SolrRequest.METHOD.PUT;
		}

		throw new IllegalArgumentException("Unsupported request method type");
	}

	/**
	 * @since 1.3
	 * @param mappingContext
	 */
	public void setMappingContext(
			MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext) {
		this.mappingContext = mappingContext;
	}

	/**
	 * @since 1.3
	 * @param schemaCreationFeatures
	 */
	public void setSchemaCreationFeatures(Collection<Feature> schemaCreationFeatures) {
		this.schemaCreationFeatures = new HashSet<Feature>(schemaCreationFeatures);
	}

	/**
	 * @since 1.3
	 * @return
	 */
	public Set<Feature> getSchemaCreationFeatures() {

		if (CollectionUtils.isEmpty(this.schemaCreationFeatures)) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(this.schemaCreationFeatures);
	}

	/**
	 * @return never {@literal null}.
	 */
	public RequestMethod getDefaultRequestMethod() {
		return defaultRequestMethod;
	}
}
