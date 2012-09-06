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
package at.pagu.soldockr.repository.cdi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import at.pagu.soldockr.SolrServerFactory;
import at.pagu.soldockr.core.HttpSolrServerFactory;
import at.pagu.soldockr.core.SolrOperations;
import at.pagu.soldockr.core.SolrTemplate;

/**
 * @author Christoph Strobl
 */
class SolrTemplateProducer {

  private static final String SOLR_SERVER_URL = "http://localhost:8983/solr";

  @Produces
  @ApplicationScoped
  public SolrOperations createSolrTemplate() {
    SolrServer solrServer = new HttpSolrServer(SOLR_SERVER_URL);
    SolrServerFactory factory = new HttpSolrServerFactory(solrServer);
    return new SolrTemplate(factory);
  }

}
