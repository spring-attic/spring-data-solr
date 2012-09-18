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

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.GroupParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;

/**
 * @author Christoph Strobl
 */
public class QueryParserTest {

	private QueryParser queryParser;

	@Before
	public void setUp() {
		this.queryParser = new QueryParser();
	}

	@Test
	public void testConstructSimpleSolrQuery() {
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingNotPresent(solrQuery);
	}

	@Test
	public void testConstructSolrQueryWithPagination() {
		int page = 1;
		int pageSize = 100;
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"))
				.setPageRequest(new PageRequest(page, pageSize));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationPresent(solrQuery, page * pageSize, pageSize);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingNotPresent(solrQuery);
	}

	@Test
	public void testConstructSimpleSolrQueryWithProjection() {
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1")).addProjectionOnField("projection_1")
				.addProjectionOnField(new SimpleField("projection_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionPresent(solrQuery, "projection_1,projection_2");
		assertGroupingNotPresent(solrQuery);
		assertFactingNotPresent(solrQuery);
	}

	@Test
	public void testConstructSolrQueryWithSingleGroupBy() {
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1")).addGroupByField("group_1");
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingPresent(solrQuery, "group_1");
		assertFactingNotPresent(solrQuery);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testConstructSolrQueryWithMultiGroupBy() {
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1")).addGroupByField("group_1").addGroupByField(
				new SimpleField("group_2"));
		queryParser.constructSolrQuery(query);
	}

	@Test
	public void testConstructSolrQueryWithSingleFacet() {
		Query query = new SimpleFacetQuery(new Criteria("field_1").is("value_1")).setFacetOptions(new FacetOptions(
				"facet_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingPresent(solrQuery, "facet_1");
	}

	@Test
	public void testConstructSolrQueryWithMultipleFacet() {
		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1")).setFacetOptions(new FacetOptions(
				"facet_1", "facet_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingPresent(solrQuery, "facet_1", "facet_2");
	}

	@Test
	public void testConstructSolrQueryWithFacetSort() {
		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1")).setFacetOptions(new FacetOptions(
				"facet_1").setFacetSort(FacetOptions.FacetSort.INDEX));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("index", solrQuery.getFacetSortString());

		query.getFacetOptions().setFacetSort(FacetOptions.FacetSort.COUNT);
		solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("count", solrQuery.getFacetSortString());
	}

	@Test
	public void testWithFilterQuery() {
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1")).addFilterQuery(new SimpleFilterQuery(
				new Criteria("filter_field").is("filter_value")));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		String[] filterQueries = solrQuery.getFilterQueries();
		Assert.assertEquals(1, filterQueries.length);
		Assert.assertEquals("filter_field:filter_value", filterQueries[0]);
	}

	@Test
	public void testWithEmptyFilterQuery() {
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1")).addFilterQuery(new SimpleQuery());
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		Assert.assertNull(solrQuery.getFilterQueries());
	}

	@Test
	public void testWithSimpleStringCriteria() {
		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingNotPresent(solrQuery);

		Assert.assertEquals(criteria.getQueryString(), solrQuery.getQuery());

	}

	private void assertFactingPresent(SolrQuery solrQuery, String... expected) {
		Assert.assertArrayEquals(expected, solrQuery.getFacetFields());
	}

	private void assertFactingNotPresent(SolrQuery solrQuery) {
		Assert.assertNull(solrQuery.get(FacetParams.FACET_FIELD));
	}

	private void assertQueryStringPresent(SolrQuery solrQuery) {
		Assert.assertNotNull(solrQuery.get(CommonParams.Q));
	}

	private void assertProjectionNotPresent(SolrQuery solrQuery) {
		Assert.assertNull(solrQuery.getFields());
	}

	private void assertProjectionPresent(SolrQuery solrQuery, String expected) {
		Assert.assertNotNull(solrQuery.get(CommonParams.FL));
		Assert.assertEquals(expected, solrQuery.get(CommonParams.FL));
	}

	private void assertPaginationNotPresent(SolrQuery solrQuery) {
		Assert.assertNull(solrQuery.getStart());
		Assert.assertNull(solrQuery.getRows());
	}

	private void assertPaginationPresent(SolrQuery solrQuery, int start, int rows) {
		Assert.assertEquals(Integer.valueOf(start), solrQuery.getStart());
		Assert.assertEquals(Integer.valueOf(rows), solrQuery.getRows());
	}

	private void assertGroupingNotPresent(SolrQuery solrQuery) {
		Assert.assertNull(solrQuery.get(GroupParams.GROUP));
		Assert.assertNull(solrQuery.get(GroupParams.GROUP_FIELD));
		Assert.assertNull(solrQuery.get(GroupParams.GROUP_MAIN));
	}

	private void assertGroupingPresent(SolrQuery solrQuery, String expected) {
		Assert.assertNotNull(solrQuery.get(GroupParams.GROUP));
		Assert.assertNotNull(solrQuery.get(GroupParams.GROUP_FIELD));
		Assert.assertNotNull(solrQuery.get(GroupParams.GROUP_MAIN));
		Assert.assertEquals(expected, solrQuery.get(GroupParams.GROUP_FIELD));
	}

}
