/*
 * Copyright 2016 the original author or authors.
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
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.core.query.TermsQuery;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.core.query.result.ScoredPage;
import org.springframework.data.solr.core.query.result.StatsPage;
import org.springframework.data.solr.core.query.result.TermsPage;

/**
 * Interface that specifies a set of multicore Solr operations.
 * 
 * @author Venil Noronha
 */
public interface MulticoreSolrOperations extends SolrOperations {

	/**
	 * Get the SolrClient instance for the given solr core.
	 * 
	 * @param coreName
	 * 
	 * @return
	 */
	SolrClient getSolrClient(String coreName);

	/**
	 * Execute ping against the given solr core and return duration in msec.
	 * 
	 * @param coreName
	 * 
	 * @return
	 */
	SolrPingResponse ping(String coreName);

	/**
	 * Return number of elements found by for given query against the given
	 * solr core.
	 * 
	 * @param coreName
	 * @param query
	 * 
	 * @return
	 */
	long count(String coreName, SolrDataQuery query);

	/**
	 * Return number of elements found by for given query against the given
	 * solr core.
	 * 
	 * @param coreName
	 * @param query
	 * @param method
	 * 
	 * @return
	 */
	long count(String coreName, SolrDataQuery query, RequestMethod method);

	/**
	 * Execute add operation against the given solr core, which will do either
	 * insert or update.
	 * 
	 * @param coreName
	 * @param obj
	 * 
	 * @return
	 */
	UpdateResponse saveBean(String coreName, Object obj);

	/**
	 * Execute add operation against the given solr core, which will do either
	 * insert or update with support for commitWithin strategy.
	 * 
	 * @param coreName
	 * @param obj
	 * @param commitWithinMs
	 * 
	 * @return
	 */
	UpdateResponse saveBean(String coreName, Object obj, int commitWithinMs);

	/**
	 * Add a collection of beans to the given solr core, which will do either
	 * insert or update.
	 * 
	 * @param coreName
	 * @param beans
	 * 
	 * @return
	 */
	UpdateResponse saveBeans(String coreName, Collection<?> beans);

	/**
	 * Add a collection of beans to the given solr core, which will do either
	 * insert or update with support for commitWithin strategy.
	 * 
	 * @param coreName
	 * @param beans
	 * @param commitWithinMs
	 * 
	 * @return
	 */
	UpdateResponse saveBeans(String coreName, Collection<?> beans, int commitWithinMs);

	/**
	 * Add a solrj input document to the given solr core, which will do either
	 * insert or update.
	 * 
	 * @param coreName
	 * @param document
	 * 
	 * @return
	 */
	UpdateResponse saveDocument(String coreName, SolrInputDocument document);

	/**
	 * Add a solrj input document to the given solr core, which will do either
	 * insert or update with support for commitWithin strategy.
	 * 
	 * @param coreName
	 * @param document
	 * @param commitWithinMs
	 * 
	 * @return
	 */
	UpdateResponse saveDocument(String coreName, SolrInputDocument document, int commitWithinMs);

	/**
	 * Add multiple solrj input documents to the given solr core, which will do
	 * either insert or update.
	 * 
	 * @param coreName
	 * @param documents
	 * 
	 * @return
	 */
	UpdateResponse saveDocuments(String coreName, Collection<SolrInputDocument> documents);

	/**
	 * Add multiple solrj input documents to the given solr core, which will do
	 * either insert or update with support for commitWithin strategy.
	 * 
	 * @param coreName
	 * @param documents
	 * @param commitWithinMs
	 * 
	 * @return
	 */
	UpdateResponse saveDocuments(String coreName, Collection<SolrInputDocument> documents, int commitWithinMs);

	/**
	 * Find and delete all objects matching the provided Query from the given
	 * solr core.
	 * 
	 * @param coreName
	 * @param query
	 * 
	 * @return
	 */
	UpdateResponse delete(String coreName, SolrDataQuery query);

	/**
	 * Delete the one object with provided id from the given solr core.
	 * 
	 * @param coreName
	 * @param id
	 * 
	 * @return
	 */
	UpdateResponse deleteById(String coreName, String id);

	/**
	 * Delete objects with given ids from the given solr core.
	 * 
	 * @param coreName
	 * @param ids
	 * 
	 * @return
	 */
	UpdateResponse deleteById(String coreName, Collection<String> ids);

	/**
	 * Execute the query against the given solr core and return the
	 * first returned object.
	 * 
	 * @param coreName
	 * @param query
	 * @param clazz
	 * 
	 * @return the first matching object
	 */
	<T> T queryForObject(String coreName, Query query, Class<T> clazz);

