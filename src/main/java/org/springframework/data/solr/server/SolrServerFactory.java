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
package org.springframework.data.solr.server;

import java.util.List;

import org.apache.solr.client.solrj.SolrServer;

/**
 * SolrServerFactory provides access to the solrj {@link SolrServer}.
 * 
 * @author Christoph Strobl
 */
public interface SolrServerFactory {

	/**
	 * Get base {@link SolrServer} instance
	 * 
	 * @return a reusable solrServer instance
	 */
	SolrServer getSolrServer();

	/**
	 * Get configured {@link SolrServer} for specific core
	 * 
	 * @param core
	 * @return
	 */
	SolrServer getSolrServer(String core);

	/**
	 * Get list of cores
	 * 
	 * @return empty list if no cores defined
	 */
	List<String> getCores();

}
