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

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.Assert;

import com.google.soldockr.SolDockRException;
import com.google.soldockr.SolrServerFactory;
import com.google.soldockr.core.query.Query;

public class SolrTemplate implements SolrOperations, InitializingBean, ApplicationContextAware {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrTemplate.class);
  private static final QueryParser DEFAULT_QUERY_PARSER = new QueryParser();

  private SolrServerFactory solrServerFactory;
  private QueryParser queryParser = DEFAULT_QUERY_PARSER;

  public SolrTemplate(SolrServer solrServer, String core) {
    this(new SimpleSolrServerFactory(solrServer, core));
  }

  public SolrTemplate(SolrServerFactory solrServerFactory) {
    Assert.notNull(solrServerFactory, "SolrServerFactory must not be null.");
    Assert.notNull(solrServerFactory.getSolrServer(), "SolrServerFactory has to return a SolrServer.");

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
  public UpdateResponse executeAddBean(final Object objectToAdd) {
    return execute(new SolrCallback<UpdateResponse>() {
      @Override
      public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.addBean(objectToAdd);
      }
    });
  }

  @Override
  public UpdateResponse executeAddBeans(final Collection<?> beansToAdd) {
    return execute(new SolrCallback<UpdateResponse>() {
      @Override
      public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.addBeans(beansToAdd);
      }
    });
  }

  @Override
  public UpdateResponse executeAddDocument(final SolrInputDocument documentToAdd) {
    return execute(new SolrCallback<UpdateResponse>() {
      @Override
      public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.add(documentToAdd);
      }
    });
  }

  @Override
  public UpdateResponse executeAddDocuments(final Collection<SolrInputDocument> documentsToAdd) {
    return execute(new SolrCallback<UpdateResponse>() {
      @Override
      public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.add(documentsToAdd);
      }
    });
  }
  
  @Override
  public UpdateResponse executeDelete(Query query) {
    Assert.notNull(query, "Query must not be null.");
    final String queryString = this.queryParser.getQueryString(query);
    
    return execute(new SolrCallback<UpdateResponse>() {
      @Override
      public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.deleteByQuery(queryString);
      }
    });
  }
  
  @Override
  public UpdateResponse executeDeleteById(final String id) {
    return execute(new SolrCallback<UpdateResponse>() {
      @Override
      public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.deleteById(id);
      }
    });
  }

  @Override
  public <T> T executeObjectQuery(Query query, Class<T> clazz) {
    Assert.notNull(query, "Query must not be null.");
    Assert.notNull(clazz, "Target class must not be null.");

    query.setPageRequest(new PageRequest(0, 1));
    QueryResponse response = executeQuery(query);

    if (response.getResults().size() > 0) {
      if (response.getResults().size() > 1) {
        LOGGER.warn("More than 1 result found for singe result query ('{}'), returning first entry in list");
      }
      return response.getBeans(clazz).get(0);
    }
    return null;
  }

  public <T> Page<T> executeListQuery(Query query, Class<T> clazz) {
    Assert.notNull(query, "Query must not be null.");
    Assert.notNull(clazz, "Target class must not be null.");

    // queryParser.assertNoFacets(query);

    QueryResponse response = executeQuery(query);
    // if (query.hasGroupBy() && query.getGroupBy().size() > 1) {
    // return SolrResultHelper.flattenGroupedQueryResult(query, response, clazz, getSolrServer().getBinder());
    // }
    return new PageImpl<T>(response.getBeans(clazz), query.getPageRequest(), response.getResults().getNumFound());
  }

  public final QueryResponse executeQuery(Query query) {
    Assert.notNull(query, "Query must not be null");

    SolrQuery solrQuery = queryParser.constructSolrQuery(query);
   LOGGER.debug("Executing query '" + solrQuery + "' againes solr.");

    return executeSolrQuery(solrQuery);
  }

  final QueryResponse executeSolrQuery(final SolrQuery solrQuery) {
    return execute(new SolrCallback<QueryResponse>() {
      @Override
      public QueryResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.query(solrQuery);
      }
    });
  }

  @Override
  public final SolrServer getSolrServer() {
    return solrServerFactory.getSolrServer();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    // TODO Auto-generated method stub
  }

  @Override
  public void afterPropertiesSet() {
    if (this.queryParser == null) {
      LOGGER.warn("QueryParser not set, using default one.");
      queryParser = DEFAULT_QUERY_PARSER;
    }
  }

  @Override
  public void executeCommit() {
    execute(new SolrCallback<UpdateResponse>() {
      @Override
      public UpdateResponse doInSolr(SolrServer solrServer) throws SolrServerException, IOException {
        return solrServer.commit();
      }
    });
  }



  
}
