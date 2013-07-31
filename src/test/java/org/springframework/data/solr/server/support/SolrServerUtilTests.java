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

import java.net.MalformedURLException;
import java.util.Map;

import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Christoph Strobl
 */
public class SolrServerUtilTests {

	private static final String FIELD_ZOO_KEEPER = "zkHost";
	private static final String FIELD_ALIVE_SERVERS = "aliveServers";

	private static final String CORE_NAME = "core1";
	private static final String ZOO_KEEPER_URL = "http://127.0.0.1/zk";
	private static final String BASE_URL = "http://127.0.0.1:8983/solr";
	private static final String ALTERNATE_BASE_URL = "http://localhost:8983/solr";

	private static final String CORE_URL = BASE_URL + "/" + CORE_NAME;
	private static final String ALTERNATE_CORE_URL = ALTERNATE_BASE_URL + "/" + CORE_NAME;

	@Test(expected = IllegalArgumentException.class)
	public void testCloneNullThrowsIllegalArgumentException() {
		SolrServerUtils.clone(null);
	}

	@Test
	public void testClonesHttpSolrServerCorrectly() {
		HttpSolrServer httpSolrServer = new HttpSolrServer(BASE_URL);
		httpSolrServer.setDefaultMaxConnectionsPerHost(10);
		httpSolrServer.setFollowRedirects(true);
		httpSolrServer.setMaxRetries(3);
		httpSolrServer.setUseMultiPartPost(true);

		HttpSolrServer clone = SolrServerUtils.clone(httpSolrServer);
		Assert.assertEquals(httpSolrServer.getBaseURL(), clone.getBaseURL());
		assertHttpSolrServerProperties(httpSolrServer, clone);
	}

	@Test
	public void testClonesHttpSolrServerForCoreCorrectly() {
		HttpSolrServer httpSolrServer = new HttpSolrServer(BASE_URL);
		httpSolrServer.setDefaultMaxConnectionsPerHost(10);
		httpSolrServer.setFollowRedirects(true);
		httpSolrServer.setMaxRetries(3);
		httpSolrServer.setUseMultiPartPost(true);

		HttpSolrServer clone = SolrServerUtils.clone(httpSolrServer, CORE_NAME);

		Assert.assertEquals(CORE_URL, clone.getBaseURL());

		assertHttpSolrServerProperties(httpSolrServer, clone);
	}

