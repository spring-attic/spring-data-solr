/*
 * Copyright 2012 - 2018 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableList;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.client.solrj.response.TermsResponse.Term;
import org.apache.solr.client.solrj.response.json.NestableJsonFacet;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;
import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 * @author Vitezslav Zak
 * @author Joe Linn
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ResultHelperTests {

	@Mock private QueryResponse response;

	@Test
	public void testConvertFacetQueryResponseForNullQueryResponse() {
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPageMap(createFacetQuery("field_1"), null);
		assertThat(result).isNotNull();
		assertThat(result.isEmpty()).isTrue();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertFacetQueryResponseForNullQuery() {
		ResultHelper.convertFacetQueryResponseToFacetPageMap(null, null);
	}

	@Test
	public void testConvertFacetQueryResponseForQueryWithoutFacetOptions() {
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPageMap(new SimpleFacetQuery(new Criteria("field_1")), null);
		assertThat(result).isNotNull();
		assertThat(result.isEmpty()).isTrue();
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithNullFacetFields() {
		Mockito.when(response.getFacetFields()).thenReturn(null);
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPageMap(createFacetQuery("field_1"), response);
		assertThat(result).isNotNull();
		assertThat(result.isEmpty()).isTrue();
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithEmptyFacetFields() {
		Mockito.when(response.getFacetFields()).thenReturn(Collections.<FacetField> emptyList());
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPageMap(createFacetQuery("field_1"), response);
		assertThat(result).isNotNull();
		assertThat(result.isEmpty()).isTrue();
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithSingleFacetFieldWithoutValues() {
		List<FacetField> fieldList = new ArrayList<>(1);
		FacetField ffield = new FacetField("field_1");
		fieldList.add(ffield);

		Mockito.when(response.getFacetFields()).thenReturn(fieldList);

		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPageMap(createFacetQuery("field_1"), response);
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(1);
		Entry<Field, Page<FacetFieldEntry>> resultEntry = result.entrySet().iterator().next();

		assertThat(resultEntry.getKey().getName()).isEqualTo(ffield.getName());
		assertThat(resultEntry.getValue().getContent().isEmpty()).isTrue();
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithSingeFacetField() {
		List<FacetField> fieldList = new ArrayList<>(1);
		FacetField ffield = createFacetField("field_1", 1, 2);
		fieldList.add(ffield);

		Mockito.when(response.getFacetFields()).thenReturn(fieldList);

		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToFacetPageMap(createFacetQuery("field_1"), response);
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(1);
		Entry<Field, Page<FacetFieldEntry>> resultEntry = result.entrySet().iterator().next();

		assertThat(resultEntry.getKey().getName()).isEqualTo(ffield.getName());
		assertThat(resultEntry.getValue().getContent().size()).isEqualTo(2);
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithNullFacetQueries() {
		Mockito.when(response.getFacetQuery()).thenReturn(null);
		List<FacetQueryEntry> result = ResultHelper.convertFacetQueryResponseToFacetQueryResult(
				createFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_1:[* TO 5]"))), response);
		assertThat(result).isNotNull();
		assertThat(result.isEmpty()).isTrue();
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithEmptyFacetQueries() {
		Mockito.when(response.getFacetQuery()).thenReturn(Collections.<String, Integer> emptyMap());
		List<FacetQueryEntry> result = ResultHelper.convertFacetQueryResponseToFacetQueryResult(
				createFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_1:[* TO 5]"))), response);
		assertThat(result).isNotNull();
		assertThat(result.isEmpty()).isTrue();
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
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(2);

		assertThat(result.get(0).getValueCount()).isEqualTo(5);
		assertThat(result.get(0).getValue()).isEqualTo("field_1:[* TO 5]");
		assertThat(result.get(0).getKey()).isEqualTo("field_1:[* TO 5]");
		assertThat(result.get(0).getQuery().getCriteria().toString()).isEqualTo("field_1:[* TO 5]");

		assertThat(result.get(1).getValueCount()).isEqualTo(10);
		assertThat(result.get(1).getValue()).isEqualTo("field_1:[6 TO *]");
		assertThat(result.get(1).getKey()).isEqualTo("field_1:[6 TO *]");
		assertThat(result.get(1).getQuery().getCriteria().toString()).isEqualTo("field_1:[6 TO *]");
	}

	@Test
	public void testParseAndAddHighlightQueryResponseToResultPageWithEmptyHighlighting() {
		Mockito.when(response.getHighlighting()).thenReturn(Collections.<String, Map<String, List<String>>> emptyMap());
		assertThat(ResultHelper.convertAndAddHighlightQueryResponseToResultPage(response,
				new SolrResultPage<>(Collections.singletonList(new Object()))).isEmpty()).isTrue();
	}

	@Test
	public void testParseAndAddHighlightQueryResponseToResultPageWithNullHighlighting() {
		Mockito.when(response.getHighlighting()).thenReturn(null);
		assertThat(ResultHelper.convertAndAddHighlightQueryResponseToResultPage(response,
				new SolrResultPage<>(Collections.singletonList(new Object()))).isEmpty()).isTrue();
	}

	@Test
	public void testParseAndAddHighlightQueryResponseToResultPageWithNullResponse() {
		assertThat(ResultHelper.convertAndAddHighlightQueryResponseToResultPage(null,
				new SolrResultPage<>(Collections.singletonList(new Object()))).isEmpty()).isTrue();
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

		assertThat(result.size()).isEqualTo(1);
		assertThat(result.get(0).getEntity()).isEqualTo(resultBean);
		assertThat(result.get(0).getHighlights().size()).isEqualTo(2);
		for (HighlightEntry<SolrBeanWithIdNamedField> entry : result) {
			assertThat(entry.getEntity()).isEqualTo(resultBean);
			for (Highlight highlight : entry.getHighlights()) {
				assertThat(fieldHighlights.containsKey(highlight.getField().getName())).isTrue();
				assertThat(highlight.getSnipplets()).isEqualTo(fieldHighlights.get(highlight.getField().getName()));
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

		assertThat(result.size()).isEqualTo(2);
		assertThat(result.get(0).getEntity()).isEqualTo(resultBean1);
		assertThat(result.get(1).getEntity()).isEqualTo(resultBean2);
		assertThat(result.get(0).getHighlights().size()).isEqualTo(2);
		assertThat(result.get(1).getHighlights().size()).isEqualTo(1);
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

		assertThat(result.size()).isEqualTo(1);
		assertThat(result.get(0).getEntity()).isEqualTo(resultBean);
		assertThat(result.get(0).getHighlights().size()).isEqualTo(2);
		for (HighlightEntry<SolrBeanWithAnnoteatedIdNamedField> entry : result) {
			assertThat(entry.getEntity()).isEqualTo(resultBean);
			for (Highlight highlight : entry.getHighlights()) {
				assertThat(fieldHighlights.containsKey(highlight.getField().getName())).isTrue();
				assertThat(highlight.getSnipplets()).isEqualTo(fieldHighlights.get(highlight.getField().getName()));
			}
		}
	}

	@Test
	public void testConvertFacetRangeQueryResponseToFacetPageMapForNullQueryResponse() {
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToRangeFacetPageMap(this.createFacetQuery("field_1"), null);
		assertThat(result).isNotNull();
		assertThat(result.isEmpty()).isTrue();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertFacetRangeQueryResponseForNullQuery() {
		ResultHelper.convertFacetQueryResponseToRangeFacetPageMap(null, null);
	}

	@Test
	public void testConvertFacetRangeQueryResponseForQueryWithoutFacetOptions() {
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToRangeFacetPageMap(new SimpleFacetQuery(new SimpleStringCriteria("*:*")), null);
		assertThat(result).isNotNull();
		assertThat(result.isEmpty()).isTrue();
	}

	@Test
	public void testConvertFacetRangeQueryResponseForQueryResultWithNullFacetFields() {
		Mockito.when(response.getFacetFields()).thenReturn(null);
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToRangeFacetPageMap(createFacetQuery("field_1"), response);
		assertThat(result).isNotNull();
		assertThat(result.isEmpty()).isTrue();
	}

	@Test
	public void testConvertFacetRangeQueryResponseForQueryResultWithEmptyFacetFields() {
		Mockito.when(response.getFacetFields()).thenReturn(Collections.<FacetField> emptyList());
		Map<Field, Page<FacetFieldEntry>> result = ResultHelper
				.convertFacetQueryResponseToRangeFacetPageMap(createFacetQuery("field_1"), response);
		assertThat(result).isNotNull();
		assertThat(result.isEmpty()).isTrue();
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
		assertThat(result).isNotNull();
		assertThat(resultPivot.size()).isEqualTo(2);

		assertThat(resultPivot.get(0)).isNotNull();
		assertThat(resultPivot.get(0).getValue()).isEqualTo("value_1");
		assertThat(resultPivot.get(0).getField()).isNotNull();
		assertThat(resultPivot.get(0).getField().getName()).isEqualTo("field_1");
		assertThat(resultPivot.get(0).getValueCount()).isEqualTo(10);
		assertThat(resultPivot.get(0).getPivot()).isNotNull();
		assertThat(resultPivot.get(0).getPivot().size()).isEqualTo(2);

		{
			List<FacetPivotFieldEntry> pivot = resultPivot.get(0).getPivot();
			assertThat(pivot.get(0).getValue()).isEqualTo("value_1_1");
			assertThat(pivot.get(0).getField()).isNotNull();
			assertThat(pivot.get(0).getField().getName()).isEqualTo("field_2");
			assertThat(pivot.get(0).getValueCount()).isEqualTo(7);
			assertThat(Collections.emptyList()).isEqualTo(pivot.get(0).getPivot());
			assertThat(pivot.get(1).getValue()).isEqualTo("value_1_2");
			assertThat(pivot.get(1).getField()).isNotNull();
			assertThat(pivot.get(1).getField().getName()).isEqualTo("field_2");
			assertThat(pivot.get(1).getValueCount()).isEqualTo(3);
			assertThat(Collections.emptyList()).isEqualTo(pivot.get(1).getPivot());
		}

		{
			List<FacetPivotFieldEntry> pivot = resultPivot.get(1).getPivot();
			assertThat(pivot.get(0).getValue()).isEqualTo("value_2_1");
			assertThat(pivot.get(0).getField()).isNotNull();
			assertThat(pivot.get(0).getField().getName()).isEqualTo("field_2");
			assertThat(pivot.get(0).getValueCount()).isEqualTo(2);
			assertThat(Collections.emptyList()).isEqualTo(pivot.get(0).getPivot());
		}

		assertThat(resultPivot.get(0).getPivot().get(0)).isNotNull();

	}

	@Test
	public void testConvertTermsQueryResponseReturnsTermsMapCorrectlyWhenOneFieldReturned() {
		TermsResponse termsResponse = new TermsResponse(new NamedList<>());
		termsResponse.getTermMap().put("field_1", Arrays.asList(new Term("term_1", 10), new Term("term_2", 5)));

		Mockito.when(response.getTermsResponse()).thenReturn(termsResponse);

		Map<String, List<TermsFieldEntry>> result = ResultHelper.convertTermsQueryResponseToTermsMap(response);

		assertThat(result.size()).isEqualTo(1);
		assertThat(result.get("field_1").get(0).getValue()).isEqualTo("term_1");
		assertThat(result.get("field_1").get(0).getValueCount()).isEqualTo(10L);
		assertThat(result.get("field_1").get(0).getField().getName()).isEqualTo("field_1");

		assertThat(result.get("field_1").get(1).getValue()).isEqualTo("term_2");
		assertThat(result.get("field_1").get(1).getValueCount()).isEqualTo(5L);
		assertThat(result.get("field_1").get(1).getField().getName()).isEqualTo("field_1");
	}

	@Test
	public void testConvertTermsQueryResponseReturnsTermsMapCorrectlyWhenMultipleFieldsReturned() {
		TermsResponse termsResponse = new TermsResponse(new NamedList<>());
		termsResponse.getTermMap().put("field_1", Arrays.asList(new Term("term_1", 10), new Term("term_2", 5)));
		termsResponse.getTermMap().put("field_2", Arrays.asList(new Term("term_2", 2), new Term("term_3", 1)));

		Mockito.when(response.getTermsResponse()).thenReturn(termsResponse);

		Map<String, List<TermsFieldEntry>> result = ResultHelper.convertTermsQueryResponseToTermsMap(response);

		assertThat(result.size()).isEqualTo(2);
		assertThat(result.get("field_1").get(0).getField().getName()).isEqualTo("field_1");
		assertThat(result.get("field_2").get(0).getField().getName()).isEqualTo("field_2");
	}

	@Test
	public void testConvertTermsQueryResponseReturnsEmtpyMapWhenResponseIsNull() {
		assertThat(ResultHelper.convertTermsQueryResponseToTermsMap(null))
				.isEqualTo(Collections.<String, List<TermsFieldEntry>> emptyMap());
	}

	@Test
	public void testConvertTermsQueryResponseReturnsEmtpyMapWhenTermsResponseIsNull() {
		Mockito.when(response.getTermsResponse()).thenReturn(null);

		assertThat(ResultHelper.convertTermsQueryResponseToTermsMap(response))
				.isEqualTo(Collections.<String, List<TermsFieldEntry>> emptyMap());
	}

	@Test
	public void testConvertTermsQueryResponseReturnsEmtpyMapWhenTermsMapIsEmpty() {
		TermsResponse termsResponse = new TermsResponse(new NamedList<>());
		Mockito.when(response.getTermsResponse()).thenReturn(termsResponse);

		assertThat(ResultHelper.convertTermsQueryResponseToTermsMap(response))
				.isEqualTo(Collections.<String, List<TermsFieldEntry>> emptyMap());
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

		Mockito.when(query.getPageRequest()).thenReturn(PageRequest.of(0, 1));
		Mockito.when(query.getGroupOptions()).thenReturn(groupOptions);

		Object group1Key = new Object();
		Map<String, Object> objectNames = new HashMap<>();
		objectNames.put("group1_name", group1Key);

		Map<Object, GroupResult<Object>> result = ResultHelper.convertGroupQueryResponseToGroupResultMap(query, objectNames,
				response, solrTemplate, Object.class);
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(2);

		GroupResult<Object> groupResult = result.get("group1_name");
		assertThat(result.get(group1Key)).isEqualTo(groupResult);
		assertThat(groupResult.getName()).isEqualTo("group1_name");
		assertThat(groupResult.getMatches()).isEqualTo(1);
		assertThat(groupResult.getGroupsCount()).isEqualTo(Integer.valueOf(2));

		Page<GroupEntry<Object>> groupEntries = groupResult.getGroupEntries();
		assertThat(groupEntries.getTotalElements()).isEqualTo(2);
		assertThat(groupEntries.getTotalPages()).isEqualTo(2);
		assertThat(groupEntries.hasNext()).isEqualTo(true);

		List<GroupEntry<Object>> groupEntriesContent = groupEntries.getContent();
		assertThat(groupEntriesContent).isNotNull();
		assertThat(groupEntriesContent.size()).isEqualTo(1);

		GroupEntry<Object> groupEntriesContentElement = groupEntriesContent.get(0);
		assertThat(groupEntriesContentElement.getGroupValue()).isEqualTo("group1_1_value");

		Page<Object> group1result = groupEntriesContentElement.getResult();
		assertThat(group1result.getTotalElements()).isEqualTo(3);
		assertThat(group1result.getTotalPages()).isEqualTo(3);
		assertThat(group1result.hasNext()).isEqualTo(true);
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

		Mockito.when(query.getPageRequest()).thenReturn(PageRequest.of(0, 1));
		Mockito.when(query.getGroupOptions()).thenReturn(groupOptions);

		Object group1Key = new Object();
		Map<String, Object> objectNames = new HashMap<>();
		objectNames.put("group1_name", group1Key);

		Map<Object, GroupResult<Object>> result = ResultHelper.convertGroupQueryResponseToGroupResultMap(query, objectNames,
				response, solrTemplate, Object.class);

		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(2);

		GroupResult<Object> groupResult = result.get("group1_name");
		assertThat(groupResult).isEqualTo(result.get(group1Key));
		assertThat(groupResult.getName()).isEqualTo("group1_name");
		assertThat(groupResult.getMatches()).isEqualTo(1);
		assertThat(groupResult.getGroupsCount()).isEqualTo(null);

		Page<GroupEntry<Object>> groupEntries = groupResult.getGroupEntries();
		assertThat(groupEntries.getTotalElements()).isEqualTo(1);
		assertThat(groupEntries.getTotalPages()).isEqualTo(1);
		assertThat(groupEntries.hasNext()).isEqualTo(false);

		List<GroupEntry<Object>> groupEntriesContent = groupEntries.getContent();
		assertThat(groupEntriesContent).isNotNull();
		assertThat(groupEntriesContent.size()).isEqualTo(1);

		GroupEntry<Object> groupEntriesContentElement = groupEntriesContent.get(0);
		assertThat(groupEntriesContentElement.getGroupValue()).isEqualTo("group1_1_value");

		Page<Object> group1result = groupEntriesContentElement.getResult();
		assertThat(group1result.getTotalElements()).isEqualTo(3);
		assertThat(group1result.getTotalPages()).isEqualTo(3);
		assertThat(group1result.hasNext()).isEqualTo(true);
	}

	@Test // DATASOLR-160
	public void testConvertSingleFieldStatsInfoToStatsResultMap() {

		Map<String, FieldStatsInfo> fieldStatsInfos = new HashMap<>();

		NamedList<Object> nl = createFieldStatNameList("min", "max", 20D, 10L, 5L, 22.5D, 15.5D, 1D);

		fieldStatsInfos.put("field", new FieldStatsInfo(nl, "field"));

		Map<String, FieldStatsResult> converted = ResultHelper.convertFieldStatsInfoToFieldStatsResultMap(fieldStatsInfos);

		FieldStatsResult fieldStatsResult = converted.get("field");

		assertThat(fieldStatsResult.getMin()).isEqualTo("min");
		assertThat(fieldStatsResult.getMax()).isEqualTo("max");
		assertThat(fieldStatsResult.getSum()).isEqualTo(20d);
		assertThat(fieldStatsResult.getMean()).isEqualTo(22.5);
		assertThat(fieldStatsResult.getCount()).isEqualTo(Long.valueOf(10));
		assertThat(fieldStatsResult.getMissing()).isEqualTo(Long.valueOf(5));
		assertThat(fieldStatsResult.getStddev()).isEqualTo(Double.valueOf(15.5));
		assertThat(fieldStatsResult.getSumOfSquares()).isEqualTo(Double.valueOf(1D));
	}

	@Test // DATASOLR-160
	public void testConvertNullFieldStatsInfoToStatsResultMap() {

		Map<String, FieldStatsResult> converted = ResultHelper.convertFieldStatsInfoToFieldStatsResultMap(null);

		assertThat(converted).isNotNull();
		assertThat(converted.entrySet()).isEmpty();
	}

	@Test // DATASOLR-160
	public void testConvertEmptyFieldStatsInfoMapToStatsResultMap() {

		Map<String, FieldStatsResult> converted = ResultHelper.convertFieldStatsInfoToFieldStatsResultMap(new HashMap<>());

		assertThat(converted).isNotNull();
		assertThat(converted.entrySet()).isEmpty();
	}

	@Test // DATASOLR-160
	public void testConvertFieldStatsInfoMapWithNullToStatsResultMap() {

		Map<String, FieldStatsResult> converted = ResultHelper
				.convertFieldStatsInfoToFieldStatsResultMap(Collections.<String, FieldStatsInfo> singletonMap("field", null));

		assertThat(converted).isNotNull();
		assertThat(converted.keySet()).containsExactly("field");
	}

	@Test // DATASOLR-160
	public void testConvertFieldStatsInfoMapWithEmptyNamedListToStatsResultMap() {

		Map<String, FieldStatsResult> converted = ResultHelper.convertFieldStatsInfoToFieldStatsResultMap(
				Collections.<String, FieldStatsInfo> singletonMap("field", new FieldStatsInfo(new NamedList<>(), "field")));

		assertThat(converted).isNotNull();
		assertThat(converted.keySet()).containsExactly("field");
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
		assertThat(fieldStatsResult.getMin()).isEqualTo("min");
		assertThat(fieldStatsResult.getMax()).isEqualTo("max");
		assertThat(fieldStatsResult.getSum()).isEqualTo(20d);
		assertThat(fieldStatsResult.getMean()).isEqualTo(22.5);
		assertThat(fieldStatsResult.getCount()).isEqualTo(Long.valueOf(10));
		assertThat(fieldStatsResult.getMissing()).isEqualTo(Long.valueOf(5));
		assertThat(fieldStatsResult.getStddev()).isEqualTo(Double.valueOf(15.5));

		// facets
		Map<String, Map<String, StatsResult>> facetStatsResults = fieldStatsResult.getFacetStatsResults();
		assertThat(facetStatsResults.size()).isEqualTo(1);

		// facet field
		Map<String, StatsResult> facetStatsResult = facetStatsResults.get("facetField");
		assertThat(facetStatsResult).isNotNull();

		// facet values
		StatsResult facetValue1StatsResult = facetStatsResult.get("value1");
		assertThat(facetValue1StatsResult.getMin()).isEqualTo("f1v1min");
		assertThat(facetValue1StatsResult.getMax()).isEqualTo("f1v1max");
		assertThat(facetValue1StatsResult.getSum()).isEqualTo(110d);
		assertThat(facetValue1StatsResult.getMean()).isEqualTo(113d);
		assertThat(facetValue1StatsResult.getCount()).isEqualTo(Long.valueOf(111));
		assertThat(facetValue1StatsResult.getMissing()).isEqualTo(Long.valueOf(112));
		assertThat(facetValue1StatsResult.getStddev()).isEqualTo(Double.valueOf(11.3));

		StatsResult facetValue2StatsResult = facetStatsResult.get("value2");
		assertThat(facetValue2StatsResult.getMin()).isEqualTo("f1v2min");
		assertThat(facetValue2StatsResult.getMax()).isEqualTo("f1v2max");
		assertThat(facetValue2StatsResult.getSum()).isEqualTo(120d);
		assertThat(facetValue2StatsResult.getMean()).isEqualTo(123d);
		assertThat(facetValue2StatsResult.getCount()).isEqualTo(Long.valueOf(121));
		assertThat(facetValue2StatsResult.getMissing()).isEqualTo(Long.valueOf(122));
		assertThat(facetValue2StatsResult.getStddev()).isEqualTo(Double.valueOf(12.3));
	}

	@Test // DATSOLR-86
	public void testConvertEmptyFacetRangeQueryResponseToFacetPageMap() {
		SimpleFacetQuery facetQuery = new SimpleFacetQuery(new SimpleStringCriteria("*:*"))
				.setFacetOptions(new FacetOptions("field1"));
		SimpleFacetQuery emptyQuery = new SimpleFacetQuery();

		assertThat(ResultHelper.convertFacetQueryResponseToRangeFacetPageMap(facetQuery, null).isEmpty()).isTrue();
		assertThat(ResultHelper.convertFacetQueryResponseToRangeFacetPageMap(emptyQuery, response).isEmpty()).isTrue();
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

		assertThat(converted.size()).isEqualTo(1);
		assertThat(page.getTotalElements()).isEqualTo(2);

		List<FacetFieldEntry> content = page.getContent();
		assertThat(content.size()).isEqualTo(2);
		assertThat(content.get(0).getValueCount()).isEqualTo(1);
		assertThat(content.get(0).getValue()).isEqualTo("count1");
		assertThat(content.get(1).getValueCount()).isEqualTo(2);
		assertThat(content.get(1).getValue()).isEqualTo("count2");
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
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(1);
	}

	@Test // /DATASOLR-506
	public void testconvertFacetQueryResponseToRangeFacetPageMapForNegativeFacetLimit() {
		RangeFacet.Numeric rangeFacet = new RangeFacet.Numeric("name", 10, 20, 2, 4, 6, 8);
		rangeFacet.addCount("count1", 1);
		rangeFacet.addCount("count2", 2);

		List<RangeFacet> rangeFacetList = new ArrayList<>(1);
		rangeFacetList.add(rangeFacet);

		Mockito.when(response.getFacetRanges()).thenReturn(rangeFacetList);

		FacetQuery query = createFacetQuery("field_1");
		query.getFacetOptions().setFacetLimit(-1);

		Map<Field, Page<FacetFieldEntry>> result = ResultHelper.convertFacetQueryResponseToRangeFacetPageMap(query,
				response);
		assertThat(result).isNotNull();
		assertThat(result.size()).isEqualTo(1);
	}

	@Test // DATASOLR-564
	public void testConvertJsonTermsFacetResult() {
		NamedList<Object> terms = new NamedList<>();
		terms.add("buckets", ImmutableList.of(createTermsFacetBucket("foo", 5), createTermsFacetBucket("bar", 20)));
		NamedList<Object> parent = new NamedList<>();
		parent.add("count", 25);
		final String facetName = "termsFacet";
		parent.add(facetName, terms);
		NestableJsonFacet facet = new NestableJsonFacet(parent);

		Mockito.when(response.getJsonFacetingResponse()).thenReturn(facet);

		Map<String, JsonFacetResult> result = ResultHelper
				.convertJsonFacetQueryResponseToFacetResultMap(createFacetQuery("foo"), response);
		assertThat(result).isNotNull().hasSize(1).containsKey(facetName);
		MultiBucketJsonFacetResult termsResult = (MultiBucketJsonFacetResult) result.get(facetName);
		assertThat(termsResult).isNotNull();
		assertThat(termsResult.getCount()).isEqualTo(25);
		List<BucketFacetEntry> buckets = termsResult.getBuckets();
		assertThat(buckets).hasSize(2);
		BucketFacetEntry bucket1 = buckets.get(0);
		assertThat(bucket1.getKey()).isEqualTo("foo");
		assertThat(bucket1.getValueCount()).isEqualTo(5);
		BucketFacetEntry bucket2 = buckets.get(1);
		assertThat(bucket2.getKey()).isEqualTo("bar");
		assertThat(bucket2.getValueCount()).isEqualTo(20);
	}

	@Test // DATASOLR-564
	public void testConvertJsonStatsFacetResult() {
		NamedList<Object> facetsList = new NamedList<>();
		facetsList.add("count", 5);
		facetsList.add("sum", 42d);
		facetsList.add("unique", 3);

		NamedList<Object> relatedness = new SimpleOrderedMap<>();
		relatedness.add("relatedness", 4.2d);
		relatedness.add("foreground_popularity", 1.5d);
		relatedness.add("background_popularity", 1.1d);
		facetsList.add("relatedness", relatedness);

		NestableJsonFacet facet = new NestableJsonFacet(facetsList);
		Mockito.when(response.getJsonFacetingResponse()).thenReturn(facet);

		Map<String, JsonFacetResult> result = ResultHelper
				.convertJsonFacetQueryResponseToFacetResultMap(createFacetQuery("bar"), response);
		assertThat(result).isNotNull().hasSize(3).containsKeys("sum", "unique", "relatedness");
		SingleStatJsonFacetResult sumResult = (SingleStatJsonFacetResult) result.get("sum");
		assertThat(sumResult).isNotNull();
		assertThat(sumResult.getValue().doubleValue()).isEqualTo(42d);
		SingleStatJsonFacetResult uniqueResult = (SingleStatJsonFacetResult) result.get("unique");
		assertThat(uniqueResult.getValue().intValue()).isEqualTo(3);
		assertThat(uniqueResult.getCount()).isEqualTo(5);
		SingleBucketJsonFacetResult relatednessResult = (SingleBucketJsonFacetResult) result.get("relatedness");
		assertThat(relatednessResult.getFacets()).hasSize(3).containsKeys("relatedness", "foreground_popularity",
				"background_popularity");
		SingleStatJsonFacetResult relatednessStat = (SingleStatJsonFacetResult) relatednessResult.getFacets()
				.get("relatedness");
		assertThat(relatednessStat.getValue().doubleValue()).isEqualTo(4.2d);
	}

	private SimpleOrderedMap<Object> createTermsFacetBucket(String value, int count) {
		SimpleOrderedMap<Object> bucket = new SimpleOrderedMap<>();
		bucket.add("val", value);
		bucket.add("count", count);
		return bucket;
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
