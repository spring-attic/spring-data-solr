/*
 * Copyright 2012 - 2017 the original author or authors.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
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
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of {@link SolrOperations}
 * 
 * @author Christoph Strobl
 * @author Joachim Uhrlass
 * @author Francisco Spaeth
 * @author Shiradwade Sateesh Krishna
 * @author David Webb
 * @author Petar Tahchiev
 */
public class SolrTemplate implements SolrOperations, InitializingBean, ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrTemplate.class);
	private static final PersistenceExceptionTranslator EXCEPTION_TRANSLATOR = new SolrExceptionTranslator();
	private final QueryParsers queryParsers = new QueryParsers();
	private MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext;

	private ApplicationContext applicationContext;
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
		this(new HttpSolrClientFactory(solrClient));
	}

	public SolrTemplate(SolrClient solrClient, RequestMethod requestMethod) {
		this(new HttpSolrClientFactory(solrClient), requestMethod);
	}

	public SolrTemplate(SolrClientFactory solrClientFactory) {
		this(solrClientFactory, (SolrConverter) null);
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
		this.solrConverter = solrConverter;
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
		return execute(SolrClient::ping);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#count(org.springframework.data.solr.core.query.SolrDataQuery)
	 */
	@Override
	public long count(final SolrDataQuery query) {
		return count(query, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#count(org.springframework.data.solr.core.query.SolrDataQuery, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public long count(final SolrDataQuery query, final RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(method, "Method must not be 'null'.");

		return execute(solrClient -> {

			SolrQuery solrQuery = queryParsers.getForClass(query.getClass()).constructSolrQuery(query);
			solrQuery.setStart(0);
			solrQuery.setRows(0);

			return solrClient.query(solrQuery, getSolrRequestMethod(method)).getResults().getNumFound();
		});
	}

	@Override
	public long count(String collection, SolrDataQuery query) {
		return count(collection, query, getDefaultRequestMethod());
	}

	@Override
	public long count(String collection, SolrDataQuery query, RequestMethod method) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(method, "Method must not be 'null'.");

		return execute(solrClient -> {

			SolrQuery solrQuery = queryParsers.getForClass(query.getClass()).constructSolrQuery(query);
			solrQuery.setStart(0);
			solrQuery.setRows(0);

			return solrClient.query(collection, solrQuery, getSolrRequestMethod(method)).getResults().getNumFound();
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

	@Override
	public UpdateResponse saveBean(String collection, Object obj) {
		return saveBean(collection, obj, -1);
	}

	@Override
	public UpdateResponse saveBean(String collection, Object obj, int commitWithinMs) {

		assertNoCollection(obj);
		return execute(solrClient -> solrClient.add(collection, convertBeanToSolrInputDocument(obj), commitWithinMs));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveBean(java.lang.Object, int)
	 */
	@Override
	public UpdateResponse saveBean(final Object objectToAdd, final int commitWithinMs) {
		assertNoCollection(objectToAdd);
		return execute(solrClient -> solrClient.add(convertBeanToSolrInputDocument(objectToAdd), commitWithinMs));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveBeans(java.util.Collection)
	 */
	@Override
	public UpdateResponse saveBeans(Collection<?> beans) {
		return saveBeans(beans, -1);
	}

	@Override
	public UpdateResponse saveBeans(String collection, Collection<?> beans) {
		return saveBeans(collection, beans, -1);
	}

	@Override
	public UpdateResponse saveBeans(String collection, Collection<?> beans, int commitWithinMs) {
		return execute(solrClient -> solrClient.add(collection, convertBeansToSolrInputDocuments(beans), commitWithinMs));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveBeans(java.util.Collection, int)
	 */
	@Override
	public UpdateResponse saveBeans(final Collection<?> beansToAdd, final int commitWithinMs) {
		return execute(solrClient -> solrClient.add(convertBeansToSolrInputDocuments(beansToAdd), commitWithinMs));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveDocument(org.apache.solr.common.SolrInputDocument)
	 */
	@Override
	public UpdateResponse saveDocument(SolrInputDocument document) {
		return saveDocument(document, -1);
	}

	@Override
	public UpdateResponse saveDocument(String collection, SolrInputDocument document) {
		return saveDocument(collection, document, -1);
	}

	@Override
	public UpdateResponse saveDocument(String collection, SolrInputDocument document, int commitWithinMs) {
		return execute(solrClient -> solrClient.add(collection, document, commitWithinMs));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveDocument(org.apache.solr.common.SolrInputDocument, int)
	 */
	@Override
	public UpdateResponse saveDocument(final SolrInputDocument documentToAdd, final int commitWithinMs) {
		return execute(solrClient -> solrClient.add(documentToAdd, commitWithinMs));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#saveDocuments(java.util.Collection)
	 */
	@Override
	public UpdateResponse saveDocuments(Collection<SolrInputDocument> documents) {
		return saveDocuments(documents, -1);
	}

	@Override
	public UpdateResponse saveDocuments(String collection, Collection<SolrInputDocument> documents) {
		return saveDocuments(collection, documents, -1);
	}

	@Override
	public UpdateResponse saveDocuments(String collection, Collection<SolrInputDocument> documents, int commitWithinMs) {
		return execute(solrClient -> solrClient.add(collection, documents, commitWithinMs));
	}

	/*
		 * (non-Javadoc)
			 * @see org.springframework.data.solr.core.SolrOperations#saveDocuments(java.util.Collection, int)
			 */
	@Override
	public UpdateResponse saveDocuments(final Collection<SolrInputDocument> documentsToAdd, final int commitWithinMs) {
		return execute(solrClient -> solrClient.add(documentsToAdd, commitWithinMs));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#delete(org.springframework.data.solr.core.query.SolrDataQuery)
	 */
	@Override
	public UpdateResponse delete(SolrDataQuery query) {

		Assert.notNull(query, "Query must not be 'null'.");

		final String queryString = this.queryParsers.getForClass(query.getClass()).getQueryString(query);

		return execute(solrClient -> solrClient.deleteByQuery(queryString));
	}

	@Override
	public UpdateResponse delete(String collection, SolrDataQuery query) {

		Assert.notNull(query, "Query must not be 'null'.");

		final String queryString = this.queryParsers.getForClass(query.getClass()).getQueryString(query);

		return execute(solrClient -> solrClient.deleteByQuery(collection, queryString));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#deleteById(java.lang.String)
	 */
	@Override
	public UpdateResponse deleteById(final String id) {

		Assert.notNull(id, "Cannot delete 'null' id.");

		return execute(solrClient -> solrClient.deleteById(id));
	}

	@Override
	public UpdateResponse deleteById(String collection, String id) {

		Assert.notNull(id, "Cannot delete 'null' id.");

		return execute(solrClient -> solrClient.deleteById(collection, id));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#deleteById(java.util.Collection)
	 */
	@Override
	public UpdateResponse deleteById(Collection<String> ids) {

		Assert.notNull(ids, "Cannot delete 'null' collection.");

		return execute(solrClient -> solrClient.deleteById(ids.stream().collect(Collectors.toList())));
	}

	@Override
	public UpdateResponse deleteById(String collection, Collection<String> ids) {

		Assert.notNull(ids, "Cannot delete 'null' collection.");

		return execute(solrClient -> solrClient.deleteById(collection, ids.stream().collect(Collectors.toList())));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForObject(org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	@Override
	public <T> Optional<T> queryForObject(Query query, Class<T> clazz) {
		return queryForObject(query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> Optional<T> queryForObject(String collection, Query query, Class<T> clazz) {
		return queryForObject(collection, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForObject(org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> Optional<T> queryForObject(String collection, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		query.setPageRequest(PageRequest.of(0, 1));
		QueryResponse response = querySolr(collection, query, clazz, method);

		if (response.getResults().size() > 0) {
			if (response.getResults().size() > 1) {
				LOGGER.warn("More than 1 result found for singe result query ('{}'), returning first entry in list");
			}
			return Optional.ofNullable(convertSolrDocumentListToBeans(response.getResults(), clazz).get(0));
		}
		return Optional.empty();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForObject(org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> Optional<T> queryForObject(Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		query.setPageRequest(PageRequest.of(0, 1));
		QueryResponse response = querySolr(query, clazz, method);

		if (response.getResults().size() > 0) {
			if (response.getResults().size() > 1) {
				LOGGER.warn("More than 1 result found for singe result query ('{}'), returning first entry in list");
			}
			return Optional.ofNullable(convertSolrDocumentListToBeans(response.getResults(), clazz).get(0));
		}
		return Optional.empty();
	}

	private <T> SolrResultPage<T> doQueryForPage(Query query, Class<T> clazz, RequestMethod requestMethod) {
		return doQueryForPage(null, query, clazz, requestMethod);
	}

	private <T> SolrResultPage<T> doQueryForPage(String collection, Query query, Class<T> clazz,
			RequestMethod requestMethod) {

		QueryResponse response = null;
		NamedObjectsQuery namedObjectsQuery = new NamedObjectsQuery(query);
		response = querySolr(collection, namedObjectsQuery, clazz,
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

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		return doQueryForPage(query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> ScoredPage<T> queryForPage(String collection, Query query, Class<T> clazz) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		return doQueryForPage(collection, query, clazz, getDefaultRequestMethod());
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
	public <T, S extends Page<T>> S query(String collection, Query query, Class<T> clazz) {
		return query(collection, query, clazz, getDefaultRequestMethod());
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

		return (S) doQueryForPage(query, clazz, method);
	}

	public <T, S extends Page<T>> S query(String collection, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(clazz, "Method must not be 'null'.");

		return (S) doQueryForPage(collection, query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForPage(org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> ScoredPage<T> queryForPage(Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");

		return doQueryForPage(query, clazz, method);
	}

	@Override
	public <T> ScoredPage<T> queryForPage(String collection, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");

		return doQueryForPage(collection, query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForGroupPage(org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	@Override
	public <T> GroupPage<T> queryForGroupPage(Query query, Class<T> clazz) {
		return queryForGroupPage(query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> GroupPage<T> queryForGroupPage(String collection, Query query, Class<T> clazz) {
		return queryForGroupPage(collection, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForGroupPage(org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> GroupPage<T> queryForGroupPage(Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");

		return doQueryForPage(query, clazz, method);
	}

	@Override
	public <T> GroupPage<T> queryForGroupPage(String collection, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");

		return doQueryForPage(collection, query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForStatsPage(org.springframework.data.solr.core.query.Query, java.lang.Class)
	 */
	@Override
	public <T> StatsPage<T> queryForStatsPage(Query query, Class<T> clazz) {
		return queryForStatsPage(query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> StatsPage<T> queryForStatsPage(String collection, Query query, Class<T> clazz) {
		return queryForStatsPage(collection, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForStatsPage(org.springframework.data.solr.core.query.Query, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> StatsPage<T> queryForStatsPage(Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");

		return doQueryForPage(query, clazz, method);
	}

	public <T> StatsPage<T> queryForStatsPage(String collection, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");

		return doQueryForPage(collection, query, clazz, method);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForFacetPage(org.springframework.data.solr.core.query.FacetQuery, java.lang.Class)
	 */
	@Override
	public <T> FacetPage<T> queryForFacetPage(FacetQuery query, Class<T> clazz) {
		return queryForFacetPage(query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> FacetPage<T> queryForFacetPage(String collection, FacetQuery query, Class<T> clazz) {
		return queryForFacetPage(collection, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForFacetPage(org.springframework.data.solr.core.query.FacetQuery, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> FacetPage<T> queryForFacetPage(FacetQuery query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		NamedObjectsFacetQuery namedObjectsQuery = new NamedObjectsFacetQuery(query);

		return createSolrResultPage(query, clazz, querySolr(namedObjectsQuery, clazz, method),
				namedObjectsQuery.getNamesAssociation());

	}

	@Override
	public <T> FacetPage<T> queryForFacetPage(String collection, FacetQuery query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		NamedObjectsFacetQuery namedObjectsQuery = new NamedObjectsFacetQuery(query);

		return createSolrResultPage(query, clazz, querySolr(collection, namedObjectsQuery, clazz, method),
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

	@Override
	public <T> HighlightPage<T> queryForHighlightPage(String collection, HighlightQuery query, Class<T> clazz) {
		return queryForHighlightPage(collection, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForHighlightPage(org.springframework.data.solr.core.query.HighlightQuery, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> HighlightPage<T> queryForHighlightPage(HighlightQuery query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		NamedObjectsHighlightQuery namedObjectsQuery = new NamedObjectsHighlightQuery(query);
		QueryResponse response = querySolr(namedObjectsQuery, clazz, getDefaultRequestMethod());

		return createSolrResultPage(query, clazz, response, namedObjectsQuery.getNamesAssociation());
	}

	@Override
	public <T> HighlightPage<T> queryForHighlightPage(String collection, HighlightQuery query, Class<T> clazz,
			RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		NamedObjectsHighlightQuery namedObjectsQuery = new NamedObjectsHighlightQuery(query);
		QueryResponse response = querySolr(collection, namedObjectsQuery, clazz, getDefaultRequestMethod());

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

	@Override
	public <T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(String collection, FacetAndHighlightQuery query,
			Class<T> clazz) {
		return queryForFacetAndHighlightPage(collection, query, clazz, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForFacetAndHighlightPage(org.springframework.data.solr.core.query.FacetAndHighlightQuery, java.lang.Class, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public <T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(FacetAndHighlightQuery query, Class<T> clazz,
			RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		NamedObjectsFacetAndHighlightQuery namedObjectsFacetAndHighlightQuery = new NamedObjectsFacetAndHighlightQuery(
				query);

		QueryResponse response = querySolr(namedObjectsFacetAndHighlightQuery, clazz, method);
		Map<String, Object> objectsName = namedObjectsFacetAndHighlightQuery.getNamesAssociation();

		return createSolrResultPage(query, clazz, response, objectsName);
	}

	@Override
	public <T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(String collection, FacetAndHighlightQuery query,
			Class<T> clazz, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		NamedObjectsFacetAndHighlightQuery namedObjectsFacetAndHighlightQuery = new NamedObjectsFacetAndHighlightQuery(
				query);

		QueryResponse response = querySolr(collection, namedObjectsFacetAndHighlightQuery, clazz, method);
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

		SolrResultPage<T> page = new SolrResultPage<>(beans, pageRequest, numFound, maxScore);

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

	@Override
	public TermsPage queryForTermsPage(String collection, TermsQuery query) {
		return queryForTermsPage(collection, query, getDefaultRequestMethod());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#queryForTermsPage(org.springframework.data.solr.core.query.TermsQuery, org.springframework.data.solr.core.RequestMethod)
	 */
	@Override
	public TermsPage queryForTermsPage(TermsQuery query, RequestMethod method) {

		Assert.notNull(query, "Query must not be 'null'.");

		QueryResponse response = querySolr(null, query, null, method);

		TermsResultPage page = new TermsResultPage();
		page.addAllTerms(ResultHelper.convertTermsQueryResponseToTermsMap(response));
		return page;
	}

	@Override
	public TermsPage queryForTermsPage(String collection, TermsQuery query, RequestMethod method) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");

		QueryResponse response = querySolr(collection, query, null, method);

		TermsResultPage page = new TermsResultPage();
		page.addAllTerms(ResultHelper.convertTermsQueryResponseToTermsMap(response));
		return page;
	}

	final QueryResponse querySolr(SolrDataQuery query, Class<?> clazz) {
		return querySolr(query, clazz, defaultRequestMethod);
	}

	final QueryResponse querySolr(SolrDataQuery query, Class<?> clazz, RequestMethod requestMethod) {
		return querySolr(null, query, clazz, requestMethod);
	}

	final QueryResponse querySolr(String collection, SolrDataQuery query, Class<?> clazz, RequestMethod requestMethod) {

		Assert.notNull(query, "Query must not be 'null'");
		Assert.notNull(requestMethod, "RequestMethod must not be 'null'");

		SolrQuery solrQuery = queryParsers.getForClass(query.getClass()).constructSolrQuery(query);

		if (clazz != null) {
			SolrPersistentEntity<?> persistedEntity = mappingContext.getPersistentEntity(clazz)
					.orElseThrow(() -> new IllegalArgumentException("No persistent entity found."));
			if (persistedEntity.hasScoreProperty()) {
				solrQuery.setIncludeScore(true);
			}
		}

		LOGGER.debug("Executing query '" + solrQuery + "' against solr.");

		return executeSolrQuery(collection, solrQuery, getSolrRequestMethod(requestMethod));
	}

	final QueryResponse executeSolrQuery(final SolrQuery solrQuery, final SolrRequest.METHOD method) {
		return executeSolrQuery(null, solrQuery, method);
	}

	final QueryResponse executeSolrQuery(String collection, final SolrQuery solrQuery, final SolrRequest.METHOD method) {

		return execute(solrServer -> solrServer.query(collection, solrQuery, method));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#commit()
	 */
	@Override
	public void commit() {
		execute(SolrClient::commit);
	}

	@Override
	public void commit(String collection) {
		execute(solrClient -> solrClient.commit(collection));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#softCommit()
	 */
	@Override
	public void softCommit() {

		execute(solrClient -> solrClient.commit(true, true, true));
	}

	@Override
	public void softCommit(String collection) {

		execute(solrClient -> solrClient.commit(collection, true, true, true));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#rollback()
	 */
	@Override
	public void rollback() {
		execute(SolrClient::rollback);
	}

	@Override
	public void rollback(String collection) {
		execute(SolrClient::rollback);
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
					return new PartialResult<>("", Collections.<T> emptyList());
				}

				return new PartialResult<>(response.getNextCursorMark(), convertQueryResponseToBeans(response, clazz));
			}

		}.open();
	}

	public <T> Cursor<T> queryForCursor(String collection, Query query, final Class<T> clazz) {

		return new DelegatingCursor<T>(queryParsers.getForClass(query.getClass()).constructSolrQuery(query)) {

			@Override
			protected org.springframework.data.solr.core.query.result.DelegatingCursor.PartialResult<T> doLoad(
					SolrQuery nativeQuery) {

				QueryResponse response = executeSolrQuery(collection, nativeQuery,
						getSolrRequestMethod(getDefaultRequestMethod()));
				if (response == null) {
					return new PartialResult<>("", Collections.<T> emptyList());
				}

				return new PartialResult<>(response.getNextCursorMark(), convertQueryResponseToBeans(response, clazz));
			}

		}.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#getById(java.util.Collection, java.lang.Class)
	 */
	@Override
	public <T> Collection<T> getById(final Collection<? extends Serializable> ids, final Class<T> clazz) {

		if (CollectionUtils.isEmpty(ids)) {
			return Collections.emptyList();
		}

		return execute(solrClient -> convertSolrDocumentListToBeans(
				solrClient.getById(ids.stream().map(Object::toString).collect(Collectors.toList())), clazz));
	}

	@Override
	public <T> Collection<T> getById(String collection, final Collection<? extends Serializable> ids,
			final Class<T> clazz) {

		if (CollectionUtils.isEmpty(ids)) {
			return Collections.emptyList();
		}

		return execute(solrClient -> convertSolrDocumentListToBeans(
				solrClient.getById(collection, ids.stream().map(Object::toString).collect(Collectors.toList())), clazz));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.SolrOperations#getById(java.io.Serializable, java.lang.Class)
	 */
	@Override
	public <T> T getById(Serializable id, Class<T> clazz) {

		Assert.notNull(id, "Id must not be 'null'.");

		Collection<T> result = getById(Collections.singletonList(id), clazz);
		if (result.isEmpty()) {
			return null;
		}
		return result.iterator().next();
	}

	public <T> T getById(String collection, Serializable id, Class<T> clazz) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(id, "Id must not be 'null'.");

		Collection<T> result = getById(collection, Collections.singletonList(id), clazz);
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

		List<SolrInputDocument> resultList = new ArrayList<>();
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

	private SolrConverter getDefaultSolrConverter() {
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
		return solrClientFactory.getSolrClient();
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
		this.schemaCreationFeatures = new HashSet<>(schemaCreationFeatures);
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
