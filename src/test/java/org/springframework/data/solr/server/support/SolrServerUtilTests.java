/*
 * Copyright 2012 - 2014 the original author or authors.
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
package org.springframework.data.solr.server.support;

import static org.hamcrest.core.IsCollectionContaining.*;
import static org.hamcrest.core.IsEqual.*;
import static org.hamcrest.text.IsEmptyString.*;

import java.net.MalformedURLException;
import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;
import org.apache.solr.core.CoreContainer;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsSame;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.solr.core.mapping.SolrDocument;
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
		Assert.assertThat(aliveServers.keySet(), hasItems(CORE_URL, ALTERNATE_CORE_URL));

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
	public void testClonesCloudSolrServerForCoreCorrectlyWhenCoreNameIsNotEmpty() throws MalformedURLException {
		LBHttpSolrServer lbSolrServer = new LBHttpSolrServer(BASE_URL, ALTERNATE_BASE_URL);
		CloudSolrServer cloudServer = new CloudSolrServer(ZOO_KEEPER_URL, lbSolrServer);

		CloudSolrServer clone = SolrServerUtils.clone(cloudServer, CORE_NAME);
		Assert.assertEquals(ZOO_KEEPER_URL, ReflectionTestUtils.getField(clone, FIELD_ZOO_KEEPER));

		LBHttpSolrServer lbClone = clone.getLbServer();
		Map<String, ?> aliveServers = (Map<String, ?>) ReflectionTestUtils.getField(lbClone, FIELD_ALIVE_SERVERS);
		Assert.assertThat(aliveServers.keySet(), hasItems(CORE_URL, ALTERNATE_CORE_URL));

		assertLBHttpSolrServerProperties(lbSolrServer, lbClone);
		Assert.assertThat(clone.getDefaultCollection(), equalTo(CORE_NAME));
	}

	@Test
	public void testClonesCloudSolrServerForCoreCorrectlyWhenNoLBHttpServerPresent() throws MalformedURLException {
		CloudSolrServer cloudServer = new CloudSolrServer(ZOO_KEEPER_URL);

		CloudSolrServer clone = SolrServerUtils.clone(cloudServer, CORE_NAME);
		Assert.assertEquals(ZOO_KEEPER_URL, ReflectionTestUtils.getField(clone, FIELD_ZOO_KEEPER));

		LBHttpSolrServer lbClone = clone.getLbServer();

		assertLBHttpSolrServerProperties(cloudServer.getLbServer(), lbClone);
		Assert.assertThat(clone.getDefaultCollection(), equalTo(CORE_NAME));
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

	@Test
	public void testResolveSolrCoreNameShouldReturnEmptyStringWhenNoAnnotationPresent() {
		Assert.assertThat(SolrServerUtils.resolveSolrCoreName(ClassWithoutSolrDocumentAnnotation.class), isEmptyString());
	}

	@Test
	public void testResolveSolrCoreNameShouldReturnEmptyStringWhenAnnotationHasNoValue() {
		Assert.assertThat(SolrServerUtils.resolveSolrCoreName(ClassWithEmptySolrDocumentAnnotation.class), isEmptyString());
	}

	@Test
	public void testResolveSolrCoreNameShouldReturnAnnotationValueWhenPresent() {
		Assert.assertThat(SolrServerUtils.resolveSolrCoreName(ClassWithSolrDocumentAnnotation.class), equalTo("core1"));
	}

	/**
	 * @see DATASOLR-189
	 */
	@Test
	public void cloningLBHttpSolrServerShouldCopyHttpParamsCorrectly() throws MalformedURLException {

		HttpParams params = new BasicHttpParams();
		params.setParameter("foo", "bar");
		DefaultHttpClient client = new DefaultHttpClient(params);

		LBHttpSolrServer lbSolrServer = new LBHttpSolrServer(client, BASE_URL, ALTERNATE_BASE_URL);

		LBHttpSolrServer cloned = SolrServerUtils.clone(lbSolrServer, CORE_NAME);
		Assert.assertThat(cloned.getHttpClient().getParams(), IsEqual.equalTo(params));

	}

	/**
	 * @see DATASOLR-189
	 */
	@Test
	public void cloningLBHttpSolrServerShouldCopyCredentialsProviderCorrectly() {

		BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("foo", "bar"));

		DefaultHttpClient client = new DefaultHttpClient();
		client.setCredentialsProvider(credentialsProvider);

		LBHttpSolrServer lbSolrServer = new LBHttpSolrServer(client, BASE_URL, ALTERNATE_BASE_URL);

		LBHttpSolrServer cloned = SolrServerUtils.clone(lbSolrServer, CORE_NAME);
		Assert.assertThat(((AbstractHttpClient) cloned.getHttpClient()).getCredentialsProvider(),
				IsEqual.<CredentialsProvider> equalTo(credentialsProvider));
	}

	/**
	 * @see DATASOLR-189
	 */
	@Test
	public void cloningHttpSolrServerShouldCopyHttpParamsCorrectly() {

		HttpParams params = new BasicHttpParams();
		params.setParameter("foo", "bar");
		DefaultHttpClient client = new DefaultHttpClient(params);

		HttpSolrServer solrServer = new HttpSolrServer(BASE_URL, client);
		HttpSolrServer cloned = SolrServerUtils.clone(solrServer);

		Assert.assertThat(cloned.getHttpClient().getParams(), IsEqual.equalTo(params));
	}

	/**
	 * @see DATASOLR-189
	 */
	@Test
	public void cloningHttpSolrServerShouldCopyCredentialsProviderCorrectly() {

		BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("foo", "bar"));

		DefaultHttpClient client = new DefaultHttpClient();
		client.setCredentialsProvider(credentialsProvider);

		HttpSolrServer solrServer = new HttpSolrServer(BASE_URL, client);
		HttpSolrServer cloned = SolrServerUtils.clone(solrServer);

		Assert.assertThat(((AbstractHttpClient) cloned.getHttpClient()).getCredentialsProvider(),
				IsEqual.<CredentialsProvider> equalTo(credentialsProvider));
	}

	/**
	 * @see DATASOLR-203
	 */
	@Test
	public void cloningEmbeddedSolrServerShouldReuseCoreContainer() {

		CoreContainer coreContainer = Mockito.mock(CoreContainer.class);
		EmbeddedSolrServer solrServer = new EmbeddedSolrServer(coreContainer, null);

		EmbeddedSolrServer clone = SolrServerUtils.clone(solrServer, "core1");
		Assert.assertThat(clone.getCoreContainer(), IsSame.sameInstance(coreContainer));
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

	private static class ClassWithoutSolrDocumentAnnotation {

	}

	@SolrDocument
	private static class ClassWithEmptySolrDocumentAnnotation {

	}

	@SolrDocument(solrCoreName = "core1")
	private static class ClassWithSolrDocumentAnnotation {

	}

}
