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

import static org.hamcrest.core.IsEqual.*;
import static org.junit.Assert.*;

import java.util.Collections;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.data.solr.core.schema.SchemaDefinition.FieldDefinition;
import org.springframework.data.solr.server.SolrServerFactory;
import org.springframework.data.solr.server.support.MulticoreSolrServerFactory;
import org.springframework.data.solr.test.util.ExternalServerWithManagedSchemaRule;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 */
public class ITestSolrSchemaWriter {

	public @Rule ExternalServerWithManagedSchemaRule requiresExternalServer = ExternalServerWithManagedSchemaRule
			.onLocalhost();

	private SolrServer solrServer;
	private SolrServerFactory factory;
	private SolrSchemaWriter schemaWriter;

	@Before
	public void setUp() {
		solrServer = new HttpSolrServer("http://localhost:8983/solr");
		factory = new MulticoreSolrServerFactory(solrServer);
		schemaWriter = new SolrSchemaWriter(factory);
	}

	@Test
	public void getSchemaVersionShouldReturnVersionNumberCorrectly() {

		Double version = schemaWriter.retrieveSchemaVersion("collection1");
		assertThat(version, equalTo(1.5D));
	}

	@Test
	@Ignore("creating new schema on the fly does not work")
	public void createSchema() {
		SchemaDefinition def = new SchemaDefinition("foobar");
		schemaWriter.writeSchema(def);
	}

	@Test
	public void loadSchema() {
		SchemaDefinition def = schemaWriter.loadExistingSchema("collection1");
		Assert.notNull(def);
	}

	@Test
	public void writeSchemaDefintion() {

		SchemaDefinition def = new SchemaDefinition("collection1");
		FieldDefinition df = new FieldDefinition();
		df.setName("hululu");
		df.setType("string");
		df.setStored(true);
		df.setIndexed(false);

		def.setFields(Collections.singletonList(df));

		schemaWriter.writeSchema(def);
	}
}
