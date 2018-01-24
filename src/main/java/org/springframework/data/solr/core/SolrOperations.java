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

import java.io.Serializable;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.query.FacetAndHighlightQuery;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.core.query.TermsQuery;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.data.solr.core.query.result.FacetAndHighlightPage;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.core.query.result.ScoredPage;
import org.springframework.data.solr.core.query.result.StatsPage;
import org.springframework.data.solr.core.query.result.TermsPage;
import org.springframework.data.solr.core.schema.SchemaOperations;

/**
 * Interface that specifies a basic set of Solr operations.
 *
 * @author Christoph Strobl
 * @author Joachim Uhrlass
 * @author Francisco Spaeth
 * @author Shiradwade Sateesh Krishna
 * @author David Webb
 */
public interface SolrOperations {

	/**
	 * Get the underlying SolrClient instance
	 *
	 * @return
	 */
	SolrClient getSolrClient();

	/**
	 * Execute ping against SolrClient and return duration in msec
	 *
	 * @return
	 */
	SolrPingResponse ping();

	/**
	 * return number of elements found by for given query
	 *
	 * @param query must not be {@literal null}.
	 * @return
	 */
	long count(SolrDataQuery query);

	/**
	 * Return number of elements found in given collection by for given query
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	long count(String collectionName, SolrDataQuery query);

	/**
	 * return number of elements found by for given query
	 *
	 * @param query must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	long count(SolrDataQuery query, RequestMethod method);

	/**
	 * Return number of elements found in collection by for given query.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	long count(String collectionName, SolrDataQuery query, RequestMethod method);

	/**
	 * Execute add operation against solr, which will do either insert or update
	 *
	 * @param obj
	 * @return
	 */
	UpdateResponse saveBean(Object obj);

	/**
	 * Execute add operation against specific collection, which will do either insert or update
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param obj must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	UpdateResponse saveBean(String collectionName, Object obj);

	/**
	 * Execute add operation against solr, which will do either insert or update with support for commitWithin strategy
	 *
	 * @param obj must not be {@literal null}.
	 * @param commitWithinMs
	 * @return
	 */
	UpdateResponse saveBean(Object obj, int commitWithinMs);

	/**
	 * Execute add operation against specific collection, which will do either insert or update with support for
	 * commitWithin strategy.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param objectToAdd must not be {@literal null}.
	 * @param commitWithinMs
	 * @return
	 * @since 2.1
	 */
	UpdateResponse saveBean(String collectionName, Object objectToAdd, int commitWithinMs);

	/**
	 * Add a collection of beans to solr, which will do either insert or update.
	 *
	 * @param beans must not be {@literal null}.
	 * @return
	 */
	UpdateResponse saveBeans(Collection<?> beans);

	/**
	 * Add a collection of beans to specific collection, which will do either insert or update.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param beans must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	UpdateResponse saveBeans(String collectionName, Collection<?> beans);

	/**
	 * Add a collection of beans to solr, which will do either insert or update with support for commitWithin strategy
	 *
	 * @param beans must not be {@literal null}.
	 * @param commitWithinMs
	 * @return
	 */
	UpdateResponse saveBeans(Collection<?> beans, int commitWithinMs);

	/**
	 * Add a collection of beans to specific collection, which will do either insert or update with support for
	 * commitWithin strategy.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param beansToAdd must not be {@literal null}.
	 * @param commitWithinMs
	 * @return
	 * @since 2.1
	 */
	UpdateResponse saveBeans(String collectionName, Collection<?> beansToAdd, int commitWithinMs);

	/**
	 * Add a solrj input document to solr, which will do either insert or update
	 *
	 * @param document must not be {@literal null}.
	 * @return
	 */
	UpdateResponse saveDocument(SolrInputDocument document);

	/**
	 * Add a solrj input document to specific collection, which will do either insert or update.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param document must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	UpdateResponse saveDocument(String collectionName, SolrInputDocument document);

	/**
	 * Add a solrj input document to solr, which will do either insert or update with support for commitWithin strategy
	 *
	 * @param document must not be {@literal null}.
	 * @param commitWithinMs
	 * @return
	 */
	UpdateResponse saveDocument(SolrInputDocument document, int commitWithinMs);

	/**
	 * Add a solrj input document to specific collection, which will do either insert or update with support for
	 * commitWithin strategy
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param documentToAdd must not be {@literal null}.
	 * @param commitWithinMs
	 * @return
	 * @since 2.1
	 */
	UpdateResponse saveDocument(String collectionName, SolrInputDocument documentToAdd, int commitWithinMs);

	/**
	 * Add multiple solrj input documents to solr, which will do either insert or update
	 *
	 * @param documents must not be {@literal null}.
	 * @return
	 */
	UpdateResponse saveDocuments(Collection<SolrInputDocument> documents);

