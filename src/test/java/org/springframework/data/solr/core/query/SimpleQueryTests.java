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
package org.springframework.data.solr.core.query;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
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

		assertThat(query.getCriteria().getSiblings()).containsExactly(criteria1, criteria2);
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
		Query query = new SimpleQuery().addProjectionOnField(new SimpleField("field_1"))
				.addProjectionOnField(new SimpleField("field_2"));
		assertThat(((List) query.getProjectionOnFields()).size()).isEqualTo(2);
	}

	@Test
	public void testSetPageRequest() {
		SimpleQuery query = new SimpleQuery();
		assertThat(query.getPageRequest().isUnpaged()).isTrue();
		assertThat(query.getOffset()).isNull();
		assertThat(query.getRows()).isNull();

		Pageable alteredPage = PageRequest.of(0, 20);

		query.setPageRequest(alteredPage);
		assertThat(query.getPageRequest()).isEqualTo(alteredPage);
		assertThat(query.getSort()).isEqualTo(Sort.unsorted());
	}

	@Test
	public void testSetPageRequestWithSort() {
		SimpleQuery query = new SimpleQuery();

		Pageable alteredPage = PageRequest.of(0, 20, Sort.Direction.DESC, "value_1", "value_2");

		query.setPageRequest(alteredPage);
		assertThat(query.getPageRequest()).isEqualTo(alteredPage);
		assertThat(query.getSort()).isNotNull();

		int i = 0;
		for (Order order : query.getSort()) {
			assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
			assertThat(order.getProperty()).isEqualTo("value_" + (++i));
		}
	}

	@Test
	public void testCreateQueryWithSortedPageRequest() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("*:*"),
				PageRequest.of(0, 20, Sort.Direction.DESC, "value_1", "value_2"));
		assertThat(query.getPageRequest()).isNotNull();
		assertThat(query.getSort()).isNotNull();

		int i = 0;
		for (Order order : query.getSort()) {
			assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
			assertThat(order.getProperty()).isEqualTo("value_" + (++i));
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
		assertThat(query.getFacetOptions()).isEqualTo(facetOptions);
	}

	@Test
	public void testAddFacetOptionsWithNullValue() {
		FacetQuery query = new SimpleFacetQuery().setFacetOptions(null);
		assertThat(query.getFacetOptions()).isNull();
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
		Query query = new SimpleQuery().addGroupByField(new SimpleField("field_1"))
				.addGroupByField(new SimpleField("field_2"));
		assertThat(((List) query.getGroupByFields()).size()).isEqualTo(2);
	}

	@Test
	public void testCloneQuery() {
		Query query = new SimpleQuery();
		assertThat(SimpleQuery.fromQuery(query)).isNotSameAs(query);
	}

	@Test
	public void testCloneNullQuery() {
		assertThat(SimpleQuery.fromQuery(null)).isNull();
	}

	@Test
	public void testCloneQueryWithCriteria() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		Query destination = SimpleQuery.fromQuery(source);
		assertThat(destination).isNotSameAs(source);
		assertThat(destination.getCriteria().getField().getName()).isEqualTo("field_1");
	}

	@Test
	public void testCloneQueryWithFilterQuery() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.addFilterQuery(new SimpleQuery(new Criteria("field_2").startsWith("value_2")));

		Query destination = SimpleQuery.fromQuery(source);
		assertThat(destination.getFilterQueries().size()).isEqualTo(1);
	}

	@Test
	public void testCloneQueryWithProjection() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.addProjectionOnField(new SimpleField("field_2"));

		Query destination = SimpleQuery.fromQuery(source);
		assertThat(destination.getProjectionOnFields().size()).isEqualTo(1);
	}

	@Test
	public void testCloneQueryWithGroupBy() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.addGroupByField(new SimpleField("field_2"));

		Query destination = SimpleQuery.fromQuery(source);
		assertThat(destination.getGroupByFields().size()).isEqualTo(1);
	}

	@Test
	public void testCloneQueryWithSort() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.addSort(Sort.by(Sort.Direction.DESC, "field_3"));

		Query destination = SimpleQuery.fromQuery(source);
		assertThat(destination.getSort()).isEqualTo(source.getSort());
	}

	@Test
	public void testCloneWithDefType() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.setDefType("defType");

		Query destination = SimpleQuery.fromQuery(source);
		assertThat(destination.getDefType()).isEqualTo(source.getDefType());
	}

	@Test
	public void testCloneWithDefaultOperator() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.setDefaultOperator(Operator.OR);

		Query destination = SimpleQuery.fromQuery(source);
		assertThat(destination.getDefaultOperator()).isEqualTo(source.getDefaultOperator());
	}

	@Test
	public void testCloneWithTimeAllowed() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.setTimeAllowed(10);

		Query destination = SimpleQuery.fromQuery(source);
		assertThat(destination.getTimeAllowed()).isEqualTo(source.getTimeAllowed());
	}

	@Test
	public void testCloneWithRequestHandler() {
		Query source = new SimpleQuery(new Criteria("field_1").is("value_1"));
		source.setRequestHandler("requestHandler");

		Query destination = SimpleQuery.fromQuery(source);
		assertThat(destination.getRequestHandler()).isEqualTo(source.getRequestHandler());
	}

	@Test
	public void testAddSort() {
		Sort sort = Sort.by("field_2", "field_3");
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		query.addSort(sort);

		assertThat(query.getSort()).isNotNull();
	}

	@Test
	public void testAddNullSort() {
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		query.addSort(null);

		assertThat(Sort.unsorted()).isEqualTo(query.getSort());
	}

	@Test
	public void testAddNullToExistingSort() {
		Sort sort = Sort.by("field_2", "field_3");
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		query.addSort(sort);
		query.addSort(null);

		assertThat(query.getSort()).isNotNull();
		assertThat(query.getSort()).isEqualTo(sort);
	}

	@Test
	public void testAddMultipleSort() {
		Sort sort1 = Sort.by("field_2", "field_3");
		Sort sort2 = Sort.by("field_1");
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		query.addSort(sort1);
		query.addSort(sort2);

		assertThat(query.getSort()).isNotNull();
		assertThat(query.getSort().getOrderFor("field_1")).isNotNull();
		assertThat(query.getSort().getOrderFor("field_2")).isNotNull();
		assertThat(query.getSort().getOrderFor("field_3")).isNotNull();
	}

	@Test
	public void testTimeAllowed() {
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		assertThat(query.getTimeAllowed()).isNull();

		query.setTimeAllowed(100);
		assertThat(query.getTimeAllowed()).isEqualTo(new Integer(100));
	}

	@Test
	public void shouldOverridePagableArgsByUsingExplicitSetters() {
		SimpleQuery query = new SimpleQuery("*:*").setPageRequest(PageRequest.of(1, 10));
		query.setOffset(2L);
		query.setRows(20);

		assertThat(query.getOffset()).isEqualTo(2L);
		assertThat(query.getRows()).isEqualTo(20);
	}
}
