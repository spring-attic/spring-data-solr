/*
 * Copyright 2012-207 the original author or authors.
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

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.repository.query.SolrEntityInformation;
import org.springframework.data.solr.repository.query.SolrEntityInformationCreator;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 */
public class SolrEntityInformationCreatorImpl implements SolrEntityInformationCreator {

	private final MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext;

	public SolrEntityInformationCreatorImpl(
			MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext) {
		Assert.notNull(mappingContext, "MappingContext must not be null!");
		this.mappingContext = mappingContext;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, ID extends Serializable> SolrEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		SolrPersistentEntity<T> persistentEntity = (SolrPersistentEntity<T>) mappingContext
				.getPersistentEntity(domainClass);

		Assert.notNull(persistentEntity, "not an managed type: " + domainClass);

		return new MappingSolrEntityInformation<T, ID>(persistentEntity);
	}

}