	/**
	 * Add multiple solrj input documents to specific collection, which will do either insert or update.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param documents must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	UpdateResponse saveDocuments(String collectionName, Collection<SolrInputDocument> documents);

	/**
	 * Add multiple solrj input documents to solr, which will do either insert or update with support for commitWithin
	 * strategy
	 *
	 * @param documents must not be {@literal null}.
	 * @return
	 */
	UpdateResponse saveDocuments(Collection<SolrInputDocument> documents, int commitWithinMs);

	/**
	 * Add multiple solrj input documents to specific collection, which will do either insert or update with support for
	 * commitWithin strategy.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param documents must not be {@literal null}.
	 * @param commitWithinMs
	 * @return
	 * @since 2.1
	 */
	UpdateResponse saveDocuments(String collectionName, Collection<SolrInputDocument> documents, int commitWithinMs);

	/**
	 * Find and delete all objects matching the provided Query
	 *
	 * @param query must not be {@literal null}.
	 * @return
	 */
	UpdateResponse delete(SolrDataQuery query);

	/**
	 * Find and delete all objects matching the provided Query in specific collection.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	UpdateResponse delete(String collectionName, SolrDataQuery query);

	/**
	 * Delete the one object with provided id
	 *
	 * @param id must not be {@literal null}.
	 * @return
	 */
	UpdateResponse deleteById(String id);

	/**
	 * Delete the one object with provided id in collection.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param id must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	UpdateResponse deleteById(String collectionName, String id);

	/**
	 * Delete objects with given ids
	 *
	 * @param id must not be {@literal null}.
	 * @return
	 */
	UpdateResponse deleteById(Collection<String> id);

	/**
	 * Delete objects with given ids in collection.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param ids must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	UpdateResponse deleteById(String collectionName, Collection<String> ids);

	/**
	 * Execute the query against solr and return the first returned object
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return the first matching object
	 */
	<T> T queryForObject(Query query, Class<T> clazz);

