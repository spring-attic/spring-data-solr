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
package com.google.soldockr;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;

public abstract class AbstractITestWithEmbeddedSolrServer {

  protected static SolrServer solrServer;
  protected static String DEFAULT_BEAN_ID = "1";

  @BeforeClass
  public static void initSolrServer() throws IOException, ParserConfigurationException, SAXException {
    System.setProperty("solr.solr.home", StringUtils.remove(ResourceUtils.getURL("classpath:com/google/soldockr").toString(), "file:/"));
    CoreContainer.Initializer initializer = new CoreContainer.Initializer();
    CoreContainer coreContainer = initializer.initialize();
    solrServer = new EmbeddedSolrServer(coreContainer, "");
  }
  
  @AfterClass
  public static void cleanDataInSolr() throws SolrServerException, IOException {
    solrServer.deleteByQuery("*:*");
    solrServer.commit();
  }
  
  public ExampleSolrBean createDefaultExampleBean() {
    return createExampleBeanWithId(DEFAULT_BEAN_ID);
  }
  
  public ExampleSolrBean createExampleBeanWithId(String id) {
    return new ExampleSolrBean(id, "bean_"+id, "category_"+id);
  }

}
