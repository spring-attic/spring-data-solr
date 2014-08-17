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
package org.springframework.data.solr.core;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.HighlightParams;
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
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetOptions.FacetParameter;
import org.springframework.data.solr.core.query.FacetOptions.FacetSort;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithFacetParameters;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.Join;
import org.springframework.data.solr.core.query.MaxFunction;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.Query.Operator;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleGroupQuery;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.SolrPageRequest;

/**
 * @author Christoph Strobl
 * @author John Dorman
 * @author Rosty Kerei
 * @author Andrey Paramonov
 * @author Philipp Jardas
 * @author Francisco Spaeth
 */
public class DefaultQueryParserTests {

	private DefaultQueryParser queryParser;

	@Before
	public void setUp() {
		this.queryParser = new DefaultQueryParser();
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
		Assert.assertEquals("field_1:(start* *end) AND field_2:(2start* *2end)",
				queryParser.createQueryStringFromNode(criteria));
	}

	@Test
	public void testOr() {
		Criteria criteria = new Criteria("field_1").startsWith("start").or("field_2").endsWith("end").startsWith("start2");
		Assert.assertEquals("field_1:start* OR field_2:(*end start2*)", queryParser.createQueryStringFromNode(criteria));
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
	public void testSloppy() {
		Criteria criteria = new Criteria("field_1").sloppy("value1 value2", 2);
		Assert.assertEquals("field_1:\"value1 value2\"~2", queryParser.createQueryStringFromCriteria(criteria));
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
				queryParser.createQueryStringFromNode(criteria));
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
		Criteria criteria = new Criteria("field_1").near(new Point(48.303056, 14.290556), new Distance(5));
		Assert.assertEquals("{!bbox pt=48.303056,14.290556 sfield=field_1 d=5.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	/**
	 * @see DATASOLR-142
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCircleForNearMustNotBeNull() {
		new Criteria("field_1").near((Circle) null);
	}

	@Test
	public void testNearWithDistanceUnitMiles() {
		Criteria criteria = new Criteria("field_1").near(new Point(48.303056, 14.290556), new Distance(1, Metrics.MILES));
		Assert.assertEquals("{!bbox pt=48.303056,14.290556 sfield=field_1 d=1.609344}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testNearWithDistanceUnitKilometers() {
		Criteria criteria = new Criteria("field_1").near(new Point(48.303056, 14.290556), new Distance(1,
				Metrics.KILOMETERS));
		Assert.assertEquals("{!bbox pt=48.303056,14.290556 sfield=field_1 d=1.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testNearWithCoords() {
		Criteria criteria = new Criteria("field_1").near(new Box(new Point(48.303056, 14.290556), new Point(48.303056,
				14.290556)));
		Assert.assertEquals("field_1:[48.303056,14.290556 TO 48.303056,14.290556]",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testWithinWithDistanceUnitMiles() {
		Criteria criteria = new Criteria("field_1").within(new Point(48.303056, 14.290556), new Distance(1, Metrics.MILES));
		Assert.assertEquals("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.609344}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testWithinWithDistanceUnitKilometers() {
		Criteria criteria = new Criteria("field_1").within(new Point(48.303056, 14.290556), new Distance(1,
				Metrics.KILOMETERS));
		Assert.assertEquals("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	/**
	 * @see DATASOLR-142
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCircleForWithinMustNotBeNull() {
		new Criteria("field_1").within((Circle) null);
	}

	/**
	 * @see DATASOLR-142
	 */
	@Test
	public void testWithinCircleWorksCorrectly() {
		Criteria criteria = new Criteria("field_1").within(new Circle(new Point(48.303056, 14.290556), new Distance(1,
				Metrics.KILOMETERS)));
		Assert.assertEquals("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testWithinWithNullDistance() {
		Criteria criteria = new Criteria("field_1").within(new Point(48.303056, 14.290556), null);
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
				queryParser.createQueryStringFromNode(criteria));
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
	public void testConstructSolrQueryWithSinglePivot() {
		Query query = new SimpleFacetQuery(new Criteria("field_1").is("value_1")).setFacetOptions(new FacetOptions()
				.addFacetOnPivot("field_1", "field_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertPivotFactingPresent(solrQuery, "field_1,field_2");
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
	public void testConstructSolrQueryWithMultiplePivot() {
		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1")).setFacetOptions(new FacetOptions()
				.addFacetOnPivot("field_1", "field_2").addFacetOnPivot("field_2", "field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertPivotFactingPresent(solrQuery, "field_1,field_2", "field_2,field_3");
	}

	@Test
	public void testConstructSolrQueryWithFacetPrefix() {
		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"));
		FacetOptions facetOptions = new FacetOptions(new SimpleField("facet_1"), new SimpleField("facet_2"));
		facetOptions.setFacetPrefix("prefix");
		query.setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingPresent(solrQuery, "facet_1", "facet_2");
		Assert.assertEquals(facetOptions.getFacetPrefix(), solrQuery.getParams("facet.prefix")[0]);
	}

	@Test
	public void testConstructSolrQueryWithFieldFacetParameters() {
		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"));
		FieldWithFacetParameters fieldWithFacetParameters = new FieldWithFacetParameters("facet_2").setPrefix("prefix")
				.setSort(FacetSort.INDEX).setLimit(3).setOffset(2).setMethod("method").setMissing(true);
		FacetOptions facetOptions = new FacetOptions(new SimpleField("facet_1"), fieldWithFacetParameters);
		query.setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingPresent(solrQuery, "facet_1", "facet_2");
		Assert.assertEquals(fieldWithFacetParameters.getPrefix(),
				solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.prefix")[0]);
		Assert.assertEquals(FacetParams.FACET_SORT_INDEX,
				solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.sort")[0]);
		Assert.assertEquals(Integer.toString(fieldWithFacetParameters.getOffset()),
				solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.offset")[0]);
		Assert.assertEquals(Integer.toString(fieldWithFacetParameters.getLimit()),
				solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.limit")[0]);
		Assert.assertEquals(fieldWithFacetParameters.getMethod(),
				solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.method")[0]);
		Assert.assertEquals(fieldWithFacetParameters.getMissing().toString(),
				solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.missing")[0]);
	}

	@Test
	public void testConstructSolrQueryWithCustomFieldFacetParameters() {
		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"));
		FieldWithFacetParameters fieldWithFacetParameters = new FieldWithFacetParameters("facet_2")
				.addFacetParameter(new FacetParameter(FacetParams.FACET_ZEROS, "on"));
		FacetOptions facetOptions = new FacetOptions(new SimpleField("facet_1"), fieldWithFacetParameters);
		query.setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("on", solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.zeros")[0]);
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
		Assert.assertTrue(solrQuery.getSorts().isEmpty());
	}

	@Test
	public void testWithSortAscOnSingleField() {
		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort("field_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("field_2 asc", solrQuery.getSortField());
		Assert.assertEquals(1, solrQuery.getSorts().size());
	}

	@Test
	public void testWithSortDescOnSingleField() {
		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort(Sort.Direction.DESC, "field_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("field_2 desc", solrQuery.getSortField());
		Assert.assertEquals(1, solrQuery.getSorts().size());
	}

	@Test
	public void testWithSortAscMultipleFields() {
		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort("field_2", "field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("field_2 asc,field_3 asc", solrQuery.getSortField());
		Assert.assertEquals(2, solrQuery.getSorts().size());
	}

	@Test
	public void testWithSortDescMultipleFields() {
		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort(Sort.Direction.DESC, "field_2", "field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("field_2 desc,field_3 desc", solrQuery.getSortField());
		Assert.assertEquals(2, solrQuery.getSorts().size());
	}

	@Test
	public void testWithSortMixedDirections() {
		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort("field_1"));
		query.addSort(new Sort(Sort.Direction.DESC, "field_2", "field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("field_1 asc,field_2 desc,field_3 desc", solrQuery.getSortField());
		Assert.assertEquals(3, solrQuery.getSorts().size());
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

	@Test
	public void testWithJoinOperator() {
		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setJoin(Join.from("inner_id").to("outer_id"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals("{!join from=inner_id to=outer_id}field_1:value_1", solrQuery.getQuery());
	}

	@Test
	public void testConstructSolrQueryWithEmptyHighlightOption() {
		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setHighlightOptions(new HighlightOptions());

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertTrue(solrQuery.getHighlight());
		Assert.assertArrayEquals(new String[] { Criteria.WILDCARD }, solrQuery.getHighlightFields());
	}

	@Test
	public void testConstructSolrQueryWithoutHighlightOption() {
		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertFalse(solrQuery.getHighlight());
	}

	@Test
	public void testConstructSolrQueryWithHighlightOptionHavingFields() {
		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.addField("field_2", "field_3");
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertArrayEquals(new String[] { "field_2", "field_3" }, solrQuery.getHighlightFields());
	}

	@Test
	public void testConstructSorlQueryWithHighlightOptionFragsize() {
		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.setFragsize(10);
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals(options.getFragsize().intValue(), solrQuery.getHighlightFragsize());
	}

	@Test
	public void testConstructSorlQueryWithHighlightOptionFormatter() {
		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.setFormatter("formatter");
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals(options.getFormatter(), solrQuery.getParams(HighlightParams.FORMATTER)[0]);
	}

	@Test
	public void testConstructSorlQueryWithHighlightOptionNrSnipplets() {
		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.setNrSnipplets(10);
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals(options.getNrSnipplets().intValue(), solrQuery.getHighlightSnippets());
	}

	@Test
	public void testConstructSorlQueryWithHighlightOptionsAndAnySolrParameter() {
		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.addHighlightParameter(HighlightParams.SIMPLE_PRE, "{pre}");
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertEquals(options.<String> getHighlightParameterValue(HighlightParams.SIMPLE_PRE),
				solrQuery.getHighlightSimplePre());
	}

	@Test
	public void testConstructSorlQueryWithFieldSpecificHighlightOptions() {
		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();

		HighlightOptions.FieldWithHighlightParameters fieldWithHighlightParameters = new HighlightOptions.FieldWithHighlightParameters(
				"field_2");
		fieldWithHighlightParameters.setFormatter("formatter");
		fieldWithHighlightParameters.setFragsize(10);

		options.addField(fieldWithHighlightParameters);
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		Assert.assertArrayEquals(new String[] { "field_2" }, solrQuery.getHighlightFields());
		Assert.assertEquals(fieldWithHighlightParameters.getFormatter(),
				solrQuery.getParams("f.field_2." + HighlightParams.FORMATTER)[0]);
		Assert.assertEquals(fieldWithHighlightParameters.getFragsize().toString(),
				solrQuery.getParams("f.field_2." + HighlightParams.FRAGSIZE)[0]);
	}

	/**
	 * @see DATASOLR-105
	 */
	@Test
	public void testNestedOrPartWithAnd() {

		Criteria criteria = Criteria.where("field_1").is("foo")
				.and(Criteria.where("field_2").is("bar").or("field_3").is("roo"))//
				.or(Criteria.where("field_4").is("spring").and("field_5").is("data"));

		Assert.assertEquals("field_1:foo AND (field_2:bar OR field_3:roo) OR (field_4:spring AND field_5:data)",
				queryParser.createQueryStringFromNode(criteria));
	}

	/**
	 * @see DATASOLR-105
	 */
	@Test
	public void testNestedOrPartWithAndSomeOtherThings() {

		Criteria criteria = Criteria.where("field_1").is("foo").is("bar")
				.and(Criteria.where("field_2").is("bar").is("lala").or("field_3").is("roo"))
				.or(Criteria.where("field_4").is("spring").and("field_5").is("data"));

		Assert.assertEquals(
				"field_1:(foo bar) AND (field_2:(bar lala) OR field_3:roo) OR (field_4:spring AND field_5:data)",
				queryParser.createQueryStringFromNode(criteria));
	}

	/**
	 * @see DATASOLR-105
	 */
	@Test
	public void testMultipleAnd() {
		Criteria criteria = Criteria.where("field_1").is("foo").and("field_2").is("bar").and("field_3").is("roo");

		Assert.assertEquals("field_1:foo AND field_2:bar AND field_3:roo", queryParser.createQueryStringFromNode(criteria));
	}

	/**
	 * @see DATASOLR-105
	 */
	@Test
	public void testMultipleOr() {
		Criteria criteria = Criteria.where("field_1").is("foo").or("field_2").is("bar").or("field_3").is("roo");

		Assert.assertEquals("field_1:foo OR field_2:bar OR field_3:roo", queryParser.createQueryStringFromNode(criteria));
	}

	/**
	 * @see DATASOLR-105
	 */
	@Test
	public void testEmptyCriteriaShouldBeDefaultedToNotNUll() {
		Criteria criteria = Criteria.where("field_1").is("foo").and("field_2").or("field_3");

		Assert.assertEquals("field_1:foo AND field_2:[* TO *] OR field_3:[* TO *]",
				queryParser.createQueryStringFromNode(criteria));
	}

	/**
	 * @see DATASOLR-105
	 */
	@Test
	public void testDeepNesting() {

		Criteria criteria = Criteria.where("field_1").is("foo")
				.and(Criteria.where("field_2").is("bar").and("field_3").is("roo")//
						.and(Criteria.where("field_4").is("spring").and("field_5").is("data").or("field_6").is("solr")));

		Assert.assertEquals(
				"field_1:foo AND (field_2:bar AND field_3:roo AND (field_4:spring AND field_5:data OR field_6:solr))",
				queryParser.createQueryStringFromNode(criteria));
	}

	/**
	 * @see DATASOLR-168
	 */
	@Test
	public void testNotCritieraCarriedOnPorperlyForNullAndNotNull() {

		Criteria criteria = new Criteria("param1").isNotNull().and("param2").isNull();
		Assert.assertEquals("param1:[* TO *] AND -param2:[* TO *]", queryParser.createQueryStringFromNode(criteria));
	}

	/**
	 * @see DATASOLR-112
	 */
	@Test
	public void pageableUsingZeroShouldBeParsedCorrectlyWhenSetUsingPageable() {

		SimpleQuery query = new SimpleQuery("*:*").setPageRequest(new SolrPageRequest(0, 0));
		assertPaginationPresent(queryParser.constructSolrQuery(query), 0, 0);
	}

	/**
	 * @see DATASOLR-112
	 */
	@Test
	public void pageableUsingZeroShouldBeParsedCorrectlyWhenSetUsingExplititMethods() {

		SimpleQuery query = new SimpleQuery("*:*").setOffset(0).setRows(0);
		assertPaginationPresent(queryParser.constructSolrQuery(query), 0, 0);
	}
	
	/**
	 * @see DATASOLR-121
	 */
	@Test
	public void testConstructGroupQueryWithAllPossibleParameters() {
		SimpleGroupQuery query = new SimpleGroupQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.setGroupOffset(1);
		query.setGroupLimit(2);
		query.addGroupByField("field_1");
		query.addGroupByFunction(MaxFunction.max("field_1", "field_2"));
		query.addGroupByQuery(new SimpleQuery("*:*"));
		query.addGroupSort(new Sort(Sort.Direction.DESC, "field_3"));
		query.setGroupTotalCount(true);
		
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		
		assertGroupFormatPresent(solrQuery, true);
		Assert.assertEquals("field_1", solrQuery.get(GroupParams.GROUP_FIELD));
		Assert.assertEquals("max(field_1,field_2)", solrQuery.get(GroupParams.GROUP_FUNC));
		Assert.assertEquals("*:*", solrQuery.get(GroupParams.GROUP_QUERY));
		Assert.assertEquals("field_3 desc", solrQuery.get(GroupParams.GROUP_SORT));
		Assert.assertEquals("1", solrQuery.get(GroupParams.GROUP_OFFSET));
		Assert.assertEquals("2", solrQuery.get(GroupParams.GROUP_LIMIT));
	}

	/**
	 * @see DATASOLR-121
	 */
	@Test
	public void testConstructGroupQueryWithoutPagingParameters() {
		SimpleGroupQuery query = new SimpleGroupQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		
		assertGroupFormatPresent(solrQuery, false);
		Assert.assertNull(solrQuery.get(GroupParams.GROUP_SORT));
		Assert.assertNull(solrQuery.get(GroupParams.GROUP_OFFSET));
		Assert.assertNull(solrQuery.get(GroupParams.GROUP_LIMIT));
	}

	/**
	 * @see DATASOLR-121
	 */
	@Test
	public void testConstructGroupQueryWithMultipleFunctions() {
		SimpleGroupQuery query = new SimpleGroupQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.addGroupByFunction(MaxFunction.max("field_1", "field_2"));
		query.addGroupByFunction(MaxFunction.max("field_3", "field_4"));
		
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		
		assertGroupFormatPresent(solrQuery, false);
		Assert.assertArrayEquals(new String[] {"max(field_1,field_2)","max(field_3,field_4)"}, solrQuery.getParams(GroupParams.GROUP_FUNC));
		Assert.assertNull(solrQuery.getParams(GroupParams.GROUP_QUERY));
		Assert.assertNull(solrQuery.getParams(GroupParams.GROUP_FIELD));
	}

	/**
	 * @see DATASOLR-121
	 */
	@Test
	public void testConstructGroupQueryWithMultipleQueries() {
		SimpleGroupQuery query = new SimpleGroupQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.addGroupByQuery(new SimpleQuery("query1"));
		query.addGroupByQuery(new SimpleQuery("query2"));
		
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		
		assertGroupFormatPresent(solrQuery, false);
		Assert.assertArrayEquals(new String[] {"query1","query2"}, solrQuery.getParams(GroupParams.GROUP_QUERY));
		Assert.assertNull(solrQuery.getParams(GroupParams.GROUP_FUNC));
		Assert.assertNull(solrQuery.getParams(GroupParams.GROUP_FIELD));
	}

	private void assertPivotFactingPresent(SolrQuery solrQuery, String... expected) {
		Assert.assertArrayEquals(expected, solrQuery.getParams(FacetParams.FACET_PIVOT));
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

	private void assertGroupFormatPresent(SolrQuery solrQuery, boolean groupTotalCount) {
		Assert.assertEquals("true", solrQuery.get(GroupParams.GROUP));
		Assert.assertEquals("false", solrQuery.get(GroupParams.GROUP_MAIN));
		Assert.assertEquals("grouped", solrQuery.get(GroupParams.GROUP_FORMAT));
		Assert.assertEquals(String.valueOf(groupTotalCount), solrQuery.get(GroupParams.GROUP_TOTAL_COUNT));
	}

}
