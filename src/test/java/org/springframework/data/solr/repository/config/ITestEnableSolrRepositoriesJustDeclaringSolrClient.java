/*
 * Copyright 2017 the original author or authors.
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration test for {@link EnableSolrRepositories} just declaring a {@link SolrClient} {@link Bean} but no
 * {@link org.springframework.data.solr.core.SolrTemplate}.
 * 
 * @author Christoph Strobl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ITestEnableSolrRepositoriesJustDeclaringSolrClient extends AbstractITestWithEmbeddedSolrServer {

	@Configuration
	@EnableSolrRepositories
	static class Config {

		@Bean
		public SolrClient solrClient() {
			return server.getSolrClient("collection1");
		}

	}

	@Autowired PersonRepository repository;

	@Autowired ApplicationContext context;

	@Test
	public void bootstrapsRepository() {
		assertThat(repository, is(notNullValue()));
	}

	@Test // DATASOLR-163
	public void shouldRegisterMappingContextWhenNotPresent() {
		assertThat(context.containsBean("solrMappingContext"), is(true));
	}

	@Test // DATASOLR-372
	public void shouldRegisterSolrTemplateWhenNotPresent() {
		assertThat(context.containsBean("solrTemplate"), is(true));
	}
}
