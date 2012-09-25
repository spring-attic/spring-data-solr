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
package org.springframework.data.solr.support;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.solr.HttpSolrServerFactory;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 */
public class HttpSolrServerFactoryBean extends HttpSolrServerFactory implements FactoryBean<SolrServer>,
		InitializingBean, DisposableBean {

	private String url;
	private Integer timeout;
	private Integer maxConnections;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(url);
		initSolrServer();
	}

	private void initSolrServer() {
		HttpSolrServer httpSolrServer = new HttpSolrServer(this.url);
		if (timeout != null) {
			httpSolrServer.setConnectionTimeout(timeout.intValue());
		}
		if (maxConnections != null) {
			httpSolrServer.setMaxTotalConnections(maxConnections);
		}
		this.setSolrServer(httpSolrServer);
	}

	@Override
	public SolrServer getObject() throws Exception {
		return getSolrServer();
	}

	@Override
	public Class<?> getObjectType() {
		return HttpSolrServer.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public void setMaxConnections(Integer maxConnections) {
		this.maxConnections = maxConnections;
	}

}
