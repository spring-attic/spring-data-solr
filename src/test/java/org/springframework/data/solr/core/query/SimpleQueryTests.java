/*
 * Copyright 2012 - 2014 the original author or authors.
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
package org.springframework.data.solr.core.query;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.solr.core.query.Query.Operator;

/**
 * @author Christoph Strobl
 * @author Rosty Kerei
 */
public class SimpleQueryTests {

	@Test(expected = IllegalArgumentException.class)
	public void testAddNullCriteria() {
		new SimpleQuery().addCriteria(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddCriteriaWithEmptyFieldname() {
		new SimpleQuery().addCriteria(new Criteria(new SimpleField("")));
	}

	@Test
	public void testAddCriteria() {
		Criteria criteria1 = new Criteria("field_1");
		Criteria criteria2 = new Criteria("field_2");
		Query query = new SimpleQuery().addCriteria(criteria1).addCriteria(criteria2);

		Assert.assertThat(query.getCriteria().getSiblings(), IsIterableContainingInOrder.contains(criteria1, criteria2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddProjectionNullField() {
		new SimpleQuery().addProjectionOnField((Field) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddProjectionNullFieldName() {
		new SimpleQuery().addProjectionOnField(new SimpleField(StringUtils.EMPTY));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testAddProjection() {
		Query query = new SimpleQuery().addProjectionOnField(new SimpleField("field_1")).addProjectionOnField(
				new SimpleField("field_2"));
		Assert.assertEquals(2, ((List) query.getProjectionOnFields()).size());
	}

	@Test
	public void testSetPageRequest() {
		SimpleQuery query = new SimpleQuery();
		Assert.assertNull(query.getPageRequest());
		Assert.assertNull(query.getOffset());
		Assert.assertNull(query.getRows());

		Pageable alteredPage = new PageRequest(0, 20);

		query.setPageRequest(alteredPage);
		Assert.assertThat(query.getPageRequest(), IsEqual.equalTo(alteredPage));
		Assert.assertNull(query.getSort());
	}

	@Test
	public void testSetPageRequestWithSort() {
		SimpleQuery query = new SimpleQuery();

		Pageable alteredPage = new PageRequest(0, 20, Sort.Direction.DESC, "value_1", "value_2");

		query.setPageRequest(alteredPage);
		Assert.assertThat(query.getPageRequest(), IsEqual.equalTo(alteredPage));
		Assert.assertNotNull(query.getSort());

		int i = 0;
		for (Order order : query.getSort()) {
			Assert.assertEquals(Sort.Direction.DESC, order.getDirection());
			Assert.assertEquals("value_" + (++i), order.getProperty());
		}
	}

	@Test
	public void testCreateQueryWithSortedPageRequest() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("*:*"), new PageRequest(0, 20, Sort.Direction.DESC,
				"value_1", "value_2"));
		Assert.assertNotNull(query.getPageRequest());
		Assert.assertNotNull(query.getSort());

		int i = 0;
		for (Order order : query.getSort()) {
			Assert.assertEquals(Sort.Direction.DESC, order.getDirection());
			Assert.assertEquals("value_" + (++i), order.getProperty());
		}

	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetPageRequestWithNullValue() {
		new SimpleQuery().setPageRequest(null);
	}

	@Test
	public void testAddFacetOptions() {
		FacetOptions facetOptions = new FacetOptions("field_1", "field_2");
		FacetQuery query = new SimpleFacetQuery().setFacetOptions(facetOptions);
		Assert.assertEquals(facetOptions, query.getFacetOptions());
	}

	@Test
	public void testAddFacetOptionsWithNullValue() {
		FacetQuery query = new SimpleFacetQuery().setFacetOptions(null);
		Assert.assertNull(query.getFacetOptions());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFacetOptionsWithoutFacetFields() {
		new SimpleFacetQuery().setFacetOptions(new FacetOptions());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddGroupByNullField() {
		new SimpleQuery().addGroupByField((Field) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddGroupByNullFieldName() {
		new SimpleQuery().addGroupByField(new SimpleField(StringUtils.EMPTY));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testAddGroupBy() {
		Query query = new SimpleQuery().addGroupByField(new SimpleField("field_1")).addGroupByField(
				new SimpleField("field_2"));
		Assert.assertEquals(2, ((List) query.getGroupByFields()).size());
	}

	@Test
	public void testCloneQuery() {
		Query query = new SimpleQuery();
		Assert.assertNotSame(query, SimpleQuery.fromQuery(query));
	}

	@Test
	public void testCloneNullQuery() {
		Assert.assertNull(SimpleQuery.fromQuery(null));
	}

	@Test
	public void testCloneQueryWithCriteria() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		Query destination = SimpleQuery.fromQuery(source);
		Assert.assertNotSame(source, destination);
		Assert.assertEquals("field_1", destination.getCriteria().getField().getName());
	}

	@Test
	public void testCloneQueryWithFilterQuery() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.addFilterQuery(new SimpleQuery(new Criteria("field_2").startsWith("value_2")));

		Query destination = SimpleQuery.fromQuery(source);
		Assert.assertEquals(1, destination.getFilterQueries().size());
	}

	@Test
	public void testCloneQueryWithProjection() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.addProjectionOnField(new SimpleField("field_2"));

		Query destination = SimpleQuery.fromQuery(source);
		Assert.assertEquals(1, destination.getProjectionOnFields().size());
	}

	@Test
	public void testCloneQueryWithGroupBy() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.addGroupByField(new SimpleField("field_2"));

		Query destination = SimpleQuery.fromQuery(source);
		Assert.assertEquals(1, destination.getGroupByFields().size());
	}

	@Test
	public void testCloneQueryWithSort() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.addSort(new Sort(Sort.Direction.DESC, "field_3"));

		Query destination = SimpleQuery.fromQuery(source);
		Assert.assertEquals(source.getSort(), destination.getSort());
	}

	@Test
	public void testCloneWithDefType() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.setDefType("defType");

		Query destination = SimpleQuery.fromQuery(source);
		Assert.assertEquals(source.getDefType(), destination.getDefType());
	}

	@Test
	public void testCloneWithDefaultOperator() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.setDefaultOperator(Operator.OR);

		Query destination = SimpleQuery.fromQuery(source);
		Assert.assertEquals(source.getDefaultOperator(), destination.getDefaultOperator());
	}

	@Test
	public void testCloneWithTimeAllowed() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.setTimeAllowed(Integer.valueOf(10));

		Query destination = SimpleQuery.fromQuery(source);
		Assert.assertEquals(source.getTimeAllowed(), destination.getTimeAllowed());
	}

	@Test
	public void testCloneWithRequestHandler() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.setRequestHandler("requestHandler");

		Query destination = SimpleQuery.fromQuery(source);
		Assert.assertEquals(source.getRequestHandler(), destination.getRequestHandler());
	}

	@Test
	public void testAddSort() {
		Sort sort = new Sort("field_2", "field_3");
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		query.addSort(sort);

		Assert.assertNotNull(query.getSort());
	}

	@Test
	public void testAddNullSort() {
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		query.addSort(null);

		Assert.assertNull(query.getSort());
	}

	@Test
	public void testAddNullToExistingSort() {
		Sort sort = new Sort("field_2", "field_3");
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		query.addSort(sort);
		query.addSort(null);

		Assert.assertNotNull(query.getSort());
		Assert.assertEquals(sort, query.getSort());
	}

	@Test
	public void testAddMultipleSort() {
		Sort sort1 = new Sort("field_2", "field_3");
		Sort sort2 = new Sort("field_1");
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		query.addSort(sort1);
		query.addSort(sort2);

		Assert.assertNotNull(query.getSort());
		Assert.assertNotNull(query.getSort().getOrderFor("field_1"));
		Assert.assertNotNull(query.getSort().getOrderFor("field_2"));
		Assert.assertNotNull(query.getSort().getOrderFor("field_3"));
	}

	@Test
	public void testTimeAllowed() {
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		Assert.assertNull(query.getTimeAllowed());

		query.setTimeAllowed(100);
		Assert.assertEquals(new Integer(100), query.getTimeAllowed());
	}

	@Test
	public void shouldOverridePagableArgsByUsingExplicitSetters() {
		SimpleQuery query = new SimpleQuery("*:*").setPageRequest(new PageRequest(1, 10));
		query.setOffset(2);
		query.setRows(20);

		Assert.assertThat(query.getOffset(), Is.is(2));
		Assert.assertThat(query.getRows(), Is.is(20));
	}
}
