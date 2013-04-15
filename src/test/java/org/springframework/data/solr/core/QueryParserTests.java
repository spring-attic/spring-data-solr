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

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.GroupParams;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.geo.BoundingBox;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.Distance.Unit;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.Query.Operator;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;

/**
 * @author Christoph Strobl
 * @author John Dorman
 * @author Rosty Kerei
 */
public class QueryParserTests {

	private QueryParser queryParser;

	@Before
	public void setUp() {
		this.queryParser = new QueryParser();
	}

	@Test
	public void testIs() {
		Criteria criteria = new Criteria("field_1").is("is");
		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:is", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testMultipleIs() {
		Criteria criteria = new Criteria("field_1").is("is").is("another is");
		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:(is \"another is\")", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testEndsWith() {
		Criteria criteria = new Criteria("field_1").endsWith("end");

		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:*end", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testEndsWithMulitpleValues() {
		Criteria criteria = new Criteria("field_1").endsWith(Arrays.asList("one", "two", "three"));

		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:(*one *two *three)", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testStartsWith() {
		Criteria criteria = new Criteria("field_1").startsWith("start");

		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:start*", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testStartsWithMultipleValues() {
		Criteria criteria = new Criteria("field_1").startsWith(Arrays.asList("one", "two", "three"));

		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:(one* two* three*)", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testContains() {
		Criteria criteria = new Criteria("field_1").contains("contains");

		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:*contains*", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testContainsWithMultipleValues() {
		Criteria criteria = new Criteria("field_1").contains(Arrays.asList("one", "two", "three"));

		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:(*one* *two* *three*)", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testExpression() {
		Criteria criteria = new Criteria("field_1").expression("(have fun using +solr && expressions*)");
		Assert.assertEquals("field_1:(have fun using +solr && expressions*)",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testCriteriaChain() {
		Criteria criteria = new Criteria("field_1").startsWith("start").endsWith("end").contains("contains").is("is");
		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:(start* *end *contains* is)", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testAnd() {
		Criteria criteria = new Criteria("field_1").startsWith("start").endsWith("end").and("field_2").startsWith("2start")
				.endsWith("2end");
		Assert.assertEquals("field_2", criteria.getField().getName());
		Assert.assertEquals("field_1:(start* *end) AND field_2:(2start* *2end)",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testOr() {
		Criteria criteria = new Criteria("field_1").startsWith("start").or("field_2").endsWith("end").startsWith("start2");
		Assert
				.assertEquals("field_1:start* OR field_2:(*end start2*)", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testCriteriaWithWhiteSpace() {
		Criteria criteria = new Criteria("field_1").is("white space");
		Assert.assertEquals("field_1:\"white space\"", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testCriteriaWithDoubleQuotes() {
		Criteria criteria = new Criteria("field_1").is("with \"quote");
		Assert.assertEquals("field_1:\"with \\\"quote\"", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testIsNot() {
		Criteria criteria = new Criteria("field_1").is("value_1").not();
		Assert.assertEquals("-field_1:value_1", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testFuzzy() {
		Criteria criteria = new Criteria("field_1").fuzzy("value_1");
		Assert.assertEquals("field_1:value_1~", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testFuzzyWithDistance() {
		Criteria criteria = new Criteria("field_1").fuzzy("value_1", 0.5f);
		Assert.assertEquals("field_1:value_1~0.5", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBoost() {
		Criteria criteria = new Criteria("field_1").is("value_1").boost(2f);
		Assert.assertEquals("field_1:value_1^2.0", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBoostMultipleValues() {
		Criteria criteria = new Criteria("field_1").is("value_1").is("value_2").boost(2f);
		Assert.assertEquals("field_1:(value_1 value_2)^2.0", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBoostMultipleCriteriasValues() {
		Criteria criteria = new Criteria("field_1").is("value_1").is("value_2").boost(2f).and("field_3").is("value_3");
		Assert.assertEquals("field_1:(value_1 value_2)^2.0 AND field_3:value_3",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetween() {
		Criteria criteria = new Criteria("field_1").between(100, 200);
		Assert.assertEquals("field_1:[100 TO 200]", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetweenExcludeLowerBound() {
		Criteria criteria = new Criteria("field_1").between(100, 200, false, true);
		Assert.assertEquals("field_1:{100 TO 200]", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetweenExcludeUpperBound() {
		Criteria criteria = new Criteria("field_1").between(100, 200, true, false);
		Assert.assertEquals("field_1:[100 TO 200}", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetweenWithoutUpperBound() {
		Criteria criteria = new Criteria("field_1").between(100, null);
		Assert.assertEquals("field_1:[100 TO *]", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetweenWithoutLowerBound() {
		Criteria criteria = new Criteria("field_1").between(null, 200);
		Assert.assertEquals("field_1:[* TO 200]", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetweenWithDateValue() {
		DateTime lowerBound = new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC);
		DateTime upperBound = new DateTime(2012, 8, 21, 19, 30, 0, DateTimeZone.UTC);

		Criteria criteria = new Criteria("field_1").between(lowerBound, upperBound);
		Assert.assertEquals("field_1:[2012\\-08\\-21T06\\:35\\:00.000Z TO 2012\\-08\\-21T19\\:30\\:00.000Z]",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetweenNegativeNumber() {
		Criteria criteria = new Criteria("field_1").between(-200, -100);
		Assert.assertEquals("field_1:[\\-200 TO \\-100]", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testIn() {
		Criteria criteria = new Criteria("field_1").in(1, 2, 3, 5, 8, 13, 21);
		Assert.assertEquals("field_1:(1 2 3 5 8 13 21)", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testIsWithJavaDateValue() {
		DateTime dateTime = new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(dateTime.getMillis());

		Criteria criteria = new Criteria("dateField").is(calendar.getTime());
		Assert.assertEquals("dateField:2012\\-08\\-21T06\\:35\\:00.000Z",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testIsWithJodaDateTime() {
		DateTime dateTime = new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC);

		Criteria criteria = new Criteria("dateField").is(dateTime);
		Assert.assertEquals("dateField:2012\\-08\\-21T06\\:35\\:00.000Z",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testIsWithJodaLocalDateTime() {
		LocalDateTime dateTime = new LocalDateTime(new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC).getMillis(),
				DateTimeZone.UTC);

		Criteria criteria = new Criteria("dateField").is(dateTime);
		Assert.assertEquals("dateField:2012\\-08\\-21T06\\:35\\:00.000Z",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testIsWithNegativeNumner() {
		Criteria criteria = new Criteria("field_1").is(-100);
		Assert.assertEquals("field_1:\\-100", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testNear() {
		Criteria criteria = new Criteria("field_1").near(new GeoLocation(48.303056, 14.290556), new Distance(5));
		Assert.assertEquals("{!bbox pt=48.303056,14.290556 sfield=field_1 d=5.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testNearWithDistanceUnitMiles() {
		Criteria criteria = new Criteria("field_1")
				.near(new GeoLocation(48.303056, 14.290556), new Distance(1, Unit.MILES));
		Assert.assertEquals("{!bbox pt=48.303056,14.290556 sfield=field_1 d=1.609344}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testNearWithDistanceUnitKilometers() {
		Criteria criteria = new Criteria("field_1").near(new GeoLocation(48.303056, 14.290556), new Distance(1,
				Unit.KILOMETERS));
		Assert.assertEquals("{!bbox pt=48.303056,14.290556 sfield=field_1 d=1.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testNearWithCoords() {
		Criteria criteria = new Criteria("field_1").near(new BoundingBox(new GeoLocation(48.303056, 14.290556),
				new GeoLocation(48.303056, 14.290556)));
		Assert.assertEquals("field_1:[48.303056,14.290556 TO 48.303056,14.290556]",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testWithinWithDistanceUnitMiles() {
		Criteria criteria = new Criteria("field_1").within(new GeoLocation(48.303056, 14.290556), new Distance(1,
				Unit.MILES));
		Assert.assertEquals("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.609344}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testWithinWithDistanceUnitKilometers() {
		Criteria criteria = new Criteria("field_1").within(new GeoLocation(48.303056, 14.290556), new Distance(1,
				Unit.KILOMETERS));
		Assert.assertEquals("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testWithinWithNullDistance() {
		Criteria criteria = new Criteria("field_1").within(new GeoLocation(48.303056, 14.290556), null);
		Assert.assertEquals("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=0.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testStringCriteria() {
		Criteria criteria = new SimpleStringCriteria("field_1:value_1 AND field_2:value_2");
		Assert.assertEquals("field_1:value_1 AND field_2:value_2", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testStringCriteriaWithMoreFragments() {
		Criteria criteria = new SimpleStringCriteria("field_1:value_1 AND field_2:value_2");
		criteria = criteria.and("field_3").is("value_3");
		Assert.assertEquals("field_1:value_1 AND field_2:value_2 AND field_3:value_3",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testRegisterAlternateConverter() {
		Criteria criteria = new Criteria("field_1").is(100);
		queryParser.registerConverter(new Converter<Number, String>() {

			@Override
			public String convert(Number arg0) {
				return StringUtils.reverse(arg0.toString());
			}

		});
		Assert.assertEquals("field_1:001", queryParser.createQueryStringFromCriteria(criteria));
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
	public void testConstructSolrQueryWithSingleFacetOnField() {
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
	public void testConstructSolrQueryWithMultipleFacetOnFields() {
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
	public void testConstructSolrQueryWithSingleFacetFilterQuery() {
		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1")).setFacetOptions(new FacetOptions()
				.addFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_2:[* TO 5]"))));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		Assert.assertArrayEquals(new String[] { "field_2:[* TO 5]" }, solrQuery.getFacetQuery());
	}

	@Test
	public void testConstructSolrQueryWithMultipleFacetFilterQuerues() {
		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1")).setFacetOptions(new FacetOptions()
				.addFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_2:[* TO 5]"))).addFacetQuery(
						new SimpleQuery(new Criteria("field_3").startsWith("prefix"))));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		Assert.assertArrayEquals(new String[] { "field_2:[* TO 5]", "field_3:prefix*" }, solrQuery.getFacetQuery());
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

	@Test
	public void testWithNullSort() {
		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(null); // do this explicitly

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNull(solrQuery.getSortField());
		Assert.assertNull(solrQuery.getSortFields());
	}

	@Test
	public void testWithSortAscOnSingleField() {
		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort("field_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("field_2 asc", solrQuery.getSortField());
		Assert.assertEquals(1, solrQuery.getSortFields().length);
	}

	@Test
	public void testWithSortDescOnSingleField() {
		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort(Sort.Direction.DESC, "field_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("field_2 desc", solrQuery.getSortField());
		Assert.assertEquals(1, solrQuery.getSortFields().length);
	}

	@Test
	public void testWithSortAscMultipleFields() {
		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort("field_2, field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("field_2, field_3 asc", solrQuery.getSortField());
		Assert.assertEquals(2, solrQuery.getSortFields().length);
	}

	@Test
	public void testWithSortDescMultipleFields() {
		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort(Sort.Direction.DESC, "field_2, field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("field_2, field_3 desc", solrQuery.getSortField());
		Assert.assertEquals(2, solrQuery.getSortFields().length);
	}

	@Test
	public void testWithSortMixedDirections() {
		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort("field_1"));
		query.addSort(new Sort(Sort.Direction.DESC, "field_2, field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("field_1 asc,field_2, field_3 desc", solrQuery.getSortField());
		Assert.assertEquals(3, solrQuery.getSortFields().length);
	}

	@Test
	public void testWithORDefaultOperator() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefaultOperator(Operator.OR);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("OR", solrQuery.get("q.op"));
	}

	@Test
	public void testWithANDDefaultOperator() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefaultOperator(Operator.AND);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("AND", solrQuery.get("q.op"));
	}

	@Test
	public void testWithNONEDefaultOperator() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefaultOperator(Operator.NONE);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNull(solrQuery.get("q.op"));
	}

	@Test
	public void testWithoutDefaultOperator() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNull(solrQuery.get("q.op"));
	}

	@Test
	public void testWithNullDefaultOperator() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefaultOperator(null);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNull(solrQuery.get("q.op"));
	}

	@Test
	public void testWithTimeAllowed() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setTimeAllowed(100);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals(new Integer(100), solrQuery.getTimeAllowed());
	}

	@Test
	public void testWithoutTimeAllowed() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNull(solrQuery.getTimeAllowed());
	}

	@Test
	public void testWithLuceneDefType() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefType("lucene");
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery.get("defType"));
	}

	@Test
	public void testWithEdismaxDefType() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefType("edismax");
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery.get("defType"));
	}

	@Test
	public void testWithUndefindedDefType() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNull(solrQuery.get("defType"));
	}

	@Test
	public void testWithFooRequestHandler() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setRequestHandler("/foo");
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery.get("qt"));
	}

	@Test
	public void testWithUndefinedRequestHandler() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNull(solrQuery.get("qt"));
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
