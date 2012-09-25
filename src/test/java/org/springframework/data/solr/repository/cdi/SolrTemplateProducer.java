/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.solr.repository.cdi;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.springframework.data.solr.HttpSolrServerFactory;
import org.springframework.data.solr.SolrServerFactory;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;

/**
 * @author Christoph Strobl
 */
class SolrTemplateProducer {

	@Produces
	@ApplicationScoped
	public SolrOperations createSolrTemplate() throws IOException, ParserConfigurationException, SAXException {
		SolrServer solrServer = getSolrServerInstance();
		SolrServerFactory factory = new HttpSolrServerFactory(solrServer);
		return new SolrTemplate(factory);
	}

	private SolrServer getSolrServerInstance() throws IOException, ParserConfigurationException, SAXException {
		System.setProperty("solr.solr.home", ResourceUtils.getURL("classpath:org/springframework/data/solr").getPath());
		CoreContainer.Initializer initializer = new CoreContainer.Initializer();
		CoreContainer coreContainer = initializer.initialize();
		return new EmbeddedSolrServer(coreContainer, "");
	}

}
