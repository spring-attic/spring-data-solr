/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.solr.core;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.server.SolrClientFactory;

/**
 * 
 * @author Venil Noronha
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrTemplateMulticoreTests {

	private SolrTemplate solrTemplate;
	private @Mock SolrClient defaultSolrClient;
	private @Mock SolrClient core1Client;
	private @Mock SolrClient core2Client;
	private @Mock SolrClientFactory solrClientFactory;

	@Before
	public void setUp() {
		Mockito.when(solrClientFactory.getSolrClient()).thenReturn(defaultSolrClient);
		Mockito.when(solrClientFactory.getSolrClient("core1")).thenReturn(core1Client);
		Mockito.when(solrClientFactory.getSolrClient("core2")).thenReturn(core2Client);
		solrTemplate = new SolrTemplate(solrClientFactory);
		solrTemplate.afterPropertiesSet();
	}

	@Test
	public void testGetSolrClients() throws SolrServerException, IOException {
		SolrClient client1 = solrClientFactory.getSolrClient("core1");
		SolrClient client2 = solrClientFactory.getSolrClient("core2");
		Assert.assertNotNull(client1);
		Assert.assertNotNull(client2);
		Assert.assertEquals(core1Client, client1);
		Assert.assertEquals(core2Client, client2);
	}

	@Test
	public void testPingSpecificCores() throws SolrServerException, IOException {
		Mockito.when(core1Client.ping()).thenReturn(new SolrPingResponse());
		Mockito.when(core2Client.ping()).thenReturn(new SolrPingResponse());
		SolrPingResponse pingResult1 = solrTemplate.ping("core1");
		SolrPingResponse pingResult2 = solrTemplate.ping("core2");
		Assert.assertNotNull(pingResult1);
		Assert.assertNotNull(pingResult2);
		Mockito.verify(core1Client, Mockito.times(1)).ping();
		Mockito.verify(core2Client, Mockito.times(1)).ping();
	}

	@Test
	public void testCountQueries() throws SolrServerException, IOException {
		ArgumentCaptor<SolrQuery> captor1 = ArgumentCaptor.forClass(SolrQuery.class);
		ArgumentCaptor<SolrQuery> captor2 = ArgumentCaptor.forClass(SolrQuery.class);

		QueryResponse response1Mock = Mockito.mock(QueryResponse.class);
		SolrDocumentList resultList1 = new SolrDocumentList();
		resultList1.setNumFound(10);
		Mockito.when(response1Mock.getResults()).thenReturn(resultList1);
		QueryResponse response2Mock = Mockito.mock(QueryResponse.class);
		SolrDocumentList resultList2 = new SolrDocumentList();
		resultList2.setNumFound(10);
		Mockito.when(response2Mock.getResults()).thenReturn(resultList2);

		Mockito.when(core1Client.query(Mockito.any(SolrQuery.class), Mockito.eq(SolrRequest.METHOD.GET))).thenReturn(response1Mock);
		Mockito.when(core2Client.query(Mockito.any(SolrQuery.class), Mockito.eq(SolrRequest.METHOD.GET))).thenReturn(response2Mock);

		long result1 = solrTemplate.count("core1", new SimpleQuery(new Criteria("field_1").is("value1")));
		long result2 = solrTemplate.count("core2", new SimpleQuery(new Criteria("field_2").is("value2")));
		Assert.assertEquals(resultList1.getNumFound(), result1);
		Assert.assertEquals(resultList2.getNumFound(), result2);

		Mockito.verify(core1Client, Mockito.times(1)).query(captor1.capture(), Mockito.eq(SolrRequest.METHOD.GET));
		Mockito.verify(core2Client, Mockito.times(1)).query(captor2.capture(), Mockito.eq(SolrRequest.METHOD.GET));
	}

}
