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
package at.pagu.soldockr.repository;

import java.util.Arrays;

import junit.framework.Assert;

import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import at.pagu.soldockr.ExampleSolrBean;
import at.pagu.soldockr.core.SolrOperations;
import at.pagu.soldockr.core.SolrTemplate;
import at.pagu.soldockr.core.query.Query;
import at.pagu.soldockr.core.query.SolDockRQuery;

@RunWith(MockitoJUnitRunner.class)
public class SimpleSolrRepositoryTest {
  
  private SimpleSolrRepository<ExampleSolrBean> repository;
  
  @Mock
  private SolrOperations solrOperationsMock;
  
  @Before
  public void setUp() {
    repository = new SimpleSolrRepository<ExampleSolrBean>(solrOperationsMock, ExampleSolrBean.class);
  }

  
  @Test(expected=IllegalArgumentException.class)
  public void testInitRepositoryWithNullSolrOperations() {
    new SimpleSolrRepository<ExampleSolrBean>(null);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testInitRepositoryWithNullEntityClass() {
    new SimpleSolrRepository<ExampleSolrBean>(new SolrTemplate(new HttpSolrServer("http://localhost:8080/solr"), null), null);
  }
  
  @Test
  public void testInitRepository() {
    repository = new SimpleSolrRepository<ExampleSolrBean>(new SolrTemplate(new HttpSolrServer("http://localhost:8080/solr"), null), ExampleSolrBean.class);
    Assert.assertEquals(ExampleSolrBean.class, repository.getEntityClass());
  }
  
  @Test
  public void testFindAllByIdQuery() {
    Mockito.when(solrOperationsMock.executeQuery(Mockito.any(SolDockRQuery.class))).thenReturn(initFakeResponse(12345));
    
    repository.findAll(Arrays.asList("id-1", "id-2", "id-3"));
    ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
    
    Mockito.verify(solrOperationsMock, Mockito.times(1)).executeQuery(captor.capture());
    Mockito.verify(solrOperationsMock, Mockito.times(1)).executeListQuery(captor.capture(), Mockito.eq(ExampleSolrBean.class));
    
    Assert.assertEquals(1,captor.getAllValues().get(0).getPageRequest().getPageSize());
    Assert.assertEquals(12345,captor.getAllValues().get(1).getPageRequest().getPageSize());
  }
  
  private QueryResponse initFakeResponse(int nrFound) {
    QueryResponse response = new QueryResponse();
    
    NamedList<Object> namedList = new NamedList<Object>();
    SolrDocumentList docList = new SolrDocumentList();
    docList.setNumFound(nrFound);
    namedList.add("response", docList);
    response.setResponse(namedList);
    
    return response;
  }

}
