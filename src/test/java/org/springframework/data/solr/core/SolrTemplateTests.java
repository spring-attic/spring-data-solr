/*
 * Copyright 2012 - 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.ParseException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
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
@RunWith(MockitoJUnitRunner.class)
public class SolrTemplateTests {

	private SolrTemplate solrTemplate;

	private static final SimpleJavaObject SIMPLE_OBJECT = new SimpleJavaObject("simple-string-id", 123l);
	private static final SimpleBoostedJavaObject SIMPLE_BOOSTED_OBJECT = new SimpleBoostedJavaObject("simple-string-id",
			123l, "simple-string-boost");
	private static final SolrInputDocument SIMPLE_DOCUMENT = new SolrInputDocument();

	private @Mock SolrClient solrClientMock;

	@Before
	public void setUp() {
		solrTemplate = new SolrTemplate(solrClientMock, "core1");
		solrTemplate.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullServerFactory() {
		new SolrTemplate((SolrClientFactory) null);
	}

	@Test
	public void testPing() throws SolrServerException, IOException {
		Mockito.when(solrClientMock.ping()).thenReturn(new SolrPingResponse());
		SolrPingResponse pingResult = solrTemplate.ping();
		Assert.assertNotNull(pingResult);
		Mockito.verify(solrClientMock, Mockito.times(1)).ping();
	}

	@Test(expected = DataAccessException.class)
	public void testPingThrowsException() throws SolrServerException, IOException {
		Mockito.when(solrClientMock.ping())
				.thenThrow(new SolrServerException("error", new SolrException(ErrorCode.NOT_FOUND, "not found")));
		solrTemplate.ping();
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testQueryThrowsParseException() throws SolrServerException, IOException {
		Mockito.when(solrClientMock.query(Matchers.any(SolrParams.class), Mockito.eq(SolrRequest.METHOD.GET))).thenThrow(
				new SolrServerException("error", new SolrException(ErrorCode.BAD_REQUEST, new ParseException("parse error"))));
		solrTemplate.executeSolrQuery(new SolrQuery(), SolrRequest.METHOD.GET);
	}

	@Test(expected = UncategorizedSolrException.class)
	public void testQueryThrowsUntranslateableException() throws SolrServerException, IOException {
		Mockito.when(solrClientMock.query(Matchers.any(SolrParams.class), Mockito.eq(SolrRequest.METHOD.GET)))
				.thenThrow(new SecurityException());
		solrTemplate.executeSolrQuery(new SolrQuery(), SolrRequest.METHOD.GET);
	}

	@Test
	public void testSaveBean() throws IOException, SolrServerException {
		Mockito.when(solrClientMock.add(Mockito.any(SolrInputDocument.class), Mockito.eq(-1)))
				.thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.saveBean(SIMPLE_OBJECT);
		Assert.assertNotNull(updateResponse);

		ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
		Mockito.verify(solrClientMock, Mockito.times(1)).add(captor.capture(), Mockito.eq(-1));

		Assert.assertEquals(SIMPLE_OBJECT.getId(), captor.getValue().getFieldValue("id"));
		Assert.assertEquals(SIMPLE_OBJECT.getValue(), captor.getValue().getFieldValue("value"));
	}

	@Test
	public void testSaveBeanCommitWithin() throws IOException, SolrServerException {
		Mockito.when(solrClientMock.add(Mockito.any(SolrInputDocument.class), Mockito.eq(10000)))
				.thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.saveBean(SIMPLE_OBJECT, 10000);
		Assert.assertNotNull(updateResponse);

		ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
		Mockito.verify(solrClientMock, Mockito.times(1)).add(captor.capture(), Mockito.eq(10000));

		Assert.assertEquals(SIMPLE_OBJECT.getId(), captor.getValue().getFieldValue("id"));
		Assert.assertEquals(SIMPLE_OBJECT.getValue(), captor.getValue().getFieldValue("value"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPartialUpdate() throws SolrServerException, IOException {
		Mockito.when(solrClientMock.add(Mockito.any(SolrInputDocument.class), Mockito.eq(-1)))
				.thenReturn(new UpdateResponse());

		PartialUpdate update = new PartialUpdate("id", "update-id");
		update.add("field_1", "update");

		solrTemplate.saveBean(update);
		ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
		Mockito.verify(solrClientMock, Mockito.times(1)).add(captor.capture(), Mockito.eq(-1));

		Assert.assertTrue(captor.getValue().getFieldValue("field_1") instanceof Map);
		Assert.assertEquals("update", ((Map<String, Object>) captor.getValue().getFieldValue("field_1")).get("set"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSaveBeans() throws IOException, SolrServerException {
		Mockito.when(solrClientMock.add(Mockito.anyCollectionOf(SolrInputDocument.class), Mockito.eq(-1)))
				.thenReturn(new UpdateResponse());
		List<SimpleJavaObject> collection = Arrays.asList(new SimpleJavaObject("1", 1l), new SimpleJavaObject("2", 2l),
				new SimpleJavaObject("3", 3l));
		UpdateResponse updateResponse = solrTemplate.saveBeans(collection);
		Assert.assertNotNull(updateResponse);

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		Mockito.verify(solrClientMock, Mockito.times(1)).add(captor.capture(), Mockito.eq(-1));

		Assert.assertEquals(3, captor.getValue().size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSaveBeansCommitWithin() throws IOException, SolrServerException {
		Mockito.when(solrClientMock.add(Mockito.anyCollectionOf(SolrInputDocument.class), Mockito.eq(10000)))
				.thenReturn(new UpdateResponse());
		List<SimpleJavaObject> collection = Arrays.asList(new SimpleJavaObject("1", 1l), new SimpleJavaObject("2", 2l),
				new SimpleJavaObject("3", 3l));
		UpdateResponse updateResponse = solrTemplate.saveBeans(collection, 10000);
		Assert.assertNotNull(updateResponse);

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		Mockito.verify(solrClientMock, Mockito.times(1)).add(captor.capture(), Mockito.eq(10000));

		Assert.assertEquals(3, captor.getValue().size());
	}

	@Test
	public void testSaveDocument() throws IOException, SolrServerException {
		Mockito.when(solrClientMock.add(Mockito.any(SolrInputDocument.class), Mockito.eq(-1)))
				.thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.saveDocument(SIMPLE_DOCUMENT);
		Assert.assertNotNull(updateResponse);
		Mockito.verify(solrClientMock, Mockito.times(1)).add(Mockito.eq(SIMPLE_DOCUMENT), Mockito.eq(-1));
	}

	@Test
	public void testSaveDocumentCommitWithin() throws IOException, SolrServerException {
		Mockito.when(solrClientMock.add(Mockito.any(SolrInputDocument.class), Mockito.eq(10000)))
				.thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.saveDocument(SIMPLE_DOCUMENT, 10000);
		Assert.assertNotNull(updateResponse);
		Mockito.verify(solrClientMock, Mockito.times(1)).add(Mockito.eq(SIMPLE_DOCUMENT), Mockito.eq(10000));
	}

	@Test
	public void testSaveDocuments() throws IOException, SolrServerException {
		Mockito.when(solrClientMock.add(Mockito.anyCollectionOf(SolrInputDocument.class), Mockito.eq(-1)))
				.thenReturn(new UpdateResponse());
		List<SolrInputDocument> collection = Arrays.asList(SIMPLE_DOCUMENT);
		UpdateResponse updateResponse = solrTemplate.saveDocuments(collection);
		Assert.assertNotNull(updateResponse);
		Mockito.verify(solrClientMock, Mockito.times(1)).add(Mockito.eq(collection), Mockito.eq(-1));
	}

	@Test
	public void testSaveDocumentsCommitWithin() throws IOException, SolrServerException {
		Mockito.when(solrClientMock.add(Mockito.anyCollectionOf(SolrInputDocument.class), Mockito.eq(10000)))
				.thenReturn(new UpdateResponse());
		List<SolrInputDocument> collection = Arrays.asList(SIMPLE_DOCUMENT);
		UpdateResponse updateResponse = solrTemplate.saveDocuments(collection, 10000);
		Assert.assertNotNull(updateResponse);
		Mockito.verify(solrClientMock, Mockito.times(1)).add(Mockito.eq(collection), Mockito.eq(10000));
	}

	@Test
	public void testDeleteById() throws IOException, SolrServerException {
		Mockito.when(solrClientMock.deleteById(Mockito.anyString())).thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.deleteById("1");
		Assert.assertNotNull(updateResponse);
		Mockito.verify(solrClientMock, Mockito.times(1)).deleteById(Mockito.eq("1"));
	}

	@Test
	public void testDeleteByIdWithCollection() throws IOException, SolrServerException {
		Mockito.when(solrClientMock.deleteById(Mockito.anyListOf(String.class))).thenReturn(new UpdateResponse());
		List<String> idsToDelete = Arrays.asList("1", "2");
		UpdateResponse updateResponse = solrTemplate.deleteById(idsToDelete);
		Assert.assertNotNull(updateResponse);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<String>> captor = (ArgumentCaptor<List<String>>) (Object) ArgumentCaptor.forClass(List.class);

		Mockito.verify(solrClientMock, Mockito.times(1)).deleteById(captor.capture());

		Assert.assertEquals(idsToDelete.size(), captor.getValue().size());
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
		Mockito.when(responseMock.getResults()).thenReturn(resultList);
		Mockito.when(solrClientMock.query(Mockito.any(SolrQuery.class), Mockito.eq(SolrRequest.METHOD.GET)))
				.thenReturn(responseMock);

		long result = solrTemplate.count(new SimpleQuery(new Criteria("field_1").is("value1")));
		Assert.assertEquals(resultList.getNumFound(), result);

		Mockito.verify(solrClientMock, Mockito.times(1)).query(captor.capture(), Mockito.eq(SolrRequest.METHOD.GET));

		Assert.assertEquals(Integer.valueOf(0), captor.getValue().getStart());
		Assert.assertEquals(Integer.valueOf(0), captor.getValue().getRows());
	}

	@Test
	public void testCountWhenPagingSet() throws SolrServerException, IOException {
		ArgumentCaptor<SolrQuery> captor = ArgumentCaptor.forClass(SolrQuery.class);
		QueryResponse responseMock = Mockito.mock(QueryResponse.class);
		SolrDocumentList resultList = new SolrDocumentList();
		resultList.setNumFound(10);
		Mockito.when(responseMock.getResults()).thenReturn(resultList);
		Mockito.when(solrClientMock.query(Mockito.any(SolrQuery.class), Mockito.eq(SolrRequest.METHOD.GET)))
				.thenReturn(responseMock);

		Query query = new SimpleQuery(new Criteria("field_1").is("value1"));
		query.setPageRequest(new PageRequest(0, 5));
		long result = solrTemplate.count(query);
		Assert.assertEquals(resultList.getNumFound(), result);

		Mockito.verify(solrClientMock, Mockito.times(1)).query(captor.capture(), Mockito.eq(SolrRequest.METHOD.GET));

		Assert.assertEquals(Integer.valueOf(0), captor.getValue().getStart());
		Assert.assertEquals(Integer.valueOf(0), captor.getValue().getRows());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCountNullQuery() {
		solrTemplate.count(null);
	}

	@Test
	public void testCommit() throws SolrServerException, IOException {
		solrTemplate.commit();
		Mockito.verify(solrClientMock, Mockito.times(1)).commit();
	}

	@Test
	public void testSoftCommit() throws SolrServerException, IOException {
		solrTemplate.softCommit();
		Mockito.verify(solrClientMock, Mockito.times(1)).commit(Matchers.eq(true), Matchers.eq(true), Matchers.eq(true));
	}

	@Test
	public void testRollback() throws SolrServerException, IOException {
		solrTemplate.rollback();
		Mockito.verify(solrClientMock, Mockito.times(1)).rollback();
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
		solrTemplate.querySolr(new SimpleQuery(new SimpleStringCriteria("my:criteria")), null);

		ArgumentCaptor<SolrParams> captor = ArgumentCaptor.forClass(SolrParams.class);

		Mockito.verify(solrClientMock, Mockito.times(1)).query(captor.capture(), Mockito.eq(SolrRequest.METHOD.GET));
		Assert.assertEquals("*:*", captor.getValue().getParams(CommonParams.Q)[0]);
	}

	/**
	 * @see DATASOLR-88
	 */
	@Test
	public void testSaveBoostedShouldUseDocumentBoost()
			throws IOException, SolrServerException, SecurityException, NoSuchFieldException {

		solrTemplate.saveBean(SIMPLE_BOOSTED_OBJECT);

		ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
		Mockito.verify(solrClientMock, Mockito.times(1)).add(captor.capture(), Mockito.eq(-1));

		Assert.assertEquals(SIMPLE_BOOSTED_OBJECT.getId(), captor.getValue().getFieldValue("id"));
		Assert.assertEquals(SIMPLE_BOOSTED_OBJECT.getValue(), captor.getValue().getFieldValue("value"));

		float entityBoost = AnnotationUtils.getAnnotation(SIMPLE_BOOSTED_OBJECT.getClass(), SolrDocument.class).boost();
		Assert.assertThat(captor.getValue().getDocumentBoost(), Is.is(entityBoost));
	}

	/**
	 * @see DATASOLR-88
	 */
	@Test
	public void testSaveBoostedShouldUseFieldBoostViaIndexedAnnotation()
			throws IOException, SolrServerException, SecurityException, NoSuchFieldException {

		solrTemplate.saveBean(SIMPLE_BOOSTED_OBJECT);

		ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
		Mockito.verify(solrClientMock, Mockito.times(1)).add(captor.capture(), Mockito.eq(-1));

		Assert.assertEquals(SIMPLE_BOOSTED_OBJECT.getId(), captor.getValue().getFieldValue("id"));
		Assert.assertEquals(SIMPLE_BOOSTED_OBJECT.getValue(), captor.getValue().getFieldValue("value"));

		float fieldBoost = AnnotationUtils
				.getAnnotation(SIMPLE_BOOSTED_OBJECT.getClass().getDeclaredField("boostedField"), Indexed.class).boost();
		Assert.assertThat(captor.getValue().getField("boostedField").getBoost(), Is.is(fieldBoost));
	}

	/**
	 * @throws IOException
	 * @throws SolrServerException
	 * @see DATASOLR-72
	 * @see DATASOLR-313
	 */
	@Test
	public void schemaShouldBeUpdatedPriorToSavingEntity() throws SolrServerException, IOException {

		NamedList<Object> nl = new NamedList<Object>();
		NamedList<Object> schema = new NamedList<Object>();
		nl.add("version", 1.5F);
		nl.add("schema", schema);
		schema.add("version", 1.5F);
		schema.add("name", "mock");
		schema.add("fields", Collections.<NamedList<Object>> emptyList());
		schema.add("dynamicFields", Collections.<NamedList<Object>> emptyList());
		schema.add("fieldTypes", Collections.<NamedList<Object>> emptyList());
		schema.add("copyFields", Collections.<NamedList<Object>> emptyList());

		// schema.add(name, val);

		Mockito.when(solrClientMock.request(Mockito.any(SchemaVersion.class), Mockito.anyString())).thenReturn(nl);
		Mockito.when(solrClientMock.request(Mockito.any(SchemaRequest.class), Mockito.anyString())).thenReturn(nl);

		solrTemplate = new SolrTemplate(solrClientMock, "core1");
		solrTemplate.setSchemaCreationFeatures(Collections.singletonList(Feature.CREATE_MISSING_FIELDS));
		solrTemplate.afterPropertiesSet();
		solrTemplate.saveBean(new DocumentWithIndexAnnotations());

		ArgumentCaptor<SolrRequest> requestCaptor = ArgumentCaptor.forClass(SolrRequest.class);
		Mockito.verify(solrClientMock, Mockito.times(4)).request(requestCaptor.capture(), Mockito.anyString());

		SolrRequest capturedRequest = requestCaptor.getValue();

		Assert.assertThat(capturedRequest.getMethod(), IsEqual.equalTo(SolrRequest.METHOD.POST));
		Assert.assertThat(capturedRequest.getPath(), IsEqual.equalTo("/schema"));
		Assert.assertThat(capturedRequest.getContentStreams(), IsNull.notNullValue());
	}

	/**
	 * @see DATASOLR-83
	 */
	@Test
	public void testGetById() throws SolrServerException, IOException {

		ArgumentCaptor<SolrRequest> captor = ArgumentCaptor.forClass(SolrRequest.class);
		QueryResponse responseMock = Mockito.mock(QueryResponse.class);
		SolrDocumentList resultList = new SolrDocumentList();
		Mockito.when(responseMock.getResults()).thenReturn(resultList);
		Mockito.when(solrClientMock.request(captor.capture(), Matchers.anyString())).thenReturn(new NamedList<Object>());

		DocumentWithIndexAnnotations result = solrTemplate.getById("myId", DocumentWithIndexAnnotations.class);

		Mockito.verify(solrClientMock, Mockito.times(1)).request(captor.capture(), Matchers.anyString());
		Assert.assertNull(result);
		Assert.assertEquals("myId", captor.getValue().getParams().get("ids"));
		Assert.assertEquals("/get", captor.getValue().getPath());
	}

	/**
	 * @see DATASOLR-83
	 */
	@Test
	public void testGetByIds() throws SolrServerException, IOException {

		ArgumentCaptor<SolrRequest> captor = ArgumentCaptor.forClass(SolrRequest.class);

		QueryResponse responseMock = Mockito.mock(QueryResponse.class);
		SolrDocumentList resultList = new SolrDocumentList();
		Mockito.when(responseMock.getResults()).thenReturn(resultList);
		Mockito.when(solrClientMock.request(captor.capture(), Matchers.anyString())).thenReturn(new NamedList<Object>());

		List<String> ids = Arrays.asList("myId1", "myId2");
		Collection<DocumentWithIndexAnnotations> result = solrTemplate.getById(ids, DocumentWithIndexAnnotations.class);

		Mockito.verify(solrClientMock, Mockito.times(1)).request(captor.capture(), Matchers.anyString());
		Assert.assertTrue(result.isEmpty());
		Assert.assertArrayEquals(new String[] { "myId1", "myId2" }, captor.getValue().getParams().getParams("ids"));
		Assert.assertEquals("/get", captor.getValue().getPath());
	}

	/**
	 * @see DATASOLR-160
	 */
	@Test
	public void testSaveShouldNotSaveScoreField()
			throws IOException, SolrServerException, SecurityException, NoSuchFieldException {

		solrTemplate.saveBean(new DocumentWithScoreAnnotation());

		ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
		Mockito.verify(solrClientMock, Mockito.times(1)).add(captor.capture(), Mockito.eq(-1));

		Assert.assertNull(captor.getValue().getFieldValue("score"));
	}

	/**
	 * @see DATASOLR-215
	 */
	@Test
	public void usesTemplateDefaultRequestMethodForQuery() throws SolrServerException, IOException {

		solrTemplate = new SolrTemplate(solrClientMock, "core1", RequestMethod.POST);
		solrTemplate.afterPropertiesSet();

		Mockito.when(solrClientMock.query(Matchers.any(SolrParams.class), Mockito.eq(SolrRequest.METHOD.POST)))
				.thenReturn(new QueryResponse());
		solrTemplate.querySolr(new SimpleQuery("*:*"), DocumentWithIndexAnnotations.class);

		Mockito.verify(solrClientMock, Mockito.times(1)).query(Matchers.any(SolrParams.class),
				Mockito.eq(SolrRequest.METHOD.POST));
	}

	/**
	 * @see DATASOLR-215
	 */
	@Test
	public void usesTemplateMethodRequetsParameterForQuery() throws SolrServerException, IOException {

		solrTemplate = new SolrTemplate(solrClientMock, "core1", RequestMethod.POST);
		solrTemplate.afterPropertiesSet();

		Mockito.when(solrClientMock.query(Matchers.any(SolrParams.class), Mockito.eq(SolrRequest.METHOD.PUT)))
				.thenReturn(new QueryResponse());
		solrTemplate.querySolr(new SimpleQuery("*:*"), DocumentWithIndexAnnotations.class, RequestMethod.PUT);

		Mockito.verify(solrClientMock, Mockito.times(1)).query(Matchers.any(SolrParams.class),
				Mockito.eq(SolrRequest.METHOD.PUT));
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
