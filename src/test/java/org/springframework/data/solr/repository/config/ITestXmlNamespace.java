/*
 * Copyright 2012 the original author or authors.
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.apache.solr.client.solrj.SolrServer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Integration test for XML namespace configuration.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class ITestXmlNamespace {

	ApplicationContext context;

	@Before
	public void setUp() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
		context.setConfigLocation("classpath:org/springframework/data/solr/repository/config/namespace.xml");
		context.refresh();

		this.context = context;
	}

	@Test
	public void createsRepositoryAndEmbeddedServerCorrectly() {
		assertThat(context.getBean(PersonRepository.class), is(notNullValue()));
		assertThat(context.getBean(SolrServer.class), is(notNullValue()));
	}
}
