/*
 * Copyright 2012 - 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.config;

import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.data.repository.config.RepositoryBeanDefinitionParser;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.solr.repository.config.SolrRepositoryConfigExtension;
import org.springframework.data.solr.server.config.EmbeddedSolrServerBeanDefinitionParser;
import org.springframework.data.solr.server.config.HttpSolrClientBeanDefinitionParser;

/**
 * {@link NamespaceHandler} implementation to register parser for {@code <solr:repositories />},
 * {@code <solr:embedded-solr-server solrHome="path/to/solr/home/directory" />} elements.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
class SolrNamespaceHandler extends NamespaceHandlerSupport {

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
	 */
	@Override
	public void init() {

		RepositoryConfigurationExtension extension = new SolrRepositoryConfigExtension();
		RepositoryBeanDefinitionParser parser = new RepositoryBeanDefinitionParser(extension);

		registerBeanDefinitionParser("repositories", parser);
		registerBeanDefinitionParser("embedded-solr-server", new EmbeddedSolrServerBeanDefinitionParser());
		registerBeanDefinitionParser("solr-client", new HttpSolrClientBeanDefinitionParser());
	}
}
