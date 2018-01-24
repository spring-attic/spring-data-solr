/*
 * Copyright 2012-2017 the original author or authors.
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

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.server.support.EmbeddedSolrServerFactory;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;

/**
 * @author Christoph Strobl
 */
@ApplicationScoped
class SolrTemplateProducer {

	@Produces
	public SolrOperations createSolrTemplate() throws IOException, ParserConfigurationException, SAXException {

		EmbeddedSolrServerFactory factory = new EmbeddedSolrServerFactory(
				ResourceUtils.getURL("classpath:static-schema").getPath());

		SolrTemplate template = new SolrTemplate(factory);
		template.afterPropertiesSet();
		return template;
	}

	@PreDestroy
	public void shutdown() {
		// remove everything to avoid conflicts with other tests in case server not shut down properly
		deleteAll();
	}

	private void deleteAll() {
		SolrOperations template;
		try {
			template = createSolrTemplate();
			template.delete("collection1", new SimpleQuery(new SimpleStringCriteria("*:*")));
			template.commit("collection1");
		} catch (IOException | SAXException | ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

}
