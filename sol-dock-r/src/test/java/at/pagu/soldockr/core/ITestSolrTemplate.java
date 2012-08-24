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
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.xml.sax.SAXException;

import at.pagu.soldockr.AbstractITestWithEmbeddedSolrServer;
import at.pagu.soldockr.ExampleSolrBean;
import at.pagu.soldockr.core.query.Criteria;
import at.pagu.soldockr.core.query.FacetOptions;
import at.pagu.soldockr.core.query.FacetQuery;
import at.pagu.soldockr.core.query.SimpleFacetQuery;
import at.pagu.soldockr.core.query.SimpleField;
import at.pagu.soldockr.core.query.SimpleQuery;
import at.pagu.soldockr.core.query.result.FacetEntry;
import at.pagu.soldockr.core.query.result.FacetPage;


/**
 * @author Christoph Strobl
 */
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
    ExampleSolrBean toInsert = createDefaultExampleBean();

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

  @Test
  public void testRollback() {
    ExampleSolrBean toInsert = createDefaultExampleBean();
    solrTemplate.executeAddBean(toInsert);
    ExampleSolrBean recalled = solrTemplate.executeObjectQuery(new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
    Assert.assertNull(recalled);

    solrTemplate.executeRollback();
    recalled = solrTemplate.executeObjectQuery(new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
    Assert.assertNull(recalled);
  }

  @Test
  public void testFacetQuery() {
    List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>();
    for (int i = 0; i < 10; i++) {
      values.add(createExampleBeanWithId(Integer.toString(i)));
    }
    solrTemplate.executeAddBeans(values);
    solrTemplate.executeCommit();

    FacetQuery q = new SimpleFacetQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)).setFacetOptions(new FacetOptions().addFacetOnField("name").addFacetOnField("id")
        .setFacetLimit(5));

    FacetPage<ExampleSolrBean> page = solrTemplate.executeFacetQuery(q, ExampleSolrBean.class);

    for (Page<FacetEntry> facetResultPage : page.getFacetResultPages()) {
      Assert.assertEquals(5, facetResultPage.getNumberOfElements());
    }

    Page<FacetEntry> facetPage = page.getFacetResultPage(new SimpleField("name"));
    for (FacetEntry entry : facetPage) {
      Assert.assertNotNull(entry.getValue());
      Assert.assertEquals("name", entry.getField().getName());
      Assert.assertEquals(1l, entry.getValueCount());
    }

    facetPage = page.getFacetResultPage(new SimpleField("id"));
    for (FacetEntry entry : facetPage) {
      Assert.assertNotNull(entry.getValue());
      Assert.assertEquals("id", entry.getField().getName());
      Assert.assertEquals(1l, entry.getValueCount());
    }
  }

}
