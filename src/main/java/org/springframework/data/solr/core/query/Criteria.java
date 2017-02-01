/*
 * Copyright 2012 - 2017 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.util.Assert;

/**
 * Criteria is the central class when constructing queries. It follows more or less a fluent API style, which allows to
 * easily chain together multiple criteria.
 * 
 * @author Christoph Strobl
 * @author Philipp Jardas
 * @author Francisco Spaeth
 */
public class Criteria extends Node {

	public static final String WILDCARD = "*";
	public static final String CRITERIA_VALUE_SEPERATOR = " ";

	private Field field;
	private float boost = Float.NaN;

	private Set<Predicate> predicates = new LinkedHashSet<Predicate>();

	public Criteria() {}

	/**
	 * @param function
	 * @since 1.1
	 */
	public Criteria(Function function) {
		Assert.notNull(function, "Cannot create Critiera for 'null' function.");
		function(function);
	}

	/**
	 * Creates a new Criteria for the Filed with provided name
	 * 
	 * @param fieldname
	 */
	public Criteria(String fieldname) {
		this(new SimpleField(fieldname));
	}

	/**
	 * Creates a new Criteria for the given field
	 * 
	 * @param field
	 */
	public Criteria(Field field) {
		Assert.notNull(field, "Field for criteria must not be null");
		Assert.hasText(field.getName(), "Field.name for criteria must not be null/empty");

		this.field = field;
	}

	/**
	 * Static factory method to create a new Criteria for field with given name
	 * 
	 * @param fieldname must not be null
	 * @return
	 */
	public static Criteria where(String fieldname) {
		return where(new SimpleField(fieldname));
	}

	/**
	 * Static factory method to create a new Criteria for function
	 * 
	 * @param function must not be null
	 * @return
	 * @since 1.1
	 */
	public static Criteria where(Function function) {
		return new Criteria(function);
	}

	/**
	 * Static factory method to create a new Criteria for provided field
	 * 
	 * @param field must not be null
	 * @return
	 */
	public static Criteria where(Field field) {
		return new Criteria(field);
	}

	/**
	 * Crates new {@link Predicate} without any wildcards. Strings with blanks will be escaped
	 * {@code "string\ with\ blank"}
	 * 
	 * @param o
	 * @return
	 */
	public Criteria is(Object o) {
		if (o == null) {
			return isNull();
		}
		predicates.add(new Predicate(OperationKey.EQUALS, o));
		return this;
	}

	/**
	 * Crates new {@link Predicate} without any wildcards for each entry
	 * 
	 * @param values
	 * @return
	 */
	public Criteria is(Object... values) {
		return in(values);
	}

	/**
	 * Creates new {@link Predicate} without any wildcards for each entry
	 * 
	 * @param values
	 * @return
	 */
	public Criteria is(Iterable<?> values) {
		return in(values);
	}

	/**
	 * Crates new {@link Predicate} for {@code null} values
	 * 
	 * @return
	 */
	public Criteria isNull() {
		return between(null, null).not();
	}

	/**
	 * Crates new {@link Predicate} for {@code !null} values
	 * 
	 * @return
	 */
	public Criteria isNotNull() {
		return between(null, null);
	}

	/**
	 * Crates new {@link Predicate} with leading and trailing wildcards <br/>
	 * <strong>NOTE: </strong>mind your schema as leading wildcards may not be supported and/or execution might be slow.
	 * <strong>NOTE: </strong>Strings will not be automatically split on whitespace.
	 * 
	 * @param s
	 * @return
	 * @throws InvalidDataAccessApiUsageException for strings with whitespace
	 */
	public Criteria contains(String s) {
		assertNoBlankInWildcardedQuery(s, true, true);
		predicates.add(new Predicate(OperationKey.CONTAINS, s));
		return this;

	}

	/**
	 * Crates new {@link Predicate} with leading and trailing wildcards for each entry<br/>
	 * <strong>NOTE: </strong>mind your schema as leading wildcards may not be supported and/or execution might be slow.
	 * 
	 * @param values
	 * @return
	 * @throws InvalidDataAccessApiUsageException for strings with whitespace
	 */
	public Criteria contains(String... values) {
		assertValuesPresent((Object[]) values);
		return contains(Arrays.asList(values));
	}

