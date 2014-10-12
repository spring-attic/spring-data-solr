package org.springframework.data.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Francisco Spaeth
 */
public class SolrRealtimeGetRequestTest {

	@Test(expected = IllegalArgumentException.class)
	public void testCreationOfRealtimeGetRequestForNullId() {
		new SolrRealtimeGetRequest(1, 2, null);
	}

	@Test
	public void testCreationOfRealtimeGet() throws SolrServerException, IOException {
		// given
		NamedList<Object> value = new NamedList<Object>();
		SolrServer server = Mockito.mock(SolrServer.class);
		SolrRealtimeGetRequest request = new SolrRealtimeGetRequest(1L, 2F, 3, "4");
		Mockito.when(server.request(request)).thenReturn(value);

		// when
		QueryResponse result = request.process(server);

		// then
		Assert.assertEquals(value, result.getResponse());
		Assert.assertArrayEquals(new String[] { "1", "2.0", "3", "4" }, request.getParams().getParams("ids"));
		Mockito.verify(server).request(request);
	}

}
