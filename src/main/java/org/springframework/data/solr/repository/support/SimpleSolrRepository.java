/*
 * Copyright 2012 - 2018 the original author or authors.
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

import java.io.Serializable;
import java.time.Duration;
import java.util.*;

import org.apache.solr.common.SolrInputDocument;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTransactionSynchronizationAdapterBuilder;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.query.*;
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
 * @author Mark Paluch
 * @author Mayank Kumar
 */
public class SimpleSolrRepository<T, ID extends Serializable> implements SolrCrudRepository<T, ID> {

	private static final String DEFAULT_ID_FIELD = "id";

	private final SolrOperations solrOperations;
	private String idFieldName = DEFAULT_ID_FIELD;
	private final Class<T> entityClass;
	private final String solrCollectionName;
	private final SolrEntityInformation<T, ?> entityInformation;

	/**
	 * @param metadata must not be null
	 * @param solrOperations must not be null
	 */
	public SimpleSolrRepository(SolrOperations solrOperations, SolrEntityInformation<T, ?> metadata) {

		Assert.notNull(metadata, "Metadata must not be null");

		this.solrOperations = solrOperations;
		this.entityInformation = metadata;
		this.entityClass = this.entityInformation.getJavaType();
		this.idFieldName = this.entityInformation.getIdAttribute();
		this.solrCollectionName = this.entityInformation.getCollectionName();
	}

	/**
	 * @param solrOperations must not be null
	 * @param entityClass
	 */
	public SimpleSolrRepository(SolrOperations solrOperations, Class<T> entityClass) {
		this(solrOperations, getEntityInformation(entityClass));
	}

	private static <T, ID> SolrEntityInformation<T, ID> getEntityInformation(Class<T> type) {
		return new SolrEntityInformationCreatorImpl(new SimpleSolrMappingContext()).getEntityInformation(type);
	}

	@Override
	public Optional<T> findById(ID id) {
		return getSolrOperations().queryForObject(solrCollectionName,
				new SimpleQuery(new Criteria(this.idFieldName).is(id)), getEntityClass());
	}

