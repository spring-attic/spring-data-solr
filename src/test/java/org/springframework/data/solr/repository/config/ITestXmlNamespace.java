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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.apache.solr.client.solrj.SolrServer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;

/**
 * Integration thest for XML namespace configuration. We currently need to work around the issue that a
 * {@link SolrServer} instance cannot be easily set up using XML configuration. Thus we manually register the instance
 * created by the superclass with the {@link ApplicationContext} created for this test case. This should be removed once
 * we have decent support for setting up an embedded {@link SolrServer} via Spring Config.
 * 
 * @author Oliver Gierke
 */
public class ITestXmlNamespace extends AbstractITestWithEmbeddedSolrServer {

	ApplicationContext context;

	@Before
	public void setUp() {

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext() {

			@Override
			protected void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
				factory.registerSingleton("solrServer", solrServer);
			}
		};

		context.setConfigLocation("classpath:org/springframework/data/solr/repository/config/namespace.xml");
		context.refresh();

		this.context = context;
	}

	@Test
	public void someTest() {
		assertThat(context.getBean(PersonRepository.class), is(notNullValue()));
	}
}
