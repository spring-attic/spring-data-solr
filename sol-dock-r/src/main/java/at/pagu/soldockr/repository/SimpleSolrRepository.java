/*
 * Copyright (C) 2012 sol-dock-r authors.
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
package at.pagu.soldockr.repository;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import at.pagu.soldockr.ApiUsageException;
import at.pagu.soldockr.core.SolrOperations;
import at.pagu.soldockr.core.query.Criteria;
import at.pagu.soldockr.core.query.SimpleQuery;

public class SimpleSolrRepository<T> implements SolrCrudRepository<T> {

  private static final String DEFAULT_ID_FIELD = "id";

  private SolrOperations solrOperations;
  private String idFieldName = DEFAULT_ID_FIELD;
  private Class<T> entityClass;

  public SimpleSolrRepository() {}

  public SimpleSolrRepository(SolrOperations solrOperations) {
    this.setSolrOperations(solrOperations);
  }

  public SimpleSolrRepository(SolrOperations solrOperations, Class<T> entityClass) {
    this(solrOperations);

    this.setEntityClass(entityClass);
  }

  @Override
  public T findOne(String id) {
    return (T) getSolrOperations().executeObjectQuery(new SimpleQuery(new Criteria(this.idFieldName).is(id)), getEntityClass());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public Iterable<T> findAll() {
    int itemCount = (int) this.count();
    if (itemCount == 0) {
      return (Page<T>) new PageImpl(Collections.EMPTY_LIST);
    }
    return this.findAll(new PageRequest(0, (int) this.count()));
  }

  @Override
  public Page<T> findAll(Pageable pageable) {
    return getSolrOperations().executeListQuery(
        new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)).setPageRequest(pageable), getEntityClass());
  }

  @Override
  public long count() {
    QueryResponse response = getSolrOperations().executeQuery(
        new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)).setPageRequest(new PageRequest(0, 1)));
    return response.getResults().getNumFound();
  }

  @Override
  public T save(T entity) {
    Assert.notNull(entity, "Cannot save 'null' entity.");

    this.solrOperations.executeAddBean(entity);
    this.solrOperations.executeCommit();
    return entity;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterable<T> save(Iterable<? extends T> entities) {
    Assert.notNull(entities, "Cannot insert 'null' as a List.");

    if (!(entities instanceof Collection<?>)) {
      throw new ApiUsageException("Entities have to be inside a collection");
    }

    this.solrOperations.executeAddBeans((Collection<? extends T>) entities);
    this.solrOperations.executeCommit();
    return (Iterable<T>) entities;
  }

  @Override
  public boolean exists(String id) {
    return findOne(id) != null;
  }

  @Override
  public void delete(String id) {
    Assert.notNull(id, "Cannot delete entity with id 'null'.");

    this.solrOperations.executeDeleteById(id);
    this.solrOperations.executeCommit();
  }

  @SuppressWarnings("unchecked")
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
      idsToDelete.add(extractIdFromBean(entity));
    }
    this.solrOperations.executeDeleteById(idsToDelete);
    this.solrOperations.executeCommit();
  }

  @Override
  public void deleteAll() {
    this.solrOperations.executeDelete(new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)));
    this.solrOperations.executeCommit();
  }

  public final String getIdFieldName() {
    return idFieldName;
  }

  public final void setIdFieldName(String idFieldName) {
    Assert.notNull(idFieldName, "ID Field cannot be null.");

    this.idFieldName = idFieldName;
  }

  @SuppressWarnings({"unchecked"})
  private Class<T> resolveReturnedClassFromGernericType() {
    ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
    return (Class<T>) parameterizedType.getActualTypeArguments()[0];
  }

  public Class<T> getEntityClass() {
    if (!isEntityClassSet()) {
      try {
        this.entityClass = resolveReturnedClassFromGernericType();
      } catch (Exception e) {
        new ApiUsageException("Unable to resolve EntityClass. Please use according setter!", e);
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

  private String extractIdFromBean(T entity) {
    SolrInputDocument solrInputDocument = this.solrOperations.convertBeanToSolrInputDocument(entity);
    return extractIdFromSolrInputDocument(solrInputDocument);
  }

  private String extractIdFromSolrInputDocument(SolrInputDocument solrInputDocument) {
    Assert.notNull(solrInputDocument.getField(idFieldName), "Unable to find field '" + idFieldName + "' in SolrDocument.");
    Assert.notNull(solrInputDocument.getField(idFieldName).getValue(), "ID must not be 'null'.");

    return solrInputDocument.getField(idFieldName).getValue().toString();
  }

}
