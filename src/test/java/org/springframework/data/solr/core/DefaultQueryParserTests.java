/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.beans.Field;
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
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metric;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.mapping.SolrDocument;
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
		this.queryParser = new DefaultQueryParser(new SimpleSolrMappingContext());
	}

	@Test
	public void testIs() {

		Criteria criteria = new Criteria("field_1").is("is");
		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:is");
	}

	@Test
	public void testMultipleIs() {

		Criteria criteria = new Criteria("field_1").is("is").is("another is");
		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:(is \"another is\")");
	}

	@Test
	public void testEndsWith() {

		Criteria criteria = new Criteria("field_1").endsWith("end");

		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:*end");
	}

	@Test
	public void testEndsWithMulitpleValues() {

		Criteria criteria = new Criteria("field_1").endsWith(Arrays.asList("one", "two", "three"));

		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:(*one *two *three)");
	}

	@Test
	public void testStartsWith() {

		Criteria criteria = new Criteria("field_1").startsWith("start");

		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:start*");
	}

	@Test
	public void testStartsWithMultipleValues() {

		Criteria criteria = new Criteria("field_1").startsWith(Arrays.asList("one", "two", "three"));

		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:(one* two* three*)");
	}

	@Test
	public void testContains() {

		Criteria criteria = new Criteria("field_1").contains("contains");

		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:*contains*");
	}

	@Test
	public void testContainsWithMultipleValues() {

		Criteria criteria = new Criteria("field_1").contains(Arrays.asList("one", "two", "three"));

		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:(*one* *two* *three*)");
	}

	@Test
	public void testExpression() {

		Criteria criteria = new Criteria("field_1").expression("(have fun using +solr && expressions*)");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("field_1:(have fun using +solr && expressions*)");
	}

	@Test
	public void testCriteriaChain() {

		Criteria criteria = new Criteria("field_1").startsWith("start").endsWith("end").contains("contains").is("is");
		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("field_1:(start* *end *contains* is)");
	}

	@Test
	public void testAnd() {

		Criteria criteria = new Criteria("field_1").startsWith("start").endsWith("end").and("field_2").startsWith("2start")
				.endsWith("2end");
		assertThat(queryParser.createQueryStringFromNode(criteria, null))
				.isEqualTo("field_1:(start* *end) AND field_2:(2start* *2end)");
	}

	@Test
	public void testOr() {

		Criteria criteria = new Criteria("field_1").startsWith("start").or("field_2").endsWith("end").startsWith("start2");
		assertThat(queryParser.createQueryStringFromNode(criteria, null))
				.isEqualTo("field_1:start* OR field_2:(*end start2*)");
	}

	@Test
	public void testCriteriaWithWhiteSpace() {

		Criteria criteria = new Criteria("field_1").is("white space");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:\"white space\"");
	}

	@Test
	public void testCriteriaWithDoubleQuotes() {

		Criteria criteria = new Criteria("field_1").is("with \"quote");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:\"with \\\"quote\"");
	}

	@Test // DATASOLR-437
	public void testCriteriaWithANDKeyword() {

		Criteria criteria = new Criteria("field_1").is("AND");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:\"AND\"");
	}

	@Test // DATASOLR-437
	public void testCriteriaWithMultipleWorkdsContainingANDKeyword() {

		Criteria criteria = new Criteria("field_1").is("this AND that");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:\"this AND that\"");
	}

	@Test // DATASOLR-437
	public void testCriteriaWithORKeyword() {

		Criteria criteria = new Criteria("field_1").is("OR");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:\"OR\"");
	}

	@Test // DATASOLR-437
	public void testCriteriaWithNOTKeyword() {

		Criteria criteria = new Criteria("field_1").is("NOT");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:\"NOT\"");
	}

	@Test
	public void testIsNot() {

		Criteria criteria = new Criteria("field_1").is("value_1").not();
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("-field_1:value_1");
	}

	@Test
	public void testFuzzy() {

		Criteria criteria = new Criteria("field_1").fuzzy("value_1");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:value_1~");
	}

	@Test
	public void testFuzzyWithDistance() {

		Criteria criteria = new Criteria("field_1").fuzzy("value_1", 0.5f);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:value_1~0.5");
	}

	@Test
	public void testSloppy() {

		Criteria criteria = new Criteria("field_1").sloppy("value1 value2", 2);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:\"value1 value2\"~2");
	}

	@Test
	public void testBoost() {

		Criteria criteria = new Criteria("field_1").is("value_1").boost(2f);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:value_1^2.0");
	}

	@Test
	public void testBoostMultipleValues() {

		Criteria criteria = new Criteria("field_1").is("value_1").is("value_2").boost(2f);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:(value_1 value_2)^2.0");
	}

	@Test
	public void testBoostMultipleCriteriasValues() {
		Criteria criteria = new Criteria("field_1").is("value_1").is("value_2").boost(2f).and("field_3").is("value_3");
		assertThat(queryParser.createQueryStringFromNode(criteria, null))
				.isEqualTo("field_1:(value_1 value_2)^2.0 AND field_3:value_3");
	}

	@Test
	public void testBetween() {

		Criteria criteria = new Criteria("field_1").between(100, 200);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:[100 TO 200]");
	}

	@Test
	public void testBetweenExcludeLowerBound() {

		Criteria criteria = new Criteria("field_1").between(100, 200, false, true);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:{100 TO 200]");
	}

	@Test
	public void testBetweenExcludeUpperBound() {

		Criteria criteria = new Criteria("field_1").between(100, 200, true, false);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:[100 TO 200}");
	}

	@Test
	public void testBetweenWithoutUpperBound() {

		Criteria criteria = new Criteria("field_1").between(100, null);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:[100 TO *]");
	}

	@Test
	public void testBetweenWithoutLowerBound() {
		Criteria criteria = new Criteria("field_1").between(null, 200);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:[* TO 200]");
	}

	@Test
	public void testBetweenWithDateValue() {

		DateTime lowerBound = new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC);
		DateTime upperBound = new DateTime(2012, 8, 21, 19, 30, 0, DateTimeZone.UTC);

		Criteria criteria = new Criteria("field_1").between(lowerBound, upperBound);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("field_1:[2012\\-08\\-21T06\\:35\\:00.000Z TO 2012\\-08\\-21T19\\:30\\:00.000Z]");
	}

	@Test
	public void testBetweenNegativeNumber() {

		Criteria criteria = new Criteria("field_1").between(-200, -100);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:[\\-200 TO \\-100]");
	}

	@Test
	public void testIn() {

		Criteria criteria = new Criteria("field_1").in(1, 2, 3, 5, 8, 13, 21);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:(1 2 3 5 8 13 21)");
	}

	@Test
	public void testIsWithJavaDateValue() {

		DateTime dateTime = new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(dateTime.getMillis());

		Criteria criteria = new Criteria("dateField").is(calendar.getTime());
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("dateField:2012\\-08\\-21T06\\:35\\:00.000Z");
	}

	@Test
	public void testIsWithJodaDateTime() {

		DateTime dateTime = new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC);

		Criteria criteria = new Criteria("dateField").is(dateTime);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("dateField:2012\\-08\\-21T06\\:35\\:00.000Z");
	}

	@Test
	public void testIsWithJodaLocalDateTime() {

		LocalDateTime dateTime = new LocalDateTime(new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC).getMillis(),
				DateTimeZone.UTC);

		Criteria criteria = new Criteria("dateField").is(dateTime);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("dateField:2012\\-08\\-21T06\\:35\\:00.000Z");
	}

	@Test
	public void testIsWithNegativeNumner() {

		Criteria criteria = new Criteria("field_1").is(-100);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:\\-100");
	}

	@Test
	public void testNear() {

		Criteria criteria = new Criteria("field_1").near(new Point(48.303056, 14.290556), new Distance(5));
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("{!bbox pt=48.303056,14.290556 sfield=field_1 d=5.0}");
	}

	@Test(expected = IllegalArgumentException.class) // DATASOLR-142
	public void testCircleForNearMustNotBeNull() {
		new Criteria("field_1").near((Circle) null);
	}

	@Test
	public void testNearWithDistanceUnitMiles() {

		Criteria criteria = new Criteria("field_1").near(new Point(48.303056, 14.290556), new Distance(1, Metrics.MILES));
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("{!bbox pt=48.303056,14.290556 sfield=field_1 d=1.609344 score=miles}");
	}

	@Test
	public void testNearWithDistanceUnitKilometers() {

		Criteria criteria = new Criteria("field_1").near(new Point(48.303056, 14.290556),
				new Distance(1, Metrics.KILOMETERS));
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("{!bbox pt=48.303056,14.290556 sfield=field_1 d=1.0 score=kilometers}");
	}

	@Test
	public void testNearWithCoords() {

		Criteria criteria = new Criteria("field_1")
				.near(new Box(new Point(48.303056, 14.290556), new Point(48.303056, 14.290556)));
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("field_1:[48.303056,14.290556 TO 48.303056,14.290556]");
	}

	@Test
	public void testWithinWithDistanceUnitMiles() {

		Criteria criteria = new Criteria("field_1").within(new Point(48.303056, 14.290556), new Distance(1, Metrics.MILES));
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.609344 score=miles}");
	}

	@Test
	public void testWithinWithDistanceUnitKilometers() {

		Criteria criteria = new Criteria("field_1").within(new Point(48.303056, 14.290556),
				new Distance(1, Metrics.KILOMETERS));
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.0 score=kilometers}");
	}

	@Test(expected = IllegalArgumentException.class) // DATASOLR-142
	public void testCircleForWithinMustNotBeNull() {
		new Criteria("field_1").within(null);
	}

	@Test // DATASOLR-142
	public void testWithinCircleWorksCorrectly() {

		Criteria criteria = new Criteria("field_1")
				.within(new Circle(new Point(48.303056, 14.290556), new Distance(1, Metrics.KILOMETERS)));
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.0 score=kilometers}");
	}

	@Test
	public void testWithinWithNullDistance() {

		Criteria criteria = new Criteria("field_1").within(new Point(48.303056, 14.290556), null);
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=0.0}");
	}

	@Test
	public void testStringCriteria() {

		Criteria criteria = new SimpleStringCriteria("field_1:value_1 AND field_2:value_2");
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("field_1:value_1 AND field_2:value_2");
	}

	@Test
	public void testStringCriteriaWithMoreFragments() {

		Criteria criteria = new SimpleStringCriteria("field_1:value_1 AND field_2:value_2");
		criteria = criteria.and("field_3").is("value_3");
		assertThat(queryParser.createQueryStringFromNode(criteria, null))
				.isEqualTo("field_1:value_1 AND field_2:value_2 AND field_3:value_3");
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
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null)).isEqualTo("field_1:001");
	}

	@Test
	public void testConstructSimpleSolrQuery() {

		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
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
		Query query = new SimpleQuery(new Criteria("field_1").is("value_1")).setPageRequest(PageRequest.of(page, pageSize));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
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
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionPresent(solrQuery, "projection_1,projection_2");
		assertGroupingNotPresent(solrQuery);
		assertFactingNotPresent(solrQuery);
	}

	@Test
	public void testConstructSolrQueryWithSingleGroupBy() {

		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"))
				.setGroupOptions(new GroupOptions().addGroupByField("group_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
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
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
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
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
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
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
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
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
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

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingPresent(solrQuery, "facet_1", "facet_2");
		assertThat(solrQuery.getParams("facet.prefix")[0]).isEqualTo(facetOptions.getFacetPrefix());
	}

	@Test
	public void testConstructSolrQueryWithFieldFacetParameters() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"));
		FieldWithFacetParameters fieldWithFacetParameters = new FieldWithFacetParameters("facet_2").setPrefix("prefix")
				.setSort(FacetSort.INDEX).setLimit(3).setOffset(2).setMethod("method").setMissing(true);
		FacetOptions facetOptions = new FacetOptions(new SimpleField("facet_1"), fieldWithFacetParameters);
		query.setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingPresent(solrQuery, "facet_1", "facet_2");
		assertThat(solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.prefix")[0])
				.isEqualTo(fieldWithFacetParameters.getPrefix());
		assertThat(solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.sort")[0])
				.isEqualTo(FacetParams.FACET_SORT_INDEX);
		assertThat(solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.offset")[0])
				.isEqualTo(Integer.toString(fieldWithFacetParameters.getOffset()));
		assertThat(solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.limit")[0])
				.isEqualTo(Integer.toString(fieldWithFacetParameters.getLimit()));
		assertThat(solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.method")[0])
				.isEqualTo(fieldWithFacetParameters.getMethod());
		assertThat(solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.missing")[0])
				.isEqualTo(fieldWithFacetParameters.getMissing().toString());
	}

	@Test
	public void testConstructSolrQueryWithCustomFieldFacetParameters() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"));
		FieldWithFacetParameters fieldWithFacetParameters = new FieldWithFacetParameters("facet_2")
				.addFacetParameter(new FacetParameter(FacetParams.FACET_ZEROS, "on"));
		FacetOptions facetOptions = new FacetOptions(new SimpleField("facet_1"), fieldWithFacetParameters);
		query.setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getParams("f." + fieldWithFacetParameters.getName() + ".facet.zeros")[0]).isEqualTo("on");
	}

	@Test
	public void testConstructSolrQueryWithFacetSort() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"))
				.setFacetOptions(new FacetOptions("facet_1").setFacetSort(FacetOptions.FacetSort.INDEX));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getFacetSortString()).isEqualTo("index");

		query.getFacetOptions().setFacetSort(FacetOptions.FacetSort.COUNT);
		solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getFacetSortString()).isEqualTo("count");
	}

	@Test
	public void testConstructSolrQueryWithSingleFacetFilterQuery() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1")).setFacetOptions(
				new FacetOptions().addFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_2:[* TO 5]"))));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertThat(solrQuery.getFacetQuery()).isEqualTo(new String[] { "field_2:[* TO 5]" });
	}

	@Test
	public void testConstructSolrQueryWithMultipleFacetFilterQuerues() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"))
				.setFacetOptions(new FacetOptions().addFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_2:[* TO 5]")))
						.addFacetQuery(new SimpleQuery(new Criteria("field_3").startsWith("prefix"))));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertThat(solrQuery.getFacetQuery()).isEqualTo(new String[] { "field_2:[* TO 5]", "field_3:prefix*" });
	}

	@Test
	public void testWithFilterQuery() {

		Query query = new SimpleQuery(new Criteria("field_1").is("value_1"))
				.addFilterQuery(new SimpleFilterQuery(new Criteria("filter_field").is("filter_value")));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		String[] filterQueries = solrQuery.getFilterQueries();
		assertThat(filterQueries.length).isEqualTo(1);
		assertThat(filterQueries[0]).isEqualTo("filter_field:filter_value");
	}

	@Test
	public void testWithEmptyFilterQuery() {

		Query query = new SimpleQuery(new Criteria("field_1").is("value_1")).addFilterQuery(new SimpleQuery());
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		assertThat(solrQuery.getFilterQueries()).isNull();
	}

	@Test
	public void testWithSimpleStringCriteria() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery).isNotNull();
		assertQueryStringPresent(solrQuery);
		assertPaginationNotPresent(solrQuery);
		assertProjectionNotPresent(solrQuery);
		assertGroupingNotPresent(solrQuery);
		assertFactingNotPresent(solrQuery);

		assertThat(solrQuery.getQuery()).isEqualTo(criteria.getQueryString());
	}

	@Test
	public void testWithNullSort() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(null); // do this explicitly

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getSortField()).isNull();
		assertThat(solrQuery.getSorts().isEmpty()).isTrue();
	}

	@Test
	public void testWithSortAscOnSingleField() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(Sort.by("field_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getSortField()).isEqualTo("field_2 asc");
		assertThat(solrQuery.getSorts().size()).isEqualTo(1);
	}

	@Test
	public void testWithSortDescOnSingleField() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(Sort.by(Sort.Direction.DESC, "field_2"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getSortField()).isEqualTo("field_2 desc");
		assertThat(solrQuery.getSorts().size()).isEqualTo(1);
	}

	@Test
	public void testWithSortAscMultipleFields() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(Sort.by("field_2", "field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getSortField()).isEqualTo("field_2 asc,field_3 asc");
		assertThat(solrQuery.getSorts().size()).isEqualTo(2);
	}

	@Test
	public void testWithSortDescMultipleFields() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(Sort.by(Sort.Direction.DESC, "field_2", "field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getSortField()).isEqualTo("field_2 desc,field_3 desc");
		assertThat(solrQuery.getSorts().size()).isEqualTo(2);
	}

	@Test
	public void testWithSortMixedDirections() {

		SimpleStringCriteria criteria = new SimpleStringCriteria("field_1:value_1");
		Query query = new SimpleQuery(criteria);
		query.addSort(Sort.by("field_1"));
		query.addSort(Sort.by(Sort.Direction.DESC, "field_2", "field_3"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getSortField()).isEqualTo("field_1 asc,field_2 desc,field_3 desc");
		assertThat(solrQuery.getSorts().size()).isEqualTo(3);
	}

	@Test
	public void testWithORDefaultOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefaultOperator(Operator.OR);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.get("q.op")).isEqualTo("OR");
	}

	@Test
	public void testWithANDDefaultOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefaultOperator(Operator.AND);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.get("q.op")).isEqualTo("AND");
	}

	@Test
	public void testWithNONEDefaultOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefaultOperator(Operator.NONE);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.get("q.op")).isNull();
	}

	@Test
	public void testWithoutDefaultOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.get("q.op")).isNull();
	}

	@Test
	public void testWithNullDefaultOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefaultOperator(null);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.get("q.op")).isNull();
	}

	@Test
	public void testWithTimeAllowed() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setTimeAllowed(100);
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getTimeAllowed()).isEqualTo(Integer.valueOf(100));
	}

	@Test
	public void testWithoutTimeAllowed() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getTimeAllowed()).isNull();
	}

	@Test
	public void testWithLuceneDefType() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefType("lucene");
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.get("defType")).isNotNull();
	}

	@Test
	public void testWithEdismaxDefType() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setDefType("edismax");
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.get("defType")).isNotNull();
	}

	@Test
	public void testWithUndefindedDefType() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.get("defType")).isNull();
	}

	@Test
	public void testWithFooRequestHandler() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setRequestHandler("/foo");
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.get("qt")).isNotNull();
	}

	@Test
	public void testWithUndefinedRequestHandler() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.get("qt")).isNull();
	}

	@Test
	public void testWithJoinOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setJoin(Join.from("inner_id").to("outer_id"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getQuery()).isEqualTo("{!join from=inner_id to=outer_id}field_1:value_1");
	}

	@Test // DATASOLR-176
	public void testWithJoinTwoCoresOperator() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setJoin(Join.from("inner_id").fromIndex("sourceIndex").to("outer_id"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getQuery())
				.isEqualTo("{!join from=inner_id to=outer_id fromIndex=sourceIndex}field_1:value_1");
	}

	@Test
	public void testConstructSolrQueryWithEmptyHighlightOption() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setHighlightOptions(new HighlightOptions());

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getHighlight()).isTrue();
		assertThat(solrQuery.getHighlightFields()).isEqualTo(new String[] { Criteria.WILDCARD });
	}

	@Test
	public void testConstructSolrQueryWithoutHighlightOption() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getHighlight()).isFalse();
	}

	@Test
	public void testConstructSolrQueryWithHighlightOptionHavingFields() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.addField("field_2", "field_3");
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getHighlightFields()).isEqualTo(new String[] { "field_2", "field_3" });
	}

	@Test
	public void testConstructSorlQueryWithHighlightOptionFragsize() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.setFragsize(10);
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getHighlightFragsize()).isEqualTo(options.getFragsize().intValue());
	}

	@Test
	public void testConstructSorlQueryWithHighlightOptionFormatter() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.setFormatter("formatter");
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getParams(HighlightParams.FORMATTER)[0]).isEqualTo(options.getFormatter());
	}

	@Test
	public void testConstructSorlQueryWithHighlightOptionNrSnipplets() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.setNrSnipplets(10);
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getHighlightSnippets()).isEqualTo(options.getNrSnipplets().intValue());
	}

	@Test
	public void testConstructSorlQueryWithHighlightOptionsAndAnySolrParameter() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("field_1:value_1"));
		HighlightOptions options = new HighlightOptions();
		options.addHighlightParameter(HighlightParams.SIMPLE_PRE, "{pre}");
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getHighlightSimplePre())
				.isEqualTo(options.<String> getHighlightParameterValue(HighlightParams.SIMPLE_PRE));
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

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);
		assertThat(solrQuery.getHighlightFields()).isEqualTo(new String[] { "field_2" });
		assertThat(solrQuery.getParams("f.field_2." + HighlightParams.FORMATTER)[0])
				.isEqualTo(fieldWithHighlightParameters.getFormatter());
		assertThat(solrQuery.getParams("f.field_2." + HighlightParams.FRAGSIZE)[0])
				.isEqualTo(fieldWithHighlightParameters.getFragsize().toString());
	}

	@Test // DATASOLR-105
	public void testNestedOrPartWithAnd() {

		Criteria criteria = Criteria.where("field_1").is("foo")
				.and(Criteria.where("field_2").is("bar").or("field_3").is("roo"))//
				.or(Criteria.where("field_4").is("spring").and("field_5").is("data"));

		assertThat(queryParser.createQueryStringFromNode(criteria, null))
				.isEqualTo("field_1:foo AND (field_2:bar OR field_3:roo) OR (field_4:spring AND field_5:data)");
	}

	@Test // DATASOLR-105
	public void testNestedOrPartWithAndSomeOtherThings() {

		Criteria criteria = Criteria.where("field_1").is("foo").is("bar")
				.and(Criteria.where("field_2").is("bar").is("lala").or("field_3").is("roo"))
				.or(Criteria.where("field_4").is("spring").and("field_5").is("data"));

		assertThat(queryParser.createQueryStringFromNode(criteria, null))
				.isEqualTo("field_1:(foo bar) AND (field_2:(bar lala) OR field_3:roo) OR (field_4:spring AND field_5:data)");
	}

	@Test // DATASOLR-105
	public void testMultipleAnd() {
		Criteria criteria = Criteria.where("field_1").is("foo").and("field_2").is("bar").and("field_3").is("roo");

		assertThat(queryParser.createQueryStringFromNode(criteria, null))
				.isEqualTo("field_1:foo AND field_2:bar AND field_3:roo");
	}

	@Test // DATASOLR-105
	public void testMultipleOr() {
		Criteria criteria = Criteria.where("field_1").is("foo").or("field_2").is("bar").or("field_3").is("roo");

		assertThat(queryParser.createQueryStringFromNode(criteria, null))
				.isEqualTo("field_1:foo OR field_2:bar OR field_3:roo");
	}

	@Test // DATASOLR-105
	public void testEmptyCriteriaShouldBeDefaultedToNotNUll() {
		Criteria criteria = Criteria.where("field_1").is("foo").and("field_2").or("field_3");

		assertThat(queryParser.createQueryStringFromNode(criteria, null))
				.isEqualTo("field_1:foo AND field_2:[* TO *] OR field_3:[* TO *]");
	}

	@Test // DATASOLR-105
	public void testDeepNesting() {

		Criteria criteria = Criteria.where("field_1").is("foo")
				.and(Criteria.where("field_2").is("bar").and("field_3").is("roo")//
						.and(Criteria.where("field_4").is("spring").and("field_5").is("data").or("field_6").is("solr")));

		assertThat(queryParser.createQueryStringFromNode(criteria, null)).isEqualTo(
				"field_1:foo AND (field_2:bar AND field_3:roo AND (field_4:spring AND field_5:data OR field_6:solr))");
	}

	@Test // DATASOLR-168
	public void testNotCritieraCarriedOnPorperlyForNullAndNotNull() {

		Criteria criteria = new Criteria("param1").isNotNull().and("param2").isNull();
		assertThat(queryParser.createQueryStringFromNode(criteria, null)).isEqualTo("param1:[* TO *] AND -param2:[* TO *]");
	}

	@Test // DATASOLR-112
	public void pageableUsingZeroShouldBeParsedCorrectlyWhenSetUsingPageable() {

		SimpleQuery query = new SimpleQuery("*:*").setPageRequest(new SolrPageRequest(0, 0));
		assertPaginationPresent(queryParser.constructSolrQuery(query, null), 0, 0);
	}

	@Test // DATASOLR-112
	public void pageableUsingZeroShouldBeParsedCorrectlyWhenSetUsingExplititMethods() {

		SimpleQuery query = new SimpleQuery("*:*").setOffset(0L).setRows(0);
		assertPaginationPresent(queryParser.constructSolrQuery(query, null), 0, 0);
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithStatField() {

		StatsOptions statsOptions = new StatsOptions().addField(new SimpleField("field_1"));

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		assertThat(solrQuery.get(StatsParams.STATS_FIELD)).isEqualTo("field_1");
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithStatFields() {

		StatsOptions statsOptions = new StatsOptions()//
				.addField(new SimpleField("field_1"))//
				.addField(new SimpleField("field_2"));

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		List<String> fields = Arrays.asList(solrQuery.getParams(StatsParams.STATS_FIELD));
		Collections.sort(fields);
		assertThat(fields.size()).isEqualTo(2);
		assertThat(fields).isEqualTo(Arrays.asList("field_1", "field_2"));
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithStatFacets() {

		StatsOptions statsOptions = new StatsOptions()//
				.addFacet(new SimpleField("field_1"))//
				.addFacet(new SimpleField("field_2"));

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		List<String> facets = Arrays.asList(solrQuery.getParams(StatsParams.STATS_FACET));
		Collections.sort(facets);
		assertThat(facets.size()).isEqualTo(2);
		assertThat(facets).isEqualTo(Arrays.asList("field_1", "field_2"));
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithStatFieldsAndFacets() {

		StatsOptions statsOptions = new StatsOptions()//
				.addField(new SimpleField("field_1"))//
				.addFacet(new SimpleField("field_2"));

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		String[] fields = solrQuery.getParams(StatsParams.STATS_FIELD);
		String[] facets = solrQuery.getParams(StatsParams.STATS_FACET);

		assertThat(fields.length).isEqualTo(1);
		assertThat(facets.length).isEqualTo(1);
		assertThat(fields[0]).isEqualTo("field_1");
		assertThat(facets[0]).isEqualTo("field_2");
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithSelectiveStatsFacet() {

		StatsOptions statsOptions = new StatsOptions()//
				.addField(new SimpleField("field_1"))//
				.addSelectiveFacet(new SimpleField("field_2"));

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		String[] fields = solrQuery.getParams(StatsParams.STATS_FIELD);
		String[] facets = solrQuery.getParams(CommonParams.FIELD + ".field_1." + StatsParams.STATS_FACET);

		assertThat(fields.length).isEqualTo(1);
		assertThat(facets.length).isEqualTo(1);
		assertThat(fields[0]).isEqualTo("field_1");
		assertThat(facets[0]).isEqualTo("field_2");
	}

	@Test // DATASOLR-160
	public void testConstructSolrQueryWithSelectiveStatsCountDistinct() {

		StatsOptions statsOptions = new StatsOptions()//
				.addField(new SimpleField("field_1")).setSelectiveCalcDistinct(true) //
				.addField(new SimpleField("field_2")).setSelectiveCalcDistinct(false) //
				.addField(new SimpleField("field_3"));

		SimpleQuery query = new SimpleQuery("*:*");
		query.setStatsOptions(statsOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		String[] fields = solrQuery.getParams(StatsParams.STATS_FIELD);
		String[] calc1 = solrQuery.getParams(CommonParams.FIELD + ".field_1." + StatsParams.STATS_CALC_DISTINCT);
		String[] calc2 = solrQuery.getParams(CommonParams.FIELD + ".field_2." + StatsParams.STATS_CALC_DISTINCT);
		String[] calc3 = solrQuery.getParams(CommonParams.FIELD + ".field_3." + StatsParams.STATS_CALC_DISTINCT);

		Arrays.sort(fields);

		assertThat(fields.length).isEqualTo(3);
		assertThat(fields).isEqualTo(new String[] { "field_1", "field_2", "field_3" });
		assertThat(calc1[0]).isEqualTo("true");
		assertThat(calc2[0]).isEqualTo("false");
		assertThat(calc3).isNull();
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

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		List<String> fields = Arrays.asList(solrQuery.getParams(StatsParams.STATS_FIELD));
		Collections.sort(fields);
		List<String> selectiveFacets = Arrays
				.asList(solrQuery.getParams(CommonParams.FIELD + ".field_1." + StatsParams.STATS_FACET));
		String[] facets = solrQuery.getParams(StatsParams.STATS_FACET);

		assertThat(fields.size()).isEqualTo(2);
		assertThat(selectiveFacets.size()).isEqualTo(2);
		assertThat(fields.get(0)).isEqualTo("field_1");
		assertThat(fields.get(1)).isEqualTo("field_2");
		assertThat(selectiveFacets.get(0)).isEqualTo("field_1_1");
		assertThat(selectiveFacets.get(1)).isEqualTo("field_1_2");
		assertThat(facets[0]).isEqualTo("field_3");
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
		groupOptions.addSort(Sort.by(Sort.Direction.DESC, "field_3"));
		groupOptions.setTotalCount(true);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		assertGroupFormatPresent(solrQuery, true);
		assertThat(solrQuery.get(GroupParams.GROUP_FIELD)).isEqualTo("field_1");
		assertThat(solrQuery.get(GroupParams.GROUP_FUNC)).isEqualTo("{!func}max(field_1,field_2)");
		assertThat(solrQuery.get(GroupParams.GROUP_QUERY)).isEqualTo("*:*");
		assertThat(solrQuery.get(GroupParams.GROUP_SORT)).isEqualTo("field_3 desc");
		assertThat(solrQuery.get(GroupParams.GROUP_OFFSET)).isEqualTo("1");
		assertThat(solrQuery.get(GroupParams.GROUP_LIMIT)).isEqualTo("2");
	}

	@Test // DATASOLR-310
	public void testConstructGroupQueryWithLimitSetToNegative1() {

		GroupOptions groupOptions = new GroupOptions();

		SimpleQuery query = new SimpleQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.setGroupOptions(groupOptions);
		groupOptions.setLimit(-1);
		groupOptions.addGroupByField("field_1");
		groupOptions.addSort(Sort.by(Sort.Direction.DESC, "field_3"));
		groupOptions.setTotalCount(true);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		assertGroupFormatPresent(solrQuery, true);
		assertThat(solrQuery.get(GroupParams.GROUP_FIELD)).isEqualTo("field_1");
		assertThat(solrQuery.get(GroupParams.GROUP_LIMIT)).isEqualTo("-1");
	}

	@Test // DATASOLR-121
	public void testConstructGroupQueryWithoutPagingParameters() {

		SimpleQuery query = new SimpleQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.setGroupOptions(new GroupOptions().addGroupByField("fieldName"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		assertGroupFormatPresent(solrQuery, false);
		assertThat(solrQuery.get(GroupParams.GROUP_SORT)).isNull();
		assertThat(solrQuery.get(GroupParams.GROUP_OFFSET)).isNull();
		assertThat(solrQuery.get(GroupParams.GROUP_LIMIT)).isNull();
	}

	@Test // DATASOLR-121
	public void testConstructGroupQueryWithMultipleFunctions() {

		SimpleQuery query = new SimpleQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.setGroupOptions(new GroupOptions());
		query.getGroupOptions().addGroupByFunction(MaxFunction.max("field_1", "field_2"));
		query.getGroupOptions().addGroupByFunction(MaxFunction.max("field_3", "field_4"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		assertGroupFormatPresent(solrQuery, false);
		assertThat(solrQuery.getParams(GroupParams.GROUP_FUNC))
				.isEqualTo(new String[] { "{!func}max(field_1,field_2)", "{!func}max(field_3,field_4)" });
		assertThat(solrQuery.getParams(GroupParams.GROUP_QUERY)).isNull();
		assertThat(solrQuery.getParams(GroupParams.GROUP_FIELD)).isNull();
	}

	@Test // DATASOLR-121
	public void testConstructGroupQueryWithMultipleQueries() {

		SimpleQuery query = new SimpleQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.setGroupOptions(new GroupOptions());
		query.getGroupOptions().addGroupByQuery(new SimpleQuery("query1"));
		query.getGroupOptions().addGroupByQuery(new SimpleQuery("query2"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		assertGroupFormatPresent(solrQuery, false);
		assertThat(solrQuery.getParams(GroupParams.GROUP_QUERY)).isEqualTo(new String[] { "query1", "query2" });
		assertThat(solrQuery.getParams(GroupParams.GROUP_FUNC)).isNull();
		assertThat(solrQuery.getParams(GroupParams.GROUP_FIELD)).isNull();
	}

	@Test // DATASOLR-196
	public void connectShouldAllowConcatinationOfCriteriaWithAndPreservingDesiredBracketing() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar");
		Criteria criteria = part1.connect().and(part2);

		assertThat(queryParser.createQueryStringFromNode(criteria, null)).isEqualTo("z:roo AND (x:foo OR y:bar)");
	}

	@Test // DATASOLR-196
	public void connectShouldAllowConcatinationOfCriteriaWithAndPreservingDesiredBracketingReverse() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar");
		Criteria criteria = part2.connect().and(part1);

		assertThat(queryParser.createQueryStringFromNode(criteria, null)).isEqualTo("(x:foo OR y:bar) AND z:roo");
	}

	@Test // DATASOLR-196
	public void connectShouldAllowConcatinationOfCriteriaWithOrPreservingDesiredBracketing() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar");
		Criteria criteria = part1.connect().or(part2);

		assertThat(queryParser.createQueryStringFromNode(criteria, null)).isEqualTo("z:roo OR (x:foo OR y:bar)");
	}

	@Test // DATASOLR-196
	public void connectShouldAllowConcatinationOfCriteriaWithOrPreservingDesiredBracketingReverse() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar");
		Criteria criteria = part2.connect().or(part1);

		assertThat(queryParser.createQueryStringFromNode(criteria, null)).isEqualTo("(x:foo OR y:bar) OR z:roo");
	}

	@Test // DATASOLR-196
	public void notOperatorShouldWrapWholeExpression() {

		Criteria part1 = Criteria.where("text").startsWith("fx").or("product_code").startsWith("fx");
		Criteria part2 = Criteria.where("text").startsWith("option").or("product_code").startsWith("option");
		Criteria criteria = part1.connect().and(part2).notOperator();

		String expected = "-((text:fx* OR product_code:fx*) AND (text:option* OR product_code:option*))";
		assertThat(queryParser.createQueryStringFromNode(criteria, null)).isEqualTo(expected);
	}

	@Test // DATASOLR-196
	public void notOperatorShouldWrapNestedExpressionCorrectly() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar").notOperator();

		Criteria criteria = part1.connect().or(part2);

		assertThat(queryParser.createQueryStringFromNode(criteria, null)).isEqualTo("z:roo OR -(x:foo OR y:bar)");
	}

	@Test // DATASOLR-196
	public void notOperatorShouldWrapNestedExpressionCorrectlyReverse() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar").notOperator();

		Criteria criteria = part2.connect().or(part1);

		assertThat(queryParser.createQueryStringFromNode(criteria, null)).isEqualTo("-(x:foo OR y:bar) OR z:roo");
	}

	@Test // DATASOLR-196
	public void notOperatorShouldWrapNestedExpressionCorrectlyReverseWithDoubleNegation() {

		Criteria part1 = Criteria.where("z").is("roo");
		Criteria part2 = Criteria.where("x").is("foo").or("y").is("bar").notOperator();

		Criteria criteria = part2.connect().and(part1).notOperator();

		assertThat(queryParser.createQueryStringFromNode(criteria, null)).isEqualTo("-(-(x:foo OR y:bar) AND z:roo)");
	}

	@Test // DATASOLR-236
	public void testNegativeFacetLimitUsingFacetOptions_setFacetLimit() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"));
		FacetOptions facetOptions = new FacetOptions(new SimpleField("facet_1"));
		facetOptions.setFacetLimit(-1);
		query.setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		assertThat(solrQuery.getFacetLimit()).isEqualTo(-1);
		assertThat(solrQuery.get(FacetParams.FACET_OFFSET)).isEqualTo(null);
	}

	@Test // DATASOLR-236
	public void testNegativeFacetLimitUsingFacetOptions_setPageable() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"));
		FacetOptions facetOptions = new FacetOptions(new SimpleField("facet_1"));
		facetOptions.setPageable(new SolrPageRequest(0, -1));
		query.setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		assertThat(solrQuery.getFacetLimit()).isEqualTo(-1);
		assertThat(solrQuery.get(FacetParams.FACET_OFFSET)).isEqualTo(null);
	}

	@Test // DATASOLR-236
	public void testNegativeFacetOffsetAndFacetLimitUsingFacetOptions_setPageable() {

		FacetQuery query = new SimpleFacetQuery(new Criteria("field_1").is("value_1"));
		FacetOptions facetOptions = new FacetOptions(new SimpleField("facet_1"));
		facetOptions.setPageable(new SolrPageRequest(1, -1));
		query.setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, null);

		assertThat(solrQuery.getFacetLimit()).isEqualTo(-1);
		assertThat(solrQuery.getInt(FacetParams.FACET_OFFSET)).isEqualTo(Integer.valueOf(0));
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

		SolrQuery solrQuery = queryParser.constructSolrQuery(facetQuery, null);

		assertThat(facetOptions.hasFacets()).isTrue();
		assertThat(solrQuery.getFacetFields()).isEqualTo(new String[] {});
		assertThat(solrQuery.getParams(FacetParams.FACET_RANGE))
				.isEqualTo(new String[] { "field1", "field2", "field3", "field4", "field5" });

		assertThat(solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_START)).isEqualTo("4");
		assertThat(solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_GAP)).isEqualTo("2");
		assertThat(solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_END)).isEqualTo("8");
		assertThat(solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_HARD_END)).isEqualTo("true");
		assertThat(solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_INCLUDE)).isEqualTo("all");
		assertThat(solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_OTHER)).isEqualTo("all");

		assertThat(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_START)).isEqualTo("0.5");
		assertThat(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_GAP)).isEqualTo("0.7");
		assertThat(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_END)).isEqualTo("12.3");
		assertThat(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_HARD_END)).isNull();
		assertThat(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_INCLUDE)).isEqualTo("outer");
		assertThat(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_OTHER)).isEqualTo("none");

		assertThat(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_START)).isEqualTo("4");
		assertThat(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_GAP)).isEqualTo("2");
		assertThat(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_END)).isEqualTo("8");
		assertThat(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_HARD_END)).isEqualTo("true");
		assertThat(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_INCLUDE)).as("all").isNull();
		assertThat(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_OTHER)).isEqualTo("all");

		assertThat(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_START)).isEqualTo("4");
		assertThat(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_GAP)).isEqualTo("2");
		assertThat(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_END)).isEqualTo("8");
		assertThat(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_HARD_END)).isEqualTo("true");
		assertThat(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_INCLUDE)).isEqualTo("outer");
		assertThat(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_OTHER)).as("all").isNull();

		assertThat(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_START)).isEqualTo("4");
		assertThat(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_GAP)).isEqualTo("2");
		assertThat(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_END)).isEqualTo("8");
		assertThat(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_HARD_END)).as("true").isNull();
		assertThat(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_INCLUDE)).as("all").isNull();
		assertThat(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_OTHER)).as("all").isNull();
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

		SolrQuery solrQuery = queryParser.constructSolrQuery(facetQuery, null);

		assertThat(facetOptions.hasFacets()).isTrue();
		assertThat(solrQuery.getFacetFields()).isEqualTo(new String[] {});
		assertThat(solrQuery.getParams(FacetParams.FACET_RANGE))
				.isEqualTo(new String[] { "field1", "field2", "field3", "field4", "field5" });

		// RANGE is being used on SolrJ even for DATE fields
		assertThat(solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_START)).isEqualTo("1970-01-01T00:00:00.100Z");
		assertThat(solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_GAP)).isEqualTo("+1DAY");
		assertThat(solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_END)).isEqualTo("1970-01-01T02:46:40Z");
		assertThat(solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_HARD_END)).isEqualTo("true");
		assertThat(solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_INCLUDE)).isEqualTo("all");
		assertThat(solrQuery.getFieldParam("field1", FacetParams.FACET_RANGE_OTHER)).isEqualTo("all");

		assertThat(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_START)).isEqualTo("1970-01-01T00:00:00.100Z");
		assertThat(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_GAP)).isEqualTo("+2DAY");
		assertThat(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_END)).isEqualTo("1970-01-01T02:46:40Z");
		assertThat(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_HARD_END)).isNull();
		assertThat(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_INCLUDE)).isEqualTo("outer");
		assertThat(solrQuery.getFieldParam("field2", FacetParams.FACET_RANGE_OTHER)).isEqualTo("none");

		assertThat(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_START)).isEqualTo("1970-01-01T00:00:00.100Z");
		assertThat(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_GAP)).isEqualTo("+2DAY");
		assertThat(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_END)).isEqualTo("1970-01-01T02:46:40Z");
		assertThat(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_HARD_END)).isEqualTo("true");
		assertThat(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_INCLUDE)).isNull();
		assertThat(solrQuery.getFieldParam("field3", FacetParams.FACET_RANGE_OTHER)).isEqualTo("none");

		assertThat(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_START)).isEqualTo("1970-01-01T00:00:00.100Z");
		assertThat(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_GAP)).isEqualTo("+2DAY");
		assertThat(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_END)).isEqualTo("1970-01-01T02:46:40Z");
		assertThat(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_HARD_END)).isEqualTo("true");
		assertThat(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_INCLUDE)).isEqualTo("outer");
		assertThat(solrQuery.getFieldParam("field4", FacetParams.FACET_RANGE_OTHER)).isNull();

		assertThat(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_START)).isEqualTo("1970-01-01T00:00:00.100Z");
		assertThat(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_GAP)).isEqualTo("+2DAY");
		assertThat(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_END)).isEqualTo("1970-01-01T02:46:40Z");
		assertThat(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_HARD_END)).isNull();
		assertThat(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_INCLUDE)).isNull();
		assertThat(solrQuery.getFieldParam("field5", FacetParams.FACET_RANGE_OTHER)).isNull();
	}

	@Test // DATASOLR-86
	public void testNoRangeFacetAssignmentWhenNoRangeFacetsPresent() {

		FacetOptions facetOptions = new FacetOptions("field1");
		SolrDataQuery facetQuery = new SimpleFacetQuery(new SimpleStringCriteria("*:*")).setFacetOptions(facetOptions);

		SolrQuery solrQuery = queryParser.constructSolrQuery(facetQuery, null);

		assertThat(facetOptions.hasFacets()).isTrue();
		assertThat(solrQuery.getFacetFields()).isEqualTo(new String[] { "field1" });
		assertThat(solrQuery.getParams(FacetParams.FACET_DATE)).isNull();
		assertThat(solrQuery.getParams(FacetParams.FACET_RANGE)).isNull();
	}

	@Test // DATASOLR-324
	public void shouldNotEnableSpellcheckWennNoSpellcheckOptionsPresent() {

		SimpleQuery q = new SimpleQuery(AnyCriteria.any());

		SolrQuery solrQuery = queryParser.constructSolrQuery(q, null);
		assertThat(solrQuery.get("spellcheck")).isNull();
	}

	@Test // DATASOLR-324
	public void shouldEnableSpellcheckWennSpellcheckOptionsPresent() {

		SimpleQuery q = new SimpleQuery(AnyCriteria.any());
		q.setSpellcheckOptions(SpellcheckOptions.spellcheck());

		SolrQuery solrQuery = queryParser.constructSolrQuery(q, null);
		assertThat(solrQuery.get("spellcheck")).isEqualTo("on");
	}

	@Test // DATASOLR-324
	public void shouldApplySpellcheckOptionsCorrectly() {

		SimpleQuery q = new SimpleQuery(AnyCriteria.any());
		q.setSpellcheckOptions(SpellcheckOptions.spellcheck().dictionaries("dict1", "dict2").count(5).extendedResults());

		SolrQuery solrQuery = queryParser.constructSolrQuery(q, null);
		assertThat(solrQuery.get("spellcheck")).isEqualTo("on");
		assertThat(solrQuery.getParams(SpellingParams.SPELLCHECK_DICT)).isEqualTo(new String[] { "dict1", "dict2" });
		assertThat(solrQuery.get(SpellingParams.SPELLCHECK_EXTENDED_RESULTS)).isEqualTo("true");
	}

	@Test // DATASOLR-466
	public void shouldMapQueryFieldsToFieldName() {

		SolrQuery solrQuery = queryParser.constructSolrQuery(new SimpleQuery(Criteria.where("renamedField").is("foo")),
				Sample.class);
		assertThat(solrQuery.getQuery()).isEqualTo("renamed-field:foo");
	}

	@Test // DATASOLR-466
	public void shouldMapFilterQueryFieldsToFieldName() {

		SolrQuery solrQuery = queryParser.constructSolrQuery(new SimpleQuery(AnyCriteria.any())
				.addFilterQuery(new SimpleFilterQuery(Criteria.where("renamedField").is("foo"))), Sample.class);
		assertThat(solrQuery.getFilterQueries()[0]).isEqualTo("renamed-field:foo");
	}

	@Test // DATASOLR-466
	public void shouldMapProjectionToFieldName() {

		SolrQuery solrQuery = queryParser
				.constructSolrQuery(new SimpleQuery(AnyCriteria.any()).addProjectionOnField("renamedField"), Sample.class);
		assertThat(solrQuery.getFields()).isEqualTo("renamed-field");
	}

	@Test // DATASOLR-466
	public void shouldMapGroupByToFieldName() {

		SolrQuery solrQuery = queryParser.constructSolrQuery(
				new SimpleQuery(AnyCriteria.any()).setGroupOptions(new GroupOptions().addGroupByField("renamedField")),
				Sample.class);
		assertGroupingPresent(solrQuery, "renamed-field");
	}

	@Test // DATASOLR-466
	public void shouldMapFacetFieldsToFieldName() {

		SolrQuery solrQuery = queryParser.constructSolrQuery(
				new SimpleFacetQuery(AnyCriteria.any()).setFacetOptions(new FacetOptions("renamedField")), Sample.class);
		assertFactingPresent(solrQuery, "renamed-field");
	}

	@Test // DATASOLR-466
	public void shouldMapFacetPivotFieldQueryToFieldName() {

		SolrQuery solrQuery = queryParser.constructSolrQuery(new SimpleFacetQuery(AnyCriteria.any())
				.setFacetOptions(new FacetOptions().addFacetOnPivot("field1", "renamedField")), Sample.class);
		assertPivotFactingPresent(solrQuery, "field1,renamed-field");
	}

	@Test // DATASOLR-466
	public void shouldMapFacetQueryToFieldName() {

		SolrQuery solrQuery = queryParser.constructSolrQuery(new SimpleFacetQuery(AnyCriteria.any()).setFacetOptions(
				new FacetOptions().addFacetQuery(new SimpleQuery(Criteria.where("renamedField").is("foo")))), Sample.class);
		assertThat("renamed-field:foo").isEqualTo(solrQuery.getFacetQuery()[0]);
	}

	@Test // DATASOLR-466
	public void shouldMapSortToFieldName() {

		SolrQuery solrQuery = queryParser
				.constructSolrQuery(new SimpleQuery(AnyCriteria.any()).addSort(Sort.by("renamedField")), Sample.class);
		assertThat(solrQuery.getSortField()).isEqualTo("renamed-field asc");
	}

	@Test // DATASOLR-466
	public void shouldMapGeofilterToFieldName() {

		Criteria criteria = new Criteria("renamedField").within(new Point(48.303056, 14.290556),
				new Distance(1, Metrics.KILOMETERS));
		assertThat(queryParser.createQueryStringFromCriteria(criteria, Sample.class))
				.isEqualTo("{!geofilt pt=48.303056,14.290556 sfield=renamed-field d=1.0 score=kilometers}");
	}

	@Test // DATASOLR-466
	public void shouldMapJoinOperatorToFieldName() {

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("field_1:value_1"));
		query.setJoin(Join.from("field1").to("renamedField"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, Sample.class);
		assertThat(solrQuery.getQuery()).isEqualTo("{!join from=field1 to=renamed-field}field_1:value_1");
	}

	@Test // DATASOLR-466
	public void shouldMapHighlightToFieldName() {

		SimpleHighlightQuery query = new SimpleHighlightQuery(AnyCriteria.any());
		HighlightOptions options = new HighlightOptions();
		options.addField("field1", "renamedField");
		query.setHighlightOptions(options);

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, Sample.class);
		assertThat(solrQuery.getHighlightFields()).isEqualTo(new String[] { "field1", "renamed-field" });
	}

	@Test // DATASOLR-466
	public void shouldMapGroupSortFieldsToFieldName() {

		GroupOptions groupOptions = new GroupOptions();

		SimpleQuery query = new SimpleQuery();
		query.addCriteria(AnyCriteria.any());
		query.setGroupOptions(groupOptions);
		groupOptions.addGroupByField("field1");
		groupOptions.addSort(Sort.by(Sort.Direction.DESC, "renamedField"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, Sample.class);
		assertThat(solrQuery.get(GroupParams.GROUP_SORT)).isEqualTo("renamed-field desc");
	}

	@Test // DATASOLR-466
	public void shouldMapGroupByFunctionToFieldName() {

		GroupOptions groupOptions = new GroupOptions();

		SimpleQuery query = new SimpleQuery();
		query.addCriteria(new SimpleStringCriteria("*:*"));
		query.setGroupOptions(groupOptions);
		groupOptions.addGroupByField("field1");
		groupOptions.addGroupByFunction(MaxFunction.max("field1", "renamedField"));

		SolrQuery solrQuery = queryParser.constructSolrQuery(query, Sample.class);
		assertThat(solrQuery.get(GroupParams.GROUP_FUNC)).isEqualTo("{!func}max(field1,renamed-field)");
	}

	@Test // DATASOLR-510
	public void doesNotAddScoreForNeutralDistance() {

		Criteria criteria = new Criteria("field_1")
				.within(new Circle(new Point(48.303056, 14.290556), new Distance(1, Metrics.NEUTRAL)));
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.0}");
	}

	@Test // DATASOLR-510
	public void usesCustomDistanceForScore() {

		Criteria criteria = new Criteria("field_1")
				.within(new Circle(new Point(48.303056, 14.290556), new Distance(1, new Metric() {
					@Override
					public double getMultiplier() {
						return 1;
					}

					@Override
					public String getAbbreviation() {
						return "distance";
					}
				})));
		assertThat(queryParser.createQueryStringFromCriteria(criteria, null))
				.isEqualTo("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=1.0 score=distance}");
	}

	private void assertPivotFactingPresent(SolrQuery solrQuery, String... expected) {
		assertThat(solrQuery.getParams(FacetParams.FACET_PIVOT)).isEqualTo(expected);
	}

	private void assertFactingPresent(SolrQuery solrQuery, String... expected) {
		assertThat(solrQuery.getFacetFields()).isEqualTo(expected);
	}

	private void assertFactingNotPresent(SolrQuery solrQuery) {
		assertThat(solrQuery.get(FacetParams.FACET_FIELD)).isNull();
	}

	private void assertQueryStringPresent(SolrQuery solrQuery) {
		assertThat(solrQuery.get(CommonParams.Q)).isNotNull();
	}

	private void assertProjectionNotPresent(SolrQuery solrQuery) {
		assertThat(solrQuery.getFields()).isNull();
	}

	private void assertProjectionPresent(SolrQuery solrQuery, String expected) {

		assertThat(solrQuery.get(CommonParams.FL)).isNotNull();
		assertThat(solrQuery.get(CommonParams.FL)).isEqualTo(expected);
	}

	private void assertPaginationNotPresent(SolrQuery solrQuery) {

		assertThat(solrQuery.getStart()).isNull();
		assertThat(solrQuery.getRows()).isNull();
	}

	private void assertPaginationPresent(SolrQuery solrQuery, int start, int rows) {

		assertThat(solrQuery.getStart()).isEqualTo(Integer.valueOf(start));
		assertThat(solrQuery.getRows()).isEqualTo(Integer.valueOf(rows));
	}

	private void assertGroupingNotPresent(SolrQuery solrQuery) {

		assertThat(solrQuery.get(GroupParams.GROUP)).isNull();
		assertThat(solrQuery.get(GroupParams.GROUP_FIELD)).isNull();
		assertThat(solrQuery.get(GroupParams.GROUP_MAIN)).isNull();
	}

	private void assertGroupingPresent(SolrQuery solrQuery, String expected) {

		assertThat(solrQuery.get(GroupParams.GROUP)).isNotNull();
		assertThat(solrQuery.get(GroupParams.GROUP_FIELD)).isNotNull();
		assertThat(solrQuery.get(GroupParams.GROUP_MAIN)).isNotNull();
		assertThat(solrQuery.get(GroupParams.GROUP_FIELD)).isEqualTo(expected);
	}

	private void assertGroupFormatPresent(SolrQuery solrQuery, boolean groupTotalCount) {

		assertThat(solrQuery.get(GroupParams.GROUP)).isEqualTo("true");
		assertThat(solrQuery.get(GroupParams.GROUP_MAIN)).isEqualTo("false");
		assertThat(solrQuery.get(GroupParams.GROUP_FORMAT)).isEqualTo("grouped");
		assertThat(solrQuery.get(GroupParams.GROUP_TOTAL_COUNT)).isEqualTo(String.valueOf(groupTotalCount));
	}

	@SolrDocument
	static class Sample {

		@Id String id;
		String field1;

		@Field(value = "renamed-field") //
		String renamedField;
	}
}
