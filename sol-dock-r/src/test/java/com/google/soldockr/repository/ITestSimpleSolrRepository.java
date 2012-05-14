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
package com.google.soldockr.repository;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;

import com.google.soldockr.core.SolrTemplate;
import com.google.soldockr.core.query.Criteria;
import com.google.soldockr.core.query.SimpleQuery;

public class ITestSimpleSolrRepository {

  private static SolrServer solrServer;
  private SolrTemplate solrTemplate;
  
  @BeforeClass
  public static void init() throws IOException, ParserConfigurationException, SAXException {
    System.setProperty("solr.solr.home", StringUtils.remove(ResourceUtils.getURL("classpath:com/google/soldockr").toString(), "file:/"));
    CoreContainer.Initializer initializer = new CoreContainer.Initializer();
    CoreContainer coreContainer = initializer.initialize();
    solrServer = new EmbeddedSolrServer(coreContainer, "");
  }

  @Before
  public void setUp() throws IOException, ParserConfigurationException, SAXException {
     solrTemplate = new SolrTemplate(solrServer, null);
  }
  
  @After
  public void tearDown() {
    solrTemplate.executeDelete(new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)));
    solrTemplate.executeCommit();
  }
  
  @Test
  public void testBeanLifecycle() {
    ExampleSolrBean toInsert = new ExampleSolrBean("1", "bean_001", "category_1");
    
    solrTemplate.executeAddBean(toInsert);
    ExampleSolrBean recalled = solrTemplate.executeObjectQuery(new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
    Assert.assertNull(recalled);
    solrTemplate.executeCommit();
    
    recalled = solrTemplate.executeObjectQuery(new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
    Assert.assertEquals(toInsert.getId(), recalled.getId());
    
    solrTemplate.executeDeleteById(toInsert.getId());
    recalled = solrTemplate.executeObjectQuery(new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
    Assert.assertEquals(toInsert.getId(), recalled.getId());
    
    solrTemplate.executeCommit();
    recalled = solrTemplate.executeObjectQuery(new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
    Assert.assertNull(recalled);
  } 

  @Ignore
  public void testPing() throws SolrServerException, IOException {
    solrTemplate.executePing();
  }

}
