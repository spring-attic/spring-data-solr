/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.solr.repository.config;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Christoph Strobl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EnableSolrRepositoriesWithPredefinedMappingContextTests extends AbstractITestWithEmbeddedSolrServer {

	private static final SimpleSolrMappingContext SOLR_MAPPING_CONTEXT = new SimpleSolrMappingContext();

	@Configuration
	@EnableSolrRepositories
	static class Config {

		@Bean
		public SolrOperations solrTemplate() {
			return new SolrTemplate(solrClient());
		}

		@Bean
		public SolrClient solrClient() {
			return server.getSolrClient("collection1");
		}

		@Bean
		public SimpleSolrMappingContext solrMappingContext() {
			return SOLR_MAPPING_CONTEXT;
		}
	}

	@Autowired ApplicationContext context;

	@Test // DATASOLR-163
	public void shouldUseExistingMappingContextWhenPresent() {
		assertThat((SimpleSolrMappingContext) context.getBean("solrMappingContext"), is(SOLR_MAPPING_CONTEXT));
	}
}
