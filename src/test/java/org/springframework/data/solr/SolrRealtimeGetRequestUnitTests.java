/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr;

import static org.mockito.Mockito.*;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Francisco Spaeth
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrRealtimeGetRequestUnitTests {

	private @Mock SolrClient solrClientMock;

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExeptionWhenNoIdsGiven() {
		new SolrRealtimeGetRequest();
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExeptionWhenIdsContainsNullValue() {
		new SolrRealtimeGetRequest(1, 2, null);
	}

	@Test // DATASOLR-83
	public void testCreationOfRealtimeGet() throws SolrServerException, IOException {

		// given
		NamedList<Object> value = new NamedList<Object>();
		SolrRealtimeGetRequest request = new SolrRealtimeGetRequest(1L, 2F, 3, "4");
		when(solrClientMock.request(request)).thenReturn(value);

		// when
		QueryResponse result = request.process(solrClientMock);

		// then
		Assert.assertEquals(value, result.getResponse());
		Assert.assertArrayEquals(new String[] { "1", "2.0", "3", "4" }, request.getParams().getParams("ids"));
		verify(solrClientMock).request(request);
	}

}
