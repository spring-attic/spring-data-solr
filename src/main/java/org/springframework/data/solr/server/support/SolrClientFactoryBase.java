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
package org.springframework.data.solr.server.support;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.solr.server.SolrClientFactory;

/**
 * {@link SolrClientFactoryBase} replaces SolrServerFactoryBase from version 1.x.
 * 
 * @author Christoph Strobl
 * @since 2.0
 */
abstract class SolrClientFactoryBase implements SolrClientFactory, DisposableBean {

	private SolrClient solrClient;

	public SolrClientFactoryBase() {

	}

	SolrClientFactoryBase(SolrClient solrClient) {
		this.solrClient = solrClient;
	}

	protected final boolean isHttpSolrClient(SolrClient solrClient) {
		return (solrClient instanceof HttpSolrClient);
	}

	@Override
	public SolrClient getSolrClient() {
		return this.solrClient;
	}

	public void setSolrClient(SolrClient solrClient) {
		this.solrClient = solrClient;
	}

	@Override
	public void destroy() {
		destroy(this.solrClient);
	}

	/**
	 * @param client
	 */
	protected void destroy(SolrClient client) {
		SolrClientUtils.close(client);
	}

}
