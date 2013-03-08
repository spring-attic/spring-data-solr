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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.solr.core.geo.BoundingBox;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.util.Assert;

/**
 * Criteria is the central class when constructing queries. It follows more or less a fluent API style, which allows to
 * easily chain together multiple criteria.
 * 
 * @author Christoph Strobl
 */
public class Criteria {

	public static final String WILDCARD = "*";
	public static final String CRITERIA_VALUE_SEPERATOR = " ";

	private static final String OR_OPERATOR = " OR ";
	private static final String AND_OPERATOR = " AND ";

	private Field field;
	private float boost = Float.NaN;
	private boolean negating = false;

	private List<Criteria> criteriaChain = new ArrayList<Criteria>(1);

	private Set<CriteriaEntry> criteria = new LinkedHashSet<CriteriaEntry>();

	public Criteria() {
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

		this.criteriaChain.add(this);
		this.field = field;
	}

	protected Criteria(List<Criteria> criteriaChain, String fieldname) {
		this(criteriaChain, new SimpleField(fieldname));
	}

	protected Criteria(List<Criteria> criteriaChain, Field field) {
		Assert.notNull(criteriaChain, "CriteriaChain must not be null");
		Assert.notNull(field, "Field for criteria must not be null");
		Assert.hasText(field.getName(), "Field.name for criteria must not be null/empty");

		this.criteriaChain.addAll(criteriaChain);
		this.criteriaChain.add(this);
		this.field = field;
	}

	/**
	 * Static factory method to create a new Criteria for field with given name
	 * 
	 * @param field
	 * @return
	 */
	public static Criteria where(String field) {
		return where(new SimpleField(field));
	}

	/**
	 * Static factory method to create a new Criteria for provided field
	 * 
	 * @param field
	 * @return
	 */
	public static Criteria where(Field field) {
		return new Criteria(field);
	}

	/**
	 * Chain using {@code AND}
	 * 
	 * @param field
	 * @return
	 */
	public Criteria and(Field field) {
		return new Criteria(this.criteriaChain, field);
	}

	/**
	 * Chain using {@code AND}
	 * 
	 * @param field
	 * @return
	 */
	public Criteria and(String fieldname) {
		return new Criteria(this.criteriaChain, fieldname);
	}

	/**
	 * Chain using {@code AND}
	 * 
	 * @param field
	 * @return
	 */
	public Criteria and(Criteria criteria) {
		this.criteriaChain.add(criteria);
		return this;
	}

	/**
	 * Chain using {@code AND}
	 * 
	 * @param field
	 * @return
	 */
	public Criteria and(Criteria... criterias) {
		this.criteriaChain.addAll(Arrays.asList(criterias));
		return this;
	}

	/**
	 * Chain using {@code OR}
	 * 
	 * @param field
	 * @return
	 */
	public Criteria or(Field field) {
		return new OrCriteria(this.criteriaChain, field);
	}

	/**
	 * Chain using {@code OR}
	 * 
	 * @param field
	 * @return
	 */
	public Criteria or(Criteria criteria) {
		Assert.notNull(criteria, "Cannot chain 'null' criteria.");

		Criteria orConnectedCritiera = new OrCriteria(this.criteriaChain, criteria.getField());
		orConnectedCritiera.criteria.addAll(criteria.criteria);
		return orConnectedCritiera;
	}

	/**
	 * Chain using {@code OR}
	 * 
	 * @param field
	 * @return
	 */
	public Criteria or(String fieldname) {
		return or(new SimpleField(fieldname));
	}

	/**
	 * Crates new CriteriaEntry without any wildcards
	 * 
	 * @param o
	 * @return
	 */
	public Criteria is(Object o) {
		if (o == null) {
			return isNull();
		}
		criteria.add(new CriteriaEntry(OperationKey.EQUALS, o));
		return this;
	}

	/**
	 * Crates new CriteriaEntry for {@code null} values
	 * 
	 * @return
	 */
	public Criteria isNull() {
		return between(null, null).not();
	}

	/**
	 * Crates new CriteriaEntry for {@code !null} values
	 * 
	 * @return
	 */
	public Criteria isNotNull() {
		return between(null, null);
	}

	/**
	 * Crates new CriteriaEntry with leading and trailing wildcards <br/>
	 * <strong>NOTE: </strong> mind your schema as leading wildcards may not be supported and/or execution might be slow.
	 * 
	 * @param o
	 * @return
	 */
	public Criteria contains(String s) {
		assertNoBlankInWildcardedQuery(s, true, true);
		criteria.add(new CriteriaEntry(OperationKey.CONTAINS, s));
		return this;
	}

	/**
	 * Crates new CriteriaEntry with leading and trailing wildcards for each entry<br/>
	 * <strong>NOTE: </strong> mind your schema as leading wildcards may not be supported and/or execution might be slow.
	 * 
	 * @param values
	 * @return
	 */
	public Criteria contains(String... values) {
		assertValuesPresent((Object[]) values);
		return contains(Arrays.asList(values));
	}

