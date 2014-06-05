/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.schema;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationListener;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContextEvent;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.schema.SchemaDefinition.FieldDefinition;
import org.springframework.data.solr.server.SolrServerFactory;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class SolrPersistentEntitySchemaCreator implements
		ApplicationListener<MappingContextEvent<SolrPersistentEntity<?>, SolrPersistentProperty>> {

	public enum Feature {
		CREATE_MISSING_FIELDS;
	}

	private SolrServerFactory factory;
	private SolrSchemaWriter schemaWriter;
	private ConcurrentHashMap<Class<?>, Class<?>> processed;

	private Set<Feature> features = new HashSet<Feature>();

	public SolrPersistentEntitySchemaCreator(SolrServerFactory factory, SolrSchemaWriter schemaWriter) {
		super();
		this.factory = factory;
		this.schemaWriter = schemaWriter != null ? schemaWriter : new SolrSchemaWriter(this.factory);
		this.processed = new ConcurrentHashMap<Class<?>, Class<?>>();
	}

	public SolrPersistentEntitySchemaCreator(SolrServerFactory solrServerFactory) {
		this(solrServerFactory, null);
	}

	public SolrPersistentEntitySchemaCreator enable(Feature feature) {

		if (feature != null) {
			this.features.add(feature);
		}
		return this;
	}

	public SolrPersistentEntitySchemaCreator enable(Collection<Feature> features) {

		if (!CollectionUtils.isEmpty(features)) {
			this.features.addAll(features);
		}
		return this;
	}

	public SolrPersistentEntitySchemaCreator disable(Feature feature) {
		features.remove(feature);
		return this;
	}

	@Override
	public void onApplicationEvent(MappingContextEvent<SolrPersistentEntity<?>, SolrPersistentProperty> event) {

		if (features.contains(Feature.CREATE_MISSING_FIELDS)) {
			SolrPersistentEntity<?> entity = event.getPersistentEntity();
			if (!processed.contains(entity.getType())) {
				SchemaDefinition schema = resolveSchemaForEntity(entity);
				schemaWriter.writeSchema(schema);
				processed.put(entity.getType(), entity.getType());
			}
		}
	}

	private SchemaDefinition resolveSchemaForEntity(SolrPersistentEntity<?> entity) {

		final SchemaDefinition schema = new SchemaDefinition(entity.getSolrCoreName());
		entity.doWithProperties(new PropertyHandler<SolrPersistentProperty>() {

			@Override
			public void doWithPersistentProperty(SolrPersistentProperty persistentProperty) {

				FieldDefinition fieldDef = new FieldDefinition(persistentProperty.getFieldName());
				fieldDef.setMultiValued(persistentProperty.isMultiValued());
				fieldDef.setIndexed(persistentProperty.isIndexed());
				fieldDef.setStored(persistentProperty.isStored());
				fieldDef.setType(persistentProperty.getSolrTypeName());

				schema.addFieldDefinition(fieldDef);
			}
		});

		return schema;
	}

}
