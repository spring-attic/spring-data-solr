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
package at.pagu.soldockr.repository.support;

import java.io.Serializable;

import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

import at.pagu.soldockr.core.mapping.SolrPersistentEntity;
import at.pagu.soldockr.core.mapping.SolrPersistentProperty;
import at.pagu.soldockr.repository.query.SolrEntityInformation;

public class MappingSolrEntityInformation<T, ID extends Serializable> extends AbstractEntityInformation<T, ID> implements SolrEntityInformation<T, ID> {

  private final SolrPersistentEntity<T> entityMetadata;
  private final String solrCoreName;
  
  public MappingSolrEntityInformation(SolrPersistentEntity<T> entity) {
    this(entity, null);
  }
  
  public MappingSolrEntityInformation(SolrPersistentEntity<T> entity, String solrCoreName) {
    super(entity.getType());
    this.entityMetadata = entity;
    this.solrCoreName = solrCoreName; 
  }

  @SuppressWarnings("unchecked")
  @Override
  public ID getId(T entity) {
    SolrPersistentProperty id = entityMetadata.getIdProperty();
    try {
      return (ID) BeanWrapper.create(entity, null).getProperty(id);
    } catch (Exception e) {
      throw new IllegalStateException("ID could not be resolved", e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<ID> getIdType() {
    return (Class<ID>) String.class;
  }

  @Override
  public String getIdAttribute() {
    return entityMetadata.getIdProperty().getName();
  }
  
  public String getSolrCoreName() {
    return solrCoreName != null ? solrCoreName : entityMetadata.getSolrCoreName();
  }
  
}
