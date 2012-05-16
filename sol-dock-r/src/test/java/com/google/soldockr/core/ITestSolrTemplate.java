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
package com.google.soldockr.core;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.google.soldockr.AbstractITestWithEmbeddedSolrServer;
import com.google.soldockr.ExampleSolrBean;
import com.google.soldockr.core.query.Criteria;
import com.google.soldockr.core.query.SimpleQuery;

public class ITestSolrTemplate extends AbstractITestWithEmbeddedSolrServer {

  private SolrTemplate solrTemplate;

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

  @Test
  public void testPing() throws SolrServerException, IOException {
    solrTemplate.executePing();
  }

}
