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

import java.time.Duration;
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
import org.apache.solr.client.solrj.request.SolrPing;
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
import org.springframework.dao.InvalidDataAccessApiUsageException;
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
import org.springframework.data.solr.core.query.result.*;
import org.springframework.data.solr.core.query.result.SpellcheckQueryResult.Alternative;
import org.springframework.data.solr.core.schema.DefaultSchemaOperations;
import org.springframework.data.solr.core.schema.SchemaOperations;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator.Feature;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.data.solr.server.support.HttpSolrClientFactory;
import org.springframework.lang.Nullable;
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
 * @author Mark Paluch
 * @author Juan Manuel de Blas
 */
public class SolrTemplate implements SolrOperations, InitializingBean, ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(SolrTemplate.class);
	private static final PersistenceExceptionTranslator EXCEPTION_TRANSLATOR = new SolrExceptionTranslator();
	private final QueryParsers queryParsers = new QueryParsers();
	private @Nullable MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext;

	private @Nullable ApplicationContext applicationContext;
	private final RequestMethod defaultRequestMethod;

	private @Nullable SolrClientFactory solrClientFactory;

	private @Nullable SolrConverter solrConverter;

	private Set<Feature> schemaCreationFeatures = Collections.emptySet();

	@SuppressWarnings("serial") //
	private static final List<String> ITERABLE_CLASSES = new ArrayList<String>() {
		{
			add(List.class.getName());
			add(Collection.class.getName());
			add(Iterator.class.getName());
		}
	};

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

	public SolrTemplate(SolrClientFactory solrClientFactory, @Nullable SolrConverter solrConverter) {
		this(solrClientFactory, solrConverter, RequestMethod.GET);
	}

	/**
	 * @param solrClientFactory must not be {@literal null}.
	 * @param solrConverter must not be {@literal null}.
	 * @param defaultRequestMethod can be {@literal null}. Will be defaulted to {@link RequestMethod#GET}
	 * @since 2.0
	 */
	public SolrTemplate(SolrClientFactory solrClientFactory, @Nullable SolrConverter solrConverter,
			@Nullable RequestMethod defaultRequestMethod) {

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
	 * @see org.springframework.data.solr.core.SolrOperations#ping(java.lang.String)
	 */
	@Override
	public SolrPingResponse ping(String collection) {
		return execute(client -> new SolrPing().process(client, collection));
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

			SolrQuery solrQuery = constructQuery(query);
			solrQuery.setStart(0);
			solrQuery.setRows(0);

			return solrClient.query(collection, solrQuery, getSolrRequestMethod(method)).getResults().getNumFound();
		});
	}

	@Override
	public UpdateResponse saveBean(String collection, Object obj, Duration commitWithin) {

		assertNoCollection(obj);

		return execute(solrClient -> solrClient.add(collection, convertBeanToSolrInputDocument(obj),
				getCommitWithinTimeout(commitWithin)));
	}

	@Override
	public UpdateResponse saveBeans(String collection, Collection<?> beans, Duration commitWithin) {
		return execute(solrClient -> solrClient.add(collection, convertBeansToSolrInputDocuments(beans),
				getCommitWithinTimeout(commitWithin)));
	}

	@Override
	public UpdateResponse saveDocument(String collection, SolrInputDocument document, Duration commitWithin) {
		return execute(solrClient -> solrClient.add(collection, document, getCommitWithinTimeout(commitWithin)));
	}

	@Override
	public UpdateResponse saveDocuments(String collection, Collection<SolrInputDocument> documents,
			Duration commitWithin) {
		return execute(solrClient -> solrClient.add(collection, documents, getCommitWithinTimeout(commitWithin)));
	}

	@Override
	public UpdateResponse delete(String collection, SolrDataQuery query) {

		Assert.notNull(query, "Query must not be 'null'.");

		final String queryString = this.queryParsers.getForClass(query.getClass()).getQueryString(query);

		return execute(solrClient -> solrClient.deleteByQuery(collection, queryString));
	}

	@Override
	public UpdateResponse deleteByIds(String collection, String id) {

		Assert.notNull(id, "Cannot delete 'null' id.");

		return execute(solrClient -> solrClient.deleteById(collection, id));
	}

	@Override
	public UpdateResponse deleteByIds(String collection, Collection<String> ids) {

		Assert.notNull(ids, "Cannot delete 'null' collection.");

		return execute(solrClient -> solrClient.deleteById(collection, ids.stream().collect(Collectors.toList())));
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
				LOGGER.warn("More than 1 result found for singe result query ('{}'), returning first entry in list", query);
			}
			return Optional.ofNullable(convertSolrDocumentListToBeans(response.getResults(), clazz).get(0));
		}
		return Optional.empty();
	}

	private <T> SolrResultPage<T> doQueryForPage(String collection, Query query, Class<T> clazz,
			@Nullable RequestMethod requestMethod) {

		QueryResponse response = null;
		NamedObjectsQuery namedObjectsQuery = new NamedObjectsQuery(query);
		response = querySolr(collection, namedObjectsQuery, clazz,
				requestMethod != null ? requestMethod : getDefaultRequestMethod());
		Map<String, Object> objectsName = namedObjectsQuery.getNamesAssociation();

		return createSolrResultPage(query, clazz, response, objectsName);
	}

	@Override
	public <T> ScoredPage<T> queryForPage(String collection, Query query, Class<T> clazz) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		return doQueryForPage(collection, query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T, S extends Page<T>> S query(String collection, Query query, Class<T> clazz) {
		return query(collection, query, clazz, getDefaultRequestMethod());
	}

	public <T, S extends Page<T>> S query(String collection, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(clazz, "Method must not be 'null'.");

		return (S) doQueryForPage(collection, query, clazz, method);
	}

	@Override
	public <T> ScoredPage<T> queryForPage(String collection, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");

		return doQueryForPage(collection, query, clazz, method);
	}

	@Override
	public <T> GroupPage<T> queryForGroupPage(String collection, Query query, Class<T> clazz) {
		return queryForGroupPage(collection, query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> GroupPage<T> queryForGroupPage(String collection, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");

		return doQueryForPage(collection, query, clazz, method);
	}

	@Override
	public <T> StatsPage<T> queryForStatsPage(String collection, Query query, Class<T> clazz) {
		return queryForStatsPage(collection, query, clazz, getDefaultRequestMethod());
	}

	public <T> StatsPage<T> queryForStatsPage(String collection, Query query, Class<T> clazz, RequestMethod method) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");

		return doQueryForPage(collection, query, clazz, method);
	}

	@Override
	public <T> FacetPage<T> queryForFacetPage(String collection, FacetQuery query, Class<T> clazz) {
		return queryForFacetPage(collection, query, clazz, getDefaultRequestMethod());
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

	@Override
	public <T> HighlightPage<T> queryForHighlightPage(String collection, HighlightQuery query, Class<T> clazz) {
		return queryForHighlightPage(collection, query, clazz, getDefaultRequestMethod());
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

	@Override
	public <T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(String collection, FacetAndHighlightQuery query,
			Class<T> clazz) {
		return queryForFacetAndHighlightPage(collection, query, clazz, getDefaultRequestMethod());
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

	@Override
	public TermsPage queryForTermsPage(String collection, TermsQuery query) {
		return queryForTermsPage(collection, query, getDefaultRequestMethod());
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

	final QueryResponse querySolr(String collection, SolrDataQuery query, @Nullable Class<?> clazz,
			@Nullable RequestMethod requestMethod) {

		Assert.notNull(query, "Query must not be 'null'");

		SolrQuery solrQuery = constructQuery(query);

		if (clazz != null) {
			SolrPersistentEntity<?> persistedEntity = mappingContext.getRequiredPersistentEntity(clazz);
			if (persistedEntity.hasScoreProperty()) {
				solrQuery.setIncludeScore(true);
			}
		}

		LOGGER.debug("Executing query '{}' against solr.", solrQuery);

		return executeSolrQuery(collection, solrQuery, getSolrRequestMethod(requestMethod));
	}

	final QueryResponse executeSolrQuery(final SolrQuery solrQuery, final SolrRequest.METHOD method) {
		return executeSolrQuery(null, solrQuery, method);
	}

	final QueryResponse executeSolrQuery(String collection, final SolrQuery solrQuery, final SolrRequest.METHOD method) {

		return execute(solrServer -> solrServer.query(collection, solrQuery, method));
	}

	/**
	 * Create the native {@link SolrQuery} from a given {@link SolrDataQuery}.
	 *
	 * @param query never {@literal null}.
	 * @return never {@literal null}.
	 * @since 2.1.11
	 */
	protected SolrQuery constructQuery(SolrDataQuery query) {
		return lookupQueryParser(query).constructSolrQuery(query);
	}

	private QueryParser lookupQueryParser(SolrDataQuery query) {

		if (query instanceof AbstractQueryDecorator) {
			return queryParsers.getForClass((Class) ((AbstractQueryDecorator) query).getQueryType());
		}

		return queryParsers.getForClass(query.getClass());
	}

	@Override
	public void commit(String collection) {
		execute(solrClient -> solrClient.commit(collection));
	}

	@Override
	public void softCommit(String collection) {

		execute(solrClient -> solrClient.commit(collection, true, true, true));
	}

	@Override
	public void rollback(String collection) {
		execute(solrClient -> solrClient.rollback(collection));
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

	public <T> Cursor<T> queryForCursor(String collection, Query query, final Class<T> clazz) {

		return new DelegatingCursor<T>(constructQuery(query)) {

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

	@Override
	public <T> Collection<T> getByIds(String collection, final Collection<?> ids, final Class<T> clazz) {

		if (CollectionUtils.isEmpty(ids)) {
			return Collections.emptyList();
		}

		return execute(solrClient -> convertSolrDocumentListToBeans(
				solrClient.getById(collection, ids.stream().map(Object::toString).collect(Collectors.toList())), clazz));
	}

	public <T> Optional<T> getById(String collection, Object id, Class<T> clazz) {

		Assert.notNull(collection, "Collection must not be null!");
		Assert.notNull(id, "Id must not be 'null'.");

		Collection<T> result = getByIds(collection, Collections.singletonList(id), clazz);
		if (result.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(result.iterator().next());
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

	private SolrRequest.METHOD getSolrRequestMethod(@Nullable RequestMethod requestMethod) {

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

	private int getCommitWithinTimeout(Duration duration) {

		if (duration == null || duration.isZero() || duration.isNegative()) {
			return -1;
		}

		if (duration.toMillis() > Integer.MAX_VALUE) {
			throw new InvalidDataAccessApiUsageException(
					String.format("CommitWithin must must not exceed int range but was %s", duration.toMillis()));
		}

		return (int) duration.toMillis();
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
