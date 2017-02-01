/*
 * Copyright 2012 - 2017 the original author or authors.
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
package org.springframework.data.solr.repository.support;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.solr.common.SolrInputDocument;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTransactionSynchronizationAdapterBuilder;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SolrPageRequest;
import org.springframework.data.solr.repository.SolrCrudRepository;
import org.springframework.data.solr.repository.query.SolrEntityInformation;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Solr specific repository implementation. Likely to be used as target within {@link SolrRepositoryFactory}
 * 
 * @param <T>
 * @Param <ID>
 * @author Christoph Strobl
 */
public class SimpleSolrRepository<T, ID extends Serializable> implements SolrCrudRepository<T, ID> {

	private static final String DEFAULT_ID_FIELD = "id";

	private SolrOperations solrOperations;
	private String idFieldName = DEFAULT_ID_FIELD;
	private Class<T> entityClass;
	private SolrEntityInformation<T, ?> entityInformation;

	public SimpleSolrRepository() {

	}

	/**
	 * @param solrOperations must not be null
	 */
	public SimpleSolrRepository(SolrOperations solrOperations) {
		Assert.notNull(solrOperations, "SolrOperations must not be null!");

		this.setSolrOperations(solrOperations);
	}

	/**
	 * @param metadata must not be null
	 * @param solrOperations must not be null
	 */
	public SimpleSolrRepository(SolrEntityInformation<T, ?> metadata, SolrOperations solrOperations) {
		this(solrOperations);
		Assert.notNull(metadata, "Metadata must not be null!");

		this.entityInformation = metadata;
		setIdFieldName(this.entityInformation.getIdAttribute());
		setEntityClass(this.entityInformation.getJavaType());
	}

	/**
	 * @param solrOperations must not be null
	 * @param entityClass
	 */
	public SimpleSolrRepository(SolrOperations solrOperations, Class<T> entityClass) {
		this(solrOperations);

		this.setEntityClass(entityClass);
	}

	@Override
	public T findOne(ID id) {
		return getSolrOperations().queryForObject(new SimpleQuery(new Criteria(this.idFieldName).is(id)), getEntityClass());
	}

