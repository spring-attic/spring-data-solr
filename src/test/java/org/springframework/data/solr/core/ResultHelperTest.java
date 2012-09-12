/*
 * Copyright 2012 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.result.FacetEntry;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class ResultHelperTest {

	@Mock
	private QueryResponse response;

	@Test
	public void testConvertFacetQueryResponseForNullQueryResponse() {
		Map<Field, Page<FacetEntry>> result = ResultHelper.convertFacetQueryResponseToFacetPageMap(
				createFacetQuery("field_1"), null);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertFacetQueryResponseForNullQuery() {
		ResultHelper.convertFacetQueryResponseToFacetPageMap(null, null);
	}

	@Test
	public void testConvertFacetQueryResponseForQueryWithoutFacetOptions() {
		Map<Field, Page<FacetEntry>> result = ResultHelper.convertFacetQueryResponseToFacetPageMap(new SimpleFacetQuery(
				new Criteria("field_1")), null);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithNullFacetFields() {
		Mockito.when(response.getFacetFields()).thenReturn(null);
		Map<Field, Page<FacetEntry>> result = ResultHelper.convertFacetQueryResponseToFacetPageMap(
				createFacetQuery("field_1"), response);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithEmptyFacetFields() {
		Mockito.when(response.getFacetFields()).thenReturn(Collections.<FacetField> emptyList());
		Map<Field, Page<FacetEntry>> result = ResultHelper.convertFacetQueryResponseToFacetPageMap(
				createFacetQuery("field_1"), response);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isEmpty());
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithSingleFacetFieldWithoutValues() {
		List<FacetField> fieldList = new ArrayList<FacetField>(1);
		FacetField ffield = new FacetField("field_1");
		fieldList.add(ffield);

		Mockito.when(response.getFacetFields()).thenReturn(fieldList);

		Map<Field, Page<FacetEntry>> result = ResultHelper.convertFacetQueryResponseToFacetPageMap(
				createFacetQuery("field_1"), response);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Entry<Field, Page<FacetEntry>> resultEntry = result.entrySet().iterator().next();

		Assert.assertEquals(ffield.getName(), resultEntry.getKey().getName());
		Assert.assertTrue(resultEntry.getValue().getContent().isEmpty());
	}

	@Test
	public void testConvertFacetQueryResponseForQueryResultWithSingeFacetField() {
		List<FacetField> fieldList = new ArrayList<FacetField>(1);
		FacetField ffield = createFacetField("field_1", 1, 2);
		fieldList.add(ffield);

		Mockito.when(response.getFacetFields()).thenReturn(fieldList);

		Map<Field, Page<FacetEntry>> result = ResultHelper.convertFacetQueryResponseToFacetPageMap(
				createFacetQuery("field_1"), response);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.size());
		Entry<Field, Page<FacetEntry>> resultEntry = result.entrySet().iterator().next();

		Assert.assertEquals(ffield.getName(), resultEntry.getKey().getName());
		Assert.assertEquals(2, resultEntry.getValue().getContent().size());
	}

	private FacetQuery createFacetQuery(String... facetFields) {
		FacetQuery fq = new SimpleFacetQuery(new Criteria(facetFields[0]));
		fq.setFacetOptions(new FacetOptions(facetFields));
		return fq;
	}

	private FacetField createFacetField(String fieldName, long... values) {
		FacetField ffield = new FacetField(fieldName);
		for (int i = 1; i <= values.length; i++) {
			ffield.add("value_" + i, values[i - 1]);
		}
		return ffield;
	}

}
