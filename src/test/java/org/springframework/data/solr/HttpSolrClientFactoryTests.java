/*
 * Copyright 2012 - 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr;

import static org.assertj.core.api.Assertions.*;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.server.support.HttpSolrClientFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Christoph Strobl
 */
public class HttpSolrClientFactoryTests {

	private static final String URL = "https://solr.server.url";
	private SolrClient solrClient;

	@Before
	public void setUp() {

		solrClient = new HttpSolrClient.Builder().withBaseSolrUrl(URL).build();
	}

	@After
	public void tearDown() {

		solrClient = null;
	}

	@Test
	public void testInitFactory() {
		HttpSolrClientFactory factory = new HttpSolrClientFactory(solrClient);
		assertThat(factory.getSolrClient()).isEqualTo(solrClient);
		assertThat(((HttpSolrClient) factory.getSolrClient()).getBaseURL()).isEqualTo(URL);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInitFactoryWithNullServer() {
		new HttpSolrClientFactory(null);
	}

	@Test
	public void testInitFactoryWithAuthentication() {
		HttpSolrClientFactory factory = new HttpSolrClientFactory(solrClient,
				new UsernamePasswordCredentials("username", "password"), "BASIC");

		HttpClient solrHttpClient = ((HttpSolrClient) factory.getSolrClient()).getHttpClient();

		CredentialsProvider provider = (CredentialsProvider) ReflectionTestUtils.getField(solrHttpClient,
				"credentialsProvider");

		assertThat(provider.getCredentials(AuthScope.ANY)).isNotNull();

		assertThat(((UsernamePasswordCredentials) provider.getCredentials(AuthScope.ANY)).getUserName())
				.isEqualTo("username");

		assertThat(((UsernamePasswordCredentials) provider.getCredentials(AuthScope.ANY)).getPassword())
				.isEqualTo("password");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInitFactoryWithoutAuthenticationSchema() {
		new HttpSolrClientFactory(solrClient, new UsernamePasswordCredentials("username", "password"), "");
	}

}
