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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.solr.core.SolrExceptionTranslator;
import org.springframework.data.solr.core.schema.SchemaDefinition.FieldDefinition;
import org.springframework.data.solr.server.SolrServerFactory;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class SolrSchemaWriter {

	private static final PersistenceExceptionTranslator EXCEPTION_TRANSLATOR = new SolrExceptionTranslator();
	private SolrServerFactory factory;

	public SolrSchemaWriter(SolrServerFactory factory) {
		this.factory = factory;
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
			if (!existing.containsField(fieldDefinition.getName())) {
				fieldsToBeCreated.add(fieldDefinition);
			}
		}

		writeFieldDefinitions(fieldsToBeCreated, schemaDefinition.getCollectionName());
	}

	private void writeFieldDefinitions(Collection<FieldDefinition> definitions, String collectionName) {
		if (!CollectionUtils.isEmpty(definitions)) {
			try {
				SolrSchemaRequest.create().fields(definitions).build().process(factory.getSolrServer(collectionName));
			} catch (SolrServerException e) {
				throw EXCEPTION_TRANSLATOR.translateExceptionIfPossible(new RuntimeException(e));
			} catch (IOException e) {
				throw new InvalidDataAccessResourceUsageException("Failed to write schema field definitions.", e);
			}
		}
	}

	boolean isSchemaPresent(String collectionName) {
		return !retrieveSchemaVersion(collectionName).isNaN();
	}

	SchemaDefinition loadExistingSchema(String collectionName) {

		try {
			SolrJsonResponse response = SolrSchemaRequest.schema().process(factory.getSolrServer(collectionName));
			if (response != null && response.getNode("schema") != null) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.enable(MapperFeature.AUTO_DETECT_CREATORS);
				mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
				mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
				return mapper.readValue(response.getNode("schema").toString(), SchemaDefinition.class);
			}
			return null;
		} catch (SolrServerException e) {
			throw EXCEPTION_TRANSLATOR.translateExceptionIfPossible(new RuntimeException(e));
		} catch (IOException e) {
			throw new InvalidDataAccessResourceUsageException("Failed to load schema definition.", e);
		} catch (SolrException e) {
			throw EXCEPTION_TRANSLATOR.translateExceptionIfPossible(new RuntimeException(e));
		}
	}

	Double retrieveSchemaVersion(String collectionName) {
		try {
			SolrJsonResponse response = SolrSchemaRequest.version().process(factory.getSolrServer(collectionName));
			JsonNode node = response.getNode("version");
			return node != null ? node.asDouble() : Double.NaN;
		} catch (SolrServerException e) {
			EXCEPTION_TRANSLATOR.translateExceptionIfPossible(new RuntimeException(e));
		} catch (IOException e) {
			EXCEPTION_TRANSLATOR.translateExceptionIfPossible(new RuntimeException(e));
		} catch (SolrException e) {
			EXCEPTION_TRANSLATOR.translateExceptionIfPossible(new RuntimeException(e));
		}
		return Double.NaN;
	}

}
