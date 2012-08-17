/*
 * Copyright (C) 2012 sol-dock-r authors.
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
package at.pagu.soldockr.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;

import at.pagu.soldockr.SolDockRException;
import at.pagu.soldockr.SolrServerFactory;
import at.pagu.soldockr.core.query.Criteria;
import at.pagu.soldockr.core.query.Query;
import at.pagu.soldockr.core.query.SimpleQuery;

@RunWith(MockitoJUnitRunner.class)
public class SolrTemplateTest {

  private SolrTemplate solrTemplate;

  private static final SimpleJavaObject SIMPLE_OBJECT = new SimpleJavaObject("simple-string-id", 123l);
  private static final SolrInputDocument SIMPLE_DOCUMENT = new SolrInputDocument();

  @Mock
  private HttpSolrServer solrServerMock;

  @Before
  public void setUp() {
    solrTemplate = new SolrTemplate(solrServerMock, "core1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullServerFactory() {
    new SolrTemplate((SolrServerFactory) null);
  }

  @Test
  public void testExecutePing() throws SolrServerException, IOException {
    Mockito.when(solrServerMock.ping()).thenReturn(new SolrPingResponse());
    SolrPingResponse pingResult = solrTemplate.executePing();
    Assert.assertNotNull(pingResult);
    Mockito.verify(solrServerMock, Mockito.times(1)).ping();
  }

  @SuppressWarnings("unchecked")
  @Test(expected = SolDockRException.class)
  public void testExecutePingThrowsException() throws SolrServerException, IOException {
    Mockito.when(solrServerMock.ping()).thenThrow(SolrServerException.class);
    solrTemplate.executePing();
  }

  @Test
  public void testAddBean() throws IOException, SolrServerException {
    Mockito.when(solrServerMock.add(Mockito.any(SolrInputDocument.class))).thenReturn(new UpdateResponse());
    UpdateResponse updateResponse = solrTemplate.executeAddBean(SIMPLE_OBJECT);
    Assert.assertNotNull(updateResponse);

    ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
    Mockito.verify(solrServerMock, Mockito.times(1)).add(captor.capture());

    Assert.assertEquals(SIMPLE_OBJECT.getId(), captor.getValue().getFieldValue("id"));
    Assert.assertEquals(SIMPLE_OBJECT.getValue(), captor.getValue().getFieldValue("value"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddBeans() throws IOException, SolrServerException {
    Mockito.when(solrServerMock.add(Mockito.anyCollectionOf(SolrInputDocument.class))).thenReturn(new UpdateResponse());
    List<SimpleJavaObject> collection = Arrays.asList(new SimpleJavaObject("1", 1l), new SimpleJavaObject("2", 2l), new SimpleJavaObject("3", 3l));
    UpdateResponse updateResponse = solrTemplate.executeAddBeans(collection);
    Assert.assertNotNull(updateResponse);

    @SuppressWarnings("rawtypes")
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(solrServerMock, Mockito.times(1)).add(captor.capture());

    Assert.assertEquals(3, captor.getValue().size());
  }

  @Test
  public void testAddDocument() throws IOException, SolrServerException {
    Mockito.when(solrServerMock.add(Mockito.any(SolrInputDocument.class))).thenReturn(new UpdateResponse());
    UpdateResponse updateResponse = solrTemplate.executeAddDocument(SIMPLE_DOCUMENT);
    Assert.assertNotNull(updateResponse);
    Mockito.verify(solrServerMock, Mockito.times(1)).add(Mockito.eq(SIMPLE_DOCUMENT));
  }

  @Test
  public void testAddDocuments() throws IOException, SolrServerException {
    Mockito.when(solrServerMock.add(Mockito.anyCollectionOf(SolrInputDocument.class))).thenReturn(new UpdateResponse());
    List<SolrInputDocument> collection = Arrays.asList(SIMPLE_DOCUMENT);
    UpdateResponse updateResponse = solrTemplate.executeAddDocuments(collection);
    Assert.assertNotNull(updateResponse);
    Mockito.verify(solrServerMock, Mockito.times(1)).add(Mockito.eq(collection));
  }

  @Test
  public void testDeleteById() throws IOException, SolrServerException {
    Mockito.when(solrServerMock.deleteById(Mockito.anyString())).thenReturn(new UpdateResponse());
    UpdateResponse updateResponse = solrTemplate.executeDeleteById("1");
    Assert.assertNotNull(updateResponse);
    Mockito.verify(solrServerMock, Mockito.times(1)).deleteById(Mockito.eq("1"));
  }

  @Test
  public void testDeleteByIdWithCollection() throws IOException, SolrServerException {
    Mockito.when(solrServerMock.deleteById(Mockito.anyListOf(String.class))).thenReturn(new UpdateResponse());
    List<String> idsToDelete = Arrays.asList("1", "2");
    UpdateResponse updateResponse = solrTemplate.executeDeleteById(idsToDelete);
    Assert.assertNotNull(updateResponse);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<String>> captor = (ArgumentCaptor<List<String>>) (Object) ArgumentCaptor.forClass(List.class);

    Mockito.verify(solrServerMock, Mockito.times(1)).deleteById(captor.capture());

    Assert.assertEquals(idsToDelete.size(), captor.getValue().size());
    for (String s : idsToDelete) {
      Assert.assertTrue(captor.getValue().contains(s));
    }
  }

  @Test
  public void testCount() throws SolrServerException {
    ArgumentCaptor<SolrQuery> captor = ArgumentCaptor.forClass(SolrQuery.class);
    QueryResponse responseMock = Mockito.mock(QueryResponse.class);
    SolrDocumentList resultList = new SolrDocumentList();
    resultList.setNumFound(10);
    Mockito.when(responseMock.getResults()).thenReturn(resultList);
    Mockito.when(solrServerMock.query(Mockito.any(SolrQuery.class))).thenReturn(responseMock);

    long result = solrTemplate.executeCount(new SimpleQuery(new Criteria("field_1").is("value1")));
    Assert.assertEquals(resultList.getNumFound(), result);

    Mockito.verify(solrServerMock, Mockito.times(1)).query(captor.capture());

    Assert.assertEquals(Integer.valueOf(0), captor.getValue().getStart());
    Assert.assertEquals(Integer.valueOf(0), captor.getValue().getRows());
  }

  @Test
  public void testCountWhenPagingSet() throws SolrServerException {
    ArgumentCaptor<SolrQuery> captor = ArgumentCaptor.forClass(SolrQuery.class);
    QueryResponse responseMock = Mockito.mock(QueryResponse.class);
    SolrDocumentList resultList = new SolrDocumentList();
    resultList.setNumFound(10);
    Mockito.when(responseMock.getResults()).thenReturn(resultList);
    Mockito.when(solrServerMock.query(Mockito.any(SolrQuery.class))).thenReturn(responseMock);

    Query query = new SimpleQuery(new Criteria("field_1").is("value1"));
    query.setPageRequest(new PageRequest(0, 5));
    long result = solrTemplate.executeCount(query);
    Assert.assertEquals(resultList.getNumFound(), result);

    Mockito.verify(solrServerMock, Mockito.times(1)).query(captor.capture());

    Assert.assertEquals(Integer.valueOf(0), captor.getValue().getStart());
    Assert.assertEquals(Integer.valueOf(0), captor.getValue().getRows());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCountNullQuery() {
    solrTemplate.executeCount(null);
  }

  @Test
  public void testCommit() throws SolrServerException, IOException {
    Mockito.when(solrServerMock.commit()).thenReturn(new UpdateResponse());
    solrTemplate.executeCommit();
    Mockito.verify(solrServerMock, Mockito.times(1)).commit();
  }

  @Test
  public void testRollback() throws SolrServerException, IOException {
    Mockito.when(solrServerMock.rollback()).thenReturn(new UpdateResponse());
    solrTemplate.executeRollback();
    Mockito.verify(solrServerMock, Mockito.times(1)).rollback();
  }

}
