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

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;

import at.pagu.soldockr.SolrServerFactory;

/**
 * The HttpSolrServerFactory configures an {@link HttpSolrServer} to work with the provided core.
 * If provided Credentials eg. (@link UsernamePasswordCredentials} and AuthPolicy (eg. BASIC, DIGEST,...) will be applied to the underlying
 * HttpClient.
 * 
 * @author Christoph Strobl
 */
public class HttpSolrServerFactory implements SolrServerFactory, DisposableBean {

  private static final String SLASH = "/";
  private final SolrServer solrServer;
  private final String core;
  private final Credentials credentials;
  private final String authPolicy;

  public HttpSolrServerFactory(SolrServer solrServer) {
    this(solrServer, null);
  }

  public HttpSolrServerFactory(SolrServer solrServer, String core) {
    this(solrServer, core, null, null);
  }

  public HttpSolrServerFactory(SolrServer solrServer, String core, Credentials credentials, String authPolicy) {
    Assert.notNull(solrServer, "SolrServer must not be null");
    if (authPolicy != null) {
      Assert.hasText(authPolicy);
    }

    this.core = core;
    this.solrServer = solrServer;
    this.credentials = credentials;
    this.authPolicy = authPolicy;

    appendCoreToBaseUrl(this.core, this.solrServer);
    appendAuthentication(this.credentials, this.authPolicy, this.solrServer);
  }

  @Override
  public SolrServer getSolrServer() {
    return this.solrServer;
  }

  @Override
  public String getCore() {
    return this.core;
  }

  private void appendCoreToBaseUrl(String core, SolrServer solrServer) {
    if (StringUtils.isNotEmpty(core) && assertSolrServerInstance(solrServer)) {
      HttpSolrServer httpSolrServer = (HttpSolrServer) solrServer;

      String url = httpSolrServer.getBaseURL();
      if (!StringUtils.endsWith(SLASH, url)) {
        url = url + SLASH;
      }
      url = url + core;
      httpSolrServer.setBaseURL(url);
    }
  }

  private void appendAuthentication(Credentials credentials, String authPolicy, SolrServer solrServer) {
    if (assertSolrServerInstance(solrServer)) {
      HttpSolrServer httpSolrServer = (HttpSolrServer) solrServer;

      if (credentials != null && StringUtils.isNotBlank(authPolicy) && assertHttpClientInstance(httpSolrServer.getHttpClient())) {
        AbstractHttpClient httpClient = (AbstractHttpClient) httpSolrServer.getHttpClient();
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY), credentials);
        httpClient.getParams().setParameter(AuthPNames.TARGET_AUTH_PREF, Arrays.asList(authPolicy));
      }
    }
  }

  private boolean assertSolrServerInstance(SolrServer solrServer) {
    return (solrServer instanceof HttpSolrServer);
  }

  private boolean assertHttpClientInstance(HttpClient httpClient) {
    Assert.isInstanceOf(AbstractHttpClient.class, httpClient,
        "HttpClient has to be derivate of AbstractHttpClient in order to allow authentication.");
    return true;
  }

  @Override
  public void destroy() {
    if (solrServer instanceof HttpSolrServer) {
      ((HttpSolrServer) solrServer).shutdown();
    }
  }

}
