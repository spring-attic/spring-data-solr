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

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.solr.core.schema.SchemaDefinition.FieldDefinition;
import org.springframework.data.solr.server.SolrServerFactory;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrSchemaWriterTests {

	private static final String JSON_RESPONSE_EMPTY_SCHEMA = "{ \"schema\" : {} }";
	private static final String INVALID_RESPONSE = "{ \"invalid\" : \"result\" }";
	private static final String JSON_RESPONSE_SCHEMA_VERSION = "{ \"version\" : 1.5 }";
	private static final String JSON_RESPONSE_DEFAULT_SCHEMA = loadSchemaFile("default_schema.json");

	private static String loadSchemaFile(String file) {

		try {
			return FileUtils.readFileToString(new ClassPathResource("org/springframework/data/solr/core/schema/" + file)
					.getFile());
		} catch (IOException e) {
			throw new RuntimeException("cannot laod file " + file, e);
		}
	}

	private SolrSchemaWriter writer;
	private @Mock SolrServerFactory factoryMock;
	private @Mock SolrServer solrServerMock;

	@Before
	public void setUp() {

		Mockito.when(factoryMock.getSolrServer(Mockito.anyString())).thenReturn(solrServerMock);
		writer = new SolrSchemaWriter(factoryMock);
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void schemaCreationIsCurrentlyNotPossible() {
		writer.createSchema(new SchemaDefinition());
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void schemaPresentCheckSouldReturnTrueIfVersionCorrectlyReturnedFromServer() {

		setUpJsonResponse(JSON_RESPONSE_SCHEMA_VERSION);
		Assert.assertTrue(writer.isSchemaPresent("collection1"));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void schemaPresentCheckSouldIndicateFalseWhenServerReturnsNotFound() throws SolrServerException, IOException {

		Mockito.when(solrServerMock.request(Mockito.any(SolrRequest.class))).thenThrow(
				new SolrServerException(new SolrException(org.apache.solr.common.SolrException.ErrorCode.NOT_FOUND, "boom")));
		Assert.assertFalse(writer.isSchemaPresent("collection1"));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void loadExistingSchemaShouldReturnNullWhenNoInformationReturnedFromServer() {

		setUpJsonResponse(INVALID_RESPONSE);
		Assert.assertNull(writer.loadExistingSchema("collection1"));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void loadExistingSchemaShouldMapEmptySchemaCorrectly() {

		setUpJsonResponse(JSON_RESPONSE_EMPTY_SCHEMA);
		Assert.assertNotNull(writer.loadExistingSchema("collection1"));
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void loadExistingSchemaShouldMapSchemaInformationCorrectly() {

		setUpJsonResponse(JSON_RESPONSE_DEFAULT_SCHEMA);
		SchemaDefinition schemaDef = writer.loadExistingSchema("collection1");

		Assert.assertEquals("example", schemaDef.getName());
		Assert.assertEquals("id", schemaDef.getUniqueKey());
	}

	/**
	 * @see DATASOLR-72
	 */
	@Test
	public void loadExistingSchemaShouldMapFieldInformationCorrectly() {

		setUpJsonResponse(JSON_RESPONSE_DEFAULT_SCHEMA);
		FieldDefinition fieldDef = writer.loadExistingSchema("collection1").getFieldDefinition("id");

		Assert.assertEquals("id", fieldDef.getName());
		Assert.assertEquals("string", fieldDef.getType());
		Assert.assertEquals(false, fieldDef.isMultiValued());
		Assert.assertEquals(true, fieldDef.isIndexed());
		Assert.assertEquals(true, fieldDef.isRequired());
		Assert.assertEquals(true, fieldDef.isStored());
	}

	private void setUpJsonResponse(String json) {

		NamedList<Object> namedList = new NamedList<Object>();
		namedList.add("json", json);

		try {
			Mockito.when(solrServerMock.request(Mockito.any(SolrRequest.class))).thenReturn(namedList);
		} catch (Exception e) {}
	}
}