	/**
	 * Crates new {@link Predicate} with leading and trailing wildcards for each entry<br/>
	 * <strong>NOTE: </strong>mind your schema as leading wildcards may not be supported and/or execution might be slow.
	 * 
	 * @param values
	 * @return
	 * @throws InvalidDataAccessApiUsageException for strings with whitespace
	 */
	public Criteria contains(Iterable<String> values) {
		Assert.notNull(values, "Collection must not be null");
		for (String value : values) {
			contains(value);
		}
		return this;
	}

	/**
	 * Crates new {@link Predicate} with trailing wildcard <br/>
	 * <strong>NOTE: </strong>Strings will not be automatically split on whitespace.
	 * 
	 * @param s
	 * @return
	 * @throws InvalidDataAccessApiUsageException for strings with whitespace
	 */
	public Criteria startsWith(String s) {
		assertNoBlankInWildcardedQuery(s, false, true);
		predicates.add(new Predicate(OperationKey.STARTS_WITH, s));
		return this;

	}

	/**
	 * Crates new {@link Predicate} with trailing wildcard for each entry
	 * 
	 * @param values
	 * @return
	 * @throws InvalidDataAccessApiUsageException for strings with whitespace
	 */
	public Criteria startsWith(String... values) {
		assertValuesPresent((Object[]) values);
		return startsWith(Arrays.asList(values));
	}

	/**
	 * Crates new {@link Predicate} with trailing wildcard for each entry
	 * 
	 * @param values
	 * @return
	 * @throws InvalidDataAccessApiUsageException for strings with whitespace
	 */
	public Criteria startsWith(Iterable<String> values) {
		Assert.notNull(values, "Collection must not be null");
		for (String value : values) {
			startsWith(value);
		}
		return this;
	}

	/**
	 * Crates new {@link Predicate} with leading wildcard <br />
	 * <strong>NOTE: </strong>mind your schema and execution times as leading wildcards may not be supported.
	 * <strong>NOTE: </strong>Strings will not be automatically split on whitespace.
	 * 
	 * @param s
	 * @return
	 * @throws InvalidDataAccessApiUsageException for strings with whitespace
	 */
	public Criteria endsWith(String s) {
		assertNoBlankInWildcardedQuery(s, true, false);
		predicates.add(new Predicate(OperationKey.ENDS_WITH, s));
		return this;
	}

	/**
	 * Crates new {@link Predicate} with leading wildcard for each entry<br />
	 * <strong>NOTE: </strong>mind your schema and execution times as leading wildcards may not be supported.
	 * 
	 * @param values
	 * @return
	 * @throws InvalidDataAccessApiUsageException for strings with whitespace
	 */
	public Criteria endsWith(String... values) {
		assertValuesPresent((Object[]) values);
		return endsWith(Arrays.asList(values));
	}

	/**
	 * Crates new {@link Predicate} with leading wildcard for each entry<br />
	 * <strong>NOTE: </strong>mind your schema and execution times as leading wildcards may not be supported.
	 * 
	 * @param values
	 * @return
	 * @throws InvalidDataAccessApiUsageException for strings with whitespace
	 */
	public Criteria endsWith(Iterable<String> values) {
		Assert.notNull(values, "Collection must not be null");
		for (String value : values) {
			endsWith(value);
		}
		return this;

	}

	/**
	 * Negates current criteria usinng {@code -} operator
	 * 
	 * @return
	 */
	public Criteria not() {
		setNegating(true);
		return this;
	}

	/**
	 * Explicitly wrap {@link Criteria} inside not operation.
	 * 
	 * @since 1.4
	 * @return
	 */
	public Criteria notOperator() {

		Crotch c = new Crotch();
		c.setNegating(true);
		c.add(this);

		return c;
	}

