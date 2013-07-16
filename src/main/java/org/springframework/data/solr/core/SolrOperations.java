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

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.HighlightPage;

import java.util.Collection;

/**
 * Interface that specifies a basic set of Solr operations.
 * 
 * @author Christoph Strobl
 */
public interface SolrOperations {

	/**
	 * Get the underlying SolrServer instance
	 * 
	 * @return
	 */
	SolrServer getSolrServer();

	/**
	 * Execute ping against solrServer and return duration in msec
	 * 
	 * @return
	 */
	SolrPingResponse ping();

	/**
	 * return number of elements found by for given query
	 * 
	 * @param query
	 * @return
	 */
	long count(SolrDataQuery query);

	/**
	 * Execute add operation against solr, which will do either insert or update
	 * 
	 * @param obj
	 * @return
	 */
	UpdateResponse saveBean(Object obj);

	/**
	 * Add a collection of beans to solr, which will do either insert or update
	 * 
	 * @param beans
	 * @return
	 */
	UpdateResponse saveBeans(Collection<?> beans);

	/**
	 * Add a solrj input document to solr, which will do either insert or update
	 * 
	 * @param document
	 * @return
	 */
	UpdateResponse saveDocument(SolrInputDocument document);

	/**
	 * Add multiple solrj input documents to solr, which will do either insert or update
	 * 
	 * @param documents
	 * @return
	 */
	UpdateResponse saveDocuments(Collection<SolrInputDocument> documents);

	/**
	 * Find and delete all objects matching the provided Query
	 * 
	 * @param query
	 * @return
	 */
	UpdateResponse delete(SolrDataQuery query);

	/**
	 * Detele the one object with provided id
	 * 
	 * @param id
	 * @return
	 */
	UpdateResponse deleteById(String id);

	/**
	 * Delete objects with given ids
	 * 
	 * @param id
	 * @return
	 */
	UpdateResponse deleteById(Collection<String> id);

	/**
	 * Execute the query against solr and return the first returned object
	 * 
	 * @param query
	 * @param clazz
	 * @return the first matching object
	 */
	<T> T queryForObject(Query query, Class<T> clazz);

	/**
	 * Execute the query against solr and retrun result as {@link Page}
	 * 
	 * @param query
	 * @param clazz
	 * @return
	 */
	<T> Page<T> queryForPage(Query query, Class<T> clazz);

	/**
	 * Execute a facet query against solr facet result will be returned along with query result within the FacetPage
	 * 
	 * @param query
	 * @param clazz
	 * @return
	 */
	<T> FacetPage<T> queryForFacetPage(FacetQuery query, Class<T> clazz);

	/**
	 * Execute a query and highlight matches in result
	 * 
	 * @param query
	 * @param clazz
	 * @return
	 */
	<T> HighlightPage<T> queryForHighlightPage(HighlightQuery query, Class<T> clazz);

	/**
	 * Send commit command
	 */
	void commit();

	/**
	 * Send commit command with soft commit
	 */
	void commit(Boolean softCommit);

	/**
	 * send rollback command
	 */
	void rollback();

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

}