	/**
	 * Crates new CriteriaEntry with leading and trailing wildcards for each entry<br/>
	 * <strong>NOTE: </strong> mind your schema as leading wildcards may not be supported and/or execution might be slow.
	 * 
	 * @param values
	 * @return
	 */
	public Criteria contains(Iterable<String> values) {
		Assert.notNull(values, "Collection must not be null");
		for (String value : values) {
			contains(value);
		}
		return this;
	}

	/**
	 * Crates new CriteriaEntry with trailing wildcard
	 * 
	 * @param o
	 * @return
	 */
	public Criteria startsWith(String s) {
		assertNoBlankInWildcardedQuery(s, true, false);
		criteria.add(new CriteriaEntry(OperationKey.STARTS_WITH, s));
		return this;
	}

	/**
	 * Crates new CriteriaEntry with trailing wildcard for each entry
	 * 
	 * @param values
	 * @return
	 */
	public Criteria startsWith(String... values) {
		assertValuesPresent((Object[]) values);
		return startsWith(Arrays.asList(values));
	}

	/**
	 * Crates new CriteriaEntry with trailing wildcard for each entry
	 * 
	 * @param values
	 * @return
	 */
	public Criteria startsWith(Iterable<String> values) {
		Assert.notNull(values, "Collection must not be null");
		for (String value : values) {
			startsWith(value);
		}
		return this;
	}

	/**
	 * Crates new CriteriaEntry with leading wildcard <br />
	 * <strong>NOTE: </strong> mind your schema and execution times as leading wildcards may not be supported.
	 * 
	 * @param o
	 * @return
	 */
	public Criteria endsWith(String s) {
		assertNoBlankInWildcardedQuery(s, false, true);
		criteria.add(new CriteriaEntry(OperationKey.ENDS_WITH, s));
		return this;
	}

	/**
	 * Crates new CriteriaEntry with leading wildcard for each entry<br />
	 * <strong>NOTE: </strong> mind your schema and execution times as leading wildcards may not be supported.
	 * 
	 * @param values
	 * @return
	 */
	public Criteria endsWith(String... values) {
		assertValuesPresent((Object[]) values);
		return endsWith(Arrays.asList(values));
	}

	/**
	 * Crates new CriteriaEntry with leading wildcard for each entry<br />
	 * <strong>NOTE: </strong> mind your schema and execution times as leading wildcards may not be supported.
	 * 
	 * @param values
	 * @return
	 */
	public Criteria endsWith(Iterable<String> values) {
		Assert.notNull(values, "Collection must not be null");
		for (String value : values) {
			endsWith(value);
		}
		return this;
	}

	/**
	 * Crates new CriteriaEntry with leading -
	 * 
	 * @param s
	 * @return
	 */
	public Criteria not() {
		this.negating = true;
		return this;
	}

	/**
	 * Crates new CriteriaEntry with trailing ~
	 * 
	 * @param s
	 * @return
	 */
	public Criteria fuzzy(String s) {
		return fuzzy(s, Float.NaN);
	}

	/**
	 * Crates new CriteriaEntry with trailing ~ followed by levensteinDistance
	 * 
	 * @param s
	 * @param levenshteinDistance
	 * @return
	 */
	public Criteria fuzzy(String s, float levenshteinDistance) {
		if (!Float.isNaN(levenshteinDistance) && (levenshteinDistance < 0 || levenshteinDistance > 1)) {
			throw new InvalidDataAccessApiUsageException("Levenshtein Distance has to be within its bounds (0.0 - 1.0).");
		}
		criteria.add(new CriteriaEntry("$fuzzy#" + levenshteinDistance, s));
		return this;
	}