	/**
	 * Crates new {@link Predicate} with trailing {@code ~}
	 * 
	 * @param s
	 * @return
	 */
	public Criteria fuzzy(String s) {
		return fuzzy(s, Float.NaN);
	}

	/**
	 * Crates new {@link Predicate} with trailing {@code ~} followed by levensteinDistance
	 * 
	 * @param s
	 * @param levenshteinDistance
	 * @return
	 */
	public Criteria fuzzy(String s, float levenshteinDistance) {
		if (!Float.isNaN(levenshteinDistance) && (levenshteinDistance < 0 || levenshteinDistance > 1)) {
			throw new InvalidDataAccessApiUsageException("Levenshtein Distance has to be within its bounds (0.0 - 1.0).");
		}
		predicates.add(new Predicate(OperationKey.FUZZY, new Object[] { s, Float.valueOf(levenshteinDistance) }));
		return this;
	}

	/**
	 * Crates new {@link Predicate} with trailing {@code ~} followed by distance
	 * 
	 * @param phrase
	 * @param distance
	 * @return
	 */
	public Criteria sloppy(String phrase, int distance) {
		if (distance <= 0) {
			throw new InvalidDataAccessApiUsageException("Slop distance has to be greater than 0.");
		}

		if (!StringUtils.contains(phrase, CRITERIA_VALUE_SEPERATOR)) {
			throw new InvalidDataAccessApiUsageException("Phrase must consist of multiple terms, separated with spaces.");
		}

		predicates.add(new Predicate(OperationKey.SLOPPY, new Object[] { phrase, Integer.valueOf(distance) }));
		return this;
	}

	/**
	 * Crates new {@link Predicate} allowing native solr expressions
	 * 
	 * @param s
	 * @return
	 */
	public Criteria expression(String s) {
		predicates.add(new Predicate(OperationKey.EXPRESSION, s));
		return this;
	}

	/**
	 * Boost positive hit with given factor. eg. ^2.3
	 * 
	 * @param boost
	 * @return
	 */
	public Criteria boost(float boost) {
		if (boost < 0) {
			throw new InvalidDataAccessApiUsageException("Boost must not be negative.");
		}
		this.boost = boost;
		return this;
	}

	/**
	 * Crates new {@link Predicate} for {@code RANGE [lowerBound TO upperBound]}
	 * 
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	public Criteria between(Object lowerBound, Object upperBound) {
		return between(lowerBound, upperBound, true, true);
	}

	/**
	 * Crates new {@link Predicate} for {@code RANGE [lowerBound TO upperBound]}
	 * 
	 * @param lowerBound
	 * @param upperBound
	 * @param includeLowerBound
	 * @param includeUppderBound
	 * @return
	 */
	public Criteria between(Object lowerBound, Object upperBound, boolean includeLowerBound, boolean includeUppderBound) {
		predicates.add(new Predicate(OperationKey.BETWEEN, new Object[] { lowerBound, upperBound, includeLowerBound,
				includeUppderBound }));
		return this;
	}

	/**
	 * Crates new {@link Predicate} for {@code RANGE [* TO upperBound&#125;}
	 * 
	 * @param upperBound
	 * @return
	 */
	public Criteria lessThan(Object upperBound) {
		between(null, upperBound, true, false);
		return this;
	}

	/**
	 * Crates new {@link Predicate} for {@code RANGE [* TO upperBound]}
	 * 
	 * @param upperBound
	 * @return
	 */
	public Criteria lessThanEqual(Object upperBound) {
		between(null, upperBound);
		return this;
	}

	/**
	 * Crates new {@link Predicate} for {@code RANGE &#123;lowerBound TO *]}
	 * 
	 * @param lowerBound
	 * @return
	 */
	public Criteria greaterThan(Object lowerBound) {
		between(lowerBound, null, false, true);
		return this;
	}

	/**
	 * Crates new {@link Predicate} for {@code RANGE [lowerBound TO *]}
	 * 
	 * @param lowerBound
	 * @return
	 */
	public Criteria greaterThanEqual(Object lowerBound) {
		between(lowerBound, null);
		return this;
	}

