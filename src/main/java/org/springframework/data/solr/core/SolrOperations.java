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
import java.util.Collection;
import java.util.Optional;

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
	 * @return the {@link SolrClient} in use. Never {@literal null}.
	 */
	SolrClient getSolrClient();

	/**
	 * Execute ping against SolrClient and return duration in msec
	 *
	 * @return {@link SolrPingResponse} containing ping result.
	 * @throws org.springframework.dao.DataAccessResourceFailureException if ping fails.
	 */
	SolrPingResponse ping();

	/**
	 * Execute ping against SolrClient and return duration in msec
	 *
	 * @param collection must not be {@literal null}.
	 * @return {@link SolrPingResponse} containing ping result.
	 * @throws org.springframework.dao.DataAccessResourceFailureException if ping fails.
	 * @since 3.0
	 */
	SolrPingResponse ping(String collection);

	/**
	 * return number of elements found by for given query
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @return total number of documents matching given query.
	 * @since 3.0
	 */
	long count(String collection, SolrDataQuery query);

	/**
	 * return number of elements found by for given query
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return total number of documents matching given query.
	 * @since 3.0
	 */
	long count(String collection, SolrDataQuery query, RequestMethod method);

	/**
	 * Execute add operation against solr, which will do either insert or update.
	 *
	 * @param collection must not be {@literal null}.
	 * @param obj must not be {@literal null}.
	 * @return {@link UpdateResponse} containing update result.
	 * @since 3.0
	 */
	default UpdateResponse saveBean(String collection, Object obj) {
		return saveBean(collection, obj, Duration.ZERO);
	}

	/**
	 * Execute add operation against solr, which will do either insert or update with support for commitWithin strategy.
	 *
	 * @param collection must not be {@literal null}.
	 * @param obj must not be {@literal null}.
	 * @param commitWithin max time within server performs commit.
	 * @return {@link UpdateResponse} containing update result.
	 */
	UpdateResponse saveBean(String collection, Object obj, Duration commitWithin);

	/**
	 * Add a collection of beans to solr, which will do either insert or update.
	 *
	 * @param collection must not be {@literal null}.
	 * @param beans must not be {@literal null}.
	 * @return {@link UpdateResponse} containing update result.
	 * @since 3.0
	 */
	default UpdateResponse saveBeans(String collection, Collection<?> beans) {
		return saveBeans(collection, beans, Duration.ZERO);
	}

	/**
	 * Add a collection of beans to solr, which will do either insert or update with support for commitWithin strategy.
	 *
	 * @param collection must not be {@literal null}.
	 * @param beans must not be {@literal null}.
	 * @param commitWithin max time within server performs commit.
	 * @return {@link UpdateResponse} containing update result.
	 * @since 3.0
	 */
	UpdateResponse saveBeans(String collection, Collection<?> beans, Duration commitWithin);

	/**
	 * Add a solrj input document to solr, which will do either insert or update
	 *
	 * @param collection must not be {@literal null}.
	 * @param document must not be {@literal null}.
	 * @return {@link UpdateResponse} containing update result.
	 */
	default UpdateResponse saveDocument(String collection, SolrInputDocument document) {
		return saveDocument(collection, document, Duration.ZERO);
	}

	/**
	 * Add a solrj input document to solr, which will do either insert or update with support for commitWithin strategy
	 *
	 * @param document must not be {@literal null}.
	 * @param commitWithin must not be {@literal null}.
	 * @return {@link UpdateResponse} containing update result.
	 * @since 3.0
	 */
	UpdateResponse saveDocument(String collection, SolrInputDocument document, Duration commitWithin);

	/**
	 * Add multiple solrj input documents to solr, which will do either insert or update
	 *
	 * @param collection must not be {@literal null}.
	 * @param documents must not be {@literal null}.
	 * @return {@link UpdateResponse} containing update result.
	 * @since 3.0
	 */
	default UpdateResponse saveDocuments(String collection, Collection<SolrInputDocument> documents) {
		return saveDocuments(collection, documents, Duration.ZERO);
	}

	/**
	 * Add multiple solrj input documents to solr, which will do either insert or update with support for commitWithin
	 * strategy.
	 *
	 * @param collection must not be {@literal null}.
	 * @param documents must not be {@literal null}.
	 * @param commitWithin max time within server performs commit.
	 * @return {@link UpdateResponse} containing update result.
	 * @since 3.0
	 */
	UpdateResponse saveDocuments(String collection, Collection<SolrInputDocument> documents, Duration commitWithin);

	/**
	 * Find and delete all objects matching the provided Query.
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @return {@link UpdateResponse} containing delete result.
	 * @since 3.0
	 */
	UpdateResponse delete(String collection, SolrDataQuery query);

	/**
	 * Detele the one object with provided id.
	 *
	 * @param collection must not be {@literal null}.
	 * @param id must not be {@literal null}.
	 * @return {@link UpdateResponse} containing delete result.
	 * @since 3.0
	 */
	UpdateResponse deleteByIds(String collection, String id);

	/**
	 * Delete objects with given ids
	 *
	 * @param collection must not be {@literal null}.
	 * @param id must not be {@literal null}.
	 * @return {@link UpdateResponse} containing delete result.
	 * @since 3.0
	 */
	UpdateResponse deleteByIds(String collection, Collection<String> id);

	/**
	 * Execute the query against solr and return the first returned object
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return the first matching object
	 * @since 3.0
	 */
	<T> Optional<T> queryForObject(String collection, Query query, Class<T> clazz);

	/**
	 * Execute the query against solr and return the first returned object
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return the first matching object
	 * @since 3.0
	 */
	<T> Optional<T> queryForObject(String collection, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against solr and retrun result as {@link Page}
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T> ScoredPage<T> queryForPage(String collection, Query query, Class<T> clazz);

	/**
	 * Execute the query against solr and retrun result as {@link Page}
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T> ScoredPage<T> queryForPage(String collection, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute a facet query against solr facet result will be returned along with query result within the FacetPage
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T> FacetPage<T> queryForFacetPage(String collection, FacetQuery query, Class<T> clazz);

	/**
	 * Execute a facet query against solr facet result will be returned along with query result within the FacetPage
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T> FacetPage<T> queryForFacetPage(String collection, FacetQuery query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute a query and highlight matches in result
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T> HighlightPage<T> queryForHighlightPage(String collection, HighlightQuery query, Class<T> clazz);

	/**
	 * Execute a query and highlight matches in result
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T> HighlightPage<T> queryForHighlightPage(String collection, HighlightQuery query, Class<T> clazz,
			RequestMethod method);

	/**
	 * Execute a query and highlight matches in result
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(String collection, FacetAndHighlightQuery query,
			Class<T> clazz);

	/**
	 * Execute a query and highlight matches in result
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T> FacetAndHighlightPage<T> queryForFacetAndHighlightPage(String collection, FacetAndHighlightQuery query,
			Class<T> clazz, RequestMethod method);

	/**
	 * Execute query using terms handler
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	TermsPage queryForTermsPage(String collection, TermsQuery query);

	/**
	 * Execute query using terms handler
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	TermsPage queryForTermsPage(String collection, TermsQuery query, RequestMethod method);

	/**
	 * Executes the given {@link Query} and returns an open {@link Cursor} allowing to iterate of results, dynamically
	 * fetching additional ones if required.
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T> Cursor<T> queryForCursor(String collection, Query query, Class<T> clazz);

	/**
	 * Execute the query against solr and return result as {@link GroupPage}
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T> GroupPage<T> queryForGroupPage(String collection, Query query, Class<T> clazz);

	/**
	 * Execute the query against solr and return result as {@link GroupPage}
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T> GroupPage<T> queryForGroupPage(String collection, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against Solr and return result as {@link StatsPage}.
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return never {@literal null}.
	 * @size 3.0
	 */
	<T> StatsPage<T> queryForStatsPage(String collection, Query query, Class<T> clazz);

	/**
	 * Execute the query against Solr and return result as {@link StatsPage}.
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T> StatsPage<T> queryForStatsPage(String collection, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against Solr and return result as page.
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T, S extends Page<T>> S query(String collection, Query query, Class<T> clazz);

	/**
	 * Execute the query against Solr and return result as page.
	 *
	 * @param collection must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 3.0
	 */
	<T, S extends Page<T>> S query(String collection, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Executes a realtime get using given id.
	 *
	 * @param collection must not be {@literal null}.
	 * @param id must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 3.0
	 */
	<T> Optional<T> getById(String collection, Object id, Class<T> clazz);

	/**
	 * Executes a realtime get using given ids.
	 *
	 * @param collection must not be {@literal null}.
	 * @param ids must not be {@literal null}.
	 * @param clazz must not be {@literal null}.
	 * @return
	 * @since 3.0
	 */
	<T> Collection<T> getByIds(String collection, Collection<?> ids, Class<T> clazz);

	/**
	 * Send commit command {@link SolrClient#commit()}
	 *
	 * @since 3.0
	 */
	void commit(String collection);

	/**
	 * Send soft commmit command {@link SolrClient#commit(boolean, boolean, boolean)}
	 *
	 * @since 3.9
	 */
	void softCommit(String collection);

	/**
	 * send rollback command {@link SolrClient#rollback()}
	 *
	 * @since 3.0
	 */
	void rollback(String collection);

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
	 * Get the {@link SchemaOperations} executable.
	 *
	 * @param collection
	 * @return never {@literal null}.
	 * @since 2.1
	 */
	SchemaOperations getSchemaOperations(String collection);

}
