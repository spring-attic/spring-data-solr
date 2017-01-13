/*
 * Copyright 2014-2017 the original author or authors.
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

import static org.hamcrest.beans.HasPropertyWithValue.*;
import static org.hamcrest.core.AllOf.*;
import static org.hamcrest.core.IsEqual.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.mapping.SimpleSolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.schema.SchemaDefinition.FieldDefinition;
import org.springframework.data.solr.repository.Score;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author Christoph Strobl
 */
public class SolrSchmeaResolverUnitTests {

	private SolrSchemaResolver schemaResolver;
	private MappingContext<SimpleSolrPersistentEntity<?>, SolrPersistentProperty> context;

	@Before
	public void setUp() {
		this.schemaResolver = new SolrSchemaResolver();
		this.context = new SimpleSolrMappingContext();
	}

	@SuppressWarnings("unchecked")
	@Test // DATASOLR-72
	public void idPropertyShouldBeResolvedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("id", Foo.class));
		assertThat(
				fieldDef,
				allOf(hasProperty("name", equalTo("id")), hasProperty("multiValued", equalTo(false)),
						hasProperty("indexed", equalTo(true)), hasProperty("stored", equalTo(true)),
						hasProperty("type", equalTo("string")), hasProperty("defaultValue", equalTo(null))));
	}

	@Test // DATASOLR-72
	public void transientPropertyShouldNotBeMapped() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("transientProperty",
				Foo.class));
		assertThat(fieldDef, nullValue());
	}

	@Test // DATASOLR-72
	public void namedPropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("namedStringProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("name", equalTo("customName")));
	}

	@Test // DATASOLR-72
	public void untypedPropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("someStringProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("type", equalTo("string")));
	}

	@Test // DATASOLR-72
	public void typedPropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("solrTypedProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("type", equalTo("tdouble")));
	}

	@Test // DATASOLR-72
	public void defaultValueShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("stringWithDefaultValue",
				Foo.class));
		assertThat(fieldDef, hasProperty("defaultValue", equalTo("foo")));
	}

	@Test // DATASOLR-72
	public void readonlyPropertyShouldNotBeMapped() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("readonlyProperty",
				Foo.class));
		assertThat(fieldDef, nullValue());
	}

	@Test // DATASOLR-72
	public void requiredPropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("requiredProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("required", equalTo(true)));
	}

	@Test // DATASOLR-72
	public void searchablePropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("nonSearchableProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("indexed", equalTo(false)));
	}

	@Test // DATASOLR-72
	public void storedPropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("nonStoredProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("stored", equalTo(false)));
	}

	@Test // DATASOLR-72
	public void copyToPropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor(
				"propertyCopiedTo2Fields", Foo.class));
		assertThat(fieldDef, hasProperty("copyFields", equalTo(Arrays.asList("foo", "bar"))));
	}

	@Test // DATASOLR-72
	public void collectionPropertyShouldBeMappedAsMultivalued() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("collectionProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("multiValued", equalTo(true)));
	}

	@Test // DATASOLR-72
	public void collectionPropertyTypeShouldBeResolvedCorrectly() throws JsonProcessingException {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("collectionProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("type", equalTo("string")));
	}

	@Test // DATASOLR-210
	public void scorePropertyShouldNotBeMapped() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("scoreProperty",
				Foo.class));
		assertThat(fieldDef, nullValue());
	}

	SolrPersistentEntity<?> createEntity(Class<?> type) {
		return context.getPersistentEntity(type);
	}

	SolrPersistentProperty getPropertyFor(String property, Class<?> type) {

		SolrPersistentEntity<?> entity = createEntity(type);
		return entity.getPersistentProperty(property);
	}

	private static class Foo {

		@Id String id;
		@Transient String transientProperty;
		@Indexed(name = "customName") String namedStringProperty;
		@Indexed String someStringProperty;
		@Indexed(type = "tdouble") Object solrTypedProperty;
		@Indexed(defaultValue = "foo") String stringWithDefaultValue;
		@Indexed(readonly = true) Object readonlyProperty;
		@Indexed(required = true) String requiredProperty;
		@Indexed(searchable = false) String nonSearchableProperty;
		@Indexed(stored = false) String nonStoredProperty;
		@Indexed(copyTo = { "foo", "bar" }) String propertyCopiedTo2Fields;
		@Indexed List<String> collectionProperty;
		@Score Float scoreProperty;

	}

}
