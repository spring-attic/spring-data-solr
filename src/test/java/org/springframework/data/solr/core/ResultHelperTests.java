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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.client.solrj.response.TermsResponse.Term;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPivotFieldEntry;
import org.springframework.data.solr.core.query.result.FacetQueryEntry;
import org.springframework.data.solr.core.query.result.FieldStatsResult;
import org.springframework.data.solr.core.query.result.GroupEntry;
import org.springframework.data.solr.core.query.result.GroupResult;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;
import org.springframework.data.solr.core.query.result.SolrResultPage;
import org.springframework.data.solr.core.query.result.StatsResult;
import org.springframework.data.solr.core.query.result.TermsFieldEntry;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ResultHelperTests {

	@Mock private QueryResponse response;

	@Test
	public void testConvertFacetQueryResponseForNullQueryResponse() {
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPageMap(createFacetQuery("field_1"), null);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertFacetQueryResponseForNullQuery() {
		ResultHelper.convertFacetQueryResponseToFacetPageMap(null, null);
	}

	@Test
	public void testConvertFacetQueryResponseForQueryWithoutFacetOptions() {
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPageMap(new SimpleFacetQuery(new Criteria("field_1")), null);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithNullFacetFields() {
		Mockito.when(response.getFacetFields()).thenReturn(null);
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPageMap(createFacetQuery("field_1"), response);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithEmptyFacetFields() {
		Mockito.when(response.getFacetFields()).thenReturn(Collections.<FacetField> emptyList());
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPageMap(createFacetQuery("field_1"), response);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithSingleFacetFieldWithoutValues() {
		List<FacetField> fieldList = new ArrayList<>(1);
		FacetField ffield = new FacetField("field_1");
		fieldList.add(ffield);

		Mockito.when(response.getFacetFields()).thenReturn(fieldList);

		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPageMap(createFacetQuery("field_1"), response);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Entry<Field, Page<FacetFieldEntry>> resultEntry = result.entrySet().iterator().next();

		Assert.assertEquals(ffield.getName(), resultEntry.getKey().getName());
		Assert.assertTrue(resultEntry.getValue().getContent().isEmpty());
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithSingeFacetField() {
		List<FacetField> fieldList = new ArrayList<>(1);
		FacetField ffield = createFacetField("field_1", 1, 2);
		fieldList.add(ffield);

		Mockito.when(response.getFacetFields()).thenReturn(fieldList);

		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPageMap(createFacetQuery("field_1"), response);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Entry<Field, Page<FacetFieldEntry>> resultEntry = result.entrySet().iterator().next();

		Assert.assertEquals(ffield.getName(), resultEntry.getKey().getName());
		Assert.assertEquals(2, resultEntry.getValue().getContent().size());
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithNullFacetQueries() {
		Mockito.when(response.getFacetQuery()).thenReturn(null);
		List<FacetQueryEntry> result = ResultHelper.convertFacetQueryResponseToFacetQueryResult(
				createFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_1:[* TO 5]"))), response);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithEmptyFacetQueries() {
		Mockito.when(response.getFacetQuery()).thenReturn(Collections.<String, Integer> emptyMap());
		List<FacetQueryEntry> result = ResultHelper.convertFacetQueryResponseToFacetQueryResult(
				createFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_1:[* TO 5]"))), response);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithFacetQueries() {
		Map<String, Integer> resultMap = new LinkedHashMap<>(2);
		resultMap.put("field_1:[* TO 5]", 5);
		resultMap.put("field_1:[6 TO *]", 10);

		Mockito.when(response.getFacetQuery()).thenReturn(resultMap);
		List<FacetQueryEntry> result = ResultHelper.convertFacetQueryResponseToFacetQueryResult(
				createFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_1:[* TO 5]")),
						new SimpleQuery(new SimpleStringCriteria("field_1:[6 TO *]"))),
				response);
		Assert.assertNotNull(result);
		Assert.assertEquals(2, result.size());

		Assert.assertEquals(5, result.get(0).getValueCount());
		Assert.assertEquals("field_1:[* TO 5]", result.get(0).getValue());
		Assert.assertEquals("field_1:[* TO 5]", result.get(0).getKey());
		Assert.assertEquals("field_1:[* TO 5]", result.get(0).getQuery().getCriteria().toString());

		Assert.assertEquals(10, result.get(1).getValueCount());
		Assert.assertEquals("field_1:[6 TO *]", result.get(1).getValue());
		Assert.assertEquals("field_1:[6 TO *]", result.get(1).getKey());
		Assert.assertEquals("field_1:[6 TO *]", result.get(1).getQuery().getCriteria().toString());
	}

	@Test
	public void testParseAndAddHighlightQueryResponseToResultPageWithEmptyHighlighting() {
		Mockito.when(response.getHighlighting()).thenReturn(Collections.<String, Map<String, List<String>>> emptyMap());
		Assert.assertTrue(ResultHelper.convertAndAddHighlightQueryResponseToResultPage(response,
				new SolrResultPage<>(Collections.singletonList(new Object()))).isEmpty());
	}

	@Test
	public void testParseAndAddHighlightQueryResponseToResultPageWithNullHighlighting() {
		Mockito.when(response.getHighlighting()).thenReturn(null);
		Assert.assertTrue(ResultHelper.convertAndAddHighlightQueryResponseToResultPage(response,
				new SolrResultPage<>(Collections.singletonList(new Object()))).isEmpty());
	}

	@Test
	public void testParseAndAddHighlightQueryResponseToResultPageWithNullResponse() {
		Assert.assertTrue(ResultHelper.convertAndAddHighlightQueryResponseToResultPage(null,
				new SolrResultPage<>(Collections.singletonList(new Object()))).isEmpty());
	}

	@Test
	public void testParseAndAddHighlightQueryResponseToResultPage() {
		Map<String, Map<String, List<String>>> highlightingData = new LinkedHashMap<>();
		Map<String, List<String>> fieldHighlights = new LinkedHashMap<>();
		fieldHighlights.put("field_1", Arrays.asList("highlight 1", "highlight 2"));
		fieldHighlights.put("field_2", Collections.singletonList("highlight 3"));
		highlightingData.put("entity-id-1", fieldHighlights);

		Mockito.when(response.getHighlighting()).thenReturn(highlightingData);

		SolrBeanWithIdNamedField resultBean = new SolrBeanWithIdNamedField("entity-id-1");

		List<HighlightEntry<SolrBeanWithIdNamedField>> result = ResultHelper
				.convertAndAddHighlightQueryResponseToResultPage(response,
						new SolrResultPage<>(Collections.singletonList(resultBean)));

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(resultBean, result.get(0).getEntity());
		Assert.assertEquals(2, result.get(0).getHighlights().size());
		for (HighlightEntry<SolrBeanWithIdNamedField> entry : result) {
			Assert.assertEquals(resultBean, entry.getEntity());
			for (Highlight highlight : entry.getHighlights()) {
				Assert.assertTrue(fieldHighlights.containsKey(highlight.getField().getName()));
				Assert.assertEquals(fieldHighlights.get(highlight.getField().getName()), highlight.getSnipplets());
			}
		}
	}

	@Test
	public void testParseAndAddHighlightQueryResponseWithMultipleEntriesToResultPage() {
		Map<String, Map<String, List<String>>> highlightingData = new LinkedHashMap<>();

		Map<String, List<String>> fieldHighlightsEntity1 = new LinkedHashMap<>();
		fieldHighlightsEntity1.put("field_1", Arrays.asList("highlight 1", "highlight 2"));
		fieldHighlightsEntity1.put("field_2", Collections.singletonList("highlight 3"));
		highlightingData.put("entity-id-1", fieldHighlightsEntity1);

		Map<String, List<String>> fieldHighlightsEntity2 = new LinkedHashMap<>();
		fieldHighlightsEntity2.put("field_3", Collections.singletonList("highlight 3"));
		highlightingData.put("entity-id-2", fieldHighlightsEntity2);

		Mockito.when(response.getHighlighting()).thenReturn(highlightingData);

		SolrBeanWithIdNamedField resultBean1 = new SolrBeanWithIdNamedField("entity-id-1");
		SolrBeanWithIdNamedField resultBean2 = new SolrBeanWithIdNamedField("entity-id-2");

		List<HighlightEntry<SolrBeanWithIdNamedField>> result = ResultHelper
				.convertAndAddHighlightQueryResponseToResultPage(response,
						new SolrResultPage<>(Arrays.asList(resultBean1, resultBean2)));

		Assert.assertEquals(2, result.size());
		Assert.assertEquals(resultBean1, result.get(0).getEntity());
		Assert.assertEquals(resultBean2, result.get(1).getEntity());
		Assert.assertEquals(2, result.get(0).getHighlights().size());
		Assert.assertEquals(1, result.get(1).getHighlights().size());
	}

	@Test
	public void testParseAndAddHighlightQueryResponseForBeanWithAnnotatedId() {
		Map<String, Map<String, List<String>>> highlightingData = new LinkedHashMap<>();
		Map<String, List<String>> fieldHighlights = new LinkedHashMap<>();
		fieldHighlights.put("field_1", Arrays.asList("highlight 1", "highlight 2"));
		fieldHighlights.put("field_2", Collections.singletonList("highlight 3"));
		highlightingData.put("entity-id-1", fieldHighlights);

		Mockito.when(response.getHighlighting()).thenReturn(highlightingData);

		SolrBeanWithAnnoteatedIdNamedField resultBean = new SolrBeanWithAnnoteatedIdNamedField("entity-id-1");

		List<HighlightEntry<SolrBeanWithAnnoteatedIdNamedField>> result = ResultHelper
				.convertAndAddHighlightQueryResponseToResultPage(response,
						new SolrResultPage<>(Collections.singletonList(resultBean)));

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(resultBean, result.get(0).getEntity());
		Assert.assertEquals(2, result.get(0).getHighlights().size());
		for (HighlightEntry<SolrBeanWithAnnoteatedIdNamedField> entry : result) {
			Assert.assertEquals(resultBean, entry.getEntity());
			for (Highlight highlight : entry.getHighlights()) {
				Assert.assertTrue(fieldHighlights.containsKey(highlight.getField().getName()));
				Assert.assertEquals(fieldHighlights.get(highlight.getField().getName()), highlight.getSnipplets());
			}
		}
	}

	@Test
	public void testConvertFacetRangeQueryResponseToFacetPageMapForNullQueryResponse() {
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToRangeFacetPageMap(this.createFacetQuery("field_1"), null);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertFacetRangeQueryResponseForNullQuery() {
		ResultHelper.convertFacetQueryResponseToRangeFacetPageMap(null, null);
	}

	@Test
	public void testConvertFacetRangeQueryResponseForQueryWithoutFacetOptions() {
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToRangeFacetPageMap(new SimpleFacetQuery(new SimpleStringCriteria("*:*")), null);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertFacetRangeQueryResponseForQueryResultWithNullFacetFields() {
		Mockito.when(response.getFacetFields()).thenReturn(null);
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToRangeFacetPageMap(createFacetQuery("field_1"), response);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertFacetRangeQueryResponseForQueryResultWithEmptyFacetFields() {
		Mockito.when(response.getFacetFields()).thenReturn(Collections.<FacetField> emptyList());
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToRangeFacetPageMap(createFacetQuery("field_1"), response);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertFacetQueryResponseToFacetPivotMap() {
		NamedList<List<org.apache.solr.client.solrj.response.PivotField>> pivotData = new NamedList<>();
		List<org.apache.solr.client.solrj.response.PivotField> vals = new ArrayList<>();
		{
			List<org.apache.solr.client.solrj.response.PivotField> pivotValues = new ArrayList<>();
			pivotValues
					.add(new org.apache.solr.client.solrj.response.PivotField("field_2", "value_1_1", 7, null, null, null, null));
			pivotValues
					.add(new org.apache.solr.client.solrj.response.PivotField("field_2", "value_1_2", 3, null, null, null, null));
			vals.add(new org.apache.solr.client.solrj.response.PivotField("field_1", "value_1", 10, pivotValues, null, null,
					null));
		}
		{
			List<org.apache.solr.client.solrj.response.PivotField> pivotValues = new ArrayList<>();
			pivotValues
					.add(new org.apache.solr.client.solrj.response.PivotField("field_2", "value_2_1", 2, null, null, null, null));
			vals.add(
					new org.apache.solr.client.solrj.response.PivotField("field_1", "value_2", 2, pivotValues, null, null, null));
		}
		pivotData.add("field_1,field_2", vals);

		Mockito.when(response.getFacetPivot()).thenReturn(pivotData);

		Map<PivotField, List<FacetPivotFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPivotMap(createFacetPivotQuery("field_1", "field_2"), response);

		List<FacetPivotFieldEntry> resultPivot = result.get(new SimplePivotField("field_1", "field_2"));
		Assert.assertNotNull(result);
		Assert.assertEquals(2, resultPivot.size());

		Assert.assertNotNull(resultPivot.get(0));
		Assert.assertEquals("value_1", resultPivot.get(0).getValue());
		Assert.assertNotNull(resultPivot.get(0).getField());
		Assert.assertEquals("field_1", resultPivot.get(0).getField().getName());
		Assert.assertEquals(10, resultPivot.get(0).getValueCount());
		Assert.assertNotNull(resultPivot.get(0).getPivot());
		Assert.assertEquals(2, resultPivot.get(0).getPivot().size());

		{
			List<FacetPivotFieldEntry> pivot = resultPivot.get(0).getPivot();
			Assert.assertEquals("value_1_1", pivot.get(0).getValue());
			Assert.assertNotNull(pivot.get(0).getField());
			Assert.assertEquals("field_2", pivot.get(0).getField().getName());
			Assert.assertEquals(7, pivot.get(0).getValueCount());
			Assert.assertEquals(pivot.get(0).getPivot(), Collections.emptyList());
			Assert.assertEquals("value_1_2", pivot.get(1).getValue());
			Assert.assertNotNull(pivot.get(1).getField());
			Assert.assertEquals("field_2", pivot.get(1).getField().getName());
			Assert.assertEquals(3, pivot.get(1).getValueCount());
			Assert.assertEquals(pivot.get(1).getPivot(), Collections.emptyList());
		}

		{
			List<FacetPivotFieldEntry> pivot = resultPivot.get(1).getPivot();
			Assert.assertEquals("value_2_1", pivot.get(0).getValue());
			Assert.assertNotNull(pivot.get(0).getField());
			Assert.assertEquals("field_2", pivot.get(0).getField().getName());
			Assert.assertEquals(2, pivot.get(0).getValueCount());
			Assert.assertEquals(pivot.get(0).getPivot(), Collections.emptyList());
		}

		Assert.assertNotNull(resultPivot.get(0).getPivot().get(0));

	}

	@Test
	public void testConvertTermsQueryResponseReturnsTermsMapCorrectlyWhenOneFieldReturned() {
		TermsResponse termsResponse = new TermsResponse(new NamedList<>());
		termsResponse.getTermMap().put("field_1", Arrays.asList(new Term("term_1", 10), new Term("term_2", 5)));

		Mockito.when(response.getTermsResponse()).thenReturn(termsResponse);

		Map<String, List<TermsFieldEntry>> result = ResultHelper.convertTermsQueryResponseToTermsMap(response);

		Assert.assertEquals(1, result.size());
		Assert.assertEquals("term_1", result.get("field_1").get(0).getValue());
		Assert.assertEquals(10L, result.get("field_1").get(0).getValueCount());
		Assert.assertEquals("field_1", result.get("field_1").get(0).getField().getName());

		Assert.assertEquals("term_2", result.get("field_1").get(1).getValue());
		Assert.assertEquals(5L, result.get("field_1").get(1).getValueCount());
		Assert.assertEquals("field_1", result.get("field_1").get(1).getField().getName());
	}

	@Test
	public void testConvertTermsQueryResponseReturnsTermsMapCorrectlyWhenMultipleFieldsReturned() {
		TermsResponse termsResponse = new TermsResponse(new NamedList<>());
		termsResponse.getTermMap().put("field_1", Arrays.asList(new Term("term_1", 10), new Term("term_2", 5)));
		termsResponse.getTermMap().put("field_2", Arrays.asList(new Term("term_2", 2), new Term("term_3", 1)));

		Mockito.when(response.getTermsResponse()).thenReturn(termsResponse);

		Map<String, List<TermsFieldEntry>> result = ResultHelper.convertTermsQueryResponseToTermsMap(response);

		Assert.assertEquals(2, result.size());
		Assert.assertEquals("field_1", result.get("field_1").get(0).getField().getName());
		Assert.assertEquals("field_2", result.get("field_2").get(0).getField().getName());
	}

	@Test
	public void testConvertTermsQueryResponseReturnsEmtpyMapWhenResponseIsNull() {
		Assert.assertThat(ResultHelper.convertTermsQueryResponseToTermsMap(null),
				IsEqual.equalTo(Collections.<String, List<TermsFieldEntry>> emptyMap()));
	}

	@Test
	public void testConvertTermsQueryResponseReturnsEmtpyMapWhenTermsResponseIsNull() {
		Mockito.when(response.getTermsResponse()).thenReturn(null);

		Assert.assertThat(ResultHelper.convertTermsQueryResponseToTermsMap(response),
				IsEqual.equalTo(Collections.<String, List<TermsFieldEntry>> emptyMap()));
	}

	@Test
	public void testConvertTermsQueryResponseReturnsEmtpyMapWhenTermsMapIsEmpty() {
		TermsResponse termsResponse = new TermsResponse(new NamedList<>());
		Mockito.when(response.getTermsResponse()).thenReturn(termsResponse);

		Assert.assertThat(ResultHelper.convertTermsQueryResponseToTermsMap(response),
				IsEqual.equalTo(Collections.<String, List<TermsFieldEntry>> emptyMap()));
	}

	@Test // DATASOLR-121
	public void testConvertGroupQueryResponseToGroupResultList() {
		GroupResponse groupResponse = Mockito.mock(GroupResponse.class);
		Query query = Mockito.mock(Query.class);
		SolrTemplate solrTemplate = Mockito.mock(SolrTemplate.class);
		GroupCommand groupCommand1 = Mockito.mock(GroupCommand.class);
		Group group1_1 = Mockito.mock(Group.class);
		SolrDocumentList group1_1DocumentList = Mockito.mock(SolrDocumentList.class);
		List<Object> documents1_1 = Collections.singletonList(new Object());

		Mockito.when(response.getGroupResponse()).thenReturn(groupResponse);
		Mockito.when(groupResponse.getValues()).thenReturn(Collections.singletonList(groupCommand1));
		Mockito.when(groupCommand1.getValues()).thenReturn(Collections.singletonList(group1_1));
		Mockito.when(group1_1.getResult()).thenReturn(group1_1DocumentList);
		Mockito.when(group1_1.getGroupValue()).thenReturn("group1_1_value");
		Mockito.when(group1_1DocumentList.getNumFound()).thenReturn(3L);
		Mockito.when(solrTemplate.convertSolrDocumentListToBeans(group1_1DocumentList, Object.class))
				.thenReturn(documents1_1);
		Mockito.when(groupCommand1.getMatches()).thenReturn(1);
		Mockito.when(groupCommand1.getName()).thenReturn("group1_name");
		Mockito.when(groupCommand1.getNGroups()).thenReturn(2);

		GroupOptions groupOptions = new GroupOptions();
		groupOptions.setLimit(1);

		Mockito.when(query.getPageRequest()).thenReturn(new PageRequest(0, 1));
		Mockito.when(query.getGroupOptions()).thenReturn(groupOptions);

		Object group1Key = new Object();
		Map<String, Object> objectNames = new HashMap<>();
		objectNames.put("group1_name", group1Key);

		Map<Object, GroupResult<Object>> result = ResultHelper.convertGroupQueryResponseToGroupResultMap(query, objectNames,
				response, solrTemplate, Object.class);
		Assert.assertNotNull(result);
		Assert.assertEquals(2, result.size());

		GroupResult<Object> groupResult = result.get("group1_name");
		Assert.assertEquals(groupResult, result.get(group1Key));
		Assert.assertEquals("group1_name", groupResult.getName());
		Assert.assertEquals(1, groupResult.getMatches());
		Assert.assertEquals(Integer.valueOf(2), groupResult.getGroupsCount());

		Page<GroupEntry<Object>> groupEntries = groupResult.getGroupEntries();
		Assert.assertEquals(2, groupEntries.getTotalElements());
		Assert.assertEquals(2, groupEntries.getTotalPages());
		Assert.assertEquals(true, groupEntries.hasNext());

		List<GroupEntry<Object>> groupEntriesContent = groupEntries.getContent();
		Assert.assertNotNull(groupEntriesContent);
		Assert.assertEquals(1, groupEntriesContent.size());

		GroupEntry<Object> groupEntriesContentElement = groupEntriesContent.get(0);
		Assert.assertEquals("group1_1_value", groupEntriesContentElement.getGroupValue());

		Page<Object> group1result = groupEntriesContentElement.getResult();
		Assert.assertEquals(3, group1result.getTotalElements());
		Assert.assertEquals(3, group1result.getTotalPages());
		Assert.assertEquals(true, group1result.hasNext());
	}

	@Test // DATASOLR-121
	public void testConvertGroupQueryResponseToGroupResultListWhenNoCountOfGroups() {
		GroupResponse groupResponse = Mockito.mock(GroupResponse.class);
		Query query = Mockito.mock(Query.class);
		SolrTemplate solrTemplate = Mockito.mock(SolrTemplate.class);
		GroupCommand groupCommand1 = Mockito.mock(GroupCommand.class);
		Group group1_1 = Mockito.mock(Group.class);
		SolrDocumentList group1_1DocumentList = Mockito.mock(SolrDocumentList.class);
		List<Object> documents1_1 = Collections.singletonList(new Object());

		Mockito.when(response.getGroupResponse()).thenReturn(groupResponse);
		Mockito.when(groupResponse.getValues()).thenReturn(Collections.singletonList(groupCommand1));
		Mockito.when(groupCommand1.getValues()).thenReturn(Collections.singletonList(group1_1));
		Mockito.when(group1_1.getResult()).thenReturn(group1_1DocumentList);
		Mockito.when(group1_1.getGroupValue()).thenReturn("group1_1_value");
		Mockito.when(group1_1DocumentList.getNumFound()).thenReturn(3L);
		Mockito.when(solrTemplate.convertSolrDocumentListToBeans(group1_1DocumentList, Object.class))
				.thenReturn(documents1_1);
		Mockito.when(groupCommand1.getMatches()).thenReturn(1);
		Mockito.when(groupCommand1.getName()).thenReturn("group1_name");
		Mockito.when(groupCommand1.getNGroups()).thenReturn(null);

		GroupOptions groupOptions = new GroupOptions();
		groupOptions.setLimit(1);

		Mockito.when(query.getPageRequest()).thenReturn(new PageRequest(0, 1));
		Mockito.when(query.getGroupOptions()).thenReturn(groupOptions);

		Object group1Key = new Object();
		Map<String, Object> objectNames = new HashMap<>();
		objectNames.put("group1_name", group1Key);

		Map<Object, GroupResult<Object>> result = ResultHelper.convertGroupQueryResponseToGroupResultMap(query, objectNames,
				response, solrTemplate, Object.class);

		Assert.assertNotNull(result);
		Assert.assertEquals(2, result.size());

		GroupResult<Object> groupResult = result.get("group1_name");
		Assert.assertEquals(result.get(group1Key), groupResult);
		Assert.assertEquals("group1_name", groupResult.getName());
		Assert.assertEquals(1, groupResult.getMatches());
		Assert.assertEquals(null, groupResult.getGroupsCount());

		Page<GroupEntry<Object>> groupEntries = groupResult.getGroupEntries();
		Assert.assertEquals(1, groupEntries.getTotalElements());
		Assert.assertEquals(1, groupEntries.getTotalPages());
		Assert.assertEquals(false, groupEntries.hasNext());

		List<GroupEntry<Object>> groupEntriesContent = groupEntries.getContent();
		Assert.assertNotNull(groupEntriesContent);
		Assert.assertEquals(1, groupEntriesContent.size());

		GroupEntry<Object> groupEntriesContentElement = groupEntriesContent.get(0);
		Assert.assertEquals("group1_1_value", groupEntriesContentElement.getGroupValue());

		Page<Object> group1result = groupEntriesContentElement.getResult();
		Assert.assertEquals(3, group1result.getTotalElements());
		Assert.assertEquals(3, group1result.getTotalPages());
		Assert.assertEquals(true, group1result.hasNext());
	}

	@Test // DATASOLR-160
	public void testConvertSingleFieldStatsInfoToStatsResultMap() {

		Map<String, FieldStatsInfo> fieldStatsInfos = new HashMap<>();

		NamedList<Object> nl = createFieldStatNameList("min", "max", 20D, 10L, 5L, 22.5D, 15.5D, 1D);

		fieldStatsInfos.put("field", new FieldStatsInfo(nl, "field"));

		Map<String, FieldStatsResult> converted = ResultHelper.convertFieldStatsInfoToFieldStatsResultMap(fieldStatsInfos);

		FieldStatsResult fieldStatsResult = converted.get("field");

		Assert.assertEquals("min", fieldStatsResult.getMin());
		Assert.assertEquals("max", fieldStatsResult.getMax());
		Assert.assertEquals(20d, fieldStatsResult.getSum());
		Assert.assertEquals(22.5, fieldStatsResult.getMean());
		Assert.assertEquals(Long.valueOf(10), fieldStatsResult.getCount());
		Assert.assertEquals(Long.valueOf(5), fieldStatsResult.getMissing());
		Assert.assertEquals(Double.valueOf(15.5), fieldStatsResult.getStddev());
		Assert.assertEquals(Double.valueOf(1D), fieldStatsResult.getSumOfSquares());
	}

	@Test // DATASOLR-160
	public void testConvertNullFieldStatsInfoToStatsResultMap() {

		Map<String, FieldStatsResult> converted = ResultHelper.convertFieldStatsInfoToFieldStatsResultMap(null);

		Assert.assertThat(converted, IsNull.notNullValue());
		Assert.assertThat(converted.entrySet(), IsEmptyIterable.emptyIterable());
	}

	@Test // DATASOLR-160
	public void testConvertEmptyFieldStatsInfoMapToStatsResultMap() {

		Map<String, FieldStatsResult> converted = ResultHelper.convertFieldStatsInfoToFieldStatsResultMap(new HashMap<>());

		Assert.assertThat(converted, IsNull.notNullValue());
		Assert.assertThat(converted.entrySet(), IsEmptyIterable.emptyIterable());
	}

	@Test // DATASOLR-160
	public void testConvertFieldStatsInfoMapWithNullToStatsResultMap() {

		Map<String, FieldStatsResult> converted = ResultHelper
				.convertFieldStatsInfoToFieldStatsResultMap(Collections.<String, FieldStatsInfo> singletonMap("field", null));

		Assert.assertThat(converted, IsNull.notNullValue());
		Assert.assertThat(converted.keySet(), IsIterableContainingInOrder.contains("field"));
	}

	@Test // DATASOLR-160
	public void testConvertFieldStatsInfoMapWithEmptyNamedListToStatsResultMap() {

		Map<String, FieldStatsResult> converted = ResultHelper.convertFieldStatsInfoToFieldStatsResultMap(
				Collections.<String, FieldStatsInfo> singletonMap("field", new FieldStatsInfo(new NamedList<>(), "field")));

		Assert.assertThat(converted, IsNull.notNullValue());
		Assert.assertThat(converted.keySet(), IsIterableContainingInOrder.contains("field"));
	}

	@Test // DATASOLR-160
	public void testConvertFieldStatsInfoToStatsResultMap() {

		Map<String, FieldStatsInfo> fieldStatsInfos = new HashMap<>();

		NamedList<Object> values = new NamedList<>();
		NamedList<Object> facets = new NamedList<>();
		NamedList<Object> nl = createFieldStatNameList("min", "max", 20D, 10L, 5L, 22.5D, 15.5D, 1D);
		nl.add("facets", facets);
		facets.add("facetField", values);
		values.add("value1", createFieldStatNameList("f1v1min", "f1v1max", 110D, 111L, 112L, 113D, 11.3D, 1D));
		values.add("value2", createFieldStatNameList("f1v2min", "f1v2max", 120D, 121L, 122L, 123D, 12.3D, 1D));

		fieldStatsInfos.put("field", new FieldStatsInfo(nl, "field"));

		// convert
		Map<String, FieldStatsResult> converted = ResultHelper.convertFieldStatsInfoToFieldStatsResultMap(fieldStatsInfos);

		// field
		FieldStatsResult fieldStatsResult = converted.get("field");
		Assert.assertEquals("min", fieldStatsResult.getMin());
		Assert.assertEquals("max", fieldStatsResult.getMax());
		Assert.assertEquals(20d, fieldStatsResult.getSum());
		Assert.assertEquals(22.5, fieldStatsResult.getMean());
		Assert.assertEquals(Long.valueOf(10), fieldStatsResult.getCount());
		Assert.assertEquals(Long.valueOf(5), fieldStatsResult.getMissing());
		Assert.assertEquals(Double.valueOf(15.5), fieldStatsResult.getStddev());

		// facets
		Map<String, Map<String, StatsResult>> facetStatsResults = fieldStatsResult.getFacetStatsResults();
		Assert.assertEquals(1, facetStatsResults.size());

		// facet field
		Map<String, StatsResult> facetStatsResult = facetStatsResults.get("facetField");
		Assert.assertNotNull(facetStatsResult);

		// facet values
		StatsResult facetValue1StatsResult = facetStatsResult.get("value1");
		Assert.assertEquals("f1v1min", facetValue1StatsResult.getMin());
		Assert.assertEquals("f1v1max", facetValue1StatsResult.getMax());
		Assert.assertEquals(110d, facetValue1StatsResult.getSum());
		Assert.assertEquals(113d, facetValue1StatsResult.getMean());
		Assert.assertEquals(Long.valueOf(111), facetValue1StatsResult.getCount());
		Assert.assertEquals(Long.valueOf(112), facetValue1StatsResult.getMissing());
		Assert.assertEquals(Double.valueOf(11.3), facetValue1StatsResult.getStddev());

		StatsResult facetValue2StatsResult = facetStatsResult.get("value2");
		Assert.assertEquals("f1v2min", facetValue2StatsResult.getMin());
		Assert.assertEquals("f1v2max", facetValue2StatsResult.getMax());
		Assert.assertEquals(120d, facetValue2StatsResult.getSum());
		Assert.assertEquals(123d, facetValue2StatsResult.getMean());
		Assert.assertEquals(Long.valueOf(121), facetValue2StatsResult.getCount());
		Assert.assertEquals(Long.valueOf(122), facetValue2StatsResult.getMissing());
		Assert.assertEquals(Double.valueOf(12.3), facetValue2StatsResult.getStddev());
	}

	@Test // DATSOLR-86
	public void testConvertEmptyFacetRangeQueryResponseToFacetPageMap() {
		SimpleFacetQuery facetQuery = new SimpleFacetQuery(new SimpleStringCriteria("*:*"))
				.setFacetOptions(new FacetOptions("field1"));
		SimpleFacetQuery emptyQuery = new SimpleFacetQuery();

		Assert.assertTrue(ResultHelper.convertFacetQueryResponseToRangeFacetPageMap(facetQuery, null).isEmpty());
		Assert.assertTrue(ResultHelper.convertFacetQueryResponseToRangeFacetPageMap(emptyQuery, response).isEmpty());
	}

	@Test // DATSOLR-86
	public void testConvertFacetRangeQueryResponseToFacetPageMap() {
		SimpleFacetQuery facetQuery = new SimpleFacetQuery(new SimpleStringCriteria("*:*"))
				.setFacetOptions(new FacetOptions("field1"));

		RangeFacet.Numeric rangeFacet1 = new RangeFacet.Numeric("name", 10, 20, 2, 4, 6, 8);
		rangeFacet1.addCount("count1", 1);
		rangeFacet1.addCount("count2", 2);

		RangeFacet.Numeric rangeFacet2 = new RangeFacet.Numeric("", 10, 20, 2, 4, 6, 8);

		@SuppressWarnings("rawtypes")
		List<RangeFacet> value = new ArrayList<>();
		value.add(rangeFacet1);
		value.add(rangeFacet2);
		Mockito.when(response.getFacetRanges()).thenReturn(value);

		Map<Field, Page<FacetFieldEntry>> converted = ResultHelper.convertFacetQueryResponseToRangeFacetPageMap(facetQuery,
				response);

		Page<FacetFieldEntry> page = converted.get(new SimpleField("name"));

		Assert.assertEquals(1, converted.size());
		Assert.assertEquals(2, page.getTotalElements());

		List<FacetFieldEntry> content = page.getContent();
		Assert.assertEquals(2, content.size());
		Assert.assertEquals(1, content.get(0).getValueCount());
		Assert.assertEquals("count1", content.get(0).getValue());
		Assert.assertEquals(2, content.get(1).getValueCount());
		Assert.assertEquals("count2", content.get(1).getValue());
	}

	@Test // DATASOLR-305
	public void testConvertFacetQueryResponseForNegativeFacetLimit() {

		List<FacetField> fieldList = new ArrayList<>(1);
		FacetField ffield = createFacetField("field_1", 1, 2);
		fieldList.add(ffield);

		Mockito.when(response.getFacetFields()).thenReturn(fieldList);

		FacetQuery query = createFacetQuery("field_1");
		query.getFacetOptions().setFacetLimit(-1);

		Map<Field, Page<FacetFieldEntry>> result = ResultHelper.convertFacetQueryResponseToFacetPageMap(query, response);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
	}

	private NamedList<Object> createFieldStatNameList(Object min, Object max, Double sum, Long count, Long missing,
			Object mean, Double stddev, Double sumOfSquares) {
		NamedList<Object> nl = new NamedList<>();
		nl.add("min", min);
		nl.add("max", max);
		nl.add("sum", sum);
		nl.add("count", count);
		nl.add("missing", missing);
		nl.add("mean", mean);
		nl.add("stddev", stddev);
		nl.add("sumOfSquares", sumOfSquares);
		return nl;
	}

	private FacetQuery createFacetQuery(SolrDataQuery... facetQueries) {
		FacetQuery fq = new SimpleFacetQuery(new SimpleStringCriteria("*:*"));
		fq.setFacetOptions(new FacetOptions(facetQueries));
		return fq;
	}

	private FacetQuery createFacetQuery(String... facetFields) {
		FacetQuery fq = new SimpleFacetQuery(new Criteria(facetFields[0]));
		fq.setFacetOptions(new FacetOptions(facetFields));
		return fq;
	}

	private FacetQuery createFacetPivotQuery(String... pivotFieldNames) {
		FacetQuery fq = new SimpleFacetQuery(new Criteria("field_1"));
		fq.setFacetOptions(new FacetOptions().addFacetOnPivot(pivotFieldNames));
		return fq;
	}

	private FacetField createFacetField(String fieldName, long... values) {
		FacetField ffield = new FacetField(fieldName);
		for (int i = 1; i <= values.length; i++) {
			ffield.add("value_" + i, values[i - 1]);
		}
		return ffield;
	}

	private static class SolrBeanWithIdNamedField {

		@SuppressWarnings("unused") //
		private String id;

		public SolrBeanWithIdNamedField(String id) {
			this.id = id;
		}

	}

	private static class SolrBeanWithAnnoteatedIdNamedField {

		private @Id String idField;

		public SolrBeanWithAnnoteatedIdNamedField(String idField) {
			this.idField = idField;
		}

	}

}