	/**
	 * Execute the query against the given solr core and return the
	 * first returned object.
	 * 
	 * @param coreName
	 * @param query
	 * @param clazz
	 * @param method
	 * 
	 * @return the first matching object
	 */
	<T> T queryForObject(String coreName, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against the given solr core and return result
	 * as Page.
	 * 
	 * @param coreName
	 * @param query
	 * @param clazz
	 * 
	 * @return
	 */
	<T> ScoredPage<T> queryForPage(String coreName, Query query, Class<T> clazz);

	/**
	 * Execute the query against the given solr core and return result
	 * as Page.
	 *
	 * @param coreName
	 * @param query
	 * @param clazz
	 * @param method
	 * 
	 * @return
	 */
	<T> ScoredPage<T> queryForPage(String coreName, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute a facet query against the given solr core. Facet result
	 * will be returned along with query result within the FacetPage.
	 * 
	 * @param coreName
	 * @param query
	 * @param clazz
	 * 
	 * @return
	 */
	<T> FacetPage<T> queryForFacetPage(String coreName, FacetQuery query, Class<T> clazz);

	/**
	 * Execute a facet query against the given solr core. Facet result
	 * will be returned along with query result within the FacetPage.
	 * 
	 * @param coreName
	 * @param query
	 * @param clazz
	 * @param method
	 * 
	 * @return
	 */
	<T> FacetPage<T> queryForFacetPage(String coreName, FacetQuery query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute a query against the given solr core and highlight matches
	 * in result.
	 * 
	 * @param coreName
	 * @param query
	 * @param clazz
	 * 
	 * @return
	 */
	<T> HighlightPage<T> queryForHighlightPage(String coreName, HighlightQuery query, Class<T> clazz);

	/**
	 * Execute a query against the given solr core and highlight matches
	 * in result.
	 * 
	 * @param coreName
	 * @param query
	 * @param clazz
	 * @param method
	 * 
	 * @return
	 */
	<T> HighlightPage<T> queryForHighlightPage(String coreName, HighlightQuery query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute query against the given solr core using terms handler.
	 * 
	 * @param coreName
	 * @param query
	 * 
	 * @return
	 */
	TermsPage queryForTermsPage(String coreName, TermsQuery query);

	/**
	 * Execute query against the given solr core using terms handler.
	 * 
	 * @param coreName
	 * @param query
	 * @param method
	 * 
	 * @return
	 */
	TermsPage queryForTermsPage(String coreName, TermsQuery query, RequestMethod method);

	/**
	 * Executes the given Query against the given solr core and returns an
	 * open Cursor allowing to iterate of results, dynamically fetching
	 * additional ones if required.
	 * 
	 * @param coreName
	 * @param query
	 * @param clazz
	 * 
	 * @return
	 */
	<T> Cursor<T> queryForCursor(String coreName, Query query, Class<T> clazz);

	/**
	 * Execute the query against the given solr core and return result as
	 * GroupPage.
	 * 
	 * @param coreName
	 * @param query
	 * @param clazz
	 * 
	 * @return
	 */
	<T> GroupPage<T> queryForGroupPage(String coreName, Query query, Class<T> clazz);

	/**
	 * Execute the query against the given solr core and return result as
	 * GroupPage.
	 * 
	 * @param coreName
	 * @param query
	 * @param clazz
	 * @param method
	 * 
	 * @return
	 */
	<T> GroupPage<T> queryForGroupPage(String coreName, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Execute the query against the given solr core and return result as
	 * StatsPage.
	 * 
	 * @param coreName
	 * @param query
	 * @param clazz
	 * 
	 * @return
	 */
	<T> StatsPage<T> queryForStatsPage(String coreName, Query query, Class<T> clazz);

	/**
	 * Execute the query against the given solr core and return result as
	 * StatsPage.
	 * 
	 * @param coreName
	 * @param query
	 * @param clazz
	 * @param method
	 * 
	 * @return
	 */
	<T> StatsPage<T> queryForStatsPage(String coreName, Query query, Class<T> clazz, RequestMethod method);

	/**
	 * Executes a realtime get against the given solr core using given id.
	 * 
	 * @param coreName
	 * @param id
	 * @param clazz
	 * 
	 * @return
	 */
	<T> T getById(String coreName, Serializable id, Class<T> clazz);

	/**
	 * Executes a realtime get against the given solr core using given ids.
	 * 
	 * @param coreName
	 * @param ids
	 * @param clazz
	 * 
	 * @return
	 */
	<T> Collection<T> getById(String coreName, Collection<? extends Serializable> ids, Class<T> clazz);

	/**
	 * Send commit command to the given solr core
	 * 
	 * @param coreName
	 */
	void commit(String coreName);

	/**
	 * Send soft commit command to the given solr core.
	 * 
	 * @param coreName
	 */
	void softCommit(String coreName);

	/**
	 * Send rollback command to the given solr core.
	 * 
	 * @param coreName
	 */
	void rollback(String coreName);

	/**
	 * Execute action within callback against the given solr core.
	 * 
	 * @param coreName
	 * @param action
	 * 
	 * @return
	 */
	<T> T execute(String coreName, SolrCallback<T> action);

}
