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

import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.schema.SchemaDefinition.FieldDefinition;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class SolrSchemaResolver {

	public SchemaDefinition resolveSchemaForEntity(SolrPersistentEntity<?> entity) {

		Assert.notNull(entity, "Schema cannot be resolved for 'null'.");

		final SchemaDefinition schemaDefinition = new SchemaDefinition(entity.getSolrCoreName());

		entity.doWithProperties(new PropertyHandler<SolrPersistentProperty>() {

			@Override
			public void doWithPersistentProperty(SolrPersistentProperty persistentProperty) {

				FieldDefinition fieldDefinition = createFieldDefinitionForProperty(persistentProperty);
				if (fieldDefinition != null) {
					schemaDefinition.addFieldDefinition(fieldDefinition);
				}
			}
		});

		return schemaDefinition;
	}

	protected FieldDefinition createFieldDefinitionForProperty(SolrPersistentProperty property) {

		if (property == null || property.isReadonly() || property.isTransient()) {
			return null;
		}

		FieldDefinition definition = new FieldDefinition(property.getFieldName());
		definition.setMultiValued(property.isMultiValued());
		definition.setIndexed(property.isSearchable());
		definition.setStored(property.isStored());
		definition.setType(property.getSolrTypeName());
		definition.setDefaultValue(property.getDefaultValue());
		definition.setRequired(property.isRequired());

		Collection<String> copyFields = property.getCopyFields();
		if (!CollectionUtils.isEmpty(copyFields)) {
			definition.setCopyFields(copyFields);
		}

		return definition;
	}

}
