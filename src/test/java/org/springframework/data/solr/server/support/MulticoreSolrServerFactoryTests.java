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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;
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
public class MulticoreSolrServerFactoryTests {

	private SolrServer solrServer;
	private MulticoreSolrServerFactory factory;
	private static final List<String> CORES = Arrays.asList("spring", "data", "solr");

	public MulticoreSolrServerFactoryTests(SolrServer solrServer) {
		this.solrServer = solrServer;
	}

	@Parameters
	public static Collection<Object[]> data() throws MalformedURLException {
		Object[][] data = new Object[][] { { new HttpSolrServer("http://127.0.0.1:8983") },
				{ new LBHttpSolrServer("http://127.0.0.1:8983", "http://127.0.0.1:6666") },
				{ new CloudSolrServer("http://127.0.0.1:8080") } };
		return Arrays.asList(data);
	}

	@Before
	public void setUp() throws MalformedURLException {
		this.factory = new MulticoreSolrServerFactory(this.solrServer, CORES);
	}

	@Test
	public void testGetCores() {
		Assert.assertThat(factory.getCores(), IsEqual.equalTo(CORES));
	}

	@Test
	public void testGetSolrServerForDefinedCore() {
		SolrServer solrServer = factory.getSolrServer("spring");
		Assert.assertNotNull(solrServer);
		Assert.assertNotSame(this.solrServer, solrServer);
	}

	@Test
	public void testGetSolrServerForUndefinedCore() {
		SolrServer solrServer = factory.getSolrServer("gini");
		Assert.assertNotNull(solrServer);
		Assert.assertNotSame(this.solrServer, solrServer);
		Assert.assertThat(factory.getCores(), IsEqual.equalTo(Arrays.asList("spring", "data", "solr", "gini")));
	}

	@Test
	public void testGetSolrServerForNullCoreReturnsDefaultInstance() {
		SolrServer solrServer = factory.getSolrServer((String) null);
		Assert.assertSame(this.solrServer, solrServer);
	}

	@Test
	public void testGetSolrServerForEmptyCoreReturnsDefaultInstance() {
		SolrServer solrServer = factory.getSolrServer(" ");
		Assert.assertSame(this.solrServer, solrServer);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetSolrServerForNullClassThrowsIllegalArgumentException() {
		factory.getSolrServer((Class<?>) null);
	}

	@Test
	public void testGetSolrServerForClassWithoutSolrDocumentAnnotationReturnedCorrectly() {
		SolrServer solrServer = factory.getSolrServer(ClassWithoutSolrDocumentAnnotation.class);
		Assert.assertNotSame(this.solrServer, solrServer);
		Assert.assertThat(factory.getCores(),
				IsEqual.equalTo(Arrays.asList("spring", "data", "solr", "ClassWithoutSolrDocumentAnnotation")));
	}

	@Test
	public void testGetSolrServerForClassWithEmptySolrDocumentAnnotationReturnedCorrectly() {
		SolrServer solrServer = factory.getSolrServer(ClassWithEmptySolrDocumentAnnotation.class);
		Assert.assertNotSame(this.solrServer, solrServer);
		Assert.assertThat(factory.getCores(),
				IsEqual.equalTo(Arrays.asList("spring", "data", "solr", "ClassWithEmptySolrDocumentAnnotation")));
	}

	@Test
	public void testGetSolrServerForClassWithSolrDocumentAnnotationReturnedCorrectly() {
		SolrServer solrServer = factory.getSolrServer(ClassWithSolrDocumentAnnotation.class);
		Assert.assertNotSame(this.solrServer, solrServer);
		Assert.assertThat(factory.getCores(), IsEqual.equalTo(Arrays.asList("spring", "data", "solr", "core1")));
	}

	@Test
	public void testGetDefaultSolrServer() {
		SolrServer solrServer = factory.getSolrServer();
		Assert.assertSame(this.solrServer, solrServer);
	}

	@Test
	public void testGetSolrServerReturnsNullWhenCoreDoesNotExistAndCreateMissingIsFalse() {
		factory.setCreateMissingSolrServer(false);
		Assert.assertNull(factory.getSolrServer("NoSuchCore"));
	}

	@Test
	public void testAddSolrServerForCoreNotConfigured() {
		HttpSolrServer ref = new HttpSolrServer("http://some.server.added/manually");
		factory.addSolrServerForCore(ref, "core1");

		Assert.assertThat(factory.getCores(), IsEqual.equalTo(Arrays.asList("spring", "data", "solr", "core1")));

		SolrServer solrServer = factory.getSolrServer("core1");
		Assert.assertNotSame(this.solrServer, solrServer);
		Assert.assertSame(ref, solrServer);
	}

	@Test
	public void testAddSolrServerOverwritesExistingWhenCoreAlreayConfigured() {
		HttpSolrServer ref = new HttpSolrServer("http://some.server.added/manually");
		factory.addSolrServerForCore(ref, "solr");

		Assert.assertThat(factory.getCores(), IsEqual.equalTo(Arrays.asList("spring", "data", "solr")));

		SolrServer solrServer = factory.getSolrServer("solr");
		Assert.assertNotSame(this.solrServer, solrServer);
		Assert.assertSame(ref, solrServer);
	}

	@Test
	public void testRemoveSolrServerCallsShutownOnRemovedInstance() {
		SolrServer solrServerMock = Mockito.mock(this.solrServer.getClass());
		factory.addSolrServerForCore(solrServerMock, "mock");

		Assert.assertThat(factory.getCores(), IsEqual.equalTo(Arrays.asList("spring", "data", "solr", "mock")));
		factory.removeSolrSever("mock");
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
