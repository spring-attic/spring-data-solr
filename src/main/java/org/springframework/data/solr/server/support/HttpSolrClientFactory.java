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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.util.Assert;

/**
 * The {@link HttpSolrClientFactory} replaces HttpSolrServerFactory from version 1.x and configures an
 * {@link HttpSolrClient} to work with the provided core. If provided Credentials eg. (@link
 * UsernamePasswordCredentials} and AuthPolicy (eg. BASIC, DIGEST,...) will be applied to the underlying HttpClient.
 * 
 * @author Christoph Strobl
 * @since 2.0
 */
public class HttpSolrClientFactory extends SolrClientFactoryBase {

	private String core;
	private Credentials credentials;
	private String authPolicy;

	protected HttpSolrClientFactory() {

	}

	public HttpSolrClientFactory(SolrClient solrClient) {
		this(solrClient, null);
	}

	public HttpSolrClientFactory(SolrClient solrClient, String core) {
		this(solrClient, core, null, null);
	}

	public HttpSolrClientFactory(SolrClient solrClient, String core, Credentials credentials, String authPolicy) {
		super(solrClient);
		Assert.notNull(solrClient, "SolrServer must not be null");

		if (authPolicy != null) {
			Assert.hasText(authPolicy, "AuthPolicy must not be null nor empty!");
		}

		this.core = core;
		this.credentials = credentials;
		this.authPolicy = authPolicy;

		appendCoreToBaseUrl(this.core, this.getSolrClient());
		appendAuthentication(this.credentials, this.authPolicy, this.getSolrClient());
	}

	@Override
	public List<String> getCores() {
		return this.core != null ? Arrays.asList(this.core) : Collections.<String> emptyList();
	}

	/**
	 * returns the reference {@link SolrClient}
	 * 
	 * @see SolrClientFactory#getSolrClient()
	 */
	@Override
	public SolrClient getSolrClient(String core) {
		return getSolrClient();
	}

	protected void appendCoreToBaseUrl(String core, SolrClient solrClient) {
		if (StringUtils.isNotEmpty(core) && isHttpSolrClient(solrClient)) {
			HttpSolrClient httpSolrClient = (HttpSolrClient) solrClient;

			String url = SolrClientUtils.appendCoreToBaseUrl(httpSolrClient.getBaseURL(), core);
			httpSolrClient.setBaseURL(url);
		}
	}

	private void appendAuthentication(Credentials credentials, String authPolicy, SolrClient solrClient) {
		if (isHttpSolrClient(solrClient)) {
			HttpSolrClient httpSolrClient = (HttpSolrClient) solrClient;

			if (credentials != null && StringUtils.isNotBlank(authPolicy)
					&& assertHttpClientInstance(httpSolrClient.getHttpClient())) {
				AbstractHttpClient httpClient = (AbstractHttpClient) httpSolrClient.getHttpClient();
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
