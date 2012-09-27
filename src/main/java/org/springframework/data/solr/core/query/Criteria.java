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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.solr.core.convert.DateTimeConverters;
import org.springframework.data.solr.core.convert.GeoConverters;
import org.springframework.data.solr.core.convert.NumberConverters;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.util.Assert;

/**
 * Criteria is the central class when constructing queries. It follows more or less a fluent API style, which allows to
 * easily chain together multiple criteria.
 * 
 * @author Christoph Strobl
 */
public class Criteria implements QueryStringHolder {

	public static final String WILDCARD = "*";
	public static final String CRITERIA_VALUE_SEPERATOR = " ";

	private static final String OR_OPERATOR = " OR ";
	private static final String DELIMINATOR = ":";
	private static final String AND_OPERATOR = " AND ";
	private static final String RANGE_OPERATOR = " TO ";
	private static final String DOUBLEQUOTE = "\"";
	private static final String[] RESERVED_CHARS = { DOUBLEQUOTE, "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[",
			"]", "^", "~", "*", "?", ":", "\\" };
	private static final String[] RESERVED_CHARS_REPLACEMENT = { "\\" + DOUBLEQUOTE, "\\+", "\\-", "\\&\\&", "\\|\\|",
			"\\!", "\\(", "\\)", "\\{", "\\}", "\\[", "\\]", "\\^", "\\~", "\\*", "\\?", "\\:", "\\\\" };

	private final GenericConversionService conversionService = new GenericConversionService();

	private Field field;
	private float boost = Float.NaN;

	private List<Criteria> criteriaChain = new ArrayList<Criteria>(1);

	private Set<CriteriaEntry> criteria = new LinkedHashSet<CriteriaEntry>();

	{
		if (!conversionService.canConvert(java.util.Date.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JavaDateConverter.INSTANCE);
		}
		if (!conversionService.canConvert(org.joda.time.ReadableInstant.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JodaDateTimeConverter.INSTANCE);
		}
		if (!conversionService.canConvert(org.joda.time.LocalDateTime.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JodaLocalDateTimeConverter.INSTANCE);
		}
		if (!conversionService.canConvert(Number.class, String.class)) {
			conversionService.addConverter(NumberConverters.NumberConverter.INSTANCE);
		}
		if (!conversionService.canConvert(GeoLocation.class, String.class)) {
			conversionService.addConverter(GeoConverters.GeoLocationToStringConverter.INSTANCE);
		}
	}

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
	 * Chain using AND
	 * 
	 * @param field
	 * @return
	 */
	public Criteria and(Field field) {
		return new Criteria(this.criteriaChain, field);
	}

	/**
	 * Chain using AND
	 * 
	 * @param field
	 * @return
	 */
	public Criteria and(String fieldname) {
		return new Criteria(this.criteriaChain, fieldname);
	}

	/**
	 * Chain using AND
	 * 
	 * @param field
	 * @return
	 */
	public Criteria and(Criteria criteria) {
		this.criteriaChain.add(criteria);
		return this;
	}

	/**
	 * Chain using AND
	 * 
	 * @param field
	 * @return
	 */
	public Criteria and(Criteria... criterias) {
		this.criteriaChain.addAll(Arrays.asList(criterias));
		return this;
	}

	/**
	 * Chain using OR
	 * 
	 * @param field
	 * @return
	 */
	public Criteria or(Field field) {
		return new OrCriteria(this.criteriaChain, field);
	}

