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

import java.net.MalformedURLException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import at.pagu.soldockr.core.SimpleSolrServerFactory;

public class SimpleSolrServerFactoryTest {
  
  private static final String URL = "http://solr.server.url";
  private SolrServer solrServer;
  
  @Before
  public void setUp() throws MalformedURLException {
    solrServer = new HttpSolrServer(URL);
  }

  @After
  public void tearDown() {
    solrServer = null;
  }
  
  @Test
  public void testInitFactory() {
    SimpleSolrServerFactory factory = new SimpleSolrServerFactory(solrServer);
    Assert.assertNull(factory.getCore());
    Assert.assertEquals(solrServer, factory.getSolrServer());
    Assert.assertEquals(URL, ((HttpSolrServer) factory.getSolrServer()).getBaseURL());
  }
  
  @Test
  public void testInitFactoryWithCore() throws MalformedURLException {
    SimpleSolrServerFactory factory = new SimpleSolrServerFactory(solrServer, "core");
    Assert.assertEquals(URL + "/core", ((HttpSolrServer) factory.getSolrServer()).getBaseURL());

    factory = new SimpleSolrServerFactory(new HttpSolrServer(URL + "/"), "core");
    Assert.assertEquals(URL + "/core", ((HttpSolrServer) factory.getSolrServer()).getBaseURL());
  }
  
  @Test
  public void testInitFactoryWithEmptyCore() {
    SimpleSolrServerFactory factory = new SimpleSolrServerFactory(solrServer, StringUtils.EMPTY);
    Assert.assertEquals(URL, ((HttpSolrServer) factory.getSolrServer()).getBaseURL());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInitFactoryWithNullServer() {
    new SimpleSolrServerFactory(null);
  }
  
  @Test
  public void testInitFactoryWithAuthentication() {
    SimpleSolrServerFactory factory = new SimpleSolrServerFactory(solrServer, "core", new UsernamePasswordCredentials("username", "password"), "BASIC");

    AbstractHttpClient solrHttpClient = (AbstractHttpClient)((HttpSolrServer) factory.getSolrServer()).getHttpClient();
    Assert.assertNotNull(solrHttpClient.getCredentialsProvider().getCredentials(AuthScope.ANY));
    Assert.assertNotNull(solrHttpClient.getParams().getParameter(AuthPNames.TARGET_AUTH_PREF));
    Assert.assertEquals("username", ((UsernamePasswordCredentials) solrHttpClient.getCredentialsProvider().getCredentials(AuthScope.ANY)).getUserName());
    Assert.assertEquals("password", ((UsernamePasswordCredentials) solrHttpClient.getCredentialsProvider().getCredentials(AuthScope.ANY)).getPassword());
  }

 
  @Test(expected = IllegalArgumentException.class)
  public void testInitFactoryWithoutAuthenticationSchema() {
    new SimpleSolrServerFactory(solrServer, "core",new UsernamePasswordCredentials("username", "password"), "");
  }

}