	/**
	 * Crates new {@link Predicate} for multiple values {@code (arg0 arg1 arg2 ...)}
	 * 
	 * @param values
	 * @return
	 */
	public Criteria in(Object... values) {
		assertValuesPresent(values);
		return (Criteria) in(Arrays.asList(values));
	}

	/**
	 * Crates new {@link Predicate} for multiple values {@code (arg0 arg1 arg2 ...)}
	 * 
	 * @param values the collection containing the values to match against
	 * @return
	 */
	public Criteria in(Iterable<?> values) {
		Assert.notNull(values, "Collection of 'in' values must not be null");
		for (Object value : values) {
			if (value instanceof Collection) {
				in((Collection<?>) value);
			} else {
				is(value);
			}
		}
		return this;
	}

	/**
	 * Creates new {@link Predicate} for {@code !geodist}
	 * 
	 * @param location {@link Point} in degrees
	 * @param distance
	 * @return
	 */
	public Criteria within(Point location, Distance distance) {
		Assert.notNull(location, "Location must not be null!");
		assertPositiveDistanceValue(distance);
		predicates.add(new Predicate(OperationKey.WITHIN, new Object[] { location,
				distance != null ? distance : new Distance(0) }));
		return this;
	}

	/**
	 * Creates new {@link Predicate} for {@code !geodist}.
	 * 
	 * @param circle
	 * @return
	 * @since 1.2
	 */
	public Criteria within(Circle circle) {

		Assert.notNull(circle, "Circle for 'within' must not be 'null'.");
		return within(circle.getCenter(), circle.getRadius());
	}

	/**
	 * Creates new {@link Predicate} for {@code !bbox} with exact coordinates
	 * 
	 * @param box
	 * @return
	 */
	public Criteria near(Box box) {
		predicates.add(new Predicate(OperationKey.NEAR, new Object[] { box }));
		return this;
	}

	/**
	 * Creates new {@link Predicate} for {@code !bbox} for a specified distance. The difference between this and
	 * {@code within} is this is approximate while {@code within} is exact.
	 * 
	 * @param location
	 * @param distance
	 * @return
	 * @throws IllegalArgumentException if location is null
	 * @throws InvalidDataAccessApiUsageException if distance is negative
	 */
	public Criteria near(Point location, Distance distance) {
		Assert.notNull(location, "Location must not be 'null' for near criteria.");
		assertPositiveDistanceValue(distance);

		predicates.add(new Predicate(OperationKey.NEAR, new Object[] { location,
				distance != null ? distance : new Distance(0) }));
		return this;
	}

	/**
	 * Creates new {@link Predicate} for {@code !circle} for a specified distance. The difference between this and
	 * {@link #within(Circle)} is this is approximate while {@code within} is exact.
	 * 
	 * @param circle
	 * @return
	 * @since 1.2
	 */
	public Criteria near(Circle circle) {

		Assert.notNull(circle, "Circle for 'near' must not be 'null'.");
		return near(circle.getCenter(), circle.getRadius());
	}

	/**
	 * Creates {@link Predicate} for given {@link Function}.
	 * 
	 * @param function must not be null
	 * @return
	 * @throws IllegalArgumentException if function is null
	 * @since 1.1
	 */
	public Criteria function(Function function) {
		Assert.notNull(function, "Cannot add 'null' function to criteria.");
		predicates.add(new Predicate(OperationKey.FUNCTION, function));
		return this;
	}

	/**
	 * Target field
	 * 
	 * @return null if not set
	 */
	public Field getField() {
		return this.field;
	}

	/**
	 * @return true if {@code not()} criteria
	 */
	public boolean isNegating() {
		return super.isNegating();
	}

	/**
	 * Boost criteria value
	 * 
	 * @return {@code Float.NaN} if not set
	 */
	public float getBoost() {
		return this.boost;
	}

	/**
	 * @return unmodifiable set of all {@link Predicate}
	 */
	public Set<Predicate> getPredicates() {
		return Collections.unmodifiableSet(this.predicates);
	}

