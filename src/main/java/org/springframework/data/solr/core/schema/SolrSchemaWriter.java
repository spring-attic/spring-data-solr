/*
 * Copyright 2014-2016 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.schema.SchemaDefinition.FieldDefinition;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class SolrSchemaWriter {

	private final SolrTemplate template;

	public SolrSchemaWriter(SolrClientFactory factory) {
		this.template = new SolrTemplate(factory);
	}

	public void writeSchema(SchemaDefinition schemaDefinition) {

		if (isSchemaPresent(schemaDefinition.getCollectionName())) {
			updateSchema(schemaDefinition);
			return;
		}

		createSchema(schemaDefinition);
	}

	protected void createSchema(SchemaDefinition schemaDefinition) {
		throw new UnsupportedOperationException("The solr rest API does not allow schema creation.");
	}

	protected void updateSchema(SchemaDefinition schemaDefinition) {

		SchemaDefinition existing = loadExistingSchema(schemaDefinition.getCollectionName());

		List<FieldDefinition> fieldsToBeCreated = new ArrayList<FieldDefinition>();
		for (FieldDefinition fieldDefinition : schemaDefinition.getFields()) {

			if (!existing.containsField(fieldDefinition.getName()))
				fieldsToBeCreated.add(fieldDefinition);
		}

		writeFieldDefinitions(fieldsToBeCreated, schemaDefinition.getCollectionName());
	}

	private void writeFieldDefinitions(Collection<FieldDefinition> definitions, String collectionName) {

		if (!CollectionUtils.isEmpty(definitions)) {

			SchemaOperations schemaOps = template.getSchemaOperations(collectionName);
			for (FieldDefinition fd : definitions) {
				schemaOps.addField(fd);
			}
		}
	}

	boolean isSchemaPresent(String collectionName) {
		return !retrieveSchemaVersion(collectionName).isNaN();
	}

	SchemaDefinition loadExistingSchema(String collectionName) {
		return template.getSchemaOperations(collectionName).readSchema();
	}

	Double retrieveSchemaVersion(String collectionName) {
		return template.getSchemaOperations(collectionName).getSchemaVersion();
	}

}
