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

import junit.framework.Assert;

import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.junit.Test;

import at.pagu.soldockr.ExampleSolrBean;
import at.pagu.soldockr.core.SolrTemplate;
import at.pagu.soldockr.repository.SimpleSolrRepository;


public class SimpleSolrRepositoryTest {
  
  private SimpleSolrRepository<ExampleSolrBean> repository;
  
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

}