	private void assertPositiveDistanceValue(Distance distance) {
		if (distance != null && distance.getValue() < 0) {
			throw new InvalidDataAccessApiUsageException("distance must not be negative.");
		}
	}

	private void assertNoBlankInWildcardedQuery(String searchString, boolean leadingWildcard, boolean trailingWildcard) {
		if (StringUtils.contains(searchString, CRITERIA_VALUE_SEPERATOR)) {
			throw new InvalidDataAccessApiUsageException("Cannot constructQuery '" + (leadingWildcard ? "*" : "") + "\""
					+ searchString + "\"" + (trailingWildcard ? "*" : "") + "'. Use epxression or mulitple clauses instead.");
		}
	}

	private void assertValuesPresent(Object... values) {
		if (values.length == 0 || (values.length > 1 && values[1] instanceof Collection)) {
			throw new InvalidDataAccessApiUsageException("At least one element "
					+ (values.length > 0 ? ("of argument of type " + values[1].getClass().getName()) : "")
					+ " has to be present.");
		}
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder(this.isOr() ? "OR " : "AND ");
		sb.append(this.isNegating() ? "!" : "");
		sb.append(this.field != null ? this.field.getName() : "");

		if (this.predicates.size() > 1) {
			sb.append('(');
		}
		for (Predicate ce : this.predicates) {
			sb.append(ce.toString());
		}
		if (this.predicates.size() > 1) {
			sb.append(')');
		}
		sb.append(' ');
		return sb.toString();
	}

	// -------- PREDICATE STUFF --------

	public enum OperationKey {
		EQUALS("$equals"), CONTAINS("$contains"), STARTS_WITH("$startsWith"), ENDS_WITH("$endsWith"), EXPRESSION(
				"$expression"), BETWEEN("$between"), NEAR("$near"), WITHIN("$within"), FUZZY("$fuzzy"), SLOPPY("$sloppy"), FUNCTION(
				"$function");

		private final String key;

		private OperationKey(String key) {
			this.key = key;
		}

		public String getKey() {
			return this.key;
		}

	}

	/**
	 * Single entry to be used when defining search criteria
	 * 
	 * @author Christoph Strobl
	 * @author Francisco Spaeth
	 */
	public static class Predicate {

		private String key;
		private Object value;

		public Predicate(OperationKey key, Object value) {
			this(key.getKey(), value);
		}

		public Predicate(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		/**
		 * @return null if not set
		 */
		public String getKey() {
			return key;
		}

		/**
		 * set the operation key to be applied when parsing query
		 * 
		 * @param key
		 */
		public void setKey(String key) {
			this.key = key;
		}

		/**
		 * @return null if not set
		 */
		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return key + ":" + value;
		}

	}

	// -------- NODE STUFF ---------

	/**
	 * Explicitly connect {@link Criteria} with another one allows to create explicit bracketing.
	 * 
	 * @since 1.4
	 * @return
	 */
	public Criteria connect() {
		Crotch c = new Crotch();
		c.add(this);
		return c;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Crotch and(Node node) {

		if (!(node instanceof Criteria)) {
			throw new IllegalArgumentException("Can only add instances of Criteria");
		}

		Crotch crotch = new Crotch();
		crotch.setParent(this.getParent());
		crotch.add(this);
		crotch.add((Criteria) node);
		return crotch;
	}

	@SuppressWarnings("unchecked")
	public Crotch and(String fieldname) {

		Criteria node = new Criteria(fieldname);
		return and(node);
	}

	@SuppressWarnings("unchecked")
	public Crotch or(Node node) {

		if (!(node instanceof Criteria)) {
			throw new IllegalArgumentException("Can only add instances of Criteria");
		}

		node.setPartIsOr(true);

		Crotch crotch = new Crotch();
		crotch.setParent(this.getParent());
		crotch.add(this);
		crotch.add((Criteria) node);
		return crotch;
	}

	@SuppressWarnings("unchecked")
	public Crotch or(String fieldname) {
		Criteria node = new Criteria(fieldname);
		node.setPartIsOr(true);
		return or(node);
	}

}