	/**
	 * Chain using OR
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
	 * Chain using OR
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
		criteria.add(new CriteriaEntry(OperationKey.EQUALS, o));
		return this;
	}

	/**
	 * Crates new CriteriaEntry with leading and trailing wildcards
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
	 * Crates new CriteriaEntry with leading wildcard
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
	 * Crates new CriteriaEntry with trailing wildcards
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
	 * Crates new CriteriaEntry with trailing -
	 * 
	 * @param s
	 * @return
	 */
	public Criteria isNot(Object o) {
		criteria.add(new CriteriaEntry(OperationKey.IS_NOT, o));
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
		if (!Float.isNaN(levenshteinDistance)) {
			if (levenshteinDistance < 0 || levenshteinDistance > 1) {
				throw new InvalidDataAccessApiUsageException("Levenshtein Distance has to be within its bounds (0.0 - 1.0).");
			}
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
	 * Crates new CriteriaEntry for RANGE expressions [lowerBound TO upperBound]
	 * 
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	public Criteria between(Object lowerBound, Object upperBound) {
		if (lowerBound == null && upperBound == null) {
			throw new InvalidDataAccessApiUsageException("Range [* TO *] is not allowed");
		}

		criteria.add(new CriteriaEntry(OperationKey.BETWEEN, new Object[] { lowerBound, upperBound }));
		return this;
	}

	/**
	 * Crates new CriteriaEntry for RANGE [* TO upperBound]
	 * 
	 * @param upperBound
	 * @return
	 */
	public Criteria lessThanEqual(Object upperBound) {
		between(null, upperBound);
		return this;
	}

	/**
	 * Crates new CriteriaEntry for RANGE [lowerBound TO *]
	 * 
	 * @param lowerBound
	 * @return
	 */
	public Criteria greaterThanEqual(Object lowerBound) {
		between(lowerBound, null);
		return this;
	}

	/**
	 * Crates new CriteriaEntry for multiple values (arg0 arg1 arg2 ...)
	 * 
	 * @param lowerBound
	 * @return
	 */
	public Criteria in(Object... values) {
		if (values.length == 0 || (values.length > 1 && values[1] instanceof Collection)) {
			throw new InvalidDataAccessApiUsageException("At least one element "
					+ (values.length > 0 ? ("of argument of type " + values[1].getClass().getName()) : "")
					+ " has to be present.");
		}
		return in(Arrays.asList(values));
	}

	/**
	 * Crates new CriteriaEntry for multiple values (arg0 arg1 arg2 ...)
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
	public Criteria near(GeoLocation location, Distance distance) {
		Assert.notNull(location);
		if (distance != null) {
			if (distance.getValue() < 0) {
				throw new InvalidDataAccessApiUsageException("distance must not be negative.");
			}
		}
		criteria.add(new CriteriaEntry(OperationKey.NEAR, new Object[] { location,
				distance != null ? distance : new Distance(0) }));
		return this;
	}

	/**
	 * get the QueryString used for executing query
	 * 
	 * @return
	 */
	public String createQueryString() {
		StringBuilder query = new StringBuilder(StringUtils.EMPTY);

		ListIterator<Criteria> chainIterator = this.criteriaChain.listIterator();
		while (chainIterator.hasNext()) {
			Criteria chainedCriteria = chainIterator.next();

			query.append(createQueryFragmentForCriteria(chainedCriteria));

			if (chainIterator.hasNext()) {
				query.append(chainIterator.next().getConjunctionOperator());
				chainIterator.previous();
			}
		}

		return query.toString();
	}

	protected String createQueryFragmentForCriteria(Criteria chainedCriteria) {
		StringBuilder queryFragment = new StringBuilder();
		Iterator<CriteriaEntry> it = chainedCriteria.criteria.iterator();
		boolean singeEntryCriteria = (chainedCriteria.criteria.size() == 1);
		if (chainedCriteria.field != null) {
			String fieldName = chainedCriteria.field.getName();
			if (!containsFunctionCriteria(chainedCriteria.criteria)) {
				queryFragment.append(fieldName);
				queryFragment.append(DELIMINATOR);
			}
			if (!singeEntryCriteria) {
				queryFragment.append("(");
			}
			while (it.hasNext()) {
				CriteriaEntry entry = it.next();
				queryFragment.append(processCriteriaEntry(entry.getKey(), entry.getValue(), fieldName));
				if (it.hasNext()) {
					queryFragment.append(CRITERIA_VALUE_SEPERATOR);
				}
			}
			if (!singeEntryCriteria) {
				queryFragment.append(")");
			}
			if (!Float.isNaN(chainedCriteria.boost)) {
				queryFragment.append("^" + chainedCriteria.boost);
			}
		} else {
			return chainedCriteria.getQueryString();
		}
		return queryFragment.toString();
	}

	public String getQueryString() {
		return field != null ? createQueryString() : "";
	}

	private boolean containsFunctionCriteria(Set<CriteriaEntry> chainedCriterias) {
		for (CriteriaEntry entry : chainedCriterias) {
			if (StringUtils.equals(OperationKey.NEAR.getKey(), entry.getKey())) {
				return true;
			}
		}
		return false;
	}

	private String processCriteriaEntry(String key, Object value, String fieldName) {
		if (value == null) {
			return null;
		}

		// do not filter espressions
		if (StringUtils.equals(OperationKey.EXPRESSION.getKey(), key)) {
			return value.toString();
		}

		if (StringUtils.equals(OperationKey.BETWEEN.getKey(), key)) {
			Object[] args = (Object[]) value;
			String rangeFragment = "[";
			rangeFragment += args[0] != null ? filterCriteriaValue(args[0]) : WILDCARD;
			rangeFragment += RANGE_OPERATOR;
			rangeFragment += args[1] != null ? filterCriteriaValue(args[1]) : WILDCARD;
			rangeFragment += "]";
			return rangeFragment;
		}

		if (StringUtils.equals(OperationKey.NEAR.getKey(), key)) {
			String nearFragment = "{!geofilt pt=";
			Object[] args = (Object[]) value;
			nearFragment += filterCriteriaValue(args[0]);
			nearFragment += " sfield=" + fieldName;
			nearFragment += " d=" + ((Distance) args[1]).getValue();
			nearFragment += "}";
			return nearFragment;
		}

		Object filteredValue = filterCriteriaValue(value);
		if (StringUtils.equals(OperationKey.CONTAINS.getKey(), key)) {
			return WILDCARD + filteredValue + WILDCARD;
		}
		if (StringUtils.equals(OperationKey.STARTS_WITH.getKey(), key)) {
			return filteredValue + WILDCARD;
		}
		if (StringUtils.equals(OperationKey.ENDS_WITH.getKey(), key)) {
			return WILDCARD + filteredValue;
		}
		if (StringUtils.equals(OperationKey.IS_NOT.getKey(), key)) {
			return "-" + filteredValue;
		}

		if (StringUtils.startsWith(key, "$fuzzy")) {
			String sDistance = StringUtils.substringAfter(key, "$fuzzy#");
			float distance = Float.NaN;
			if (StringUtils.isNotBlank(sDistance)) {
				distance = Float.parseFloat(sDistance);
			}
			return filteredValue + "~" + (Float.isNaN(distance) ? "" : sDistance);
		}

		return filteredValue.toString();
	}

	private Object filterCriteriaValue(Object criteriaValue) {
		if (!(criteriaValue instanceof String)) {
			if (conversionService.canConvert(criteriaValue.getClass(), String.class)) {
				return conversionService.convert(criteriaValue, String.class);
			}
			return criteriaValue;
		}
		String value = escapeCriteriaValue((String) criteriaValue);
		return processWhiteSpaces(value);
	}

	private String escapeCriteriaValue(String criteriaValue) {
		return StringUtils.replaceEach(criteriaValue, RESERVED_CHARS, RESERVED_CHARS_REPLACEMENT);
	}

	private String processWhiteSpaces(String criteriaValue) {
		if (StringUtils.contains(criteriaValue, CRITERIA_VALUE_SEPERATOR)) {
			return DOUBLEQUOTE + criteriaValue + DOUBLEQUOTE;
		}
		return criteriaValue;
	}

	private void assertNoBlankInWildcardedQuery(String searchString, boolean leadingWildcard, boolean trailingWildcard) {
		if (StringUtils.contains(searchString, CRITERIA_VALUE_SEPERATOR)) {
			throw new InvalidDataAccessApiUsageException("Cannot constructQuery '" + (leadingWildcard ? "*" : "") + "\""
					+ searchString + "\"" + (trailingWildcard ? "*" : "") + "'. Use epxression or mulitple clauses instead.");
		}
	}

	/**
	 * Field targeted by this Criteria
	 * 
	 * @return
	 */
	public Field getField() {
		return this.field;
	}

	/**
	 * Conjunction to be used with this criteria (AND | OR)
	 * 
	 * @return
	 */
	public String getConjunctionOperator() {
		return AND_OPERATOR;
	}

	List<Criteria> getCriteriaChain() {
		return this.criteriaChain;
	}

	/**
	 * Register an additional converter for transforming object values to solr readable format
	 * 
	 * @param converter
	 */
	public void registerConverter(Converter<?, ?> converter) {
		conversionService.addConverter(converter);
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

	enum OperationKey {
		IS_NOT("$isNot"), EQUALS("$equals"), CONTAINS("$contains"), STARTS_WITH("$startsWith"), ENDS_WITH("$endsWith"), EXPRESSION(
				"$expression"), BETWEEN("$between"), NEAR("$near");

		private final String key;

		private OperationKey(String key) {
			this.key = key;
		}

		public String getKey() {
			return this.key;
		}

	}

	static class CriteriaEntry {

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