	@Test
	public void testClonesLBHttpSolrServerCorrectly() throws MalformedURLException {
		LBHttpSolrServer lbSolrServer = new LBHttpSolrServer(BASE_URL, ALTERNATE_BASE_URL);
		lbSolrServer.setAliveCheckInterval(10);

		LBHttpSolrServer clone = SolrServerUtils.clone(lbSolrServer);

		Assert.assertEquals(ReflectionTestUtils.getField(lbSolrServer, FIELD_ALIVE_SERVERS),
				ReflectionTestUtils.getField(clone, FIELD_ALIVE_SERVERS));
		assertLBHttpSolrServerProperties(lbSolrServer, clone);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testClonesLBHttpSolrServerForCoreCorrectly() throws MalformedURLException {
		LBHttpSolrServer lbSolrServer = new LBHttpSolrServer(BASE_URL, ALTERNATE_BASE_URL);
		lbSolrServer.setAliveCheckInterval(10);

		LBHttpSolrServer clone = SolrServerUtils.clone(lbSolrServer, CORE_NAME);

		Map<String, ?> aliveServers = (Map<String, ?>) ReflectionTestUtils.getField(clone, FIELD_ALIVE_SERVERS);
		Assert.assertThat(aliveServers.keySet(), IsCollectionContaining.hasItems(CORE_URL, ALTERNATE_CORE_URL));

		assertLBHttpSolrServerProperties(lbSolrServer, clone);
	}

	@Test
	public void testClonesCloudSolrServerCorrectly() throws MalformedURLException {
		LBHttpSolrServer lbSolrServer = new LBHttpSolrServer(BASE_URL, ALTERNATE_BASE_URL);
		CloudSolrServer cloudServer = new CloudSolrServer(ZOO_KEEPER_URL, lbSolrServer);

		CloudSolrServer clone = SolrServerUtils.clone(cloudServer);
		Assert.assertEquals(ZOO_KEEPER_URL, ReflectionTestUtils.getField(clone, FIELD_ZOO_KEEPER));

		LBHttpSolrServer lbClone = clone.getLbServer();
		Assert.assertEquals(ReflectionTestUtils.getField(lbSolrServer, FIELD_ALIVE_SERVERS),
				ReflectionTestUtils.getField(lbClone, FIELD_ALIVE_SERVERS));

		assertLBHttpSolrServerProperties(lbSolrServer, lbClone);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testClonesCloudSolrServerForCoreCorrectly() throws MalformedURLException {
		LBHttpSolrServer lbSolrServer = new LBHttpSolrServer(BASE_URL, ALTERNATE_BASE_URL);
		CloudSolrServer cloudServer = new CloudSolrServer(ZOO_KEEPER_URL, lbSolrServer);

		CloudSolrServer clone = SolrServerUtils.clone(cloudServer, CORE_NAME);
		Assert.assertEquals(ZOO_KEEPER_URL, ReflectionTestUtils.getField(clone, FIELD_ZOO_KEEPER));

		LBHttpSolrServer lbClone = clone.getLbServer();
		Map<String, ?> aliveServers = (Map<String, ?>) ReflectionTestUtils.getField(lbClone, FIELD_ALIVE_SERVERS);
		Assert.assertThat(aliveServers.keySet(), IsCollectionContaining.hasItems(CORE_URL, ALTERNATE_CORE_URL));

		assertLBHttpSolrServerProperties(lbSolrServer, lbClone);
	}

	@Test
	public void testCreateUrlForCoreAppendsCoreCorrectly() {
		Assert.assertEquals(CORE_URL, SolrServerUtils.appendCoreToBaseUrl(BASE_URL, CORE_NAME));
	}

	@Test
	public void testCreateUrlForCoreAppendsCoreCorrectlyWhenBaseUrlHasTrailingSlash() {
		Assert.assertEquals(CORE_URL, SolrServerUtils.appendCoreToBaseUrl(BASE_URL + "/", CORE_NAME));
	}

	@Test
	public void testCreateUrlForCoreAppendsCoreCorrectlyWhenCoreIsEmpty() {
		Assert.assertEquals(BASE_URL, SolrServerUtils.appendCoreToBaseUrl(BASE_URL, "  "));
	}

	@Test
	public void testCreateUrlForCoreAppendsCoreCorrectlyWhenCoreIsNull() {
		Assert.assertEquals(BASE_URL, SolrServerUtils.appendCoreToBaseUrl(BASE_URL, null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateUrlForCoreThrowsIllegalArgumentExceptionWhenBaseUrlIsNull() {
		Assert.assertEquals(BASE_URL, SolrServerUtils.appendCoreToBaseUrl(null, null));
	}

	private void assertHttpSolrServerProperties(HttpSolrServer httpSolrServer, HttpSolrServer clone) {
		Assert.assertEquals(ReflectionTestUtils.getField(httpSolrServer, "followRedirects"),
				ReflectionTestUtils.getField(clone, "followRedirects"));
		Assert.assertEquals(ReflectionTestUtils.getField(httpSolrServer, "maxRetries"),
				ReflectionTestUtils.getField(clone, "maxRetries"));
		Assert.assertEquals(ReflectionTestUtils.getField(httpSolrServer, "useMultiPartPost"),
				ReflectionTestUtils.getField(clone, "useMultiPartPost"));
	}

	private void assertLBHttpSolrServerProperties(LBHttpSolrServer lbSolrServer, LBHttpSolrServer clone) {
		Assert.assertEquals(ReflectionTestUtils.getField(lbSolrServer, "interval"),
				ReflectionTestUtils.getField(clone, "interval"));
	}

}
