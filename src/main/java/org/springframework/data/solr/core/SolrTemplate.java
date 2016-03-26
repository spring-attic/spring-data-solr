/*
 * Copyright 2012 - 2016 the original author or authors.
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.solr.SolrRealtimeGetRequest;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.VersionUtil;
import org.springframework.data.solr.core.QueryParserBase.NamedObjectsFacetQuery;
import org.springframework.data.solr.core.QueryParserBase.NamedObjectsHighlightQuery;
import org.springframework.data.solr.core.QueryParserBase.NamedObjectsQuery;
import org.springframework.data.solr.core.convert.MappingSolrConverter;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.core.query.TermsQuery;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.data.solr.core.query.result.DelegatingCursor;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.core.query.result.ScoredPage;
import org.springframework.data.solr.core.query.result.SolrResultPage;
import org.springframework.data.solr.core.query.result.StatsPage;
import org.springframework.data.solr.core.query.result.TermsPage;
import org.springframework.data.solr.core.query.result.TermsResultPage;
import org.springframework.data.solr.core.schema.SolrJsonResponse;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator.Feature;
import org.springframework.data.solr.core.schema.SolrSchemaRequest;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.data.solr.server.support.HttpSolrClientFactory;
import org.springframework.data.solr.server.support.SolrClientUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of {@link SolrOperations}
 * 
 * @author Christoph Strobl
 * @author Joachim Uhrlass
 * @author Francisco Spaeth
 * @author Shiradwade Sateesh Krishna
 * @author Venil Noronha
 */
public class SolrTemplate implements SolrOperations, MulticoreSolrOperations, InitializingBean, ApplicationContextAware {

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
		Assert.notNull(solrClientFactory.getSolrClient(), "SolrClientFactory has to return a SolrClient.");

