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
package org.springframework.data.solr.core;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.springframework.data.solr.SolrServerFactory;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;

/**
 * The EmbeddedSolrServerFactory allows hosting of an SolrServer instance in embedded mode. Configuration files are
 * loaded via {@link ResourceUtils}, therefore it is possible to place them in classpath. Use this class for Testing. It
 * is not recommended for production.
 * 
 * @author Christoph Strobl
 */
public class EmbeddedSolrServerFactory implements SolrServerFactory {

	private static final String SOLR_HOME_SYSTEM_PROPERTY = "solr.solr.home";

	private EmbeddedSolrServer solrServer;

	/**
	 * @param path Any Path expression valid for use with {@link ResourceUtils}
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public EmbeddedSolrServerFactory(String path) throws ParserConfigurationException, IOException, SAXException {
		this.solrServer = createPathConfiguredSolrServer(path);
	}

	/**
	 * @param path Any Path expression valid for use with {@link ResourceUtils}
	 * @return new instance of {@link EmbeddedSolrServer}
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public final EmbeddedSolrServer createPathConfiguredSolrServer(String path) throws ParserConfigurationException,
			IOException, SAXException {
		String solrHome = System.getProperty(SOLR_HOME_SYSTEM_PROPERTY);

		if (StringUtils.isBlank(solrHome)) {
			solrHome = StringUtils.remove(ResourceUtils.getURL(path).toString(), "file:/");
		}
		return new EmbeddedSolrServer(new CoreContainer(solrHome, new File(solrHome + "/solr.xml")), null);
	}

	@Override
	public SolrServer getSolrServer() {
		return this.solrServer;
	}

	@Override
	public String getCore() {
		return null;
	}

}
