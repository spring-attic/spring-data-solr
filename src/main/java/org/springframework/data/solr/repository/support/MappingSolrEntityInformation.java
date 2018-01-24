/*
 * Copyright 2012-2015 the original author or authors.
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

import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.data.repository.core.support.PersistentEntityInformation;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.repository.query.SolrEntityInformation;

/**
 * Solr specific implementation of {@link AbstractEntityInformation}
 * 
 * @param <T>
 * @param <ID>
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
public class MappingSolrEntityInformation<T, ID extends Serializable> extends PersistentEntityInformation<T, ID>
		implements SolrEntityInformation<T, ID> {

	private final SolrPersistentEntity<T> entityMetadata;
	private final String solrCoreName;

	public MappingSolrEntityInformation(SolrPersistentEntity<T> entity) {
		this(entity, null);
	}

	public MappingSolrEntityInformation(SolrPersistentEntity<T> entity, String solrCoreName) {
		super(entity);
		this.entityMetadata = entity;
		this.solrCoreName = solrCoreName;
	}

	@Override
	public String getIdAttribute() {
		return entityMetadata.getIdProperty().getFieldName();
	}

	public String getSolrCoreName() {
		return solrCoreName != null ? solrCoreName : entityMetadata.getSolrCoreName();
	}
}
