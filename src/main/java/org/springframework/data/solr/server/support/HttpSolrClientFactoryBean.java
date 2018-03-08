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
package org.springframework.data.solr.server.support;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link HttpSolrClientFactoryBean} replaces HttpSolrServerFactoryBean from version 1.x.
 *
 * @author Christoph Strobl
 * @since 2.0
 */
public class HttpSolrClientFactoryBean extends HttpSolrClientFactory
		implements FactoryBean<SolrClient>, InitializingBean, DisposableBean {

	private static final String SERVER_URL_SEPARATOR = ",";
	private @Nullable String url;
	private @Nullable Integer timeout;
	private @Nullable Integer maxConnections;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(url, "Solr url must not be null nor empty!");
		initSolrClient();
	}

	private void initSolrClient() {
		if (this.url.contains(SERVER_URL_SEPARATOR)) {
			createLoadBalancedHttpSolrClient();
		} else {
			createHttpSolrClient();
		}
	}

	private void createHttpSolrClient() {

		HttpSolrClient.Builder builder = new HttpSolrClient.Builder().withBaseSolrUrl(this.url);

		if (timeout != null) {
			builder = builder.withConnectionTimeout(timeout);
		}

		if (maxConnections != null) {

			ModifiableSolrParams params = new ModifiableSolrParams();
			params.set(HttpClientUtil.PROP_MAX_CONNECTIONS, maxConnections);

			builder.withHttpClient(HttpClientUtil.createClient(params));
		}

		this.setSolrClient(builder.build());
	}

	private void createLoadBalancedHttpSolrClient() {

		LBHttpSolrClient.Builder builder = new LBHttpSolrClient.Builder()
				.withBaseSolrUrls(StringUtils.split(this.url, SERVER_URL_SEPARATOR));
		if (timeout != null) {
			builder.withConnectionTimeout(timeout);
		}
		this.setSolrClient(builder.build());
	}

	@Override
	public SolrClient getObject() throws Exception {
		return getSolrClient();
	}

	@Override
	public Class<?> getObjectType() {
		if (getSolrClient() == null) {
			return HttpSolrClient.class;
		}
		return getSolrClient().getClass();
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
