/*
 * Copyright 2012 - 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.repository.support;

import static org.springframework.data.querydsl.QueryDslUtils.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator.Feature;
import org.springframework.data.solr.repository.SolrRepository;
import org.springframework.data.solr.repository.query.PartTreeSolrQuery;
import org.springframework.data.solr.repository.query.SolrEntityInformation;
import org.springframework.data.solr.repository.query.SolrEntityInformationCreator;
import org.springframework.data.solr.repository.query.SolrQueryMethod;
import org.springframework.data.solr.repository.query.StringBasedSolrQuery;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.data.solr.server.support.MulticoreSolrClientFactory;
import org.springframework.data.solr.server.support.SolrClientUtils;
import org.springframework.util.Assert;

/**
 * Factory to create {@link SolrRepository}
 * 
 * @author Christoph Strobl
 */
public class SolrRepositoryFactory extends RepositoryFactorySupport {

	private SolrOperations solrOperations;
	private final SolrEntityInformationCreator entityInformationCreator;
	private SolrClientFactory factory;
	private SolrTemplateHolder templateHolder = new SolrTemplateHolder();
	private boolean schemaCreationSupport;

	public SolrRepositoryFactory(SolrOperations solrOperations) {
		Assert.notNull(solrOperations);

		if (solrOperations instanceof SolrTemplate) {
			addSchemaCreationFeaturesIfEnabled((SolrTemplate) solrOperations);
		}

		this.solrOperations = solrOperations;
		this.entityInformationCreator = new SolrEntityInformationCreatorImpl(
				solrOperations.getConverter().getMappingContext());
	}

	public SolrRepositoryFactory(SolrClient solrClient) {
		Assert.notNull(solrClient);

		this.solrOperations = createTemplate(solrClient, null);

		factory = new MulticoreSolrClientFactory(solrClient);
		this.entityInformationCreator = new SolrEntityInformationCreatorImpl(
				this.solrOperations.getConverter().getMappingContext());

	}

	public SolrRepositoryFactory(SolrClient solrClient, SolrConverter converter) {
		Assert.notNull(solrClient);

		this.solrOperations = createTemplate(solrClient, converter);

		factory = new MulticoreSolrClientFactory(solrClient);
		this.entityInformationCreator = new SolrEntityInformationCreatorImpl(
				this.solrOperations.getConverter().getMappingContext());

	}

	private SolrTemplate createTemplate(SolrClient solrClient, SolrConverter converter) {

		SolrTemplate template = new SolrTemplate(solrClient);

		if (converter != null) {
			template.setSolrConverter(converter);
		}
		addSchemaCreationFeaturesIfEnabled(template);
		template.afterPropertiesSet();
		return template;
	}

	@Override
	public <T, ID extends Serializable> SolrEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return entityInformationCreator.getEntityInformation(domainClass);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Object getTargetRepository(RepositoryInformation metadata) {

		SolrOperations operations = this.solrOperations;
		if (factory != null) {
			SolrTemplate template = new SolrTemplate(factory);
			if (this.solrOperations.getConverter() != null) {

				template.setMappingContext(this.solrOperations.getConverter().getMappingContext());
				template.setSolrConverter(this.solrOperations.getConverter());
			}
			template.setSolrCore(SolrClientUtils.resolveSolrCoreName(metadata.getDomainType()));
			addSchemaCreationFeaturesIfEnabled(template);
			template.afterPropertiesSet();
			operations = template;
		}

		SimpleSolrRepository repository = getTargetRepositoryViaReflection(metadata,
				getEntityInformation(metadata.getDomainType()), operations);
		repository.setEntityClass(metadata.getDomainType());

		this.templateHolder.add(metadata.getDomainType(), operations);
		return repository;
	}

	private void addSchemaCreationFeaturesIfEnabled(SolrTemplate template) {
		if (isSchemaCreationSupport()) {
			template.setSchemaCreationFeatures(Collections.singletonList(Feature.CREATE_MISSING_FIELDS));
		}
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		if (isQueryDslRepository(metadata.getRepositoryInterface())) {
			throw new IllegalArgumentException("QueryDsl Support has not been implemented yet.");
		}
		return SimpleSolrRepository.class;
	}

	private static boolean isQueryDslRepository(Class<?> repositoryInterface) {
		return QUERY_DSL_PRESENT && QueryDslPredicateExecutor.class.isAssignableFrom(repositoryInterface);
	}

	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(Key key) {
		return new SolrQueryLookupStrategy();
	}

	/**
	 * @return
	 * @since 1.3
	 */
	public boolean isSchemaCreationSupport() {
		return schemaCreationSupport;
	}

	/**
	 * @param schemaCreationSupport
	 * @since 1.3
	 */
	public void setSchemaCreationSupport(boolean schemaCreationSupport) {
		this.schemaCreationSupport = schemaCreationSupport;
	}

	private class SolrQueryLookupStrategy implements QueryLookupStrategy {

		@Override
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {

			SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, factory, entityInformationCreator);
			String namedQueryName = queryMethod.getNamedQueryName();

			SolrOperations solrOperations = selectSolrOperations(metadata);

			if (namedQueries.hasQuery(namedQueryName)) {
				String namedQuery = namedQueries.getQuery(namedQueryName);
				return new StringBasedSolrQuery(namedQuery, queryMethod, solrOperations);
			} else if (queryMethod.hasAnnotatedQuery()) {
				return new StringBasedSolrQuery(queryMethod, solrOperations);
			} else {
				return new PartTreeSolrQuery(queryMethod, solrOperations);
			}
		}

		private SolrOperations selectSolrOperations(RepositoryMetadata metadata) {
			SolrOperations ops = templateHolder.getSolrOperations(metadata.getDomainType());
			if (ops == null) {
				ops = solrOperations;
			}
			return ops;
		}

	}

	private static class SolrTemplateHolder {

		private Map<Class<?>, SolrOperations> operationsMap = new WeakHashMap<Class<?>, SolrOperations>();

		void add(Class<?> domainType, SolrOperations repository) {
			operationsMap.put(domainType, repository);
		}

		SolrOperations getSolrOperations(Class<?> type) {
			return operationsMap.get(type);
		}
	}

}
