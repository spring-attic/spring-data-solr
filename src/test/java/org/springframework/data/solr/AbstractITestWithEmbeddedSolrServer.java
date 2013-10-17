/*
 * Copyright 2012 - 2013 the original author or authors.
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
package org.springframework.data.solr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;

/**
 * @author Christoph Strobl
 */
public abstract class AbstractITestWithEmbeddedSolrServer {

	protected static SolrServer solrServer;
	protected static String DEFAULT_BEAN_ID = "1";

	@BeforeClass
	public static void initSolrServer() throws IOException, ParserConfigurationException, SAXException,
			InterruptedException {
		String solrHome = ResourceUtils.getURL("classpath:org/springframework/data/solr").getPath();
		CoreContainer coreContainer = CoreContainer.createAndLoad(solrHome, new File(solrHome + "/solr.xml"));
		solrServer = new EmbeddedSolrServer(coreContainer, null);
	}

	public static void cleanDataInSolr() throws SolrServerException, IOException {
		solrServer.deleteByQuery("*:*");
		solrServer.commit();
	}

	@AfterClass
	public static void shutdown() throws SolrServerException, IOException {
		cleanDataInSolr();
		solrServer.shutdown();
	}

	public ExampleSolrBean createDefaultExampleBean() {
		return createExampleBeanWithId(DEFAULT_BEAN_ID);
	}

	public ExampleSolrBean createExampleBeanWithId(String id) {
		return new ExampleSolrBean(id, "bean_" + id, "category_" + id);
	}

	public List<ExampleSolrBean> createBeansWithId(int nrObjectsToCreate) {
		return createBeansWithIdAndPrefix(nrObjectsToCreate, null);
	}

	public List<ExampleSolrBean> createBeansWithIdAndPrefix(int nrObjectsToCreate, String idPrefix) {
		ArrayList<ExampleSolrBean> list = new ArrayList<ExampleSolrBean>(nrObjectsToCreate);
		for (int i = 1; i <= nrObjectsToCreate; i++) {
			list.add(createExampleBeanWithId(idPrefix != null ? (idPrefix + Integer.toString(i)) : Integer.toString(i)));
		}
		return list;
	}

}
