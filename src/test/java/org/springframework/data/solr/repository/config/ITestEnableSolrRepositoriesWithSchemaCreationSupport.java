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

import org.apache.solr.client.solrj.SolrClient;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator.Feature;
import org.springframework.data.solr.repository.support.SimpleSolrRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Christoph Strobl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ITestEnableSolrRepositoriesWithSchemaCreationSupport extends AbstractITestWithEmbeddedSolrServer {

	@Autowired ApplicationContext context;

	@Configuration
	@EnableSolrRepositories(schemaCreationSupport = true, multicoreSupport = true)
	static class Config {

		@Bean
		public SolrClient solrClient() {
			return server.getSolrClient("collection1");
		}
	}

	@Autowired PersonRepository repository;

	@SuppressWarnings("rawtypes")
	@Test // DATASOLR-72
	public void bootstrapsRepository() throws Exception {

		Assume.assumeTrue(repository instanceof Advised);

		SimpleSolrRepository simpleSolrRepository = (SimpleSolrRepository) ((Advised) repository).getTargetSource()
				.getTarget();
		SolrTemplate solrTemplate = (SolrTemplate) simpleSolrRepository.getSolrOperations();
		solrTemplate.getSchemaCreationFeatures().contains(Feature.CREATE_MISSING_FIELDS);

		Assert.assertThat(repository, is(notNullValue()));
	}

}
