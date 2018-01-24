/*
 * Copyright 2012-2017 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.FacetParams.FacetRangeInclude;
import org.apache.solr.common.params.FacetParams.FacetRangeOther;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.SpellingParams;
import org.apache.solr.common.params.StatsParams;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.FacetOptions.FacetParameter;
import org.springframework.data.solr.core.query.FacetOptions.FacetSort;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithDateRangeParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithFacetParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithNumericRangeParameters;
import org.springframework.data.solr.core.query.Query.Operator;

/**
 * @author Christoph Strobl
 * @author John Dorman
 * @author Rosty Kerei
 * @author Andrey Paramonov
 * @author Philipp Jardas
 * @author Francisco Spaeth
 * @author Petar Tahchiev
 * @author Michael Rocke
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
		assertEquals("field_1", criteria.getField().getName());
		assertEquals("field_1:is", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testMultipleIs() {

		Criteria criteria = new Criteria("field_1").is("is").is("another is");
		assertEquals("field_1", criteria.getField().getName());
		assertEquals("field_1:(is \"another is\")", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testEndsWith() {

		Criteria criteria = new Criteria("field_1").endsWith("end");

		assertEquals("field_1", criteria.getField().getName());
		assertEquals("field_1:*end", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testEndsWithMulitpleValues() {

		Criteria criteria = new Criteria("field_1").endsWith(Arrays.asList("one", "two", "three"));

		assertEquals("field_1", criteria.getField().getName());
		assertEquals("field_1:(*one *two *three)", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testStartsWith() {

		Criteria criteria = new Criteria("field_1").startsWith("start");

		assertEquals("field_1", criteria.getField().getName());
		assertEquals("field_1:start*", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testStartsWithMultipleValues() {

		Criteria criteria = new Criteria("field_1").startsWith(Arrays.asList("one", "two", "three"));

		assertEquals("field_1", criteria.getField().getName());
		assertEquals("field_1:(one* two* three*)", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testContains() {

		Criteria criteria = new Criteria("field_1").contains("contains");

		assertEquals("field_1", criteria.getField().getName());
		assertEquals("field_1:*contains*", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testContainsWithMultipleValues() {

		Criteria criteria = new Criteria("field_1").contains(Arrays.asList("one", "two", "three"));

		assertEquals("field_1", criteria.getField().getName());
		assertEquals("field_1:(*one* *two* *three*)", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testExpression() {

		Criteria criteria = new Criteria("field_1").expression("(have fun using +solr && expressions*)");
		assertEquals("field_1:(have fun using +solr && expressions*)", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testCriteriaChain() {

		Criteria criteria = new Criteria("field_1").startsWith("start").endsWith("end").contains("contains").is("is");
		assertEquals("field_1", criteria.getField().getName());
		assertEquals("field_1:(start* *end *contains* is)", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testAnd() {

		Criteria criteria = new Criteria("field_1").startsWith("start").endsWith("end").and("field_2").startsWith("2start")
				.endsWith("2end");
		assertEquals("field_1:(start* *end) AND field_2:(2start* *2end)", queryParser.createQueryStringFromNode(criteria));
	}

	@Test
	public void testOr() {

		Criteria criteria = new Criteria("field_1").startsWith("start").or("field_2").endsWith("end").startsWith("start2");
		assertEquals("field_1:start* OR field_2:(*end start2*)", queryParser.createQueryStringFromNode(criteria));
	}

	@Test
	public void testCriteriaWithWhiteSpace() {

		Criteria criteria = new Criteria("field_1").is("white space");
		assertEquals("field_1:\"white space\"", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testCriteriaWithDoubleQuotes() {

		Criteria criteria = new Criteria("field_1").is("with \"quote");
		assertEquals("field_1:\"with \\\"quote\"", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test // DATASOLR-437
	public void testCriteriaWithANDKeyword() {

		Criteria criteria = new Criteria("field_1").is("AND");
		assertEquals("field_1:\"AND\"", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test // DATASOLR-437
	public void testCriteriaWithMultipleWorkdsContainingANDKeyword() {

		Criteria criteria = new Criteria("field_1").is("this AND that");
		assertEquals("field_1:\"this AND that\"", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test // DATASOLR-437
	public void testCriteriaWithORKeyword() {

		Criteria criteria = new Criteria("field_1").is("OR");
		assertEquals("field_1:\"OR\"", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test // DATASOLR-437
	public void testCriteriaWithNOTKeyword() {

		Criteria criteria = new Criteria("field_1").is("NOT");
		assertEquals("field_1:\"NOT\"", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testIsNot() {

		Criteria criteria = new Criteria("field_1").is("value_1").not();
		assertEquals("-field_1:value_1", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testFuzzy() {

		Criteria criteria = new Criteria("field_1").fuzzy("value_1");
		assertEquals("field_1:value_1~", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testFuzzyWithDistance() {

		Criteria criteria = new Criteria("field_1").fuzzy("value_1", 0.5f);
		assertEquals("field_1:value_1~0.5", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testSloppy() {

		Criteria criteria = new Criteria("field_1").sloppy("value1 value2", 2);
		assertEquals("field_1:\"value1 value2\"~2", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBoost() {

		Criteria criteria = new Criteria("field_1").is("value_1").boost(2f);
		assertEquals("field_1:value_1^2.0", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBoostMultipleValues() {

		Criteria criteria = new Criteria("field_1").is("value_1").is("value_2").boost(2f);
		assertEquals("field_1:(value_1 value_2)^2.0", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBoostMultipleCriteriasValues() {
		Criteria criteria = new Criteria("field_1").is("value_1").is("value_2").boost(2f).and("field_3").is("value_3");
		assertEquals("field_1:(value_1 value_2)^2.0 AND field_3:value_3", queryParser.createQueryStringFromNode(criteria));
	}

	@Test
	public void testBetween() {

		Criteria criteria = new Criteria("field_1").between(100, 200);
		assertEquals("field_1:[100 TO 200]", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetweenExcludeLowerBound() {

		Criteria criteria = new Criteria("field_1").between(100, 200, false, true);
		assertEquals("field_1:{100 TO 200]", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetweenExcludeUpperBound() {

		Criteria criteria = new Criteria("field_1").between(100, 200, true, false);
		assertEquals("field_1:[100 TO 200}", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetweenWithoutUpperBound() {

		Criteria criteria = new Criteria("field_1").between(100, null);
		assertEquals("field_1:[100 TO *]", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetweenWithoutLowerBound() {
		Criteria criteria = new Criteria("field_1").between(null, 200);
		assertEquals("field_1:[* TO 200]", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetweenWithDateValue() {

		DateTime lowerBound = new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC);
		DateTime upperBound = new DateTime(2012, 8, 21, 19, 30, 0, DateTimeZone.UTC);

		Criteria criteria = new Criteria("field_1").between(lowerBound, upperBound);
		assertEquals("field_1:[2012\\-08\\-21T06\\:35\\:00.000Z TO 2012\\-08\\-21T19\\:30\\:00.000Z]",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testBetweenNegativeNumber() {

		Criteria criteria = new Criteria("field_1").between(-200, -100);
		assertEquals("field_1:[\\-200 TO \\-100]", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testIn() {

		Criteria criteria = new Criteria("field_1").in(1, 2, 3, 5, 8, 13, 21);
		assertEquals("field_1:(1 2 3 5 8 13 21)", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testIsWithJavaDateValue() {

		DateTime dateTime = new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(dateTime.getMillis());

		Criteria criteria = new Criteria("dateField").is(calendar.getTime());
		assertEquals("dateField:2012\\-08\\-21T06\\:35\\:00.000Z", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testIsWithJodaDateTime() {

		DateTime dateTime = new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC);

		Criteria criteria = new Criteria("dateField").is(dateTime);
		assertEquals("dateField:2012\\-08\\-21T06\\:35\\:00.000Z", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testIsWithJodaLocalDateTime() {

		LocalDateTime dateTime = new LocalDateTime(new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC).getMillis(),
				DateTimeZone.UTC);

		Criteria criteria = new Criteria("dateField").is(dateTime);
		assertEquals("dateField:2012\\-08\\-21T06\\:35\\:00.000Z", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testIsWithNegativeNumner() {

		Criteria criteria = new Criteria("field_1").is(-100);
		assertEquals("field_1:\\-100", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testNear() {

		Criteria criteria = new Criteria("field_1").near(new Point(48.303056, 14.290556), new Distance(5));
		assertEquals("{!bbox pt=48.303056,14.290556 sfield=field_1 d=5.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test(expected = IllegalArgumentException.class) // DATASOLR-142
	public void testCircleForNearMustNotBeNull() {
		new Criteria("field_1").near((Circle) null);
	}

	@Test
	public void testNearWithDistanceUnitMiles() {

		Criteria criteria = new Criteria("field_1").near(new Point(48.303056, 14.290556), new Distance(1, Metrics.MILES));
		assertEquals("{!bbox pt=48.303056,14.290556 sfield=field_1 d=1.609344}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testNearWithDistanceUnitKilometers() {

		Criteria criteria = new Criteria("field_1").near(new Point(48.303056, 14.290556),
				new Distance(1, Metrics.KILOMETERS));
		assertEquals("{!bbox pt=48.303056,14.290556 sfield=field_1 d=1.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testNearWithCoords() {

		Criteria criteria = new Criteria("field_1")
				.near(new Box(new Point(48.303056, 14.290556), new Point(48.303056, 14.290556)));
		assertEquals("field_1:[48.303056,14.290556 TO 48.303056,14.290556]",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testWithinWithDistanceUnitMiles() {

		Criteria criteria = new Criteria("field_1").within(new Point(48.303056, 14.290556), new Distance(1, Metrics.MILES));
		assertEquals("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.609344}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testWithinWithDistanceUnitKilometers() {

		Criteria criteria = new Criteria("field_1").within(new Point(48.303056, 14.290556),
				new Distance(1, Metrics.KILOMETERS));
		assertEquals("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test(expected = IllegalArgumentException.class) // DATASOLR-142
	public void testCircleForWithinMustNotBeNull() {
		new Criteria("field_1").within((Circle) null);
	}

	@Test // DATASOLR-142
	public void testWithinCircleWorksCorrectly() {

		Criteria criteria = new Criteria("field_1")
				.within(new Circle(new Point(48.303056, 14.290556), new Distance(1, Metrics.KILOMETERS)));
		assertEquals("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testWithinWithNullDistance() {

		Criteria criteria = new Criteria("field_1").within(new Point(48.303056, 14.290556), null);
		assertEquals("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=0.0}",
				queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testStringCriteria() {

		Criteria criteria = new SimpleStringCriteria("field_1:value_1 AND field_2:value_2");
		assertEquals("field_1:value_1 AND field_2:value_2", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testStringCriteriaWithMoreFragments() {

		Criteria criteria = new SimpleStringCriteria("field_1:value_1 AND field_2:value_2");
		criteria = criteria.and("field_3").is("value_3");
		assertEquals("field_1:value_1 AND field_2:value_2 AND field_3:value_3",
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
		assertEquals("field_1:001", queryParser.createQueryStringFromCriteria(criteria));
	}

	@Test
	public void testConstructSimpleSolrQuery() {

		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNotNull(solrQuery);
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
		assertNotNull(solrQuery);
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
		assertNotNull(solrQuery);
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
		assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingPresent(solrQuery, "group_1");
		assertFactingNotPresent(solrQuery);
	}

	@Test
	public void testConstructSolrQueryWithSingleFacetOnField() {

		Query query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"))
				.setFacetOptions(new FacetOptions("facet_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingPresent(solrQuery, "facet_1");
	}

	@Test
	public void testConstructSolrQueryWithSinglePivot() {

		Query query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"))
				.setFacetOptions(new FacetOptions().addFacetOnPivot("field_1", "field_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertPivotFactingPresent(solrQuery, "field_1,field_2");
	}

	@Test
	public void testConstructSolrQueryWithMultipleFacetOnFields() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"))
				.setFacetOptions(new FacetOptions("facet_1", "facet_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingPresent(solrQuery, "facet_1", "facet_2");
	}

	@Test
	public void testConstructSolrQueryWithMultiplePivot() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1")).setFacetOptions(
				new FacetOptions().addFacetOnPivot("field_1", "field_2").addFacetOnPivot("field_2", "field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNotNull(solrQuery);
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
		assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingPresent(solrQuery, "facet_1", "facet_2");
		assertEquals(facetOptions.getFacetPrefix(), solrQuery.getParams("facet.prefix")[0]);
	}

	@Test
	public void testConstructSolrQueryWithFieldFacetParameters() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"));
		FieldWithFacetParameters fieldWithFacetParameters = new FieldWithFacetParameters("facet_2").setPrefix("prefix")
				.setSort(FacetSort.INDEX).setLimit(3).setOffset(2).setMethod("method").setMissing(true);
		FacetOptions facetOptions = new FacetOptions(new SimpleField("facet_1"), fieldWithFacetParameters);
		query.setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingPresent(solrQuery, "facet_1", "facet_2");
		assertEquals(fieldWithFacetParameters.getPrefix(),
				solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.prefix")[0]);
		assertEquals(FacetParams.FACET_SORT_INDEX,
				solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.sort")[0]);
		assertEquals(Integer.toString(fieldWithFacetParameters.getOffset()),
				solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.offset")[0]);
		assertEquals(Integer.toString(fieldWithFacetParameters.getLimit()),
				solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.limit")[0]);
		assertEquals(fieldWithFacetParameters.getMethod(),
				solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.method")[0]);
		assertEquals(fieldWithFacetParameters.getMissing().toString(),
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
		assertEquals("on", solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.zeros")[0]);
	}

	@Test
	public void testConstructSolrQueryWithFacetSort() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"))
				.setFacetOptions(new FacetOptions("facet_1").setFacetSort(FacetOptions.FacetSort.INDEX));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals("index", solrQuery.getFacetSortString());

		query.getFacetOptions().setFacetSort(FacetOptions.FacetSort.COUNT);
		solrQuery = queryParser.constructSolrQuery(query);
		assertEquals("count", solrQuery.getFacetSortString());
	}

	@Test
	public void testConstructSolrQueryWithSingleFacetFilterQuery() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1")).setFacetOptions(
				new FacetOptions().addFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_2:[* TO 5]"))));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertArrayEquals(new String[] { "field_2:[* TO 5]" }, solrQuery.getFacetQuery());
	}

	@Test
	public void testConstructSolrQueryWithMultipleFacetFilterQuerues() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"))
				.setFacetOptions(new FacetOptions().addFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_2:[* TO 5]")))
						.addFacetQuery(new SimpleQuery(new Criteria("field_3").startsWith("prefix"))));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertArrayEquals(new String[] { "field_2:[* TO 5]", "field_3:prefix*" }, solrQuery.getFacetQuery());
	}

	@Test
	public void testWithFilterQuery() {

		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"))
				.addFilterQuery(new SimpleFilterQuery(new Criteria("filter_field").is("filter_value")));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		String[] filterQueries = solrQuery.getFilterQueries();
		assertEquals(1, filterQueries.length);
		assertEquals("filter_field:filter_value", filterQueries[0]);
	}

	@Test
	public void testWithEmptyFilterQuery() {

		Query query = new SimpleQuery(new Criteria("field_1").is("value_1")).addFilterQuery(new SimpleQuery());
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		assertNull(solrQuery.getFilterQueries());
	}

	@Test
	public void testWithSimpleStringCriteria() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNotNull(solrQuery);
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingNotPresent(solrQuery);

		assertEquals(criteria.getQueryString(), solrQuery.getQuery());
	}

	@Test
	public void testWithNullSort() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(null); // do this explicitly

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNull(solrQuery.getSortField());
		assertTrue(solrQuery.getSorts().isEmpty());
	}

	@Test
	public void testWithSortAscOnSingleField() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort("field_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals("field_2 asc", solrQuery.getSortField());
		assertEquals(1, solrQuery.getSorts().size());
	}

	@Test
	public void testWithSortDescOnSingleField() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort(Sort.Direction.DESC, "field_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals("field_2 desc", solrQuery.getSortField());
		assertEquals(1, solrQuery.getSorts().size());
	}

	@Test
	public void testWithSortAscMultipleFields() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort("field_2", "field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals("field_2 asc,field_3 asc", solrQuery.getSortField());
		assertEquals(2, solrQuery.getSorts().size());
	}

	@Test
	public void testWithSortDescMultipleFields() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort(Sort.Direction.DESC, "field_2", "field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals("field_2 desc,field_3 desc", solrQuery.getSortField());
		assertEquals(2, solrQuery.getSorts().size());
	}

	@Test
	public void testWithSortMixedDirections() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(new Sort("field_1"));
		query.addSort(new Sort(Sort.Direction.DESC, "field_2", "field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals("field_1 asc,field_2 desc,field_3 desc", solrQuery.getSortField());
		assertEquals(3, solrQuery.getSorts().size());
	}

	@Test
	public void testWithORDefaultOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefaultOperator(Operator.OR);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals("OR", solrQuery.get("q.op"));
	}

	@Test
	public void testWithANDDefaultOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefaultOperator(Operator.AND);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals("AND", solrQuery.get("q.op"));
	}

	@Test
	public void testWithNONEDefaultOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefaultOperator(Operator.NONE);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNull(solrQuery.get("q.op"));
	}

	@Test
	public void testWithoutDefaultOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNull(solrQuery.get("q.op"));
	}

	@Test
	public void testWithNullDefaultOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefaultOperator(null);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNull(solrQuery.get("q.op"));
	}

	@Test
	public void testWithTimeAllowed() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setTimeAllowed(100);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals(new Integer(100), solrQuery.getTimeAllowed());
	}

	@Test
	public void testWithoutTimeAllowed() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNull(solrQuery.getTimeAllowed());
	}

	@Test
	public void testWithLuceneDefType() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefType("lucene");
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNotNull(solrQuery.get("defType"));
	}

	@Test
	public void testWithEdismaxDefType() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefType("edismax");
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNotNull(solrQuery.get("defType"));
	}

	@Test
	public void testWithUndefindedDefType() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNull(solrQuery.get("defType"));
	}

	@Test
	public void testWithFooRequestHandler() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setRequestHandler("/foo");
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNotNull(solrQuery.get("qt"));
	}

	@Test
	public void testWithUndefinedRequestHandler() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertNull(solrQuery.get("qt"));
	}

	@Test
	public void testWithJoinOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setJoin(Join.from("inner_id").to("outer_id"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals("{!join from=inner_id to=outer_id}field_1:value_1", solrQuery.getQuery());
	}

	@Test // DATASOLR-176
	public void testWithJoinTwoCoresOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setJoin(Join.from("inner_id").fromIndex("sourceIndex").to("outer_id"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals("{!join from=inner_id to=outer_id fromIndex=sourceIndex}field_1:value_1", solrQuery.getQuery());
	}

	@Test
	public void testConstructSolrQueryWithEmptyHighlightOption() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setHighlightOptions(new HighlightOptions());

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertTrue(solrQuery.getHighlight());
		assertArrayEquals(new String[] { Criteria.WILDCARD }, solrQuery.getHighlightFields());
	}

	@Test
	public void testConstructSolrQueryWithoutHighlightOption() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertFalse(solrQuery.getHighlight());
	}

	@Test
	public void testConstructSolrQueryWithHighlightOptionHavingFields() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.addField("field_2", "field_3");
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertArrayEquals(new String[] { "field_2", "field_3" }, solrQuery.getHighlightFields());
	}

	@Test
	public void testConstructSorlQueryWithHighlightOptionFragsize() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.setFragsize(10);
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals(options.getFragsize().intValue(), solrQuery.getHighlightFragsize());
	}

	@Test
	public void testConstructSorlQueryWithHighlightOptionFormatter() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.setFormatter("formatter");
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals(options.getFormatter(), solrQuery.getParams(HighlightParams.FORMATTER)[0]);
	}

	@Test
	public void testConstructSorlQueryWithHighlightOptionNrSnipplets() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.setNrSnipplets(10);
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals(options.getNrSnipplets().intValue(), solrQuery.getHighlightSnippets());
	}

	@Test
	public void testConstructSorlQueryWithHighlightOptionsAndAnySolrParameter() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.addHighlightParameter(HighlightParams.SIMPLE_PRE, "{pre}");
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);
		assertEquals(options.<String> getHighlightParameterValue(HighlightParams.SIMPLE_PRE),
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
		assertArrayEquals(new String[] { "field_2" }, solrQuery.getHighlightFields());
		assertEquals(fieldWithHighlightParameters.getFormatter(),
				solrQuery.getParams("f.field_2." + HighlightParams.FORMATTER)[0]);
		assertEquals(fieldWithHighlightParameters.getFragsize().toString(),
				solrQuery.getParams("f.field_2." + HighlightParams.FRAGSIZE)[0]);
	}

	@Test // DATASOLR-105
	public void testNestedOrPartWithAnd() {

		Criteria criteria = Criteria.where("field_1").is("foo")
				.and(Criteria.where("field_2").is("bar").or("field_3").is("roo"))//
				.or(Criteria.where("field_4").is("spring").and("field_5").is("data"));

		assertEquals("field_1:foo AND (field_2:bar OR field_3:roo) OR (field_4:spring AND field_5:data)",
				queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-105
	public void testNestedOrPartWithAndSomeOtherThings() {

		Criteria criteria = Criteria.where("field_1").is("foo").is("bar")
				.and(Criteria.where("field_2").is("bar").is("lala").or("field_3").is("roo"))
				.or(Criteria.where("field_4").is("spring").and("field_5").is("data"));

		assertEquals("field_1:(foo bar) AND (field_2:(bar lala) OR field_3:roo) OR (field_4:spring AND field_5:data)",
				queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-105
	public void testMultipleAnd() {
		Criteria criteria = Criteria.where("field_1").is("foo").and("field_2").is("bar").and("field_3").is("roo");

		assertEquals("field_1:foo AND field_2:bar AND field_3:roo", queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-105
	public void testMultipleOr() {
		Criteria criteria = Criteria.where("field_1").is("foo").or("field_2").is("bar").or("field_3").is("roo");

		assertEquals("field_1:foo OR field_2:bar OR field_3:roo", queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-105
	public void testEmptyCriteriaShouldBeDefaultedToNotNUll() {
		Criteria criteria = Criteria.where("field_1").is("foo").and("field_2").or("field_3");

		assertEquals("field_1:foo AND field_2:[* TO *] OR field_3:[* TO *]",
				queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-105
	public void testDeepNesting() {

		Criteria criteria = Criteria.where("field_1").is("foo")
				.and(Criteria.where("field_2").is("bar").and("field_3").is("roo")//
						.and(Criteria.where("field_4").is("spring").and("field_5").is("data").or("field_6").is("solr")));

		assertEquals("field_1:foo AND (field_2:bar AND field_3:roo AND (field_4:spring AND field_5:data OR field_6:solr))",
				queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-168
	public void testNotCritieraCarriedOnPorperlyForNullAndNotNull() {

		Criteria criteria = new Criteria("param1").isNotNull().and("param2").isNull();
		assertEquals("param1:[* TO *] AND -param2:[* TO *]", queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-112
	public void pageableUsingZeroShouldBeParsedCorrectlyWhenSetUsingPageable() {

		SimpleQuery query = new SimpleQuery("*:*").setPageRequest(new SolrPageRequest(0, 0));
		assertPaginationPresent(queryParser.constructSolrQuery(query), 0, 0);
	}

	@Test // DATASOLR-112
	public void pageableUsingZeroShouldBeParsedCorrectlyWhenSetUsingExplititMethods() {

		SimpleQuery query = new SimpleQuery("*:*").setOffset(0L).setRows(0);
		assertPaginationPresent(queryParser.constructSolrQuery(query), 0, 0);
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithStatField() {

		StatsOptions statsOptions = new StatsOptions().addField(new SimpleField("field_1"));

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		assertEquals("field_1", solrQuery.get(StatsParams.STATS_FIELD));
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithStatFields() {

		StatsOptions statsOptions = new StatsOptions()//
				.addField(new SimpleField("field_1"))//
				.addField(new SimpleField("field_2"));

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		List<String> fields = Arrays.asList(solrQuery.getParams(StatsParams.STATS_FIELD));
		Collections.sort(fields);
		assertEquals(2, fields.size());
		assertEquals(Arrays.asList("field_1", "field_2"), fields);
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithStatFacets() {

		StatsOptions statsOptions = new StatsOptions()//
				.addFacet(new SimpleField("field_1"))//
				.addFacet(new SimpleField("field_2"));

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		List<String> facets = Arrays.asList(solrQuery.getParams(StatsParams.STATS_FACET));
		Collections.sort(facets);
		assertEquals(2, facets.size());
		assertEquals(Arrays.asList("field_1", "field_2"), facets);
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithStatFieldsAndFacets() {

		StatsOptions statsOptions = new StatsOptions()//
				.addField(new SimpleField("field_1"))//
				.addFacet(new SimpleField("field_2"));

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		String[] fields = solrQuery.getParams(StatsParams.STATS_FIELD);
		String[] facets = solrQuery.getParams(StatsParams.STATS_FACET);

		assertEquals(1, fields.length);
		assertEquals(1, facets.length);
		assertEquals("field_1", fields[0]);
		assertEquals("field_2", facets[0]);
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithSelectiveStatsFacet() {

		StatsOptions statsOptions = new StatsOptions()//
				.addField(new SimpleField("field_1"))//
				.addSelectiveFacet(new SimpleField("field_2"));

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		String[] fields = solrQuery.getParams(StatsParams.STATS_FIELD);
		String[] facets = solrQuery.getParams(CommonParams.FIELD + ".field_1." + StatsParams.STATS_FACET);

		assertEquals(1, fields.length);
		assertEquals(1, facets.length);
		assertEquals("field_1", fields[0]);
		assertEquals("field_2", facets[0]);
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithSelectiveStatsCountDistinct() {

		StatsOptions statsOptions = new StatsOptions()//
				.addField(new SimpleField("field_1")).setSelectiveCalcDistinct(true) //
				.addField(new SimpleField("field_2")).setSelectiveCalcDistinct(false) //
				.addField(new SimpleField("field_3"));

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		String[] fields = solrQuery.getParams(StatsParams.STATS_FIELD);
		String[] calc1 = solrQuery.getParams(CommonParams.FIELD + ".field_1." + StatsParams.STATS_CALC_DISTINCT);
		String[] calc2 = solrQuery.getParams(CommonParams.FIELD + ".field_2." + StatsParams.STATS_CALC_DISTINCT);
		String[] calc3 = solrQuery.getParams(CommonParams.FIELD + ".field_3." + StatsParams.STATS_CALC_DISTINCT);

		Arrays.sort(fields);

		assertEquals(3, fields.length);
		assertArrayEquals(new String[] { "field_1", "field_2", "field_3" }, fields);
		assertEquals("true", calc1[0]);
		assertEquals("false", calc2[0]);
		assertNull(calc3);
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithStatsConfig() {

		StatsOptions statsOptions = new StatsOptions()//
				.addField(new SimpleField("field_1"))//
				.addSelectiveFacet(new SimpleField("field_1_1"))//
				.addSelectiveFacet(new SimpleField("field_1_2"))//
				.addField("field_2")//
				.addFacet("field_3");

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		List<String> fields = Arrays.asList(solrQuery.getParams(StatsParams.STATS_FIELD));
		Collections.sort(fields);
		List<String> selectiveFacets = Arrays
				.asList(solrQuery.getParams(CommonParams.FIELD + ".field_1." + StatsParams.STATS_FACET));
		String[] facets = solrQuery.getParams(StatsParams.STATS_FACET);

		assertEquals(2, fields.size());
		assertEquals(2, selectiveFacets.size());
		assertEquals("field_1", fields.get(0));
		assertEquals("field_2", fields.get(1));
		assertEquals("field_1_1", selectiveFacets.get(0));
		assertEquals("field_1_2", selectiveFacets.get(1));
		assertEquals("field_3", facets[0]);
	}

	@Test // DATASOLR-121
	public void testConstructGroupQueryWithAllPossibleParameters() {

		GroupOptions groupOptions = new GroupOptions();

		SimpleQuery query = new SimpleQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.setGroupOptions(groupOptions);
		groupOptions.setOffset(1);
		groupOptions.setLimit(2);
		groupOptions.addGroupByField("field_1");
		groupOptions.addGroupByFunction(MaxFunction.max("field_1", "field_2"));
		groupOptions.addGroupByQuery(new SimpleQuery("*:*"));
		groupOptions.addSort(new Sort(Sort.Direction.DESC, "field_3"));
		groupOptions.setTotalCount(true);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		assertGroupFormatPresent(solrQuery, true);
		assertEquals("field_1", solrQuery.get(GroupParams.GROUP_FIELD));
		assertEquals("{!func}max(field_1,field_2)", solrQuery.get(GroupParams.GROUP_FUNC));
		assertEquals("*:*", solrQuery.get(GroupParams.GROUP_QUERY));
		assertEquals("field_3 desc", solrQuery.get(GroupParams.GROUP_SORT));
		assertEquals("1", solrQuery.get(GroupParams.GROUP_OFFSET));
		assertEquals("2", solrQuery.get(GroupParams.GROUP_LIMIT));
	}

	@Test // DATASOLR-310
	public void testConstructGroupQueryWithLimitSetToNegative1() {

		GroupOptions groupOptions = new GroupOptions();

		SimpleQuery query = new SimpleQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.setGroupOptions(groupOptions);
		groupOptions.setLimit(-1);
		groupOptions.addGroupByField("field_1");
		groupOptions.addSort(new Sort(Sort.Direction.DESC, "field_3"));
		groupOptions.setTotalCount(true);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		assertGroupFormatPresent(solrQuery, true);
		assertEquals("field_1", solrQuery.get(GroupParams.GROUP_FIELD));
		assertEquals("-1", solrQuery.get(GroupParams.GROUP_LIMIT));
	}

	@Test // DATASOLR-121
	public void testConstructGroupQueryWithoutPagingParameters() {

		SimpleQuery query = new SimpleQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.setGroupOptions(new GroupOptions().addGroupByField("fieldName"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		assertGroupFormatPresent(solrQuery, false);
		assertNull(solrQuery.get(GroupParams.GROUP_SORT));
		assertNull(solrQuery.get(GroupParams.GROUP_OFFSET));
		assertNull(solrQuery.get(GroupParams.GROUP_LIMIT));
	}

	@Test // DATASOLR-121
	public void testConstructGroupQueryWithMultipleFunctions() {

		SimpleQuery query = new SimpleQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.setGroupOptions(new GroupOptions());
		query.getGroupOptions().addGroupByFunction(MaxFunction.max("field_1", "field_2"));
		query.getGroupOptions().addGroupByFunction(MaxFunction.max("field_3", "field_4"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		assertGroupFormatPresent(solrQuery, false);
		assertArrayEquals(new String[] { "{!func}max(field_1,field_2)", "{!func}max(field_3,field_4)" },
				solrQuery.getParams(GroupParams.GROUP_FUNC));
		assertNull(solrQuery.getParams(GroupParams.GROUP_QUERY));
		assertNull(solrQuery.getParams(GroupParams.GROUP_FIELD));
	}

	@Test // DATASOLR-121
	public void testConstructGroupQueryWithMultipleQueries() {

		SimpleQuery query = new SimpleQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.setGroupOptions(new GroupOptions());
		query.getGroupOptions().addGroupByQuery(new SimpleQuery("query1"));
		query.getGroupOptions().addGroupByQuery(new SimpleQuery("query2"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		assertGroupFormatPresent(solrQuery, false);
		assertArrayEquals(new String[] { "query1", "query2" }, solrQuery.getParams(GroupParams.GROUP_QUERY));
		assertNull(solrQuery.getParams(GroupParams.GROUP_FUNC));
		assertNull(solrQuery.getParams(GroupParams.GROUP_FIELD));
	}

	@Test // DATASOLR-196
	public void connectShouldAllowConcatinationOfCriteriaWithAndPreservingDesiredBracketing() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar");
		Criteria criteria = part1.connect().and(part2);

		assertEquals("z:roo AND (x:foo OR y:bar)", queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-196
	public void connectShouldAllowConcatinationOfCriteriaWithAndPreservingDesiredBracketingReverse() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar");
		Criteria criteria = part2.connect().and(part1);

		assertEquals("(x:foo OR y:bar) AND z:roo", queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-196
	public void connectShouldAllowConcatinationOfCriteriaWithOrPreservingDesiredBracketing() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar");
		Criteria criteria = part1.connect().or(part2);

		assertEquals("z:roo OR (x:foo OR y:bar)", queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-196
	public void connectShouldAllowConcatinationOfCriteriaWithOrPreservingDesiredBracketingReverse() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar");
		Criteria criteria = part2.connect().or(part1);

		assertEquals("(x:foo OR y:bar) OR z:roo", queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-196
	public void notOperatorShouldWrapWholeExpression() {

		Criteria part1 = Criteria.where("text").startsWith("fx").or("product_code").startsWith("fx");
		Criteria part2 = Criteria.where("text").startsWith("option").or("product_code").startsWith("option");
		Criteria criteria = part1.connect().and(part2).notOperator();

		String expected = "-((text:fx* OR product_code:fx*) AND (text:option* OR product_code:option*))";
		assertEquals(expected, queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-196
	public void notOperatorShouldWrapNestedExpressionCorrectly() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar").notOperator();

		Criteria criteria = part1.connect().or(part2);

		assertEquals("z:roo OR -(x:foo OR y:bar)", queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-196
	public void notOperatorShouldWrapNestedExpressionCorrectlyReverse() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar").notOperator();

		Criteria criteria = part2.connect().or(part1);

		assertEquals("-(x:foo OR y:bar) OR z:roo", queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-196
	public void notOperatorShouldWrapNestedExpressionCorrectlyReverseWithDoubleNegation() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar").notOperator();

		Criteria criteria = part2.connect().and(part1).notOperator();

		assertEquals("-(-(x:foo OR y:bar) AND z:roo)", queryParser.createQueryStringFromNode(criteria));
	}

	@Test // DATASOLR-236
	public void testNegativeFacetLimitUsingFacetOptions_setFacetLimit() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"));
		FacetOptions facetOptions = new FacetOptions(new SimpleField("facet_1"));
		facetOptions.setFacetLimit(-1);
		query.setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		assertEquals(-1, solrQuery.getFacetLimit());
		assertEquals(null, solrQuery.get(FacetParams.FACET_OFFSET));
	}

	@Test // DATASOLR-236
	public void testNegativeFacetLimitUsingFacetOptions_setPageable() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"));
		FacetOptions facetOptions = new FacetOptions(new SimpleField("facet_1"));
		facetOptions.setPageable(new SolrPageRequest(0, -1));
		query.setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		assertEquals(-1, solrQuery.getFacetLimit());
		assertEquals(null, solrQuery.get(FacetParams.FACET_OFFSET));
	}

	@Test // DATASOLR-236
	public void testNegativeFacetOffsetAndFacetLimitUsingFacetOptions_setPageable() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"));
		FacetOptions facetOptions = new FacetOptions(new SimpleField("facet_1"));
		facetOptions.setPageable(new SolrPageRequest(1, -1));
		query.setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query);

		assertEquals(-1, solrQuery.getFacetLimit());
		assertEquals(Integer.valueOf(0), solrQuery.getInt(FacetParams.FACET_OFFSET));
	}

	@Test // DATASOLR-86
	public void testRegularNumericRangeFacets() {

		FacetOptions facetOptions = new FacetOptions() //
				.addFacetByRange( //
						new FieldWithNumericRangeParameters("field1", 4, 8, 2) //
								.setHardEnd(true) //
								.setInclude(FacetRangeInclude.ALL) //
								.setOther(FacetRangeOther.ALL))//
				.addFacetByRange( //
						new FieldWithNumericRangeParameters("field2", 0.5, 12.3, 0.7) //
								.setHardEnd(false) //
								.setInclude(FacetRangeInclude.OUTER) //
								.setOther(FacetRangeOther.NONE))
				.addFacetByRange( //
						new FieldWithNumericRangeParameters("field3", 4, 8, 2) //
								.setHardEnd(true) //
								.setOther(FacetRangeOther.ALL))//
				.addFacetByRange( //
						new FieldWithNumericRangeParameters("field4", 4, 8, 2) //
								.setHardEnd(true) //
								.setInclude(FacetRangeInclude.OUTER)) //
				.addFacetByRange( //
						new FieldWithNumericRangeParameters("field5", 4, 8, 2));//

		SolrDataQuery facetQuery = new SimpleFacetQuery(new SimpleStringCriteria("*:*")).setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(facetQuery);

		assertTrue(facetOptions.hasFacets());
		assertArrayEquals(new String[] {}, solrQuery.getFacetFields());
		assertArrayEquals(new String[] { "field1", "field2", "field3", "field4", "field5" },
				solrQuery.getParams(FacetParams.FACET_RANGE));

		assertEquals("4", solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_START));
		assertEquals("2", solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_GAP));
		assertEquals("8", solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_END));
		assertEquals("true", solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_HARD_END));
		assertEquals("all", solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_INCLUDE));
		assertEquals("all", solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_OTHER));

		assertEquals("0.5", solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_START));
		assertEquals("0.7", solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_GAP));
		assertEquals("12.3", solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_END));
		assertNull(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_HARD_END));
		assertEquals("outer", solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_INCLUDE));
		assertEquals("none", solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_OTHER));

		assertEquals("4", solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_START));
		assertEquals("2", solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_GAP));
		assertEquals("8", solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_END));
		assertEquals("true", solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_HARD_END));
		assertNull("all", solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_INCLUDE));
		assertEquals("all", solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_OTHER));

		assertEquals("4", solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_START));
		assertEquals("2", solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_GAP));
		assertEquals("8", solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_END));
		assertEquals("true", solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_HARD_END));
		assertEquals("outer", solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_INCLUDE));
		assertNull("all", solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_OTHER));

		assertEquals("4", solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_START));
		assertEquals("2", solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_GAP));
		assertEquals("8", solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_END));
		assertNull("true", solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_HARD_END));
		assertNull("all", solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_INCLUDE));
		assertNull("all", solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_OTHER));
	}

	@Test // DATASOLR-86, DATASOLR-309
	public void testRegularDateRangeFacets() {

		FacetOptions facetOptions = new FacetOptions() //
				.addFacetByRange( //
						new FieldWithDateRangeParameters("field1", new Date(100), new Date(10000000L), "+1DAY") //
								.setHardEnd(true) //
								.setInclude(FacetRangeInclude.ALL) //
								.setOther(FacetRangeOther.ALL))//
				.addFacetByRange( //
						new FieldWithDateRangeParameters("field2", new Date(100), new Date(10000000L), "+2DAY") //
								.setHardEnd(false) //
								.setInclude(FacetRangeInclude.OUTER) //
								.setOther(FacetRangeOther.NONE))
				.addFacetByRange( //
						new FieldWithDateRangeParameters("field3", new Date(100), new Date(10000000L), "+2DAY") //
								.setHardEnd(true) //
								.setOther(FacetRangeOther.NONE))
				.addFacetByRange( //
						new FieldWithDateRangeParameters("field4", new Date(100), new Date(10000000L), "+2DAY") //
								.setHardEnd(true) //
								.setInclude(FacetRangeInclude.OUTER))
				.addFacetByRange( //
						new FieldWithDateRangeParameters("field5", new Date(100), new Date(10000000L), "+2DAY"));

		SolrDataQuery facetQuery = new SimpleFacetQuery(new SimpleStringCriteria("*:*")).setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(facetQuery);

		assertTrue(facetOptions.hasFacets());
		assertArrayEquals(new String[] {}, solrQuery.getFacetFields());
		assertArrayEquals(new String[] { "field1", "field2", "field3", "field4", "field5" },
				solrQuery.getParams(FacetParams.FACET_RANGE));

		// RANGE is being used on SolrJ even for DATE fields
		assertEquals("1970-01-01T00:00:00.100Z", solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_START));
		assertEquals("+1DAY", solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_GAP));
		assertEquals("1970-01-01T02:46:40Z", solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_END));
		assertEquals("true", solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_HARD_END));
		assertEquals("all", solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_INCLUDE));
		assertEquals("all", solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_OTHER));

		assertEquals("1970-01-01T00:00:00.100Z", solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_START));
		assertEquals("+2DAY", solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_GAP));
		assertEquals("1970-01-01T02:46:40Z", solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_END));
		assertNull(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_HARD_END));
		assertEquals("outer", solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_INCLUDE));
		assertEquals("none", solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_OTHER));

		assertEquals("1970-01-01T00:00:00.100Z", solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_START));
		assertEquals("+2DAY", solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_GAP));
		assertEquals("1970-01-01T02:46:40Z", solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_END));
		assertEquals("true", solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_HARD_END));
		assertNull(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_INCLUDE));
		assertEquals("none", solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_OTHER));

		assertEquals("1970-01-01T00:00:00.100Z", solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_START));
		assertEquals("+2DAY", solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_GAP));
		assertEquals("1970-01-01T02:46:40Z", solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_END));
		assertEquals("true", solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_HARD_END));
		assertEquals("outer", solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_INCLUDE));
		assertNull(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_OTHER));

		assertEquals("1970-01-01T00:00:00.100Z", solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_START));
		assertEquals("+2DAY", solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_GAP));
		assertEquals("1970-01-01T02:46:40Z", solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_END));
		assertNull(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_HARD_END));
		assertNull(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_INCLUDE));
		assertNull(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_OTHER));
	}

	@Test // DATASOLR-86
	public void testNoRangeFacetAssignmentWhenNoRangeFacetsPresent() {

		FacetOptions facetOptions = new FacetOptions("field1");
		SolrDataQuery facetQuery = new SimpleFacetQuery(new SimpleStringCriteria("*:*")).setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(facetQuery);

		assertTrue(facetOptions.hasFacets());
		assertArrayEquals(new String[] { "field1" }, solrQuery.getFacetFields());
		assertNull(solrQuery.getParams(FacetParams.FACET_DATE));
		assertNull(solrQuery.getParams(FacetParams.FACET_RANGE));
	}

	@Test // DATASOLR-324
	public void shouldNotEnableSpellcheckWennNoSpellcheckOptionsPresent() {

		SimpleQuery q = new SimpleQuery(AnyCriteria.any());

		SolrQuery solrQuery = queryParser.constructSolrQuery(q);
		assertThat(solrQuery.get("spellcheck"), is(nullValue()));
	}

	@Test // DATASOLR-324
	public void shouldEnableSpellcheckWennSpellcheckOptionsPresent() {

		SimpleQuery q = new SimpleQuery(AnyCriteria.any());
		q.setSpellcheckOptions(SpellcheckOptions.spellcheck());

		SolrQuery solrQuery = queryParser.constructSolrQuery(q);
		assertThat(solrQuery.get("spellcheck"), is(equalTo("on")));
	}

	@Test // DATASOLR-324
	public void shouldApplySpellcheckOptionsCorrectly() {

		SimpleQuery q = new SimpleQuery(AnyCriteria.any());
		q.setSpellcheckOptions(SpellcheckOptions.spellcheck().dictionaries("dict1", "dict2").count(5).extendedResults());

		SolrQuery solrQuery = queryParser.constructSolrQuery(q);
		assertThat(solrQuery.get("spellcheck"), is(equalTo("on")));
		assertThat(solrQuery.getParams(SpellingParams.SPELLCHECK_DICT), is(new String[] { "dict1", "dict2" }));
		assertThat(solrQuery.get(SpellingParams.SPELLCHECK_EXTENDED_RESULTS), is(equalTo("true")));
	}

	private void assertPivotFactingPresent(SolrQuery solrQuery, String... expected) {
		assertArrayEquals(expected, solrQuery.getParams(FacetParams.FACET_PIVOT));
	}

	private void assertFactingPresent(SolrQuery solrQuery, String... expected) {
		assertArrayEquals(expected, solrQuery.getFacetFields());
	}

	private void assertFactingNotPresent(SolrQuery solrQuery) {
		assertNull(solrQuery.get(FacetParams.FACET_FIELD));
	}

	private void assertQueryStringPresent(SolrQuery solrQuery) {
		assertNotNull(solrQuery.get(CommonParams.Q));
	}

	private void assertProjectionNotPresent(SolrQuery solrQuery) {
		assertNull(solrQuery.getFields());
	}

	private void assertProjectionPresent(SolrQuery solrQuery, String expected) {

		assertNotNull(solrQuery.get(CommonParams.FL));
		assertEquals(expected, solrQuery.get(CommonParams.FL));
	}

	private void assertPaginationNotPresent(SolrQuery solrQuery) {

		assertNull(solrQuery.getStart());
		assertNull(solrQuery.getRows());
	}

	private void assertPaginationPresent(SolrQuery solrQuery, int start, int rows) {

		assertEquals(Integer.valueOf(start), solrQuery.getStart());
		assertEquals(Integer.valueOf(rows), solrQuery.getRows());
	}

	private void assertGroupingNotPresent(SolrQuery solrQuery) {

		assertNull(solrQuery.get(GroupParams.GROUP));
		assertNull(solrQuery.get(GroupParams.GROUP_FIELD));
		assertNull(solrQuery.get(GroupParams.GROUP_MAIN));
	}

	private void assertGroupingPresent(SolrQuery solrQuery, String expected) {

		assertNotNull(solrQuery.get(GroupParams.GROUP));
		assertNotNull(solrQuery.get(GroupParams.GROUP_FIELD));
		assertNotNull(solrQuery.get(GroupParams.GROUP_MAIN));
		assertEquals(expected, solrQuery.get(GroupParams.GROUP_FIELD));
	}

	private void assertGroupFormatPresent(SolrQuery solrQuery, boolean groupTotalCount) {

		assertEquals("true", solrQuery.get(GroupParams.GROUP));
		assertEquals("false", solrQuery.get(GroupParams.GROUP_MAIN));
		assertEquals("grouped", solrQuery.get(GroupParams.GROUP_FORMAT));
		assertEquals(String.valueOf(groupTotalCount), solrQuery.get(GroupParams.GROUP_TOTAL_COUNT));
	}

}
