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

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

import com.google.soldockr.core.SolrOperations;
import com.google.soldockr.core.query.Criteria;
import com.google.soldockr.core.query.SimpleQuery;

public class SimpleSolrRepository<T, ID extends Serializable> implements SolrRepository<T, ID> {

  private SolrOperations solrOperations;

  public SimpleSolrRepository() {}

  public SimpleSolrRepository(SolrOperations solrOperations) {
    Assert.notNull(solrOperations, "SolrOperations must not be null.");
    
    this.solrOperations = solrOperations;
  }

  @SuppressWarnings("unchecked")
  public T findOne(ID id) {
    return (T) getSolrOperations().executeObjectQuery(new SimpleQuery(new Criteria("id").is(id)), returnedClass());
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
    ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
    return (Class) parameterizedType.getActualTypeArguments()[0];
  }

  public final SolrOperations getSolrOperations() {
    return solrOperations;
  }

  public final void setSolrOperations(SolrOperations template) {
    Assert.notNull("SolrOperations must not be null.");
    
    this.solrOperations = template;
  }

}
