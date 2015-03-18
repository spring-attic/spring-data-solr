/*
 * Copyright 2012 - 2015 the original author or authors.
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
package org.springframework.data.solr;

import java.net.MalformedURLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.server.support.HttpSolrClientFactory;

/**
 * @author Christoph Strobl
 */
public class HttpSolrClientFactoryTests {

	private static final String URL = "http://solr.server.url";
	private SolrClient solrClient;

	@Before
	public void setUp() throws MalformedURLException {
		solrClient = new HttpSolrClient(URL);
	}

	@After
	public void tearDown() {
		solrClient = null;
	}

	@Test
	public void testInitFactory() {
		HttpSolrClientFactory factory = new HttpSolrClientFactory(solrClient);
		Assert.assertNotNull(factory.getCores());
		Assert.assertThat(factory.getCores(), IsEmptyCollection.emptyCollectionOf(String.class));
		Assert.assertEquals(solrClient, factory.getSolrClient());
		Assert.assertEquals(URL, ((HttpSolrClient) factory.getSolrClient()).getBaseURL());
	}

	@Test
	public void testFactoryReturnsReferenceSolrClientWhenCallingGetWithCoreNameAndNoCoreSet() {
		HttpSolrClientFactory factory = new HttpSolrClientFactory(solrClient);
		Assert.assertEquals(solrClient, factory.getSolrClient("AnyCoreName"));
	}

	@Test
	public void testInitFactoryWithCore() throws MalformedURLException {
		HttpSolrClientFactory factory = new HttpSolrClientFactory(solrClient, "core");
		Assert.assertEquals(URL + "/core", ((HttpSolrClient) factory.getSolrClient()).getBaseURL());

		factory = new HttpSolrClientFactory(new HttpSolrClient(URL + "/"), "core");
		Assert.assertEquals(URL + "/core", ((HttpSolrClient) factory.getSolrClient()).getBaseURL());
	}

	@Test
	public void testFactoryReturnsReferenceSolrClientWhenCallingGetWithCoreNameAndCoreSet() {
		HttpSolrClientFactory factory = new HttpSolrClientFactory(solrClient, "core");
		Assert.assertEquals(solrClient, factory.getSolrClient("AnyCoreName"));
	}

	@Test
	public void testInitFactoryWithEmptyCore() {
		HttpSolrClientFactory factory = new HttpSolrClientFactory(solrClient, StringUtils.EMPTY);
		Assert.assertEquals(URL, ((HttpSolrClient) factory.getSolrClient()).getBaseURL());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInitFactoryWithNullServer() {
		new HttpSolrClientFactory(null);
	}

	@Test
	public void testInitFactoryWithAuthentication() {
		HttpSolrClientFactory factory = new HttpSolrClientFactory(solrClient, "core", new UsernamePasswordCredentials(
				"username", "password"), "BASIC");

		AbstractHttpClient solrHttpClient = (AbstractHttpClient) ((HttpSolrClient) factory.getSolrClient()).getHttpClient();
		Assert.assertNotNull(solrHttpClient.getCredentialsProvider().getCredentials(AuthScope.ANY));
		Assert.assertNotNull(solrHttpClient.getParams().getParameter(AuthPNames.TARGET_AUTH_PREF));
		Assert.assertEquals("username", ((UsernamePasswordCredentials) solrHttpClient.getCredentialsProvider()
				.getCredentials(AuthScope.ANY)).getUserName());
		Assert.assertEquals("password", ((UsernamePasswordCredentials) solrHttpClient.getCredentialsProvider()
				.getCredentials(AuthScope.ANY)).getPassword());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInitFactoryWithoutAuthenticationSchema() {
		new HttpSolrClientFactory(solrClient, "core", new UsernamePasswordCredentials("username", "password"), "");
	}

}
