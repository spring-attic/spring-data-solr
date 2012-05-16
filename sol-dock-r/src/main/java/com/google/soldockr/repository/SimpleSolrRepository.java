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
package com.google.soldockr.repository;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.apache.commons.lang.NotImplementedException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import com.google.soldockr.ApiUsageException;
import com.google.soldockr.core.SolrOperations;
import com.google.soldockr.core.query.Criteria;
import com.google.soldockr.core.query.SimpleQuery;

public class SimpleSolrRepository<T> implements SolrCrudRepository<T> {

  private static final String DEFAULT_ID_FIELD = "id";
  
  private SolrOperations solrOperations;
  private String idFieldName = DEFAULT_ID_FIELD;

  public SimpleSolrRepository() {}

  public SimpleSolrRepository(SolrOperations solrOperations) {
    Assert.notNull(solrOperations, "SolrOperations must not be null.");
    
    this.solrOperations = solrOperations;
  }

  @SuppressWarnings("unchecked")
  public T findOne(String id) {
    return (T) getSolrOperations().executeObjectQuery(new SimpleQuery(new Criteria(this.idFieldName).is(id)), returnedClass());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Page<T> findAll(Pageable pageable) {
    return getSolrOperations().executeListQuery(
        new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)).setPageRequest(pageable), returnedClass());
  }

  @Override
  public long count() {
    QueryResponse response = getSolrOperations().executeQuery(
        new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)).setPageRequest(new PageRequest(0, 1)));
    return response.getResults().getNumFound();
  }

  @SuppressWarnings("rawtypes")
  private Class returnedClass() {
    Class<? extends Type> interfaces = getClass().getGenericInterfaces()[0].getClass();
    ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericInterfaces()[0].getClass().getGenericSuperclass();
    return (Class) parameterizedType.getActualTypeArguments()[0];
  }

  public final SolrOperations getSolrOperations() {
    return solrOperations;
  }

  @Field
  public final void setSolrOperations(SolrOperations template) {
    Assert.notNull("SolrOperations must not be null.");
    
    this.solrOperations = template;
  }

  @Override
  public T save(T entity) {
    this.solrOperations.executeAddBean(entity);
    this.solrOperations.executeCommit();
    return entity;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterable<T> save(Iterable<? extends T> entities) {
    Assert.notNull(entities, "Cannot insert null as a List");
    if(!(entities instanceof Collection<?>)) {
      throw new ApiUsageException("Entities have to be inside a collection");
    }
    
    this.solrOperations.executeAddBeans((Collection<? extends T>)entities);
    this.solrOperations.executeCommit();
    return (Iterable<T>) entities;
  }

  @Override
  public boolean exists(String id) {
    return findOne(id)!=null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterable<T> findAll() {     
    return this.solrOperations.executeListQuery(new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)), returnedClass());
  }

  @Override
  public void delete(String id) {
    this.solrOperations.executeDeleteById(id);
    this.solrOperations.executeCommit();
  }

  @Override
  public void delete(T entity) {
    SolrInputDocument solrInputDocument = this.solrOperations.getSolrServer().getBinder().toSolrInputDocument(entity);
    Assert.notNull(solrInputDocument.getField(idFieldName), "Unable to find field id");
    Assert.notNull(solrInputDocument.getField(idFieldName).getValue(), "ID must not be null");
    
    this.solrOperations.executeDeleteById(solrInputDocument.getField(idFieldName).getValue().toString());
    this.solrOperations.executeCommit();
  }

  @Override
  public void delete(Iterable<? extends T> entities) {
        // TODO Auto-generated method stub
    throw new NotImplementedException();
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

}
