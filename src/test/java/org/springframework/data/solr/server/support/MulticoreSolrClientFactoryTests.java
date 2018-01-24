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
package org.springframework.data.solr.server.support;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.springframework.data.solr.core.mapping.SolrDocument;

/**
 * @author Christoph Strobl
 */
@RunWith(Parameterized.class)
public class MulticoreSolrClientFactoryTests {

	private SolrClient solrClient;
	private MulticoreSolrClientFactory factory;
	private static final List<String> CORES = Arrays.asList("spring", "data", "solr");

	public MulticoreSolrClientFactoryTests(SolrClient solrClient) {
		this.solrClient = solrClient;
	}

	@Parameters
	public static Collection<Object[]> data() throws MalformedURLException {
		Object[][] data = new Object[][] { { new HttpSolrClient("http://127.0.0.1:8983") },
				{ new LBHttpSolrClient("http://127.0.0.1:8983", "http://127.0.0.1:6666") },
				{ new CloudSolrClient("http://127.0.0.1:8080") } };
		return Arrays.asList(data);
	}

	@Before
	public void setUp() throws MalformedURLException {
		this.factory = new MulticoreSolrClientFactory(this.solrClient, CORES);
	}

	@Test
	public void testGetCores() {
		Assert.assertThat(factory.getCores(), IsEqual.equalTo(CORES));
	}

	@Test
	public void testGetSolrClientForDefinedCore() {
		SolrClient solrClient = factory.getSolrClient("spring");
		Assert.assertNotNull(solrClient);
		Assert.assertNotSame(this.solrClient, solrClient);
	}

	@Test
	public void testGetSolrClientForUndefinedCore() {
		SolrClient solrClient = factory.getSolrClient("gini");
		Assert.assertNotNull(solrClient);
		Assert.assertNotSame(this.solrClient, solrClient);
		Assert.assertThat(factory.getCores(), IsEqual.equalTo(Arrays.asList("spring", "data", "solr", "gini")));
	}

	@Test
	public void testGetSolrClientForNullCoreReturnsDefaultInstance() {
		SolrClient solrClient = factory.getSolrClient((String) null);
		Assert.assertSame(this.solrClient, solrClient);
	}

	@Test
	public void testGetSolrClientForEmptyCoreReturnsDefaultInstance() {
		SolrClient solrClient = factory.getSolrClient(" ");
		Assert.assertSame(this.solrClient, solrClient);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetSolrClientForNullClassThrowsIllegalArgumentException() {
		factory.getSolrClient((Class<?>) null);
	}

	@Test
	public void testGetSolrServerForClassWithoutSolrDocumentAnnotationReturnedCorrectly() {
		SolrClient solrClient = factory.getSolrClient(ClassWithoutSolrDocumentAnnotation.class);
		Assert.assertNotSame(this.solrClient, solrClient);
		Assert.assertThat(factory.getCores(),
				IsEqual.equalTo(Arrays.asList("spring", "data", "solr", "ClassWithoutSolrDocumentAnnotation")));
	}

	@Test
	public void testGetSolrServerForClassWithEmptySolrDocumentAnnotationReturnedCorrectly() {
		SolrClient solrClient = factory.getSolrClient(ClassWithEmptySolrDocumentAnnotation.class);
		Assert.assertNotSame(this.solrClient, solrClient);
		Assert.assertThat(factory.getCores(),
				IsEqual.equalTo(Arrays.asList("spring", "data", "solr", "ClassWithEmptySolrDocumentAnnotation")));
	}

	@Test
	public void testGetSolrServerForClassWithSolrDocumentAnnotationReturnedCorrectly() {
		SolrClient solrClient = factory.getSolrClient(ClassWithSolrDocumentAnnotation.class);
		Assert.assertNotSame(this.solrClient, solrClient);
		Assert.assertThat(factory.getCores(), IsEqual.equalTo(Arrays.asList("spring", "data", "solr", "core1")));
	}

	@Test
	public void testGetDefaultSolrClient() {
		SolrClient solrClient = factory.getSolrClient();
		Assert.assertSame(this.solrClient, solrClient);
	}

	@Test
	public void testGetSolrClientReturnsNullWhenCoreDoesNotExistAndCreateMissingIsFalse() {
		factory.setCreateMissingSolrClient(false);
		Assert.assertNull(factory.getSolrClient("NoSuchCore"));
	}

	@Test
	public void testAddSolrClientForCoreNotConfigured() {
		HttpSolrClient ref = new HttpSolrClient("http://some.server.added/manually");
		factory.addSolrClientForCore(ref, "core1");

		Assert.assertThat(factory.getCores(), IsEqual.equalTo(Arrays.asList("spring", "data", "solr", "core1")));

		SolrClient solrClient = factory.getSolrClient("core1");
		Assert.assertNotSame(this.solrClient, solrClient);
		Assert.assertSame(ref, solrClient);
	}

	@Test
	public void testAddSolrClientOverwritesExistingWhenCoreAlreayConfigured() {
		HttpSolrClient ref = new HttpSolrClient("http://some.server.added/manually");
		factory.addSolrClientForCore(ref, "solr");

		Assert.assertThat(factory.getCores(), IsEqual.equalTo(Arrays.asList("spring", "data", "solr")));

		SolrClient solrClient = factory.getSolrClient("solr");
		Assert.assertNotSame(this.solrClient, solrClient);
		Assert.assertSame(ref, solrClient);
	}

	@Test
	public void testRemoveSolrClientCallsShutownOnRemovedInstance() {
		SolrClient solrClientMock = Mockito.mock(this.solrClient.getClass());
		factory.addSolrClientForCore(solrClientMock, "mock");

		Assert.assertThat(factory.getCores(), IsEqual.equalTo(Arrays.asList("spring", "data", "solr", "mock")));
		factory.removeSolrClient("mock");
		Assert.assertThat(factory.getCores(), IsEqual.equalTo(Arrays.asList("spring", "data", "solr")));
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
