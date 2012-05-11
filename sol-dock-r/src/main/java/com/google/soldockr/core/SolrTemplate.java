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
import java.util.Collection;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import com.google.soldockr.SolDockRException;
import com.google.soldockr.SolrServerFactory;

public class SolrTemplate implements SolrOperations, ApplicationContextAware {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrTemplate.class);

  private SolrServerFactory solrServerFactory;

  public SolrTemplate(SolrServer solrServer, String core) {
    this(new SimpleSolrServerFactory(solrServer, core));
  }

  public SolrTemplate(SolrServerFactory solrServerFactory) {
    Assert.notNull(solrServerFactory, "SolrServerFactory must not be null");

    this.solrServerFactory = solrServerFactory;
  }

  public <T> T execute(SolrCallback<T> action) {
    Assert.notNull(action);

    try {
      SolrServer solrServer = this.getSolrServer();
      return action.doInSolr(solrServer);
    } catch (Exception e) {
      throw new SolDockRException(e);
    }
  }
  
  @Override
  public SolrPingResponse executePing() {
    return execute(new SolrCallback<SolrPingResponse>() {
      @Override
      public SolrPingResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.ping();
      }
    });
  }

  @Override
  public UpdateResponse addBean(final Object objectToAdd) {
    return execute(new SolrCallback<UpdateResponse>() {
      @Override
      public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.addBean(objectToAdd);
      }
    });
  }

  @Override
  public UpdateResponse addBeans(final Collection<?> beansToAdd) {
    return execute(new SolrCallback<UpdateResponse>() {
      @Override
      public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.addBeans(beansToAdd);
      }
    });
  }

  @Override
  public UpdateResponse addDocument(final SolrInputDocument documentToAdd) {
    return execute(new SolrCallback<UpdateResponse>() {
      @Override
      public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.add(documentToAdd);
      }
    });
  }

  @Override
  public UpdateResponse addDocuments(final Collection<SolrInputDocument> documentsToAdd) {
    return execute(new SolrCallback<UpdateResponse>() {
      @Override
      public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.add(documentsToAdd);
      }
    });
  }
  
  @Override
  public final SolrServer getSolrServer() {
    return solrServerFactory.getSolrServer();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    // TODO Auto-generated method stub
  }

}
