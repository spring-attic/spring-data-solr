/*
 * Copyright 2012 - 2013 the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.http.ParseException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.PartialUpdate;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.server.SolrServerFactory;

/*
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrTemplateTests {

	private SolrTemplate solrTemplate;

	private static final SimpleJavaObject SIMPLE_OBJECT = new SimpleJavaObject("simple-string-id", 123l);
	private static final SolrInputDocument SIMPLE_DOCUMENT = new SolrInputDocument();

	@Mock
	private SolrServer solrServerMock;

	@Before
	public void setUp() {
		solrTemplate = new SolrTemplate(solrServerMock, "core1");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullServerFactory() {
		new SolrTemplate((SolrServerFactory) null);
	}

	@Test
	public void testPing() throws SolrServerException, IOException {
		Mockito.when(solrServerMock.ping()).thenReturn(new SolrPingResponse());
		SolrPingResponse pingResult = solrTemplate.ping();
		Assert.assertNotNull(pingResult);
		Mockito.verify(solrServerMock, Mockito.times(1)).ping();
	}

	@Test(expected = DataAccessException.class)
	public void testPingThrowsException() throws SolrServerException, IOException {
		Mockito.when(solrServerMock.ping()).thenThrow(
				new SolrServerException("error", new SolrException(ErrorCode.NOT_FOUND, "not found")));
		solrTemplate.ping();
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testQueryThrowsParseException() throws SolrServerException {
		Mockito.when(solrServerMock.query(Matchers.any(SolrParams.class))).thenThrow(
				new SolrServerException("error", new SolrException(ErrorCode.BAD_REQUEST, new ParseException("parse error"))));
		solrTemplate.executeSolrQuery(new SolrQuery());
	}

	@Test(expected = UncategorizedSolrException.class)
	public void testQueryThrowsUntranslateableException() throws SolrServerException {
		Mockito.when(solrServerMock.query(Matchers.any(SolrParams.class))).thenThrow(new SecurityException());
		solrTemplate.executeSolrQuery(new SolrQuery());
	}

	@Test
	public void testSaveBean() throws IOException, SolrServerException {
		Mockito.when(solrServerMock.add(Mockito.any(SolrInputDocument.class),Mockito.eq(-1))).thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.saveBean(SIMPLE_OBJECT);
		Assert.assertNotNull(updateResponse);

		ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
		Mockito.verify(solrServerMock, Mockito.times(1)).add(captor.capture(),Mockito.eq(-1));

		Assert.assertEquals(SIMPLE_OBJECT.getId(), captor.getValue().getFieldValue("id"));
		Assert.assertEquals(SIMPLE_OBJECT.getValue(), captor.getValue().getFieldValue("value"));
	}

    @Test
    public void testSaveBeanCommitWithin() throws IOException, SolrServerException {
        Mockito.when(solrServerMock.add(Mockito.any(SolrInputDocument.class),Mockito.eq(10000))).thenReturn(new UpdateResponse());
        UpdateResponse updateResponse = solrTemplate.saveBean(SIMPLE_OBJECT,10000);
        Assert.assertNotNull(updateResponse);

        ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
        Mockito.verify(solrServerMock, Mockito.times(1)).add(captor.capture(),Mockito.eq(10000));

        Assert.assertEquals(SIMPLE_OBJECT.getId(), captor.getValue().getFieldValue("id"));
        Assert.assertEquals(SIMPLE_OBJECT.getValue(), captor.getValue().getFieldValue("value"));
    }


    @SuppressWarnings("unchecked")
	@Test
	public void testPartialUpdate() throws SolrServerException, IOException {
		Mockito.when(solrServerMock.add(Mockito.any(SolrInputDocument.class),Mockito.eq(-1))).thenReturn(new UpdateResponse());

		PartialUpdate update = new PartialUpdate("id", "update-id");
		update.add("field_1", "update");

		solrTemplate.saveBean(update);
		ArgumentCaptor<SolrInputDocument> captor = ArgumentCaptor.forClass(SolrInputDocument.class);
		Mockito.verify(solrServerMock, Mockito.times(1)).add(captor.capture(),Mockito.eq(-1));

		Assert.assertTrue(captor.getValue().getFieldValue("field_1") instanceof Map);
		Assert.assertEquals("update", ((Map<String, Object>) captor.getValue().getFieldValue("field_1")).get("set"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSaveBeans() throws IOException, SolrServerException {
		Mockito.when(solrServerMock.add(Mockito.anyCollectionOf(SolrInputDocument.class),Mockito.eq(-1))).thenReturn(new UpdateResponse());
		List<SimpleJavaObject> collection = Arrays.asList(new SimpleJavaObject("1", 1l), new SimpleJavaObject("2", 2l),
				new SimpleJavaObject("3", 3l));
		UpdateResponse updateResponse = solrTemplate.saveBeans(collection);
		Assert.assertNotNull(updateResponse);

		@SuppressWarnings("rawtypes")
		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		Mockito.verify(solrServerMock, Mockito.times(1)).add(captor.capture(),Mockito.eq(-1));

		Assert.assertEquals(3, captor.getValue().size());
	}

    @SuppressWarnings("unchecked")
    @Test
    public void testSaveBeansCommitWithin() throws IOException, SolrServerException {
        Mockito.when(solrServerMock.add(Mockito.anyCollectionOf(SolrInputDocument.class),Mockito.eq(10000))).thenReturn(new UpdateResponse());
        List<SimpleJavaObject> collection = Arrays.asList(new SimpleJavaObject("1", 1l), new SimpleJavaObject("2", 2l),
                new SimpleJavaObject("3", 3l));
        UpdateResponse updateResponse = solrTemplate.saveBeans(collection,10000);
        Assert.assertNotNull(updateResponse);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(solrServerMock, Mockito.times(1)).add(captor.capture(),Mockito.eq(10000));

        Assert.assertEquals(3, captor.getValue().size());
    }

	@Test
	public void testSaveDocument() throws IOException, SolrServerException {
		Mockito.when(solrServerMock.add(Mockito.any(SolrInputDocument.class),Mockito.eq(-1))).thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.saveDocument(SIMPLE_DOCUMENT);
		Assert.assertNotNull(updateResponse);
		Mockito.verify(solrServerMock, Mockito.times(1)).add(Mockito.eq(SIMPLE_DOCUMENT),Mockito.eq(-1));
	}

    @Test
    public void testSaveDocumentCommitWithin() throws IOException, SolrServerException {
        Mockito.when(solrServerMock.add(Mockito.any(SolrInputDocument.class),Mockito.eq(10000))).thenReturn(new UpdateResponse());
        UpdateResponse updateResponse = solrTemplate.saveDocument(SIMPLE_DOCUMENT,10000);
        Assert.assertNotNull(updateResponse);
        Mockito.verify(solrServerMock, Mockito.times(1)).add(Mockito.eq(SIMPLE_DOCUMENT),Mockito.eq(10000));
    }

	@Test
	public void testSaveDocuments() throws IOException, SolrServerException {
		Mockito.when(solrServerMock.add(Mockito.anyCollectionOf(SolrInputDocument.class),Mockito.eq(-1))).thenReturn(new UpdateResponse());
		List<SolrInputDocument> collection = Arrays.asList(SIMPLE_DOCUMENT);
		UpdateResponse updateResponse = solrTemplate.saveDocuments(collection);
		Assert.assertNotNull(updateResponse);
		Mockito.verify(solrServerMock, Mockito.times(1)).add(Mockito.eq(collection),Mockito.eq(-1));
	}

    @Test
    public void testSaveDocumentsCommitWithin() throws IOException, SolrServerException {
        Mockito.when(solrServerMock.add(Mockito.anyCollectionOf(SolrInputDocument.class),Mockito.eq(10000))).thenReturn(new UpdateResponse());
        List<SolrInputDocument> collection = Arrays.asList(SIMPLE_DOCUMENT);
        UpdateResponse updateResponse = solrTemplate.saveDocuments(collection,10000);
        Assert.assertNotNull(updateResponse);
        Mockito.verify(solrServerMock, Mockito.times(1)).add(Mockito.eq(collection),Mockito.eq(10000));
    }

	@Test
	public void testDeleteById() throws IOException, SolrServerException {
		Mockito.when(solrServerMock.deleteById(Mockito.anyString())).thenReturn(new UpdateResponse());
		UpdateResponse updateResponse = solrTemplate.deleteById("1");
		Assert.assertNotNull(updateResponse);
		Mockito.verify(solrServerMock, Mockito.times(1)).deleteById(Mockito.eq("1"));
	}

	@Test
	public void testDeleteByIdWithCollection() throws IOException, SolrServerException {
		Mockito.when(solrServerMock.deleteById(Mockito.anyListOf(String.class))).thenReturn(new UpdateResponse());
		List<String> idsToDelete = Arrays.asList("1", "2");
		UpdateResponse updateResponse = solrTemplate.deleteById(idsToDelete);
		Assert.assertNotNull(updateResponse);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<String>> captor = (ArgumentCaptor<List<String>>) (Object) ArgumentCaptor.forClass(List.class);

		Mockito.verify(solrServerMock, Mockito.times(1)).deleteById(captor.capture());

		Assert.assertEquals(idsToDelete.size(), captor.getValue().size());
		for (String s : idsToDelete) {
			Assert.assertTrue(captor.getValue().contains(s));
		}
	}

	@Test
	public void testCount() throws SolrServerException {
		ArgumentCaptor<SolrQuery> captor = ArgumentCaptor.forClass(SolrQuery.class);
		QueryResponse responseMock = Mockito.mock(QueryResponse.class);
		SolrDocumentList resultList = new SolrDocumentList();
		resultList.setNumFound(10);
		Mockito.when(responseMock.getResults()).thenReturn(resultList);
		Mockito.when(solrServerMock.query(Mockito.any(SolrQuery.class))).thenReturn(responseMock);

		long result = solrTemplate.count(new SimpleQuery(new Criteria("field_1").is("value1")));
		Assert.assertEquals(resultList.getNumFound(), result);

		Mockito.verify(solrServerMock, Mockito.times(1)).query(captor.capture());

		Assert.assertEquals(Integer.valueOf(0), captor.getValue().getStart());
		Assert.assertEquals(Integer.valueOf(0), captor.getValue().getRows());
	}

	@Test
	public void testCountWhenPagingSet() throws SolrServerException {
		ArgumentCaptor<SolrQuery> captor = ArgumentCaptor.forClass(SolrQuery.class);
		QueryResponse responseMock = Mockito.mock(QueryResponse.class);
		SolrDocumentList resultList = new SolrDocumentList();
		resultList.setNumFound(10);
		Mockito.when(responseMock.getResults()).thenReturn(resultList);
		Mockito.when(solrServerMock.query(Mockito.any(SolrQuery.class))).thenReturn(responseMock);

		Query query = new SimpleQuery(new Criteria("field_1").is("value1"));
		query.setPageRequest(new PageRequest(0, 5));
		long result = solrTemplate.count(query);
		Assert.assertEquals(resultList.getNumFound(), result);

		Mockito.verify(solrServerMock, Mockito.times(1)).query(captor.capture());

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
		Mockito.verify(solrServerMock, Mockito.times(1)).commit();
	}

	@Test
	public void testSoftCommit() throws SolrServerException, IOException {
		solrTemplate.softCommit();
		Mockito.verify(solrServerMock, Mockito.times(1)).commit(Matchers.eq(true), Matchers.eq(true), Matchers.eq(true));
	}

	@Test
	public void testRollback() throws SolrServerException, IOException {
		solrTemplate.rollback();
		Mockito.verify(solrServerMock, Mockito.times(1)).rollback();
	}

	@Test
	public void testDifferentQueryParser() throws SolrServerException {
		QueryParser parser = new QueryParser() {

			@Override
			public void registerConverter(Converter<?, ?> converter) {
			}

			@Override
			public String getQueryString(SolrDataQuery query) {
				return "*:*";
			}

			@Override
			public SolrQuery constructSolrQuery(SolrDataQuery query) {
				return new SolrQuery(getQueryString(query));
			}
		};

		solrTemplate.setQueryParser(parser);
		solrTemplate.query(new SimpleQuery(new SimpleStringCriteria("my:criteria")));

		ArgumentCaptor<SolrParams> captor = ArgumentCaptor.forClass(SolrParams.class);

		Mockito.verify(solrServerMock, Mockito.times(1)).query(captor.capture());
		Assert.assertEquals("*:*", captor.getValue().getParams(CommonParams.Q)[0]);
	}
}