	@Override
	public Iterable<T> findAll() {
		int itemCount = (int) this.count();
		if (itemCount == 0) {
			return new PageImpl<T>(Collections.<T> emptyList());
		}
		return this.findAll(new SolrPageRequest(0, itemCount));
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		return getSolrOperations().queryForPage(
				new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)).setPageRequest(pageable),
				getEntityClass());
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		int itemCount = (int) this.count();
		if (itemCount == 0) {
			return new PageImpl<T>(Collections.<T> emptyList());
		}
		return getSolrOperations().queryForPage(
				new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)).setPageRequest(
						new SolrPageRequest(0, itemCount)).addSort(sort), getEntityClass());
	}

	@Override
	public Iterable<T> findAll(Iterable<ID> ids) {
		org.springframework.data.solr.core.query.Query query = new SimpleQuery(new Criteria(this.idFieldName).in(ids));
		query.setPageRequest(new SolrPageRequest(0, (int) count(query)));

		return getSolrOperations().queryForPage(query, getEntityClass());
	}

	@Override
	public long count() {
		return count(new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)));
	}

	protected long count(org.springframework.data.solr.core.query.Query query) {
		org.springframework.data.solr.core.query.Query countQuery = SimpleQuery.fromQuery(query);
		return getSolrOperations().count(countQuery);
	}

	@Override
	public <S extends T> S save(S entity) {
		Assert.notNull(entity, "Cannot save 'null' entity.");
		registerTransactionSynchronisationIfSynchronisationActive();
		this.solrOperations.saveBean(entity);
		commitIfTransactionSynchronisationIsInactive();
		return entity;
	}

	@Override
	public <S extends T> Iterable<S> save(Iterable<S> entities) {
		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		if (!(entities instanceof Collection<?>)) {
			throw new InvalidDataAccessApiUsageException("Entities have to be inside a collection");
		}

		registerTransactionSynchronisationIfSynchronisationActive();
		this.solrOperations.saveBeans((Collection<? extends T>) entities);
		commitIfTransactionSynchronisationIsInactive();
		return entities;
	}

	@Override
	public boolean exists(ID id) {
		return findOne(id) != null;
	}

	@Override
	public void delete(ID id) {
		Assert.notNull(id, "Cannot delete entity with id 'null'.");

		registerTransactionSynchronisationIfSynchronisationActive();
		this.solrOperations.deleteById(id.toString());
		commitIfTransactionSynchronisationIsInactive();
	}

	@Override
	public void delete(T entity) {
		Assert.notNull(entity, "Cannot delete 'null' entity.");

		delete(Arrays.asList(entity));
	}

	@Override
	public void delete(Iterable<? extends T> entities) {
		Assert.notNull(entities, "Cannot delete 'null' list.");

		ArrayList<String> idsToDelete = new ArrayList<String>();
		for (T entity : entities) {
			idsToDelete.add(extractIdFromBean(entity).toString());
		}

		registerTransactionSynchronisationIfSynchronisationActive();
		this.solrOperations.deleteById(idsToDelete);
		commitIfTransactionSynchronisationIsInactive();
	}

	@Override
	public void deleteAll() {
		registerTransactionSynchronisationIfSynchronisationActive();
		this.solrOperations.delete(new SimpleFilterQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)));
		commitIfTransactionSynchronisationIsInactive();
	}

	public final String getIdFieldName() {
		return idFieldName;
	}

	public final void setIdFieldName(String idFieldName) {
		Assert.notNull(idFieldName, "ID Field cannot be null.");

		this.idFieldName = idFieldName;
	}

	@SuppressWarnings("unchecked")
	private Class<T> resolveReturnedClassFromGernericType() {
		ParameterizedType parameterizedType = resolveReturnedClassFromGernericType(getClass());
		return (Class<T>) parameterizedType.getActualTypeArguments()[0];
	}

	private ParameterizedType resolveReturnedClassFromGernericType(Class<?> clazz) {
		Object genericSuperclass = clazz.getGenericSuperclass();
		if (genericSuperclass instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
			Type rawtype = parameterizedType.getRawType();
			if (SimpleSolrRepository.class.equals(rawtype)) {
				return parameterizedType;
			}
		}
		return resolveReturnedClassFromGernericType(clazz.getSuperclass());
	}

	public Class<T> getEntityClass() {
		if (!isEntityClassSet()) {
			try {
				this.entityClass = resolveReturnedClassFromGernericType();
			} catch (Exception e) {
				throw new InvalidDataAccessApiUsageException("Unable to resolve EntityClass. Please use according setter!", e);
			}
		}
		return entityClass;
	}

	private boolean isEntityClassSet() {
		return entityClass != null;
	}

	public final void setEntityClass(Class<T> entityClass) {
		Assert.notNull(entityClass, "EntityClass must not be null.");

		this.entityClass = entityClass;
	}

	public final void setSolrOperations(SolrOperations solrOperations) {
		Assert.notNull(solrOperations, "SolrOperations must not be null.");

		this.solrOperations = solrOperations;
	}

	public final SolrOperations getSolrOperations() {
		return solrOperations;
	}

	private Object extractIdFromBean(T entity) {
		if (entityInformation != null) {
			return entityInformation.getId(entity);
		}

		SolrInputDocument solrInputDocument = this.solrOperations.convertBeanToSolrInputDocument(entity);
		return extractIdFromSolrInputDocument(solrInputDocument);
	}

	private String extractIdFromSolrInputDocument(SolrInputDocument solrInputDocument) {
		Assert.notNull(solrInputDocument.getField(idFieldName), "Unable to find field '" + idFieldName
				+ "' in SolrDocument.");
		Assert.notNull(solrInputDocument.getField(idFieldName).getValue(), "ID must not be 'null'.");

		return solrInputDocument.getField(idFieldName).getValue().toString();
	}

	private void registerTransactionSynchronisationIfSynchronisationActive() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			registerTransactionSynchronisationAdapter();
		}
	}

	private void registerTransactionSynchronisationAdapter() {
		TransactionSynchronizationManager.registerSynchronization(SolrTransactionSynchronizationAdapterBuilder
				.forOperations(this.solrOperations).withDefaultBehaviour());
	}

	private void commitIfTransactionSynchronisationIsInactive() {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			this.solrOperations.commit();
		}
	}

}