	/**
	 * Crates new CriteriaEntry allowing native solr expressions
	 * 
	 * @param o
	 * @return
	 */
	public Criteria expression(String s) {
		criteria.add(new CriteriaEntry(OperationKey.EXPRESSION, s));
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
	 * Crates new CriteriaEntry for {@code RANGE [lowerBound TO upperBound]}
	 * 
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	public Criteria between(Object lowerBound, Object upperBound) {
		return between(lowerBound, upperBound, true, true);
	}

	/**
	 * Crates new CriteriaEntry for {@code RANGE [lowerBound TO upperBound]}
	 * 
	 * @param lowerBound
	 * @param upperBound
	 * @param includeLowerBound
	 * @param includeUppderBound
	 * @return
	 */
	public Criteria between(Object lowerBound, Object upperBound, boolean includeLowerBound, boolean includeUppderBound) {
		criteria.add(new CriteriaEntry(OperationKey.BETWEEN, new Object[] { lowerBound, upperBound, includeLowerBound,
				includeUppderBound }));
		return this;
	}

	/**
	 * Crates new CriteriaEntry for {@code RANGE [* TO upperBound&#125;}
	 * 
	 * @param upperBound
	 * @return
	 */
	public Criteria lessThan(Object upperBound) {
		between(null, upperBound, true, false);
		return this;
	}

	/**
	 * Crates new CriteriaEntry for {@code RANGE [* TO upperBound]}
	 * 
	 * @param upperBound
	 * @return
	 */
	public Criteria lessThanEqual(Object upperBound) {
		between(null, upperBound);
		return this;
	}

	/**
	 * Crates new CriteriaEntry for {@code RANGE &#123;lowerBound TO *]}
	 * 
	 * @param lowerBound
	 * @return
	 */
	public Criteria greaterThan(Object lowerBound) {
		between(lowerBound, null, false, true);
		return this;
	}

	/**
	 * Crates new CriteriaEntry for {@code RANGE [lowerBound TO *]}
	 * 
	 * @param lowerBound
	 * @return
	 */
	public Criteria greaterThanEqual(Object lowerBound) {
		between(lowerBound, null);
		return this;
	}

	/**
	 * Crates new CriteriaEntry for multiple values {@code (arg0 arg1 arg2 ...)}
	 * 
	 * @param lowerBound
	 * @return
	 */
	public Criteria in(Object... values) {
		assertValuesPresent(values);
		return in(Arrays.asList(values));
	}

	/**
	 * Crates new CriteriaEntry for multiple values {@code (arg0 arg1 arg2 ...)}
	 * 
	 * @param c the collection containing the values to match against
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
	 * Creates new CriteriaEntry for {@code !geodist}
	 * 
	 * @param location Geolocation in degrees
	 * @param distance
	 * @return
	 */
	public Criteria within(GeoLocation location, Distance distance) {
		Assert.notNull(location);
		assertPositiveDistanceValue(distance);
		criteria.add(new CriteriaEntry(OperationKey.WITHIN, new Object[] { location,
				distance != null ? distance : new Distance(0) }));
		return this;
	}

	/**
	 * Creates new CriteriaEntriy for {@code !bbox} with exact coordinates
	 * 
	 * @param box
	 * @return
	 */
	public Criteria near(BoundingBox box) {
		criteria.add(new CriteriaEntry(OperationKey.NEAR, new Object[] { box }));
		return this;
	}

	/**
	 * Creates new CriteriaEntry for {@code !bbox} for a specified distance. The difference between this and
	 * {@code within} is this is approximate while {@code within} is exact.
	 * 
	 * @param location
	 * @param distance
	 * @return
	 */
	public Criteria near(GeoLocation location, Distance distance) {
		Assert.notNull(location);
		assertPositiveDistanceValue(distance);
		criteria.add(new CriteriaEntry(OperationKey.NEAR, new Object[] { location,
				distance != null ? distance : new Distance(0) }));
		return this;
	}

	/**
	 * Field targeted by this Criteria
	 * 
	 * @return
	 */
	public Field getField() {
		return this.field;
	}

	public Set<CriteriaEntry> getCriteriaEntries() {
		return Collections.unmodifiableSet(this.criteria);
	}

	/**
	 * Conjunction to be used with this criteria (AND | OR)
	 * 
	 * @return
	 */
	public String getConjunctionOperator() {
		return AND_OPERATOR;
	}

	/**
	 * Get the collection of criterias
	 * 
	 * @return
	 */
	public List<Criteria> getCriteriaChain() {
		return Collections.unmodifiableList(this.criteriaChain);
	}

	/**
	 * @return true if {@code not()} criteria
	 */
	public boolean isNegating() {
		return this.negating;
	}

	/**
	 * Boost criteria value
	 * 
	 * @return {@code Float.NaN} if not set
	 */
	public float getBoost() {
		return this.boost;
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

	static class OrCriteria extends Criteria {

		public OrCriteria() {
			super();
		}

		public OrCriteria(Field field) {
			super(field);
		}

		public OrCriteria(List<Criteria> criteriaChain, Field field) {
			super(criteriaChain, field);
		}

		public OrCriteria(List<Criteria> criteriaChain, String fieldname) {
			super(criteriaChain, fieldname);
		}

		public OrCriteria(String fieldname) {
			super(fieldname);
		}

		@Override
		public String getConjunctionOperator() {
			return OR_OPERATOR;
		}

	}

	public enum OperationKey {
		EQUALS("$equals"), CONTAINS("$contains"), STARTS_WITH("$startsWith"), ENDS_WITH("$endsWith"), EXPRESSION(
				"$expression"), BETWEEN("$between"), NEAR("$near"), WITHIN("$within");

		private final String key;

		private OperationKey(String key) {
			this.key = key;
		}

		public String getKey() {
			return this.key;
		}

	}

	public static class CriteriaEntry {

		private String key;
		private Object value;

		CriteriaEntry(OperationKey key, Object value) {
			this(key.getKey(), value);
		}

		CriteriaEntry(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

	}

}
