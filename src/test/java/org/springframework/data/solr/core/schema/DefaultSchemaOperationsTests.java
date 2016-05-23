/*
 * Copyright 2016-2017 the original author or authors.
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

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsCollectionContaining.*;
import static org.hamcrest.core.IsEqual.*;
import static org.hamcrest.core.IsNull.*;
import static org.hamcrest.number.IsCloseTo.*;
import static org.junit.Assert.*;
import static org.springframework.data.solr.core.schema.SchemaDefinition.CopyFieldDefinition.*;
import static org.springframework.data.solr.core.schema.SchemaDefinition.FieldDefinition.*;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.schema.SchemaDefinition.CopyFieldDefinition;
import org.springframework.data.solr.core.schema.SchemaDefinition.FieldDefinition;
import org.springframework.data.solr.test.util.EmbeddedSolrServer;
import org.springframework.data.solr.test.util.EmbeddedSolrServer.ClientCache;

public class DefaultSchemaOperationsTests {

	static final String COLLECTION_NAME = "collection1";

	public @Rule EmbeddedSolrServer resource = EmbeddedSolrServer.configure(new ClassPathResource("managed-schema"),
			ClientCache.ENABLED);

	SolrClient client;
	DefaultSchemaOperations schemaOps;

	@Before
	public void setUp() {

		client = resource.getSolrClient();
		schemaOps = new DefaultSchemaOperations(COLLECTION_NAME, new SolrTemplate(resource));
	}

	@Test // DATASOLR-313
	public void readsSchemaNameCorrectly() {
		assertThat(schemaOps.getSchemaName(), is(equalTo("example-data-driven-schema")));
	}

	@Test // DATASOLR-313
	public void readsSchemaFieldsCorrectly() {

		SchemaDefinition schema = schemaOps.readSchema();

		assertThat(schema.getFieldDefinition("_text_"),
				is(equalTo(newFieldDefinition().named("_text_").typedAs("text_general").muliValued().indexed().create())));
		assertThat(schema.getFieldDefinition("id"), is(equalTo(
				FieldDefinition.newFieldDefinition().named("id").typedAs("string").stored().indexed().required().create())));
	}

	@Test // DATASOLR-313
	public void addsFieldCorrectly() {

		FieldDefinition fd = newFieldDefinition().named("singleStringValue").typedAs("string").indexed().stored()
				.defaultedTo("---default---").required().create();
		schemaOps.addField(fd);

		SchemaDefinition schema = schemaOps.readSchema();

		assertThat(schema.getFieldDefinition(fd.getName()), is(equalTo(fd)));
	}

	@Test // DATASOLR-313
	public void addsCopyFieldCorrectly() {

		CopyFieldDefinition cf = newCopyFieldDefinition().copyFrom("some_field_s").to("dest1_s").create();
		schemaOps.addField(cf);

		SchemaDefinition schema = schemaOps.readSchema();

		assertThat(schema.getCopyFields(), hasItem(cf));
	}

	@Test // DATASOLR-313
	public void addsCopyFieldAsPartOfFieldDefinitionCorrectly() {

		FieldDefinition field = newFieldDefinition().named("singleStringValue").typedAs("string").indexed().stored()
				.copyTo("dest1_s").create();

		schemaOps.addField(field);

		SchemaDefinition schema = schemaOps.readSchema();

		assertThat(schema.getCopyFields(),
				hasItem(newCopyFieldDefinition().copyFrom("singleStringValue").to("dest1_s").create()));
	}

	@Test // DATASOLR-313
	public void removesFieldCorrectly() {

		addsFieldCorrectly();

		schemaOps.removeField("singleStringValue");

		SchemaDefinition schema = schemaOps.readSchema();

		assertThat(schema.getFieldDefinition("singleStringValue"), is(nullValue()));
	}

	@Test // DATASOLR-313
	public void readsSchemaVersionCorrectly() {
		assertThat(schemaOps.getSchemaVersion(), is(closeTo(1.6D, 0.1D)));
	}

	@Test(expected = SchemaModificationException.class) // DATASOLR-313
	public void throwsExceptionOnBadSchemaModification() {
		schemaOps.removeField("xxx");
	}
}
