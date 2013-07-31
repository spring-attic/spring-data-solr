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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.springframework.data.solr.server.SolrServerFactory;
import org.springframework.util.Assert;

/**
 * The HttpSolrServerFactory configures an {@link HttpSolrServer} to work with the provided core. If provided
 * Credentials eg. (@link UsernamePasswordCredentials} and AuthPolicy (eg. BASIC, DIGEST,...) will be applied to the
 * underlying HttpClient.
 * 
 * @author Christoph Strobl
 */
public class HttpSolrServerFactory extends SolrServerFactoryBase {

	private String core;
	private Credentials credentials;
	private String authPolicy;

	protected HttpSolrServerFactory() {

	}

	public HttpSolrServerFactory(SolrServer solrServer) {
		this(solrServer, null);
	}

	public HttpSolrServerFactory(SolrServer solrServer, String core) {
		this(solrServer, core, null, null);
	}

	public HttpSolrServerFactory(SolrServer solrServer, String core, Credentials credentials, String authPolicy) {
		super(solrServer);
		Assert.notNull(solrServer, "SolrServer must not be null");

		if (authPolicy != null) {
			Assert.hasText(authPolicy);
		}

		this.core = core;
		this.credentials = credentials;
		this.authPolicy = authPolicy;

		appendCoreToBaseUrl(this.core, this.getSolrServer());
		appendAuthentication(this.credentials, this.authPolicy, this.getSolrServer());
	}

	@Override
	public List<String> getCores() {
		return this.core != null ? Arrays.asList(this.core) : Collections.<String> emptyList();
	}

	/**
	 * returns the reference solrServer
	 * 
	 * @see SolrServerFactory#getSolrServer()
	 */
	@Override
	public SolrServer getSolrServer(String core) {
		return getSolrServer();
	}

	protected void appendCoreToBaseUrl(String core, SolrServer solrServer) {
		if (StringUtils.isNotEmpty(core) && isHttpSolrServer(solrServer)) {
			HttpSolrServer httpSolrServer = (HttpSolrServer) solrServer;

			String url = SolrServerUtils.appendCoreToBaseUrl(httpSolrServer.getBaseURL(), core);
			httpSolrServer.setBaseURL(url);
		}
	}

	private void appendAuthentication(Credentials credentials, String authPolicy, SolrServer solrServer) {
		if (isHttpSolrServer(solrServer)) {
			HttpSolrServer httpSolrServer = (HttpSolrServer) solrServer;

			if (credentials != null && StringUtils.isNotBlank(authPolicy)
					&& assertHttpClientInstance(httpSolrServer.getHttpClient())) {
				AbstractHttpClient httpClient = (AbstractHttpClient) httpSolrServer.getHttpClient();
				httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY), credentials);
				httpClient.getParams().setParameter(AuthPNames.TARGET_AUTH_PREF, Arrays.asList(authPolicy));
			}
		}
	}

	private boolean assertHttpClientInstance(HttpClient httpClient) {
		Assert.isInstanceOf(AbstractHttpClient.class, httpClient,
				"HttpClient has to be derivate of AbstractHttpClient in order to allow authentication.");
		return true;
	}

}
