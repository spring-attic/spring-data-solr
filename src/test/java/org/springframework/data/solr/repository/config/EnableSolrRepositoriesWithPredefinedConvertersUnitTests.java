/*
 * Copyright 2016-2017 the original author or authors.
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

import static org.hamcrest.core.IsSame.*;
import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.convert.MappingSolrConverter;
import org.springframework.data.solr.core.convert.SolrCustomConversions;
import org.springframework.data.solr.repository.ProductBean;
import org.springframework.data.solr.repository.support.SimpleSolrRepository;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Christoph Strobl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EnableSolrRepositoriesWithPredefinedConvertersUnitTests extends AbstractITestWithEmbeddedSolrServer {

	private static final CustomConversions CUSTOM_CONVERSIONS = new SolrCustomConversions(Collections.emptyList());

	@Configuration
	@EnableSolrRepositories(considerNestedRepositories = true)
	static class Config extends AbstractSolrConfiguration {

		@Override
		public SolrClientFactory solrClientFactory() {
			return server;
		}

		@Bean
		public CustomConversions customConversions() {
			CustomConversions conversions = CUSTOM_CONVERSIONS;
			return conversions;
		}

	}

	@Autowired ApplicationContext context;

	@Autowired ProductRepository repo;

	@Test // DATASOLR-163
	public void shouldUseExistingMappingContextWhenPresent() throws BeansException, IllegalArgumentException, Exception {

		SimpleSolrRepository target = (SimpleSolrRepository) ((org.springframework.aop.framework.AdvisedSupport) new org.springframework.beans.DirectFieldAccessor(
				Proxy.getInvocationHandler(repo)).getPropertyValue("advised")).getTargetSource().getTarget();

		assertThat(
				((MappingSolrConverter) ((SolrTemplate) target.getSolrOperations()).getConverter()).getCustomConversions(),
				sameInstance(CUSTOM_CONVERSIONS));
	}

	interface ProductRepository extends CrudRepository<ProductBean, String> {

	}
}
