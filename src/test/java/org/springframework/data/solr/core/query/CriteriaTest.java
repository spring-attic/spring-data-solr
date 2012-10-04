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
package org.springframework.data.solr.core.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.GeoLocation;

/**
 * @author Christoph Strobl
 */
public class CriteriaTest {

	@Test(expected = IllegalArgumentException.class)
	public void testCriteriaForNullString() {
		new Criteria((String) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCriteriaForNullField() {
		new Criteria((Field) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCriteriaForNullFieldName() {
		new Criteria(new SimpleField(StringUtils.EMPTY));
	}

	@Test
	public void testIs() {
		Criteria criteria = new Criteria("field_1").is("is");
		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:is", criteria.createQueryString());
	}

	@Test
	public void testMultipleIs() {
		Criteria criteria = new Criteria("field_1").is("is").is("another is");
		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:(is \"another is\")", criteria.createQueryString());
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testContainsWithBlank() {
		new Criteria("field_1").contains("no blank");
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testStartsWithBlank() {
		new Criteria("field_1").startsWith("no blank");
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testEndsWithBlank() {
		new Criteria("field_1").endsWith("no blank");
	}

	@Test
	public void testEndsWith() {
		Criteria criteria = new Criteria("field_1").endsWith("end");

		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:*end", criteria.createQueryString());
	}

	@Test
	public void testStartsWith() {
		Criteria criteria = new Criteria("field_1").startsWith("start");

		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:start*", criteria.createQueryString());
	}

	@Test
	public void testContains() {
		Criteria criteria = new Criteria("field_1").contains("contains");

		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:*contains*", criteria.createQueryString());
	}

	@Test
	public void testExpression() {
		Criteria criteria = new Criteria("field_1").expression("(have fun using +solr && expressions*)");
		Assert.assertEquals("field_1:(have fun using +solr && expressions*)", criteria.createQueryString());
	}

	@Test
	public void testCriteriaChain() {
		Criteria criteria = new Criteria("field_1").startsWith("start").endsWith("end").contains("contains").is("is");
		Assert.assertEquals("field_1", criteria.getField().getName());
		Assert.assertEquals("field_1:(start* *end *contains* is)", criteria.createQueryString());
	}

	@Test
	public void testAnd() {
		Criteria criteria = new Criteria("field_1").startsWith("start").endsWith("end").and("field_2").startsWith("2start")
				.endsWith("2end");
		Assert.assertEquals("field_2", criteria.getField().getName());
		Assert.assertEquals("field_1:(start* *end) AND field_2:(2start* *2end)", criteria.createQueryString());
	}

	@Test
	public void testOr() {
		Criteria criteria = new Criteria("field_1").startsWith("start").or("field_2").endsWith("end").startsWith("start2");
		Assert.assertEquals("field_1:start* OR field_2:(*end start2*)", criteria.createQueryString());
	}

	@Test
	public void testOrWithCriteria() {
		Criteria criteria = new Criteria("field_1").startsWith("start");
		Criteria orCriteria = new Criteria("field_2").endsWith("end").startsWith("start2");
		criteria = criteria.or(orCriteria);
		Assert.assertEquals("field_1:start* OR field_2:(*end start2*)", criteria.createQueryString());
	}

	@Test
	public void testCriteriaWithWhiteSpace() {
		Criteria criteria = new Criteria("field_1").is("white space");
		Assert.assertEquals("field_1:\"white space\"", criteria.createQueryString());
	}

	@Test
	public void testCriteriaWithDoubleQuotes() {
		Criteria criteria = new Criteria("field_1").is("with \"quote");
		Assert.assertEquals("field_1:\"with \\\"quote\"", criteria.createQueryString());
	}

	@Test
	public void testIsNot() {
		Criteria criteria = new Criteria("field_1").is("value_1").not();
		Assert.assertEquals("-field_1:value_1", criteria.createQueryString());
	}

	@Test
	public void testFuzzy() {
		Criteria criteria = new Criteria("field_1").fuzzy("value_1");
		Assert.assertEquals("field_1:value_1~", criteria.createQueryString());
	}

	@Test
	public void testFuzzyWithDistance() {
		Criteria criteria = new Criteria("field_1").fuzzy("value_1", 0.5f);
		Assert.assertEquals("field_1:value_1~0.5", criteria.createQueryString());
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testFuzzyWithNegativeDistance() {
		new Criteria("field_1").fuzzy("value_1", -0.5f);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testFuzzyWithTooHighDistance() {
		new Criteria("field_1").fuzzy("value_1", 1.5f);
	}

	@Test
	public void testBoost() {
		Criteria criteria = new Criteria("field_1").is("value_1").boost(2f);
		Assert.assertEquals("field_1:value_1^2.0", criteria.createQueryString());
	}

	@Test
	public void testBoostMultipleValues() {
		Criteria criteria = new Criteria("field_1").is("value_1").is("value_2").boost(2f);
		Assert.assertEquals("field_1:(value_1 value_2)^2.0", criteria.createQueryString());
	}

	@Test
	public void testBoostMultipleCriteriasValues() {
		Criteria criteria = new Criteria("field_1").is("value_1").is("value_2").boost(2f).and("field_3").is("value_3");
		Assert.assertEquals("field_1:(value_1 value_2)^2.0 AND field_3:value_3", criteria.createQueryString());
	}

	@Test
	public void testBetween() {
		Criteria criteria = new Criteria("field_1").between(100, 200);
		Assert.assertEquals("field_1:[100 TO 200]", criteria.createQueryString());
	}

	@Test
	public void testBetweenWithoutUpperBound() {
		Criteria criteria = new Criteria("field_1").between(100, null);
		Assert.assertEquals("field_1:[100 TO *]", criteria.createQueryString());
	}

	@Test
	public void testBetweenWithoutLowerBound() {
		Criteria criteria = new Criteria("field_1").between(null, 200);
		Assert.assertEquals("field_1:[* TO 200]", criteria.createQueryString());
	}

	@Test
	public void testBetweenWithDateValue() {
		DateTime lowerBound = new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC);
		DateTime upperBound = new DateTime(2012, 8, 21, 19, 30, 0, DateTimeZone.UTC);

		Criteria criteria = new Criteria("field_1").between(lowerBound, upperBound);
		Assert.assertEquals("field_1:[2012\\-08\\-21T06\\:35\\:00.000Z TO 2012\\-08\\-21T19\\:30\\:00.000Z]",
				criteria.getQueryString());
	}

	@Test
	public void testBetweenNegativeNumber() {
		Criteria criteria = new Criteria("field_1").between(-200, -100);
		Assert.assertEquals("field_1:[\\-200 TO \\-100]", criteria.createQueryString());
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testBetweenWithoutLowerAndUpperBound() {
		new Criteria("field_1").between(null, null);
	}

	@Test
	public void testLessThanEqual() {
		Criteria criteria = new Criteria("field_1").lessThanEqual(200);
		Assert.assertEquals("field_1:[* TO 200]", criteria.createQueryString());
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testLessThanEqualNull() {
		new Criteria("field_1").lessThanEqual(null);
	}

	@Test
	public void testGreaterEqualThan() {
		Criteria criteria = new Criteria("field_1").greaterThanEqual(100);
		Assert.assertEquals("field_1:[100 TO *]", criteria.createQueryString());
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testGreaterThanEqualNull() {
		new Criteria("field_1").greaterThanEqual(null);
	}

	@Test
	public void testIn() {
		Criteria criteria = new Criteria("field_1").in(1, 2, 3, 5, 8, 13, 21);
		Assert.assertEquals("field_1:(1 2 3 5 8 13 21)", criteria.createQueryString());
	}

	@Test
	public void testInWithNestedCollection() {
		List<List<String>> enclosingList = new ArrayList<List<String>>();
		enclosingList.add(Arrays.asList("spring", "data"));
		enclosingList.add(Arrays.asList("solr"));
		Criteria criteria = new Criteria("field_1").in(enclosingList);
		Assert.assertEquals("field_1:(spring data solr)", criteria.createQueryString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInWithNull() {
		new Criteria("field_1").in((Collection<?>) null);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testInWithNoValues() {
		new Criteria("field_1").in();
	}

	@Test
	public void testIsWithJavaDateValue() {
		DateTime dateTime = new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC);
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(dateTime.getMillis());

		Criteria criteria = new Criteria("dateField").is(calendar.getTime());
		Assert.assertEquals("dateField:2012\\-08\\-21T06\\:35\\:00.000Z", criteria.createQueryString());
	}

	@Test
	public void testIsWithJodaDateTime() {
		DateTime dateTime = new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC);

		Criteria criteria = new Criteria("dateField").is(dateTime);
		Assert.assertEquals("dateField:2012\\-08\\-21T06\\:35\\:00.000Z", criteria.createQueryString());
	}

	@Test
	public void testIsWithJodaLocalDateTime() {
		LocalDateTime dateTime = new LocalDateTime(new DateTime(2012, 8, 21, 6, 35, 0, DateTimeZone.UTC).getMillis(),
				DateTimeZone.UTC);

		Criteria criteria = new Criteria("dateField").is(dateTime);
		Assert.assertEquals("dateField:2012\\-08\\-21T06\\:35\\:00.000Z", criteria.createQueryString());
	}

	@Test
	public void testIsWithNegativeNumner() {
		Criteria criteria = new Criteria("field_1").is(-100);
		Assert.assertEquals("field_1:\\-100", criteria.createQueryString());
	}

	@Test
	public void testNear() {
		Criteria criteria = new Criteria("field_1").near(new GeoLocation(48.303056, 14.290556), new Distance(5));
		Assert.assertEquals("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=5.0}", criteria.createQueryString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNearWithNullLocation() {
		Criteria criteria = new Criteria("field_1").near(null, new Distance(5));
		criteria.createQueryString();
	}

	@Test
	public void testNearWithNullDistance() {
		Criteria criteria = new Criteria("field_1").near(new GeoLocation(48.303056, 14.290556), null);
		Assert.assertEquals("{!geofilt pt=48.303056,14.290556 sfield=field_1 d=0.0}", criteria.createQueryString());
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testNearWithNegativeDistance() {
		new Criteria("field_1").near(new GeoLocation(48.303056, 14.290556), new Distance(-1));
	}

	@Test
	public void testRegisterAlternateConverter() {
		Criteria criteria = new Criteria("field_1").is(100);
		criteria.registerConverter(new Converter<Number, String>() {

			@Override
			public String convert(Number arg0) {
				return StringUtils.reverse(arg0.toString());
			}

		});
		Assert.assertEquals("field_1:001", criteria.createQueryString());
	}

}
