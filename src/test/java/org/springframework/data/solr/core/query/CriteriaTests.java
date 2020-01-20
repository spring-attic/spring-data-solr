/*
 * Copyright 2012-2019 the original author or authors.
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
import static org.assertj.core.data.Offset.offset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.solr.core.query.Criteria.OperationKey;
import org.springframework.data.solr.core.query.Criteria.Predicate;

/**
 * @author Christoph Strobl
 * @author John Dorman
 * @author Philipp Jardas
 */
public class CriteriaTests {

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
		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertPredicate(criteria.getPredicates(), 0, OperationKey.EQUALS, "is");
	}

	@Test
	public void testMultipleIs() {
		Criteria criteria = new Criteria("field_1").is("is").is("another is");
		assertThat(criteria.getField().getName()).isEqualTo("field_1");

		assertPredicate(criteria.getPredicates(), 0, OperationKey.EQUALS, "is");
		assertPredicate(criteria.getPredicates(), 1, OperationKey.EQUALS, "another is");
	}

	@Test
	public void testIsWithCollection() {
		Criteria criteria = new Criteria("field_1").is(Arrays.asList("is", "another is"));
		assertThat(criteria.getField().getName()).isEqualTo("field_1");

		assertPredicate(criteria.getPredicates(), 0, OperationKey.EQUALS, "is");
		assertPredicate(criteria.getPredicates(), 1, OperationKey.EQUALS, "another is");
	}

	@Test
	public void testIsWithNull() {
		Criteria criteria = new Criteria("field_1").is((Object) null);
		assertThat(criteria.getField().getName()).isEqualTo("field_1");

		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());

		assertThat(((Object[]) entry.getValue())[0]).isNull();
		assertThat(((Object[]) entry.getValue())[1]).isNull();
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();
	}

	@Test
	public void testIsNull() {
		Criteria criteria = new Criteria("field_1").isNull();
		assertThat(criteria.getField().getName()).isEqualTo("field_1");

		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());

		assertThat(criteria.isNegating()).isTrue();
		assertThat(((Object[]) entry.getValue())[0]).isNull();
		assertThat(((Object[]) entry.getValue())[1]).isNull();
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();
	}

	@Test
	public void testIsNotNull() {
		Criteria criteria = new Criteria("field_1").isNotNull();
		assertThat(criteria.getField().getName()).isEqualTo("field_1");

		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());

		assertThat(criteria.isNegating()).isFalse();
		assertThat(((Object[]) entry.getValue())[0]).isNull();
		assertThat(((Object[]) entry.getValue())[1]).isNull();
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();
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

		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertPredicate(criteria.getPredicates(), 0, OperationKey.ENDS_WITH, "end");
	}

	@Test
	public void testEndsWithCollection() {
		Criteria criteria = new Criteria("field_1").endsWith(Arrays.asList("use", "multiple", "values"));

		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertPredicate(criteria.getPredicates(), 0, OperationKey.ENDS_WITH, "use");
		assertPredicate(criteria.getPredicates(), 1, OperationKey.ENDS_WITH, "multiple");
		assertPredicate(criteria.getPredicates(), 2, OperationKey.ENDS_WITH, "values");
	}

	@Test
	public void testStartsWith() {
		Criteria criteria = new Criteria("field_1").startsWith("start");

		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertPredicate(criteria.getPredicates(), 0, OperationKey.STARTS_WITH, "start");
	}

	@Test
	public void testStartsWithCollection() {
		Criteria criteria = new Criteria("field_1").startsWith(Arrays.asList("use", "multiple", "values"));

		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertPredicate(criteria.getPredicates(), 0, OperationKey.STARTS_WITH, "use");
		assertPredicate(criteria.getPredicates(), 1, OperationKey.STARTS_WITH, "multiple");
		assertPredicate(criteria.getPredicates(), 2, OperationKey.STARTS_WITH, "values");
	}

	@Test
	public void testContains() {
		Criteria criteria = new Criteria("field_1").contains("contains");

		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertPredicate(criteria.getPredicates(), 0, OperationKey.CONTAINS, "contains");
	}

	@Test
	public void testContainWithCollection() {
		Criteria criteria = new Criteria("field_1").contains(Arrays.asList("use", "multiple", "values"));

		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertPredicate(criteria.getPredicates(), 0, OperationKey.CONTAINS, "use");
		assertPredicate(criteria.getPredicates(), 1, OperationKey.CONTAINS, "multiple");
		assertPredicate(criteria.getPredicates(), 2, OperationKey.CONTAINS, "values");
	}

	@Test
	public void testExpression() {
		Criteria criteria = new Criteria("field_1").expression("(have fun using +solr && expressions*)");
		assertPredicate(criteria.getPredicates(), 0, OperationKey.EXPRESSION, "(have fun using +solr && expressions*)");
	}

	@Test
	public void testCriteriaChain() {
		Criteria criteria = new Criteria("field_1").startsWith("start").endsWith("end").contains("contains").is("is");
		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertPredicate(criteria.getPredicates(), 0, OperationKey.STARTS_WITH, "start");
		assertPredicate(criteria.getPredicates(), 1, OperationKey.ENDS_WITH, "end");
		assertPredicate(criteria.getPredicates(), 2, OperationKey.CONTAINS, "contains");
		assertPredicate(criteria.getPredicates(), 3, OperationKey.EQUALS, "is");
	}

	@Test // DATASOLR-105
	public void testAndShouldProduceCrotch() {

		Criteria criteria = new Criteria("field_1").startsWith("start").endsWith("end").and("field_2").startsWith("2start")
				.endsWith("2end");
		assertThat(criteria).isInstanceOf(Crotch.class);
	}

	@Test // DATASOLR-105
	public void testCirteriasJoindWihtAndShouldBeSiblingsOfCreatedCrotch() {

		Criteria c1 = new Criteria("field_1").startsWith("start").endsWith("end");
		Criteria c2 = new Criteria("field_2").startsWith("2start");
		Crotch crotch = c1.and(c2);

		assertThat(crotch.getSiblings()).containsExactly(c1, c2);
	}

	@Test // DATASOLR-105
	public void testOrShouldProduceCrotch() {
		Criteria criteria = new Criteria("field_1").startsWith("start").or("field_2").endsWith("end").startsWith("start2");
		assertThat(criteria).isInstanceOf(Crotch.class);
	}

	@Test // DATASOLR-105
	public void testCirteriasJoindWithOrShouldBeSiblingsOfCreatedCrotch() {

		Criteria c1 = new Criteria("field_1").startsWith("start").endsWith("end");
		Criteria c2 = new Criteria("field_2").startsWith("2start");
		Crotch crotch = c1.or(c2);

		assertThat(crotch.getSiblings()).containsExactly(c1, c2);
	}

	@Test // DATASOLR-105
	public void testCrotchShouldReturnFieldNameOfMostRecentSibling() {

		Criteria c1 = new Criteria("field_1").startsWith("start").endsWith("end");
		Criteria c2 = new Criteria("field_2").startsWith("2start");
		Crotch crotch = c1.or(c2);
		assertThat(crotch.getField().getName()).isEqualTo("field_2");
	}

	@Test
	public void testIsNot() {
		Criteria criteria = new Criteria("field_1").is("value_1").not();
		assertThat(criteria.isNegating()).isTrue();
		assertPredicate(criteria.getPredicates(), 0, OperationKey.EQUALS, "value_1");
	}

	@Test
	public void testFuzzy() {
		Criteria criteria = new Criteria("field_1").fuzzy("value_1");
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(((Object[]) entry.getValue())[0]).isEqualTo("value_1");
		assertThat(((Object[]) entry.getValue())[1]).isEqualTo(Float.NaN);
	}

	@Test
	public void testFuzzyWithDistance() {
		Criteria criteria = new Criteria("field_1").fuzzy("value_1", 0.5f);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(((Object[]) entry.getValue())[0]).isEqualTo("value_1");
		assertThat(((Object[]) entry.getValue())[1]).isEqualTo(0.5F);
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
	public void testSloppy() {
		Criteria criteria = new Criteria("field_1").sloppy("value0 value1", 2);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(((Object[]) entry.getValue())[0]).isEqualTo("value0 value1");
		assertThat(((Object[]) entry.getValue())[1]).isEqualTo(2);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testSloppyWithNegativeDistance() {
		new Criteria("field_1").sloppy("value0 value1", 0);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testSloppyWithSingleWordPhrase() {
		new Criteria("field_1").sloppy("value0", 0);
	}

	@Test
	public void testBoost() {
		Criteria criteria = new Criteria("field_1").is("value_1").boost(2f);
		assertPredicate(criteria.getPredicates(), 0, OperationKey.EQUALS, "value_1");
		assertThat(criteria.getBoost()).isCloseTo(2f, offset(0f));
	}

	@Test
	public void testBetween() {
		Criteria criteria = new Criteria("field_1").between(100, 200);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isEqualTo(100);
		assertThat(((Object[]) entry.getValue())[1]).isEqualTo(200);
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();
	}

	@Test
	public void testBetweenWithoutUpperBound() {
		Criteria criteria = new Criteria("field_1").between(100, null);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isEqualTo(100);
		assertThat(((Object[]) entry.getValue())[1]).isNull();
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();
	}

	@Test
	public void testBetweenWithoutLowerBound() {
		Criteria criteria = new Criteria("field_1").between(null, 200);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isNull();
		assertThat(((Object[]) entry.getValue())[1]).isEqualTo(200);
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();
	}

	@Test
	public void testBetweenExcludingLowerBound() {
		Criteria criteria = new Criteria("field_1").between(100, 200, false, true);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isEqualTo(100);
		assertThat(((Object[]) entry.getValue())[1]).isEqualTo(200);
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isFalse();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();
	}

	@Test
	public void testBetweenExcludingUpperBound() {
		Criteria criteria = new Criteria("field_1").between(100, 200, true, false);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isEqualTo(100);
		assertThat(((Object[]) entry.getValue())[1]).isEqualTo(200);
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isFalse();
	}

	@Test
	public void testBetweenWithoutLowerAndUpperBound() {
		Criteria criteria = new Criteria("field_1").between(null, null);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isNull();
		assertThat(((Object[]) entry.getValue())[1]).isNull();
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();
	}

	@Test
	public void testLessThan() {
		Criteria criteria = new Criteria("field_1").lessThan(200);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isNull();
		assertThat(((Object[]) entry.getValue())[1]).isEqualTo(200);
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isFalse();
	}

	@Test
	public void testLessThanEqual() {
		Criteria criteria = new Criteria("field_1").lessThanEqual(200);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isNull();
		assertThat(((Object[]) entry.getValue())[1]).isEqualTo(200);
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();
	}

	@Test
	public void testLessThanEqualNull() {
		Criteria criteria = new Criteria("field_1").lessThanEqual(null);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isNull();
		assertThat(((Object[]) entry.getValue())[1]).isNull();
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();
	}

	@Test
	public void testGreaterThan() {
		Criteria criteria = new Criteria("field_1").greaterThan(100);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isEqualTo(100);
		assertThat(((Object[]) entry.getValue())[1]).isNull();
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isFalse();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();
	}

	@Test
	public void testGreaterThanEqual() {
		Criteria criteria = new Criteria("field_1").greaterThanEqual(100);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isEqualTo(100);
		assertThat(((Object[]) entry.getValue())[1]).isNull();
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();
	}

	@Test
	public void testGreaterThanEqualNull() {
		Criteria criteria = new Criteria("field_1").greaterThanEqual(null);
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isNull();
		assertThat(((Object[]) entry.getValue())[1]).isNull();
		assertThat((Boolean) ((Object[]) entry.getValue())[2]).isTrue();
		assertThat((Boolean) ((Object[]) entry.getValue())[3]).isTrue();

	}

	@Test
	public void testIn() {
		Criteria criteria = new Criteria("field_1").in(1, 2, 3, 5, 8, 13, 21);
		assertThat(criteria.getField().getName()).isEqualTo("field_1");
		assertPredicate(criteria.getPredicates(), 0, OperationKey.EQUALS, 1);
		assertPredicate(criteria.getPredicates(), 1, OperationKey.EQUALS, 2);
		assertPredicate(criteria.getPredicates(), 2, OperationKey.EQUALS, 3);
		assertPredicate(criteria.getPredicates(), 3, OperationKey.EQUALS, 5);
		assertPredicate(criteria.getPredicates(), 4, OperationKey.EQUALS, 8);
		assertPredicate(criteria.getPredicates(), 5, OperationKey.EQUALS, 13);
		assertPredicate(criteria.getPredicates(), 6, OperationKey.EQUALS, 21);
	}

	@Test
	public void testInWithNestedCollection() {
		List<List<String>> enclosingList = new ArrayList<>();
		enclosingList.add(Arrays.asList("spring", "data"));
		enclosingList.add(Collections.singletonList("solr"));
		Criteria criteria = new Criteria("field_1").in(enclosingList);

		assertPredicate(criteria.getPredicates(), 0, OperationKey.EQUALS, "spring");
		assertPredicate(criteria.getPredicates(), 1, OperationKey.EQUALS, "data");
		assertPredicate(criteria.getPredicates(), 2, OperationKey.EQUALS, "solr");
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
	public void testNear() {
		Point location = new Point(48.303056, 14.290556);
		Criteria criteria = new Criteria("field_1").near(location, new Distance(5));
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.NEAR.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isEqualTo(location);
		assertThat(((Distance) ((Object[]) entry.getValue())[1]).getValue()).isCloseTo(5d, offset(0d));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNearWithNullLocation() {
		new Criteria("field_1").near(null, new Distance(5));
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testNearWithNegativeDistance() {
		new Criteria("field_1").near(new Point(48.303056, 14.290556), new Distance(-1));
	}

	@Test
	public void testWithin() {
		Point location = new Point(48.303056, 14.290556);
		Criteria criteria = new Criteria("field_1").within(location, new Distance(5));
		Predicate entry = getPredicateByPosition(criteria.getPredicates(), 0);
		assertThat(entry.getKey()).isEqualTo(OperationKey.WITHIN.getKey());
		assertThat(((Object[]) entry.getValue())[0]).isEqualTo(location);
		assertThat(((Distance) ((Object[]) entry.getValue())[1]).getValue()).isCloseTo(5d, offset(0d));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWithinWithNullLocation() {
		new Criteria("field_1").within(null, new Distance(5));
	}

	private void assertPredicate(Set<Predicate> entries, int position, OperationKey expectedKey, Object expectedValue) {
		assertPredicate(entries, position, expectedKey.getKey(), expectedValue);
	}

	private void assertPredicate(Set<Predicate> entries, int position, String expectedKey, Object expectedValue) {
		Predicate predicate = getPredicateByPosition(entries, position);
		assertThat(predicate.getValue()).isEqualTo(expectedValue);
		assertThat(predicate.getKey()).isEqualTo(expectedKey);
	}

	private Predicate getPredicateByPosition(Set<Predicate> entries, int position) {
		return (Predicate) entries.toArray()[position];
	}

	private static class TestFunction implements Function {

		@Override
		public String getOperation() {
			return "op";
		}

		@Override
		public Iterable<Object> getArguments() {
			return null;
		}

		@Override
		public boolean hasArguments() {
			return false;
		}

	}
}
