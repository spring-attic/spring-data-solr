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
package org.springframework.data.solr.embedded.support;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.solr.embedded.EmbeddedSolrServerFactory;

/**
 * Implementation of {@link FactoryBean} for registration of an EmbeddedSolrServer as a Spring bean. Implements
 * {@link DisposableBean} to shut down the core container when the enclosing Spring container is destroyed.
 * 
 * @author Christoph Strobl
 * 
 */
public class EmbeddedSolrServerFactoryBean extends EmbeddedSolrServerFactory implements FactoryBean<SolrServer>,
		InitializingBean, DisposableBean {

	@Override
	public void destroy() throws Exception {
		shutdownSolrServer();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initSolrServer();
	}

	@Override
	public SolrServer getObject() throws Exception {
		return getSolrServer();
	}

	@Override
	public Class<? extends SolrServer> getObjectType() {
		return EmbeddedSolrServer.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