	/**
	 * Execute the query against specific collection and return the first returned object.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> T queryForObject(String collectionName, Query query, Class<T> clazz);

	/**
	 * Execute the query against solr and return the first returned object
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return the first matching object
	 * @since 2.0
	 */
	<T> T queryForObject(Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against specific collection and return the first returned object.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> T queryForObject(String collectionName, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against solr and return result as {@link Page}.
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 */
	<T> ScoredPage<T> queryForPage(Query query, Class<T> clazz);

	/**
	 * Execute the query against specific collection and return result as {@link Page}.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> ScoredPage<T> queryForPage(String collectionName, Query query, Class<T> clazz);

	/**
	 * Execute the query against solr and return result as {@link Page}
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	<T> ScoredPage<T> queryForPage(Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against specific collection and return result as {@link Page}
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> ScoredPage<T> queryForPage(String collectionName, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute a facet query against solr facet result will be returned along with query result within the FacetPage
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 */
	<T> FacetPage<T> queryForFacetPage(FacetQuery query, Class<T> clazz);

	/**
	 * Execute a facet query against specific collection. The facet result will be returned along with query result within
	 * the {@link FacetPage}.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> FacetPage<T> queryForFacetPage(String collectionName, FacetQuery query, Class<T> clazz);

	/**
	 * Execute a facet query against solr facet result will be returned along with query result within the
	 * {@link FacetPage#}
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	<T> FacetPage<T> queryForFacetPage(FacetQuery query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute a facet query against specific collection. The facet result will be returned along with query result within
	 * the {@link FacetPage}.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> FacetPage<T> queryForFacetPage(String collectionName, FacetQuery query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute a query and highlight matches in result
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 */
	<T> HighlightPage<T> queryForHighlightPage(HighlightQuery query, Class<T> clazz);

	/**
	 * Execute a query and highlight matches in result within a specific collection.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> HighlightPage<T> queryForHighlightPage(String collectionName, HighlightQuery query, Class<T> clazz);

	/**
	 * Execute a query and highlight matches in result.
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	<T> HighlightPage<T> queryForHighlightPage(HighlightQuery query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute a query and highlight matches in result
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> HighlightPage<T> queryForHighlightPage(String collectionName, HighlightQuery query, Class<T> clazz,
			RequestMethod method);

	/**
	 * Execute a query and highlight matches in result
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(FacetAndHighlightQuery query, Class<T> clazz);

	/**
	 * Execute a query against specific collection and highlight matches in result.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(String collectionName, FacetAndHighlightQuery query,
			Class<T> clazz);

	/**
	 * Execute a query and highlight matches in result
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(FacetAndHighlightQuery query, Class<T> clazz,
			RequestMethod method);

	/**
	 * Execute a query against specific collection and highlight matches in result.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(String collectionName, FacetAndHighlightQuery query,
			Class<T> clazz, RequestMethod method);

	/**
	 * Execute query using terms handler
	 *
	 * @param query must not be {@literal null}.
	 * @return
	 */
	TermsPage queryForTermsPage(TermsQuery query);

	/**
	 * Execute query using terms handler against given collection.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	TermsPage queryForTermsPage(String collectionName, TermsQuery query);

	/**
	 * Execute query using terms handler
	 *
	 * @param query must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	TermsPage queryForTermsPage(TermsQuery query, RequestMethod method);

	/**
	 * Execute query using terms handler against given collection.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	TermsPage queryForTermsPage(String collectionName, TermsQuery query, RequestMethod method);

	/**
	 * Executes the given {@link Query} and returns an open {@link Cursor} allowing to iterate of results, dynamically
	 * fetching additional ones if required.
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 1.3
	 */
	<T> Cursor<T> queryForCursor(Query query, Class<T> clazz);

	/**
	 * Execute the query against solr and return result as {@link GroupPage}
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 1.4
	 */
	<T> GroupPage<T> queryForGroupPage(Query query, Class<T> clazz);

	/**
	 * Execute the query against specific collection and return result as {@link GroupPage}.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> GroupPage<T> queryForGroupPage(String collectionName, Query query, Class<T> clazz);

	/**
	 * Execute the query against solr and return result as {@link GroupPage}.
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	<T> GroupPage<T> queryForGroupPage(Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against specific collection and return result as {@link GroupPage}
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 */
	<T> GroupPage<T> queryForGroupPage(String collectionName, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against Solr and return result as {@link StatsPage}.
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @size 1.4
	 */
	<T> StatsPage<T> queryForStatsPage(Query query, Class<T> clazz);

	/**
	 * Execute the query against specific collection and return result as {@link StatsPage}.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> StatsPage<T> queryForStatsPage(String collectionName, Query query, Class<T> clazz);

	/**
	 * Execute the query against Solr and return result as {@link StatsPage}.
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	<T> StatsPage<T> queryForStatsPage(Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against specific collection and return result as {@link StatsPage}.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> StatsPage<T> queryForStatsPage(String collectionName, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against Solr and return result as page.
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 2.1
	 */
	<T, S extends Page<T>> S query(Query query, Class<T> clazz);

	/**
	 * Execute the query against Solr and return result as page.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 2.1
	 */
	<T, S extends Page<T>> S query(String collectionName, Query query, Class<T> clazz);


	/**
	 * Execute the query against Solr and return result as page.
	 *
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 2.1
	 */
	<T, S extends Page<T>> S query(Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against Solr and return result as page.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 2.1
	 */
	<T, S extends Page<T>> S query(String collectionName, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Executes a realtime get using given id.
	 *
	 * @param id must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 1.4
	 */
	<T> T getById(Serializable id, Class<T> clazz);

	/**
	 * Executes a realtime get on given collection using given id.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param id must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> T getById(String collectionName, Serializable id, Class<T> clazz);

	/**
	 * Executes a realtime get using given ids.
	 *
	 * @param ids must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 1.4
	 */
	<T> Collection<T> getById(Collection<? extends Serializable> ids, Class<T> clazz);

	/**
	 * Executes a realtime get on given collection using given ids.
	 *
	 * @param collectionName must not be {@literal null}.
	 * @param ids must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> Collection<T> getById(String collectionName, Collection<? extends Serializable> ids, Class<T> clazz);

	/**
	 * Send commit command {@link SolrClient#commit()}
	 */
	void commit();

	/**
	 * @param collectionName must not be {@literal null}.
	 * @since 2.1
	 */
	void commit(String collectionName);

	/**
	 * Send soft commmit command {@link SolrClient#commit(boolean, boolean, boolean)}
	 */
	void softCommit();

	/**
	 * @param collectionName must not be {@literal null}.
	 * @since 2.1
	 */
	void softCommit(String collectionName);

	/**
	 * send rollback command {@link SolrClient#rollback()}
	 */
	void rollback();

	/**
	 * @param collectionName must not be {@literal null}.
	 * @since 2.1
	 */
	void rollback(String collectionName);

	/**
	 * Convert given bean into a solrj InputDocument
	 *
	 * @param bean
	 * @return
	 */
	SolrInputDocument convertBeanToSolrInputDocument(Object bean);

	/**
	 * @return Converter in use
	 */
	SolrConverter getConverter();

	/**
	 * Execute action within callback
	 *
	 * @param action
	 * @return
	 */
	<T> T execute(SolrCallback<T> action);

	/**
	 * Execute action within callback on a given collection.
	 *
	 * @param collection must not be {@literal null}.
	 * @param action must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> T execute(String collection, CollectionCallback<T> action);

	/**
	 * Get the {@link SchemaOperations} executable.
	 *
	 * @param collection
	 * @return never {@literal null}.
	 * @since 2.1
	 */
	SchemaOperations getSchemaOperations(String collection);

}
