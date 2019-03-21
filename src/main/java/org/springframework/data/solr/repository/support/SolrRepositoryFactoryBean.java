/*
 * Copyright 2012 - 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.repository.support;

import java.io.Serializable;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.util.Assert;

/**
 * Spring {@link FactoryBean} implementation to ease container based configuration for XML namespace and JavaConfig.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class SolrRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
		extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

	private SolrClient solrClient;
	private SolrOperations operations;
	private boolean schemaCreationSupport;
	private SimpleSolrMappingContext solrMappingContext;
	private SolrConverter solrConverter;

	/**
	 * Configures the {@link SolrOperations} to be used to create Solr repositories.
	 * 
	 * @param operations the operations to set
	 */
	public void setSolrOperations(SolrOperations operations) {
		this.operations = operations;
	}

	public void setSolrClient(SolrClient solrClient) {
		this.solrClient = solrClient;
	}

	public void setSchemaCreationSupport(boolean schemaCreationSupport) {
		this.schemaCreationSupport = schemaCreationSupport;
	}

	/**
	 * @param solrConverter
	 * @since 2.1
	 */
	public void setSolrConverter(SolrConverter solrConverter) {
		this.solrConverter = solrConverter;
	}

	/**
	 * @param solrMappingContext
	 * @since 1.4
	 */
	public void setSolrMappingContext(SimpleSolrMappingContext solrMappingContext) {
		this.solrMappingContext = solrMappingContext;
		super.setMappingContext(solrMappingContext);
	}

	/**
	 * @return
	 * @since 1.4
	 */
	public SimpleSolrMappingContext getSolrMappingContext() {
		return solrMappingContext;
	}

	/**
	 * @return SolrOperations to be used for eg. custom implementation
	 */
	protected SolrOperations getSolrOperations() {
		return this.operations;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {

		super.afterPropertiesSet();
		Assert.isTrue((operations != null || solrClient != null), "SolrOperations or SolrClient must be configured!");
	}

	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {

		SolrRepositoryFactory factory = operations != null ? new SolrRepositoryFactory(this.operations)
				: new SolrRepositoryFactory(this.solrClient, solrConverter);
		factory.setSchemaCreationSupport(schemaCreationSupport);
		return factory;
	}
}
