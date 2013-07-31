/*
 * Copyright 2012 - 2013 the original author or authors.
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

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.solr.VersionUtil;
import org.springframework.data.solr.server.SolrServerFactory;

/**
 * @author Christoph Strobl
 */
abstract class SolrServerFactoryBase implements SolrServerFactory, DisposableBean {

	private SolrServer solrServer;

	public SolrServerFactoryBase() {

	}

	SolrServerFactoryBase(SolrServer solrServer) {
		this.solrServer = solrServer;
	}

	protected final boolean isHttpSolrServer(SolrServer solrServer) {
		return (solrServer instanceof HttpSolrServer);
	}

	@Override
	public SolrServer getSolrServer() {
		return this.solrServer;
	}

	public void setSolrServer(SolrServer solrServer) {
		this.solrServer = solrServer;
	}

	@Override
	public void destroy() {
		destroy(this.solrServer);
	}

	/**
	 * @param server
	 */
	protected void destroy(SolrServer server) {
		if (solrServer instanceof HttpSolrServer) {
			((HttpSolrServer) solrServer).shutdown();
		} else if (solrServer instanceof LBHttpSolrServer) {
			((LBHttpSolrServer) solrServer).shutdown();
		} else {
			if (VersionUtil.isSolr4XAvailable()) {
				if (solrServer instanceof CloudSolrServer) {
					((CloudSolrServer) solrServer).shutdown();
				}
			}
		}
	}

}