		this.solrClientFactory = solrClientFactory;
		this.defaultRequestMethod = defaultRequestMethod != null ? defaultRequestMethod : RequestMethod.GET;
	}

	@Override
	public <T> T execute(SolrCallback<T> action) {
		Assert.notNull(action);
		SolrClient solrClient = this.getSolrClient();
		return execute(solrClient, action);
	}
	
	@Override
	public <T> T execute(String coreName, SolrCallback<T> action) {
		Assert.notNull(coreName);
		Assert.notNull(action);
		SolrClient solrClient = this.getSolrClient(coreName);
		return execute(solrClient, action);
	}

	private <T> T execute(SolrClient solrClient, SolrCallback<T> action) {
		try {
			return action.doInSolr(solrClient);
		} catch (Exception e) {
			DataAccessException resolved = getExceptionTranslator()
					.translateExceptionIfPossible(new RuntimeException(e.getMessage(), e));
			throw resolved == null ? new UncategorizedSolrException(e.getMessage(), e) : resolved;
		}
	}

	@Override
	public SolrPingResponse ping() {
		return ping(solrCore);
	}

	@Override
	public SolrPingResponse ping(String coreName) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		return execute(coreName, new SolrCallback<SolrPingResponse>() {
			@Override
			public SolrPingResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.ping();
			}
		});
	}

	@Override
	public long count(SolrDataQuery query) {
		return count(query, getDefaultRequestMethod());
	}

	@Override
	public long count(String coreName, SolrDataQuery query) {
		return count(coreName, query, getDefaultRequestMethod());
	}

	@Override
	public long count(SolrDataQuery query, RequestMethod method) {
		return count(solrCore, query, method);
	}

	@Override
	public long count(String coreName, final SolrDataQuery query, final RequestMethod method) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(method, "Method must not be 'null'.");
		return execute(coreName, new SolrCallback<Long>() {
			@Override
			public Long doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				SolrQuery solrQuery = queryParsers.getForClass(query.getClass()).constructSolrQuery(query);
				solrQuery.setStart(0);
				solrQuery.setRows(0);
				return solrClient.query(solrQuery, getSolrRequestMethod(method)).getResults().getNumFound();
			}
		});
	}

	@Override
	public UpdateResponse saveBean(Object obj) {
		return saveBean(obj, -1);
	}

	@Override
	public UpdateResponse saveBean(String coreName, Object obj) {
		return saveBean(coreName, obj, -1);
	}

	@Override
	public UpdateResponse saveBean(Object objectToAdd, int commitWithinMs) {
		String coreName = SolrClientUtils.resolveSolrCoreName(objectToAdd.getClass(), solrCore);
		return saveBean(coreName, objectToAdd, commitWithinMs);
	}

	@Override
	public UpdateResponse saveBean(String coreName, final Object objectToAdd, final int commitWithinMs) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		assertNoCollection(objectToAdd);
		return execute(coreName, new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.add(convertBeanToSolrInputDocument(objectToAdd), commitWithinMs);
			}
		});
	}

	@Override
	public UpdateResponse saveBeans(Collection<?> beans) {
		return saveBeans(beans, -1);
	}

	@Override
	public UpdateResponse saveBeans(String coreName, Collection<?> beans) {
		return saveBeans(coreName, beans, -1);
	}

	@Override
	public UpdateResponse saveBeans(Collection<?> beansToAdd, int commitWithinMs) {
		Object oneBean = beansToAdd.iterator().next();
		String coreName = SolrClientUtils.resolveSolrCoreName(oneBean.getClass(), solrCore);
		return saveBeans(coreName, beansToAdd, commitWithinMs);
	}

	@Override
	public UpdateResponse saveBeans(String coreName, final Collection<?> beansToAdd, final int commitWithinMs) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		return execute(coreName, new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.add(convertBeansToSolrInputDocuments(beansToAdd), commitWithinMs);
			}
		});
	}

	@Override
	public UpdateResponse saveDocument(SolrInputDocument document) {
		return saveDocument(document, -1);
	}

	@Override
	public UpdateResponse saveDocument(String coreName, SolrInputDocument document) {
		return saveDocument(coreName, document, -1);
	}

	@Override
	public UpdateResponse saveDocument(SolrInputDocument documentToAdd, int commitWithinMs) {
		return saveDocument(solrCore, documentToAdd, commitWithinMs);
	}

	@Override
	public UpdateResponse saveDocument(String coreName, final SolrInputDocument documentToAdd, final int commitWithinMs) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		return execute(coreName, new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.add(documentToAdd, commitWithinMs);
			}
		});
	}

	@Override
	public UpdateResponse saveDocuments(Collection<SolrInputDocument> documents) {
		return saveDocuments(documents, -1);
	}

	@Override
	public UpdateResponse saveDocuments(String coreName, Collection<SolrInputDocument> documents) {
		return saveDocuments(coreName, documents, -1);
	}

	@Override
	public UpdateResponse saveDocuments(Collection<SolrInputDocument> documentsToAdd, int commitWithinMs) {
		return saveDocuments(solrCore, documentsToAdd, commitWithinMs);
	}

	@Override
	public UpdateResponse saveDocuments(String coreName, final Collection<SolrInputDocument> documentsToAdd, final int commitWithinMs) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		return execute(coreName, new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.add(documentsToAdd, commitWithinMs);
			}
		});
	}

	@Override
	public UpdateResponse delete(SolrDataQuery query) {
		return delete(solrCore, query);
	}

	@Override
	public UpdateResponse delete(String coreName, SolrDataQuery query) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		Assert.notNull(query, "Query must not be 'null'.");
		final String queryString = this.queryParsers.getForClass(query.getClass()).getQueryString(query);
		return execute(coreName, new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.deleteByQuery(queryString);
			}
		});
	}

	@Override
	public UpdateResponse deleteById(String id) {
		return deleteById(solrCore, id);
	}

	@Override
	public UpdateResponse deleteById(String coreName, final String id) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		Assert.notNull(id, "Cannot delete 'null' id.");
		return execute(coreName, new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.deleteById(id);
			}
		});
	}

	@Override
	public UpdateResponse deleteById(Collection<String> ids) {
		return deleteById(solrCore, ids);
	}

	@Override
	public UpdateResponse deleteById(String coreName, Collection<String> ids) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		Assert.notNull(ids, "Cannot delete 'null' collection.");
		final List<String> toBeDeleted = new ArrayList<String>(ids);
		return execute(coreName, new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.deleteById(toBeDeleted);
			}
		});
	}

	@Override
	public <T> T queryForObject(Query query, Class<T> clazz) {
		return queryForObject(query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> T queryForObject(String coreName, Query query, Class<T> clazz) {
		return queryForObject(coreName, query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> T queryForObject(Query query, Class<T> clazz, RequestMethod method) {
		String coreName = SolrClientUtils.resolveSolrCoreName(clazz, solrCore);
		return queryForObject(coreName, query, clazz, method);
	}

	@Override
	public <T> T queryForObject(String coreName, Query query, Class<T> clazz, RequestMethod method) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		query.setPageRequest(new PageRequest(0, 1));
		QueryResponse response = query(coreName, query, clazz, method);

		if (response.getResults().size() > 0) {
			if (response.getResults().size() > 1) {
				LOGGER.warn("More than 1 result found for singe result query ('{}'), returning first entry in list");
			}
			return (T) convertSolrDocumentListToBeans(response.getResults(), clazz).get(0);
		}
		return null;
	}

	private <T> SolrResultPage<T> doQueryForPage(String coreName, Query query, Class<T> clazz,
			RequestMethod requestMethod) {

		QueryResponse response = null;
		NamedObjectsQuery namedObjectsQuery = new NamedObjectsQuery(query);
		response = query(coreName, namedObjectsQuery, clazz, requestMethod != null ? requestMethod : getDefaultRequestMethod());
		Map<String, Object> objectsName = namedObjectsQuery.getNamesAssociation();

		return createSolrResultPage(query, clazz, response, objectsName);
	}

	@Override
	public <T> ScoredPage<T> queryForPage(Query query, Class<T> clazz) {
		return queryForPage(query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> ScoredPage<T> queryForPage(String coreName, Query query, Class<T> clazz) {
		return queryForPage(coreName, query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> ScoredPage<T> queryForPage(Query query, Class<T> clazz, RequestMethod method) {
		String coreName = SolrClientUtils.resolveSolrCoreName(clazz, solrCore);
		return queryForPage(coreName, query, clazz, method);
	}

	@Override
	public <T> ScoredPage<T> queryForPage(String coreName, Query query, Class<T> clazz, RequestMethod method) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");
		return doQueryForPage(coreName, query, clazz, method);
	}

	@Override
	public <T> GroupPage<T> queryForGroupPage(Query query, Class<T> clazz) {
		return queryForGroupPage(query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> GroupPage<T> queryForGroupPage(String coreName, Query query, Class<T> clazz) {
		return queryForGroupPage(coreName, query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> GroupPage<T> queryForGroupPage(Query query, Class<T> clazz, RequestMethod method) {
		String coreName = SolrClientUtils.resolveSolrCoreName(clazz, solrCore);
		return queryForGroupPage(coreName, query, clazz, method);
	}

	@Override
	public <T> GroupPage<T> queryForGroupPage(String coreName, Query query, Class<T> clazz, RequestMethod method) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");
		return doQueryForPage(coreName, query, clazz, method);
	}

	@Override
	public <T> StatsPage<T> queryForStatsPage(Query query, Class<T> clazz) {
		return queryForStatsPage(query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> StatsPage<T> queryForStatsPage(String coreName, Query query, Class<T> clazz) {
		return queryForStatsPage(coreName, query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> StatsPage<T> queryForStatsPage(Query query, Class<T> clazz, RequestMethod method) {
		String coreName = SolrClientUtils.resolveSolrCoreName(clazz, solrCore);
		return queryForStatsPage(coreName, query, clazz, method);
	}

	@Override
	public <T> StatsPage<T> queryForStatsPage(String coreName, Query query, Class<T> clazz, RequestMethod method) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");
		Assert.notNull(method, "Method class must not be 'null'.");
		return doQueryForPage(coreName, query, clazz, method);
	}

	@Override
	public <T> FacetPage<T> queryForFacetPage(FacetQuery query, Class<T> clazz) {
		return queryForFacetPage(query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> FacetPage<T> queryForFacetPage(String coreName, FacetQuery query, Class<T> clazz) {
		return queryForFacetPage(coreName, query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> FacetPage<T> queryForFacetPage(FacetQuery query, Class<T> clazz, RequestMethod method) {
		String coreName = SolrClientUtils.resolveSolrCoreName(clazz, solrCore);
		return queryForFacetPage(coreName, query, clazz, method);
	}

	@Override
	public <T> FacetPage<T> queryForFacetPage(String coreName, FacetQuery query, Class<T> clazz, RequestMethod method) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		NamedObjectsFacetQuery namedObjectsQuery = new NamedObjectsFacetQuery(query);
		QueryResponse response = query(coreName, namedObjectsQuery, clazz, method);
		Map<String, Object> objectsName = namedObjectsQuery.getNamesAssociation();

		SolrResultPage<T> page = createSolrResultPage(query, clazz, response, objectsName);

		page.addAllFacetFieldResultPages(ResultHelper.convertFacetQueryResponseToFacetPageMap(query, response));
		page.addAllFacetPivotFieldResult(ResultHelper.convertFacetQueryResponseToFacetPivotMap(query, response));
		page.addAllRangeFacetFieldResultPages(ResultHelper.convertFacetQueryResponseToRangeFacetPageMap(query, response));
		page.setFacetQueryResultPage(ResultHelper.convertFacetQueryResponseToFacetQueryResult(query, response));

		return page;
	}

	@Override
	public <T> HighlightPage<T> queryForHighlightPage(HighlightQuery query, Class<T> clazz) {
		return queryForHighlightPage(query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> HighlightPage<T> queryForHighlightPage(String coreName, HighlightQuery query, Class<T> clazz) {
		return queryForHighlightPage(coreName, query, clazz, getDefaultRequestMethod());
	}

	@Override
	public <T> HighlightPage<T> queryForHighlightPage(HighlightQuery query, Class<T> clazz, RequestMethod method) {
		String coreName = SolrClientUtils.resolveSolrCoreName(clazz, solrCore);
		return queryForHighlightPage(coreName, query, clazz, method);
	}

	@Override
	public <T> HighlightPage<T> queryForHighlightPage(String coreName, HighlightQuery query, Class<T> clazz,
			RequestMethod method) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		Assert.notNull(query, "Query must not be 'null'.");
		Assert.notNull(clazz, "Target class must not be 'null'.");

		NamedObjectsHighlightQuery namedObjectsQuery = new NamedObjectsHighlightQuery(query);
		QueryResponse response = query(coreName, namedObjectsQuery, clazz, getDefaultRequestMethod());

		Map<String, Object> objectsName = namedObjectsQuery.getNamesAssociation();

		SolrResultPage<T> page = createSolrResultPage(query, clazz, response, objectsName);

		ResultHelper.convertAndAddHighlightQueryResponseToResultPage(response, page);

		return page;
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

		return page;
	}

	@Override
	public TermsPage queryForTermsPage(TermsQuery query) {
		return queryForTermsPage(query, getDefaultRequestMethod());
	}

	@Override
	public TermsPage queryForTermsPage(String coreName, TermsQuery query) {
		return queryForTermsPage(coreName, query, getDefaultRequestMethod());
	}

	@Override
	public TermsPage queryForTermsPage(TermsQuery query, RequestMethod method) {
		return queryForTermsPage(solrCore, query, method);
	}

	@Override
	public TermsPage queryForTermsPage(String coreName, TermsQuery query, RequestMethod method) {
		QueryResponse response = query(coreName, query, null, method);
		TermsResultPage page = new TermsResultPage();
		page.addAllTerms(ResultHelper.convertTermsQueryResponseToTermsMap(response));
		return page;
	}

	final QueryResponse query(SolrDataQuery query, Class<?> clazz) {
		return query(query, clazz, getDefaultRequestMethod());
	}

	final QueryResponse query(String coreName, SolrDataQuery query, Class<?> clazz) {
		return query(coreName, query, clazz, getDefaultRequestMethod());
	}

	final QueryResponse query(SolrDataQuery query, Class<?> clazz, RequestMethod requestMethod) {
		String coreName = SolrClientUtils.resolveSolrCoreName(clazz, solrCore);
		return query(coreName, query, clazz, requestMethod);
	}
	
	final QueryResponse query(String coreName, SolrDataQuery query, Class<?> clazz, RequestMethod requestMethod) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		Assert.notNull(query, "Query must not be 'null'");
		Assert.notNull(requestMethod, "RequestMethod must not be 'null'");

		SolrQuery solrQuery = queryParsers.getForClass(query.getClass()).constructSolrQuery(query);

		if (clazz != null) {
			SolrPersistentEntity<?> persistedEntity = mappingContext.getPersistentEntity(clazz);
			if (persistedEntity.hasScoreProperty()) {
				solrQuery.setIncludeScore(true);
			}
		}

		LOGGER.debug("Executing query '" + solrQuery + "' against {} solr core.", coreName);
		return executeSolrQuery(coreName, solrQuery, getSolrRequestMethod(requestMethod));
	}

	final QueryResponse executeSolrQuery(SolrQuery solrQuery, SolrRequest.METHOD method) {
		return executeSolrQuery(solrCore, solrQuery, method);
	}
	
	final QueryResponse executeSolrQuery(final String coreName, final SolrQuery solrQuery,
			final SolrRequest.METHOD method) {
		return execute(coreName, new SolrCallback<QueryResponse>() {
			@Override
			public QueryResponse doInSolr(SolrClient solrServer) throws SolrServerException, IOException {
				return solrServer.query(solrQuery, method);
			}
		});
	}

	@Override
	public void commit() {
		commit(solrCore);
	}

	@Override
	public void commit(String coreName) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		execute(coreName, new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.commit();
			}
		});
	}

	@Override
	public void softCommit() {
		softCommit(solrCore);
	}

	@Override
	public void softCommit(String coreName) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		if (VersionUtil.isSolr3XAvailable()) {
			throw new UnsupportedOperationException(
					"Soft commit is not available for solr version lower than 4.x - Please check your depdendencies.");
		}
		execute(coreName, new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.commit(true, true, true);
			}
		});
	}

	@Override
	public void rollback() {
		rollback(solrCore);
	}

	@Override
	public void rollback(String coreName) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		execute(coreName, new SolrCallback<UpdateResponse>() {
			@Override
			public UpdateResponse doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				return solrClient.rollback();
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

	/**
	 * @param collectionName
	 * @return
	 * @since 1.3
	 */
	public String getSchemaName(String collectionName) {
		Assert.notNull(collectionName, "Collection name must not be 'null'.");
		return execute(collectionName, new SolrCallback<String>() {
			@Override
			public String doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				SolrJsonResponse response = SolrSchemaRequest.name().process(solrClient);
				if (response != null) {
					return response.getNode("name").asText();
				}
				return null;
			}
		});
	}

	public <T> Cursor<T> queryForCursor(Query query, Class<T> clazz) {
		String coreName = SolrClientUtils.resolveSolrCoreName(clazz, solrCore);
		return queryForCursor(coreName, query, clazz);
	}

	@Override
	public <T> Cursor<T> queryForCursor(final String coreName, Query query, final Class<T> clazz) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		return new DelegatingCursor<T>(queryParsers.getForClass(query.getClass()).constructSolrQuery(query)) {

			@Override
			protected org.springframework.data.solr.core.query.result.DelegatingCursor.PartialResult<T> doLoad(
					SolrQuery nativeQuery) {

				QueryResponse response = executeSolrQuery(coreName, nativeQuery, getSolrRequestMethod(getDefaultRequestMethod()));
				if (response == null) {
					return new PartialResult<T>("", Collections.<T> emptyList());
				}

				return new PartialResult<T>(response.getNextCursorMark(), convertQueryResponseToBeans(response, clazz));
			}

		}.open();
	}

	@Override
	public <T> Collection<T> getById(Collection<? extends Serializable> ids, Class<T> clazz) {
		String coreName = SolrClientUtils.resolveSolrCoreName(clazz, solrCore);
		return getById(coreName, ids, clazz);
	}

	@Override
	public <T> Collection<T> getById(String coreName, final Collection<? extends Serializable> ids, final Class<T> clazz) {
		Assert.notNull(coreName, "Core name must not be 'null'.");
		if (CollectionUtils.isEmpty(ids)) {
			return Collections.emptyList();
		}
		return execute(coreName, new SolrCallback<Collection<T>>() {
			@Override
			public Collection<T> doInSolr(SolrClient solrClient) throws SolrServerException, IOException {
				QueryResponse response = new SolrRealtimeGetRequest(ids).process(solrClient);
				return convertSolrDocumentListToBeans(response.getResults(), clazz);
			}
		});
	}

	@Override
	public <T> T getById(Serializable id, Class<T> clazz) {
		String coreName = SolrClientUtils.resolveSolrCoreName(clazz, solrCore);
		return getById(coreName, id, clazz);
	}

	@Override
	public <T> T getById(String coreName, Serializable id, Class<T> clazz) {
		Assert.notNull(id, "Id must not be 'null'.");
		Collection<T> result = getById(Collections.singletonList(id), clazz);
		if (result.isEmpty()) {
			return null;
		}
		return result.iterator().next();
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

	@Override
	public final SolrClient getSolrClient() {
		return solrClientFactory.getSolrClient(this.solrCore);
	}

	@Override
	public final SolrClient getSolrClient(String coreName) {
		return solrClientFactory.getSolrClient(coreName);
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