	@Override
	public Iterable<T> findAll() {
		int itemCount = (int) this.count();
		if (itemCount == 0) {
			return new PageImpl<>(Collections.<T> emptyList());
		}
		return this.findAll(new SolrPageRequest(0, itemCount));
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		return getSolrOperations().queryForPage(solrCollectionName,
				new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)).setPageRequest(pageable),
				getEntityClass());
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		int itemCount = (int) this.count();
		if (itemCount == 0) {
			return new PageImpl<>(Collections.<T> emptyList());
		}
		return getSolrOperations().queryForPage(solrCollectionName,
				new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
						.setPageRequest(new SolrPageRequest(0, itemCount)).addSort(sort),
				getEntityClass());
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {
		org.springframework.data.solr.core.query.Query query = new SimpleQuery(new Criteria(this.idFieldName).in(ids));
		query.setPageRequest(new SolrPageRequest(0, (int) count(query)));

		return getSolrOperations().queryForPage(solrCollectionName, query, getEntityClass());
	}

	@Override
	public long count() {
		return count(new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)));
	}

	protected long count(org.springframework.data.solr.core.query.Query query) {
		org.springframework.data.solr.core.query.Query countQuery = SimpleQuery.fromQuery(query);
		Assert.notNull(countQuery, "countQuery cannot be 'null'");
		return getSolrOperations().count(solrCollectionName, countQuery);
	}

	@Override
	public <S extends T> S save(S entity) {
		return save(entity, Duration.ZERO);
	}

	@Override
	public <S extends T> S save(S entity, Duration commitWithin) {
		Assert.notNull(entity, "Cannot save 'null' entity");
		registerTransactionSynchronisationIfSynchronisationActive();
		getSolrOperations().saveBean(solrCollectionName, entity, commitWithin);
		commitIfTransactionSynchronisationIsInactive();
		return entity;
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		return saveAll(entities, Duration.ZERO);
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities, Duration commitWithin) {
		Assert.notNull(entities, "Cannot insert 'null' as a List");

		if (!(entities instanceof Collection<?>)) {
			throw new InvalidDataAccessApiUsageException("Entities have to be inside a collection");
		}

		registerTransactionSynchronisationIfSynchronisationActive();
		this.solrOperations.saveBeans(solrCollectionName, (Collection<? extends T>) entities, commitWithin);
		commitIfTransactionSynchronisationIsInactive();
		return entities;
	}

	@Override
	public boolean existsById(ID id) {
		return findById(id).isPresent();
	}

	@Override
	public void deleteById(ID id) {
		Assert.notNull(id, "Cannot delete entity with id 'null'");

		registerTransactionSynchronisationIfSynchronisationActive();
		this.solrOperations.deleteByIds(solrCollectionName, id.toString());
		commitIfTransactionSynchronisationIsInactive();
	}

	@Override
	public void delete(T entity) {
		Assert.notNull(entity, "Cannot delete 'null' entity");

		deleteAll(Collections.singletonList(entity));
	}

	@Override
	public void deleteAllById(final Iterable<? extends ID> ids) {

		Assert.notNull(ids, "Cannot delete 'null' list.");

		List<String> idStrings = new ArrayList<>();
		for (ID id : ids) {
			if (Objects.nonNull(id)) {
				idStrings.add(String.valueOf(id));
			}
		}

		if (idStrings.isEmpty()) {
			return;
		}
		registerTransactionSynchronisationIfSynchronisationActive();
		this.solrOperations.deleteByIds(solrCollectionName, idStrings);
		commitIfTransactionSynchronisationIsInactive();
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {
		Assert.notNull(entities, "Cannot delete 'null' list");

		ArrayList<String> idsToDelete = new ArrayList<>();
		for (T entity : entities) {
			idsToDelete.add(extractIdFromBean(entity).toString());
		}

		registerTransactionSynchronisationIfSynchronisationActive();
		this.solrOperations.deleteByIds(solrCollectionName, idsToDelete);
		commitIfTransactionSynchronisationIsInactive();
	}

	@Override
	public void deleteAll() {
		registerTransactionSynchronisationIfSynchronisationActive();
		this.solrOperations.delete(solrCollectionName,
				new SimpleFilterQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)));
		commitIfTransactionSynchronisationIsInactive();
	}

	public final String getIdFieldName() {
		return idFieldName;
	}

	public Class<T> getEntityClass() {

		if (!isEntityClassSet()) {
			throw new InvalidDataAccessApiUsageException("Unable to resolve EntityClass; Please use according setter");
		}
		return entityClass;
	}

	private boolean isEntityClassSet() {
		return entityClass != null;
	}

	public final SolrOperations getSolrOperations() {
		return solrOperations;
	}

	private Object extractIdFromBean(T entity) {

		if (entityInformation != null) {
			return entityInformation.getRequiredId(entity);
		}

		SolrInputDocument solrInputDocument = this.solrOperations.convertBeanToSolrInputDocument(entity);
		return extractIdFromSolrInputDocument(solrInputDocument);
	}

	private String extractIdFromSolrInputDocument(SolrInputDocument solrInputDocument) {
		Assert.notNull(solrInputDocument.getField(idFieldName),
				"Unable to find field '" + idFieldName + "' in SolrDocument");
		Assert.notNull(solrInputDocument.getField(idFieldName).getValue(), "ID must not be 'null'");

		return solrInputDocument.getField(idFieldName).getValue().toString();
	}

	private void registerTransactionSynchronisationIfSynchronisationActive() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			registerTransactionSynchronisationAdapter();
		}
	}

	private void registerTransactionSynchronisationAdapter() {
		TransactionSynchronizationManager.registerSynchronization(SolrTransactionSynchronizationAdapterBuilder
				.forOperations(this.solrOperations).onCollection(solrCollectionName).withDefaultBehaviour());
	}

	private void commitIfTransactionSynchronisationIsInactive() {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			this.solrOperations.commit(solrCollectionName);
		}
	}
}
