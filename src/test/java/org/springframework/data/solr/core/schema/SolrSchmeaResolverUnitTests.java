/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.schema;

import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

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
import com.fasterxml.jackson.databind.ObjectMapper;

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

	/**
	 * @see DATASOLR-72
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void idPropertyShouldBeResolvedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("id", Foo.class));
		assertThat(
				fieldDef,
				allOf(hasProperty("name", equalTo("id")), hasProperty("multiValued", equalTo(false)),
						hasProperty("indexed", equalTo(true)), hasProperty("stored", equalTo(true)),
						hasProperty("type", equalTo("string")), hasProperty("defaultValue", equalTo(null))));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void transientPropertyShouldNotBeMapped() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("transientProperty",
				Foo.class));
		assertThat(fieldDef, nullValue());
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void namedPropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("namedStringProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("name", equalTo("customName")));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void untypedPropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("someStringProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("type", equalTo("string")));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void typedPropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("solrTypedProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("type", equalTo("tdouble")));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void defaultValueShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("stringWithDefaultValue",
				Foo.class));
		assertThat(fieldDef, hasProperty("defaultValue", equalTo("foo")));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void readonlyPropertyShouldNotBeMapped() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("readonlyProperty",
				Foo.class));
		assertThat(fieldDef, nullValue());
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void requiredPropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("requiredProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("required", equalTo(true)));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void searchablePropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("nonSearchableProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("indexed", equalTo(false)));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void storedPropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("nonStoredProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("stored", equalTo(false)));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void copyToPropertyShouldBeMappedCorrectly() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor(
				"propertyCopiedTo2Fields", Foo.class));
		assertThat(fieldDef, hasProperty("copyFields", equalTo(Arrays.asList("foo", "bar"))));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void collectionPropertyShouldBeMappedAsMultivalued() {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("collectionProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("multiValued", equalTo(true)));
	}

	/**
	 * @throws JsonProcessingException
	 * @see DATASOLR-72
	 */
	@Test
	public void collectionPropertyTypeShouldBeResolvedCorrectly() throws JsonProcessingException {

		FieldDefinition fieldDef = schemaResolver.createFieldDefinitionForProperty(getPropertyFor("collectionProperty",
				Foo.class));
		assertThat(fieldDef, hasProperty("type", equalTo("string")));

		ObjectMapper mapper = new ObjectMapper();
		System.out.println(mapper.writeValueAsString(fieldDef));
	}
	
	/**
	 * @see DATASOLR-210
	 */
	@Test
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
