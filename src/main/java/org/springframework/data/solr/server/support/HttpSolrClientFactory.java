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
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.lang.Nullable;
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

	private @Nullable Credentials credentials;
	private @Nullable String authPolicy;

	protected HttpSolrClientFactory() {

	}

	public HttpSolrClientFactory(SolrClient solrClient) {
		this(solrClient, null, null);
	}

	public HttpSolrClientFactory(SolrClient solrClient, @Nullable Credentials credentials, @Nullable String authPolicy) {
		super(solrClient);
		Assert.notNull(solrClient, "SolrServer must not be null");

		if (authPolicy != null) {
			Assert.hasText(authPolicy, "AuthPolicy must not be null nor empty!");
		}

		this.credentials = credentials;
		this.authPolicy = authPolicy;

		appendAuthentication(this.credentials, this.authPolicy, this.getSolrClient());
	}

	private void appendAuthentication(Credentials credentials, String authPolicy, SolrClient solrClient) {

		if (isHttpSolrClient(solrClient)) {

			HttpSolrClient httpSolrClient = (HttpSolrClient) solrClient;

			if (credentials != null && StringUtils.isNotBlank(authPolicy)
					&& assertHttpClientInstance(httpSolrClient.getHttpClient())) {

				HttpClient httpClient = httpSolrClient.getHttpClient();

				DirectFieldAccessor df = new DirectFieldAccessor(httpClient);
				CredentialsProvider provider = (CredentialsProvider) df.getPropertyValue("credentialsProvider");

				provider.setCredentials(new AuthScope(AuthScope.ANY), credentials);
			}
		}
	}

	private boolean assertHttpClientInstance(HttpClient httpClient) {
		return httpClient.getClass().getName().contains("InternalHttpClient");
	}

}
