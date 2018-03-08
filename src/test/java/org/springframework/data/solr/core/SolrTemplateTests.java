/*
 * Copyright 2012 - 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.nullable;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.ParseException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.SchemaVersion;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.PartialUpdate;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator.Feature;
import org.springframework.data.solr.repository.Score;
import org.springframework.data.solr.server.SolrClientFactory;

/**
 * @author Christoph Strobl
 * @author Joachim Uhrlass
 * @author Francisco Spaeth
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SolrTemplateTests {

	private SolrTemplate solrTemplate;

	private static final SimpleJavaObject SIMPLE_OBJECT = new SimpleJavaObject("simple-string-id", 123l);
	private static final SolrInputDocument SIMPLE_DOCUMENT = new SolrInputDocument();
	private static final String COLLECTION_NAME = "collection-1";

	private @Mock SolrClient solrClientMock;

	@Before
	public void setUp() {
		solrTemplate = new SolrTemplate(solrClientMock);
		solrTemplate.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullServerFactory() {
		new SolrTemplate((SolrClientFactory) null);
	}

	@Test
	public void testPing() throws SolrServerException, IOException {
		when(solrClientMock.ping()).thenReturn(new SolrPingResponse());
		SolrPingResponse pingResult = solrTemplate.ping();
		assertNotNull(pingResult);
		verify(solrClientMock, times(1)).ping();
	}

	@Test(expected = DataAccessResourceFailureException.class)
	public void testPingThrowsException() throws SolrServerException, IOException {
		when(solrClientMock.ping())
				.thenThrow(new SolrServerException("error", new SolrException(ErrorCode.NOT_FOUND, "not found")));
		solrTemplate.ping();
	}

	@Test(expected = DataAccessResourceFailureException.class) // DATASOLR-414
	public void testPingThrowsExceptionCorrectlyWhenMimeTypeDoesNotMatch() throws SolrServerException, IOException {

		when(solrClientMock.ping()).thenThrow(new RemoteSolrException("localhost", 404,
				"Error from server at http://localhost:8983: Expected mime type application/octet-stream but got text/html.",
				null));
		solrTemplate.ping();
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testQueryThrowsParseException() throws SolrServerException, IOException {
		when(solrClientMock.query(any(), any(SolrParams.class), eq(SolrRequest.METHOD.GET))).thenThrow(
				new SolrServerException("error", new SolrException(ErrorCode.BAD_REQUEST, new ParseException("parse error"))));
		solrTemplate.executeSolrQuery(new SolrQuery(), SolrRequest.METHOD.GET);
	}

	@Test(expected = UncategorizedSolrException.class)
	public void testQueryThrowsUntranslateableException() throws SolrServerException, IOException {
		when(solrClientMock.query(any(), any(SolrParams.class), eq(SolrRequest.METHOD.GET)))
				.thenThrow(new SecurityException());
		solrTemplate.executeSolrQuery(new SolrQuery(), SolrRequest.METHOD.GET);
	}

	@Test
	public void testSaveBean() throws IOException, SolrServerException {
		when(solrClientMock.add(eq(COLLECTION_NAME), any(SolrInputDocument.class), eq(-1)))
				.thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.saveBean(COLLECTION_NAME, SIMPLE_OBJECT);
		assertNotNull(updateResponse);

		ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
		verify(solrClientMock, times(1)).add(eq(COLLECTION_NAME), captor.capture(), eq(-1));

		assertEquals(SIMPLE_OBJECT.getId(), captor.getValue().getFieldValue("id"));
		assertEquals(SIMPLE_OBJECT.getValue(), captor.getValue().getFieldValue("value"));
	}

	@Test
	public void testSaveBeanCommitWithin() throws IOException, SolrServerException {
		when(solrClientMock.add(eq(COLLECTION_NAME), any(SolrInputDocument.class), eq(10000)))
				.thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.saveBean(COLLECTION_NAME, SIMPLE_OBJECT, Duration.ofSeconds(10));
		assertNotNull(updateResponse);

		ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
		verify(solrClientMock, times(1)).add(eq(COLLECTION_NAME), captor.capture(), eq(10000));

		assertEquals(SIMPLE_OBJECT.getId(), captor.getValue().getFieldValue("id"));
		assertEquals(SIMPLE_OBJECT.getValue(), captor.getValue().getFieldValue("value"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPartialUpdate() throws SolrServerException, IOException {
		when(solrClientMock.add(eq(COLLECTION_NAME), any(SolrInputDocument.class), eq(-1)))
				.thenReturn(new UpdateResponse());

		PartialUpdate update = new PartialUpdate("id", "update-id");
		update.add("field_1", "update");

		solrTemplate.saveBean(COLLECTION_NAME, update);
		ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
		verify(solrClientMock, times(1)).add(eq(COLLECTION_NAME), captor.capture(), eq(-1));

		Assert.assertTrue(captor.getValue().getFieldValue("field_1") instanceof Map);
		assertEquals("update", ((Map<String, Object>) captor.getValue().getFieldValue("field_1")).get("set"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSaveBeans() throws IOException, SolrServerException {
		when(solrClientMock.add(eq(COLLECTION_NAME), anyCollection(), eq(-1))).thenReturn(new UpdateResponse());
		List<SimpleJavaObject> collection = Arrays.asList(new SimpleJavaObject("1", 1l), new SimpleJavaObject("2", 2l),
				new SimpleJavaObject("3", 3l));
		UpdateResponse updateResponse = solrTemplate.saveBeans(COLLECTION_NAME, collection);
		assertNotNull(updateResponse);

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		verify(solrClientMock, times(1)).add(eq(COLLECTION_NAME), captor.capture(), eq(-1));

		assertEquals(3, captor.getValue().size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSaveBeansCommitWithin() throws IOException, SolrServerException {
		when(solrClientMock.add(eq(COLLECTION_NAME), anyCollection(), eq(10000))).thenReturn(new UpdateResponse());
		List<SimpleJavaObject> collection = Arrays.asList(new SimpleJavaObject("1", 1l), new SimpleJavaObject("2", 2l),
				new SimpleJavaObject("3", 3l));
		UpdateResponse updateResponse = solrTemplate.saveBeans(COLLECTION_NAME, collection, Duration.ofSeconds(10));
		assertNotNull(updateResponse);

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		verify(solrClientMock, times(1)).add(eq(COLLECTION_NAME), captor.capture(), eq(10000));

		assertEquals(3, captor.getValue().size());
	}

	@Test
	public void testSaveDocument() throws IOException, SolrServerException {
		when(solrClientMock.add(eq(COLLECTION_NAME), any(SolrInputDocument.class), eq(-1)))
				.thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.saveDocument(COLLECTION_NAME, SIMPLE_DOCUMENT);
		assertNotNull(updateResponse);
		verify(solrClientMock, times(1)).add(eq(COLLECTION_NAME), eq(SIMPLE_DOCUMENT), eq(-1));
	}

	@Test
	public void testSaveDocumentCommitWithin() throws IOException, SolrServerException {
		when(solrClientMock.add(eq(COLLECTION_NAME), any(SolrInputDocument.class), eq(10000)))
				.thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.saveDocument(COLLECTION_NAME, SIMPLE_DOCUMENT, Duration.ofSeconds(10));
		assertNotNull(updateResponse);
		verify(solrClientMock, times(1)).add(eq(COLLECTION_NAME), eq(SIMPLE_DOCUMENT), eq(10000));
	}

	@Test
	public void testSaveDocuments() throws IOException, SolrServerException {
		when(solrClientMock.add(eq(COLLECTION_NAME), anyCollection(), eq(-1))).thenReturn(new UpdateResponse());
		List<SolrInputDocument> collection = Collections.singletonList(SIMPLE_DOCUMENT);
		UpdateResponse updateResponse = solrTemplate.saveDocuments(COLLECTION_NAME, collection);
		assertNotNull(updateResponse);
		verify(solrClientMock, times(1)).add(eq(COLLECTION_NAME), eq(collection), eq(-1));
	}

	@Test
	public void testSaveDocumentsCommitWithin() throws IOException, SolrServerException {
		when(solrClientMock.add(eq(COLLECTION_NAME), anyCollection(), eq(10000))).thenReturn(new UpdateResponse());
		List<SolrInputDocument> collection = Collections.singletonList(SIMPLE_DOCUMENT);
		UpdateResponse updateResponse = solrTemplate.saveDocuments(COLLECTION_NAME, collection, Duration.ofSeconds(10));
		assertNotNull(updateResponse);
		verify(solrClientMock, times(1)).add(eq(COLLECTION_NAME), eq(collection), eq(10000));
	}

	@Test
	public void testDeleteById() throws IOException, SolrServerException {
		when(solrClientMock.deleteById(eq(COLLECTION_NAME), Mockito.anyString())).thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.deleteByIds(COLLECTION_NAME, "1");
		assertNotNull(updateResponse);
		verify(solrClientMock, times(1)).deleteById(eq(COLLECTION_NAME), eq("1"));
	}

	@Test
	public void testDeleteByIdWithCollection() throws IOException, SolrServerException {
		when(solrClientMock.deleteById(eq(COLLECTION_NAME), anyList())).thenReturn(new UpdateResponse());
		List<String> idsToDelete = Arrays.asList("1", "2");
		UpdateResponse updateResponse = solrTemplate.deleteByIds(COLLECTION_NAME, idsToDelete);
		assertNotNull(updateResponse);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<String>> captor = (ArgumentCaptor<List<String>>) (Object) ArgumentCaptor.forClass(List.class);

		verify(solrClientMock, times(1)).deleteById(eq(COLLECTION_NAME), captor.capture());

		assertEquals(idsToDelete.size(), captor.getValue().size());
		for (String s : idsToDelete) {
			Assert.assertTrue(captor.getValue().contains(s));
		}
	}

	@Test
	public void testCount() throws SolrServerException, IOException {
		ArgumentCaptor<SolrQuery> captor = ArgumentCaptor.forClass(SolrQuery.class);
		QueryResponse responseMock = Mockito.mock(QueryResponse.class);
		SolrDocumentList resultList = new SolrDocumentList();
		resultList.setNumFound(10);
		when(responseMock.getResults()).thenReturn(resultList);
		when(solrClientMock.query(eq(COLLECTION_NAME), any(SolrQuery.class), eq(SolrRequest.METHOD.GET)))
				.thenReturn(responseMock);

		long result = solrTemplate.count(COLLECTION_NAME, new SimpleQuery(new Criteria("field_1").is("value1")));
		assertEquals(resultList.getNumFound(), result);

		verify(solrClientMock, times(1)).query(eq(COLLECTION_NAME), captor.capture(), eq(SolrRequest.METHOD.GET));

		assertEquals(Integer.valueOf(0), captor.getValue().getStart());
		assertEquals(Integer.valueOf(0), captor.getValue().getRows());
	}

	@Test
	public void testCountWhenPagingSet() throws SolrServerException, IOException {
		ArgumentCaptor<SolrQuery> captor = ArgumentCaptor.forClass(SolrQuery.class);
		QueryResponse responseMock = Mockito.mock(QueryResponse.class);
		SolrDocumentList resultList = new SolrDocumentList();
		resultList.setNumFound(10);
		when(responseMock.getResults()).thenReturn(resultList);
		when(solrClientMock.query(eq(COLLECTION_NAME), any(SolrQuery.class), eq(SolrRequest.METHOD.GET)))
				.thenReturn(responseMock);

		Query query = new SimpleQuery(new Criteria("field_1").is("value1"));
		query.setPageRequest(PageRequest.of(0, 5));
		long result = solrTemplate.count(COLLECTION_NAME, query);
		assertEquals(resultList.getNumFound(), result);

		verify(solrClientMock, times(1)).query(eq(COLLECTION_NAME), captor.capture(), eq(SolrRequest.METHOD.GET));

		assertEquals(Integer.valueOf(0), captor.getValue().getStart());
		assertEquals(Integer.valueOf(0), captor.getValue().getRows());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCountNullQuery() {
		solrTemplate.count(COLLECTION_NAME, null);
	}

	@Test
	public void testCommit() throws SolrServerException, IOException {
		solrTemplate.commit(COLLECTION_NAME);
		verify(solrClientMock, times(1)).commit(eq(COLLECTION_NAME));
	}

	@Test
	public void testSoftCommit() throws SolrServerException, IOException {
		solrTemplate.softCommit(COLLECTION_NAME);
		verify(solrClientMock, times(1)).commit(eq(COLLECTION_NAME), eq(true), eq(true), eq(true));
	}

	@Test // DATASOLR-405, DATASOLR-438
	public void testRollback() throws SolrServerException, IOException {

		solrTemplate.rollback(COLLECTION_NAME);
		verify(solrClientMock, times(1)).rollback(COLLECTION_NAME);
	}

	@Test
	public void testDifferentQueryParser() throws SolrServerException, IOException {
		QueryParser parser = new QueryParser() {

			@Override
			public void registerConverter(Converter<?, ?> converter) {}

			@Override
			public String getQueryString(SolrDataQuery query) {
				return "*:*";
			}

			@Override
			public SolrQuery constructSolrQuery(SolrDataQuery query) {
				return new SolrQuery(getQueryString(query));
			}

		};

		solrTemplate.registerQueryParser(SimpleQuery.class, parser);
		solrTemplate.querySolr("collection-1", new SimpleQuery(new SimpleStringCriteria("my:criteria")), null, null);

		ArgumentCaptor<SolrParams> captor = ArgumentCaptor.forClass(SolrParams.class);

		verify(solrClientMock, times(1)).query(nullable(String.class), captor.capture(), eq(SolrRequest.METHOD.GET));
		assertEquals("*:*", captor.getValue().getParams(CommonParams.Q)[0]);
	}

	@Test // DATASOLR-72, DATASOLR-313, DATASOLR-309
	public void schemaShouldBeUpdatedPriorToSavingEntity() throws SolrServerException, IOException {

		NamedList<Object> nl = new NamedList<>();
		Map<String, Object> schema = new LinkedHashMap<>();
		nl.add("version", 1.5F);
		nl.add("schema", schema);
		schema.put("version", 1.5F);
		schema.put("name", "mock");
		schema.put("fields", Collections.<NamedList<Object>> emptyList());
		schema.put("dynamicFields", Collections.<NamedList<Object>> emptyList());
		schema.put("fieldTypes", Collections.<NamedList<Object>> emptyList());
		schema.put("copyFields", Collections.<NamedList<Object>> emptyList());

		// schema.add(name, val);

		when(solrClientMock.request((SchemaVersion) any(), anyString())).thenReturn(nl);
		when(solrClientMock.request((SchemaRequest) any(), anyString())).thenReturn(nl);

		solrTemplate = new SolrTemplate(solrClientMock);
		solrTemplate.setSchemaCreationFeatures(Collections.singletonList(Feature.CREATE_MISSING_FIELDS));
		solrTemplate.afterPropertiesSet();
		solrTemplate.saveBean(COLLECTION_NAME, new DocumentWithIndexAnnotations());

		ArgumentCaptor<SolrRequest> requestCaptor = ArgumentCaptor.forClass(SolrRequest.class);
		verify(solrClientMock, times(4)).request(requestCaptor.capture(), Mockito.anyString());

		SolrRequest capturedRequest = requestCaptor.getValue();

		assertThat(capturedRequest.getMethod(), IsEqual.equalTo(SolrRequest.METHOD.POST));
		assertThat(capturedRequest.getPath(), IsEqual.equalTo("/schema"));
		assertThat(capturedRequest.getContentStreams(), IsNull.notNullValue());
	}

	@Test // DATASOLR-83
	public void testGetById() throws SolrServerException, IOException {

		when(solrClientMock.getById(eq(COLLECTION_NAME), anyCollection())).thenReturn(new SolrDocumentList());

		solrTemplate.getById(COLLECTION_NAME, "myId", DocumentWithIndexAnnotations.class);

		verify(solrClientMock, times(1)).getById(eq(COLLECTION_NAME), eq(Collections.singletonList("myId")));
	}

	@Test // DATASOLR-83
	public void testGetByIds() throws SolrServerException, IOException {

		when(solrClientMock.getById(eq(COLLECTION_NAME), anyCollection())).thenReturn(new SolrDocumentList());

		List<String> ids = Arrays.asList("myId1", "myId2");
		solrTemplate.getByIds(COLLECTION_NAME, ids, DocumentWithIndexAnnotations.class);

		verify(solrClientMock, times(1)).getById(eq(COLLECTION_NAME), eq(ids));
	}

	@Test // DATASOLR-160
	public void testSaveShouldNotSaveScoreField()
			throws IOException, SolrServerException, SecurityException, NoSuchFieldException {

		solrTemplate.saveBean(COLLECTION_NAME, new DocumentWithScoreAnnotation());

		ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
		verify(solrClientMock, times(1)).add(eq(COLLECTION_NAME), captor.capture(), eq(-1));

		assertNull(captor.getValue().getFieldValue("score"));
	}

	@Test // DATASOLR-215
	public void usesTemplateDefaultRequestMethodForQuery() throws SolrServerException, IOException {

		solrTemplate = new SolrTemplate(solrClientMock, RequestMethod.POST);
		solrTemplate.afterPropertiesSet();

		when(solrClientMock.query(any(), any(SolrParams.class), eq(SolrRequest.METHOD.POST)))
				.thenReturn(new QueryResponse());
		solrTemplate.querySolr(COLLECTION_NAME, new SimpleQuery("*:*"), DocumentWithIndexAnnotations.class, null);

		verify(solrClientMock, times(1)).query(any(), any(SolrParams.class), eq(SolrRequest.METHOD.POST));
	}

	@Test // DATASOLR-215
	public void usesTemplateMethodRequetsParameterForQuery() throws SolrServerException, IOException {

		solrTemplate = new SolrTemplate(solrClientMock, RequestMethod.POST);
		solrTemplate.afterPropertiesSet();

		when(solrClientMock.query(any(), any(SolrParams.class), eq(SolrRequest.METHOD.PUT)))
				.thenReturn(new QueryResponse());
		solrTemplate.querySolr(COLLECTION_NAME, new SimpleQuery("*:*"), DocumentWithIndexAnnotations.class,
				RequestMethod.PUT);

		verify(solrClientMock, times(1)).query(any(), any(SolrParams.class), eq(SolrRequest.METHOD.PUT));
	}

	static class DocumentWithIndexAnnotations {

		@Id String id;
		@Indexed(name = "namedProperty") String renamedProperty;
	}

	static class DocumentWithScoreAnnotation {

		@Id String id;
		@Score Float scoreProperty;
	}
}
