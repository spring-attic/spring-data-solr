/*
 * Copyright 2014-2015 the original author or authors.
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
import org.springframework.data.mapping.context.MappingContextEvent;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class SolrPersistentEntitySchemaCreator implements ApplicationListener<MappingContextEvent<?, ?>> {

	public enum Feature {
		CREATE_MISSING_FIELDS;
	}

	private SolrClientFactory factory;
	private SolrSchemaWriter schemaWriter;
	private SolrSchemaResolver schemaResolver;
	private ConcurrentHashMap<Class<?>, Class<?>> processed;

	private Set<Feature> features = new HashSet<Feature>();

	public SolrPersistentEntitySchemaCreator(SolrClientFactory solrClientFactory) {
		this(solrClientFactory, null);
	}

	public SolrPersistentEntitySchemaCreator(SolrClientFactory factory, SolrSchemaWriter schemaWriter) {
		super();
		this.factory = factory;
		this.schemaWriter = schemaWriter != null ? schemaWriter : new SolrSchemaWriter(this.factory);
		this.schemaResolver = new SolrSchemaResolver();
		this.processed = new ConcurrentHashMap<Class<?>, Class<?>>();
	}

	private void process(SolrPersistentEntity<?> entity) {

		SchemaDefinition schema = schemaResolver.resolveSchemaForEntity(entity);

		beforeSchemaWrite(entity, schema);
		schemaWriter.writeSchema(schema);
		afterSchemaWrite(entity, schema);
	}

	protected void beforeSchemaWrite(SolrPersistentEntity<?> entity, SchemaDefinition schema) {
		// before hook
	}

	protected void afterSchemaWrite(SolrPersistentEntity<?> entity, SchemaDefinition schema) {
		processed.put(entity.getType(), entity.getType());
	}

	@Override
	public void onApplicationEvent(MappingContextEvent<?, ?> event) {

		if (features.contains(Feature.CREATE_MISSING_FIELDS)) {

			if (event.getPersistentEntity() instanceof SolrPersistentEntity) {
				SolrPersistentEntity<?> entity = (SolrPersistentEntity<?>) event.getPersistentEntity();
				if (!processed.contains(entity.getType())) {
					process(entity);
				}
			}
		}
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
}
