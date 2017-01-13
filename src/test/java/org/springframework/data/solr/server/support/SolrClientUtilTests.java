/*
 * Copyright 2012-2017 the original author or authors.
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

import static org.hamcrest.core.IsCollectionContaining.*;
import static org.hamcrest.core.IsEqual.*;
import static org.hamcrest.text.IsEmptyString.*;

import java.net.MalformedURLException;
import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
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
public class SolrClientUtilTests {

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
		SolrClientUtils.clone(null);
	}

	@Test
	public void testClonesHttpSolrClientCorrectly() {
		HttpSolrClient httpSolrClient = new HttpSolrClient(BASE_URL);
		httpSolrClient.setDefaultMaxConnectionsPerHost(10);
		httpSolrClient.setFollowRedirects(true);
		httpSolrClient.setUseMultiPartPost(true);

		HttpSolrClient clone = SolrClientUtils.clone(httpSolrClient);
		Assert.assertEquals(httpSolrClient.getBaseURL(), clone.getBaseURL());
		assertHttpSolrClientProperties(httpSolrClient, clone);
	}

	@Test
	public void testClonesHttpSolrClientForCoreCorrectly() {
		HttpSolrClient httpSolrClient = new HttpSolrClient(BASE_URL);
		httpSolrClient.setDefaultMaxConnectionsPerHost(10);
		httpSolrClient.setFollowRedirects(true);
		httpSolrClient.setUseMultiPartPost(true);

		HttpSolrClient clone = SolrClientUtils.clone(httpSolrClient, CORE_NAME);

		Assert.assertEquals(CORE_URL, clone.getBaseURL());

		assertHttpSolrClientProperties(httpSolrClient, clone);
	}

	@Test
	public void testClonesLBHttpSolrClientCorrectly() throws MalformedURLException {
		LBHttpSolrClient lbSolrClient = new LBHttpSolrClient(BASE_URL, ALTERNATE_BASE_URL);
		lbSolrClient.setAliveCheckInterval(10);

		LBHttpSolrClient clone = SolrClientUtils.clone(lbSolrClient);

		Assert.assertEquals(ReflectionTestUtils.getField(lbSolrClient, FIELD_ALIVE_SERVERS),
				ReflectionTestUtils.getField(clone, FIELD_ALIVE_SERVERS));
		assertLBHttpSolrClientProperties(lbSolrClient, clone);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testClonesLBHttpSolrClientForCoreCorrectly() throws MalformedURLException {
		LBHttpSolrClient lbSolrClient = new LBHttpSolrClient(BASE_URL, ALTERNATE_BASE_URL);
		lbSolrClient.setAliveCheckInterval(10);

		LBHttpSolrClient clone = SolrClientUtils.clone(lbSolrClient, CORE_NAME);

		Map<String, ?> aliveServers = (Map<String, ?>) ReflectionTestUtils.getField(clone, FIELD_ALIVE_SERVERS);
		Assert.assertThat(aliveServers.keySet(), hasItems(CORE_URL, ALTERNATE_CORE_URL));

		assertLBHttpSolrClientProperties(lbSolrClient, clone);
	}

	@Test
	public void testClonesCloudSolrClientCorrectly() throws MalformedURLException {
		LBHttpSolrClient lbSolrClient = new LBHttpSolrClient(BASE_URL, ALTERNATE_BASE_URL);
		CloudSolrClient cloudClient = new CloudSolrClient(ZOO_KEEPER_URL, lbSolrClient);

		CloudSolrClient clone = SolrClientUtils.clone(cloudClient);
		Assert.assertEquals(ZOO_KEEPER_URL, ReflectionTestUtils.getField(clone, FIELD_ZOO_KEEPER));

		LBHttpSolrClient lbClone = clone.getLbClient();
		Assert.assertEquals(ReflectionTestUtils.getField(lbSolrClient, FIELD_ALIVE_SERVERS),
				ReflectionTestUtils.getField(lbClone, FIELD_ALIVE_SERVERS));

		assertLBHttpSolrClientProperties(lbSolrClient, lbClone);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testClonesCloudSolrClientForCoreCorrectlyWhenCoreNameIsNotEmpty() throws MalformedURLException {
		LBHttpSolrClient lbSolrClient = new LBHttpSolrClient(BASE_URL, ALTERNATE_BASE_URL);
		CloudSolrClient cloudClient = new CloudSolrClient(ZOO_KEEPER_URL, lbSolrClient);

		CloudSolrClient clone = SolrClientUtils.clone(cloudClient, CORE_NAME);
		Assert.assertEquals(ZOO_KEEPER_URL, ReflectionTestUtils.getField(clone, FIELD_ZOO_KEEPER));

		LBHttpSolrClient lbClone = clone.getLbClient();
		Map<String, ?> aliveServers = (Map<String, ?>) ReflectionTestUtils.getField(lbClone, FIELD_ALIVE_SERVERS);
		Assert.assertThat(aliveServers.keySet(), hasItems(CORE_URL, ALTERNATE_CORE_URL));

		assertLBHttpSolrClientProperties(lbSolrClient, lbClone);
		Assert.assertThat(clone.getDefaultCollection(), equalTo(CORE_NAME));
	}

	@Test
	public void testClonesCloudSolrClientForCoreCorrectlyWhenNoLBHttpServerPresent() throws MalformedURLException {
		CloudSolrClient cloudClient = new CloudSolrClient(ZOO_KEEPER_URL);

		CloudSolrClient clone = SolrClientUtils.clone(cloudClient, CORE_NAME);
		Assert.assertEquals(ZOO_KEEPER_URL, ReflectionTestUtils.getField(clone, FIELD_ZOO_KEEPER));

		LBHttpSolrClient lbClone = clone.getLbClient();

		assertLBHttpSolrClientProperties(cloudClient.getLbClient(), lbClone);
		Assert.assertThat(clone.getDefaultCollection(), equalTo(CORE_NAME));
	}

	@Test
	public void testCreateUrlForCoreAppendsCoreCorrectly() {
		Assert.assertEquals(CORE_URL, SolrClientUtils.appendCoreToBaseUrl(BASE_URL, CORE_NAME));
	}

	@Test
	public void testCreateUrlForCoreAppendsCoreCorrectlyWhenBaseUrlHasTrailingSlash() {
		Assert.assertEquals(CORE_URL, SolrClientUtils.appendCoreToBaseUrl(BASE_URL + "/", CORE_NAME));
	}

	@Test
	public void testCreateUrlForCoreAppendsCoreCorrectlyWhenCoreIsEmpty() {
		Assert.assertEquals(BASE_URL, SolrClientUtils.appendCoreToBaseUrl(BASE_URL, "  "));
	}

	@Test
	public void testCreateUrlForCoreAppendsCoreCorrectlyWhenCoreIsNull() {
		Assert.assertEquals(BASE_URL, SolrClientUtils.appendCoreToBaseUrl(BASE_URL, null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateUrlForCoreThrowsIllegalArgumentExceptionWhenBaseUrlIsNull() {
		Assert.assertEquals(BASE_URL, SolrClientUtils.appendCoreToBaseUrl(null, null));
	}

	@Test
	public void testResolveSolrCoreNameShouldReturnEmptyStringWhenNoAnnotationPresent() {
		Assert.assertThat(SolrClientUtils.resolveSolrCoreName(ClassWithoutSolrDocumentAnnotation.class), isEmptyString());
	}

	@Test
	public void testResolveSolrCoreNameShouldReturnEmptyStringWhenAnnotationHasNoValue() {
		Assert.assertThat(SolrClientUtils.resolveSolrCoreName(ClassWithEmptySolrDocumentAnnotation.class), isEmptyString());
	}

	@Test
	public void testResolveSolrCoreNameShouldReturnAnnotationValueWhenPresent() {
		Assert.assertThat(SolrClientUtils.resolveSolrCoreName(ClassWithSolrDocumentAnnotation.class), equalTo("core1"));
	}

	@Test // DATASOLR-189
	public void cloningLBHttpSolrClientShouldCopyHttpParamsCorrectly() throws MalformedURLException {

		HttpParams params = new BasicHttpParams();
		params.setParameter("foo", "bar");
		DefaultHttpClient client = new DefaultHttpClient(params);

		LBHttpSolrClient lbSolrClient = new LBHttpSolrClient(client, BASE_URL, ALTERNATE_BASE_URL);

		LBHttpSolrClient cloned = SolrClientUtils.clone(lbSolrClient, CORE_NAME);
		Assert.assertThat(cloned.getHttpClient().getParams(), IsEqual.equalTo(params));

	}

	@Test // DATASOLR-189
	public void cloningLBHttpSolrClientShouldCopyCredentialsProviderCorrectly() {

		BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("foo", "bar"));

		DefaultHttpClient client = new DefaultHttpClient();
		client.setCredentialsProvider(credentialsProvider);

		LBHttpSolrClient lbSolrClient = new LBHttpSolrClient(client, BASE_URL, ALTERNATE_BASE_URL);

		LBHttpSolrClient cloned = SolrClientUtils.clone(lbSolrClient, CORE_NAME);
		Assert.assertThat(((AbstractHttpClient) cloned.getHttpClient()).getCredentialsProvider(),
				IsEqual.<CredentialsProvider> equalTo(credentialsProvider));
	}

	@Test // DATASOLR-189
	public void cloningHttpSolrClientShouldCopyHttpParamsCorrectly() {

		HttpParams params = new BasicHttpParams();
		params.setParameter("foo", "bar");
		DefaultHttpClient client = new DefaultHttpClient(params);

		HttpSolrClient solrClient = new HttpSolrClient(BASE_URL, client);
		HttpSolrClient cloned = SolrClientUtils.clone(solrClient);

		Assert.assertThat(cloned.getHttpClient().getParams(), IsEqual.equalTo(params));
	}

	@Test // DATASOLR-189
	public void cloningHttpSolrClientShouldCopyCredentialsProviderCorrectly() {

		BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("foo", "bar"));

		DefaultHttpClient client = new DefaultHttpClient();
		client.setCredentialsProvider(credentialsProvider);

		HttpSolrClient solrClient = new HttpSolrClient(BASE_URL, client);
		HttpSolrClient cloned = SolrClientUtils.clone(solrClient);

		Assert.assertThat(((AbstractHttpClient) cloned.getHttpClient()).getCredentialsProvider(),
				IsEqual.<CredentialsProvider> equalTo(credentialsProvider));
	}

	@Test // DATASOLR-227
	public void cloningHttpSolrClientShouldCopyConnectionManager() {

		ClientConnectionManager conncetionManager = new SingleClientConnManager();

		DefaultHttpClient client = new DefaultHttpClient(conncetionManager);

		HttpSolrClient solrClient = new HttpSolrClient(BASE_URL, client);
		HttpSolrClient cloned = SolrClientUtils.clone(solrClient);

		Assert.assertThat(((AbstractHttpClient) cloned.getHttpClient()).getConnectionManager(),
				IsEqual.<ClientConnectionManager> equalTo(conncetionManager));
	}

	@Test // DATASOLR-203
	public void cloningEmbeddedSolrServerShouldReuseCoreContainer() {

		CoreContainer coreContainer = Mockito.mock(CoreContainer.class);
		EmbeddedSolrServer solrServer = new EmbeddedSolrServer(coreContainer, "core1");

		EmbeddedSolrServer clone = SolrClientUtils.clone(solrServer, "core1");
		Assert.assertThat(clone.getCoreContainer(), IsSame.sameInstance(coreContainer));
	}

	private void assertHttpSolrClientProperties(HttpSolrClient httpSolrServer, HttpSolrClient clone) {
		Assert.assertEquals(ReflectionTestUtils.getField(httpSolrServer, "followRedirects"),
				ReflectionTestUtils.getField(clone, "followRedirects"));
		Assert.assertEquals(ReflectionTestUtils.getField(httpSolrServer, "useMultiPartPost"),
				ReflectionTestUtils.getField(clone, "useMultiPartPost"));
	}

	private void assertLBHttpSolrClientProperties(LBHttpSolrClient lbSolrServer, LBHttpSolrClient clone) {
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
