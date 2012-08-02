/*
 * Copyright (C) 2012 sol-dock-r authors.
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
package at.pagu.soldockr.core.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import at.pagu.soldockr.ApiUsageException;

/**
 * Criteria is the central class when constructing queries.
 * It follows more or less a fluent API style, which allows to easily chain together multiple criteria.
 */
public class Criteria implements QueryStringHolder {

  public static final String WILDCARD = "*";
  public static final String CRITERIA_VALUE_SEPERATOR = " ";

  private static final String OR_OPERATOR = " OR ";
  private static final String DELIMINATOR = ":";
  private static final String AND_OPERATOR = " AND ";
  private static final String RANGE_OPERATOR = " TO ";
  private static final String DOUBLEQUOTE = "\"";
  private static final String[] RESERVED_CHARS = {DOUBLEQUOTE, "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "~", "*", "?", ":", "\\"};
  private static final String[] RESERVED_CHARS_REPLACEMENT = {"\\" + DOUBLEQUOTE, "\\+", "\\-", "\\&\\&", "\\|\\|", "\\!", "\\(", "\\)", "\\{", "\\}", "\\[", "\\]", "\\^", "\\~", "\\*", "\\?", "\\:",
      "\\\\"};

  private Field field;
  private float boost = Float.NaN;

  private List<Criteria> criteriaChain = new ArrayList<Criteria>(1);

  private Set<CriteriaEntry> criteria = new LinkedHashSet<CriteriaEntry>();

  public Criteria() {}

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
    return new Criteria(this.criteriaChain, field) {
      @Override
      public String getConjunctionOperator() {
        return OR_OPERATOR;
      }
    };
  }

  /**
   * Chain using OR
   * 
   * @param field
   * @return
   */
  public Criteria or(Criteria criteria) {
    Assert.notNull(criteria, "Cannot chain 'null' criteria.");

    Criteria orConnectedCritiera = new Criteria(this.criteriaChain, criteria.getField()) {
      @Override
      public String getConjunctionOperator() {
        return OR_OPERATOR;
      }
    };
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
    criteria.add(new CriteriaEntry("$equals", o));
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
    criteria.add(new CriteriaEntry("$contains", s));
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
    criteria.add(new CriteriaEntry("$startsWith", s));
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
    criteria.add(new CriteriaEntry("$endsWith", s));
    return this;
  }

  /**
   * Crates new CriteriaEntry with trailing -
   * 
   * @param s
   * @return
   */
  public Criteria isNot(Object o) {
    criteria.add(new CriteriaEntry("$isNot", o));
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
    if (levenshteinDistance != Float.NaN) {
      if (levenshteinDistance < 0 || levenshteinDistance > 1) {
        throw new ApiUsageException("Levenshtein Distance has to be within its bounds (0.0 - 1.0).");
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
    criteria.add(new CriteriaEntry("$expression", s));
    return this;
  }

  public Criteria boost(float boost) {
    if (boost < 0) {
      throw new ApiUsageException("Boost must not be negative.");
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
      throw new ApiUsageException("Range [* TO *] is not allowed");
    }

    criteria.add(new CriteriaEntry("$between", new Object[] {lowerBound, upperBound}));
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
      throw new ApiUsageException("At least one element " + (values.length > 0 ? ("of argument of type " + values[1].getClass().getName()) : "") + " has to be present.");
    }
    return in(Arrays.asList(values));
  }

  /**
   * Creates a criterion using the $in operator
   * 
   * @param c the collection containing the values to match against
   * @return
   */
  public Criteria in(Collection<?> values) {
    Assert.notNull(values, "Collection of 'in' values must not be null");
    if (!values.isEmpty()) {
      for (Object value : values) {
        if (value instanceof Collection) {
          in((Collection<?>) value);
        } else {
          is(value);
        }
      }
    }
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
      queryFragment.append(chainedCriteria.field.getName());
      queryFragment.append(DELIMINATOR);
      if (!singeEntryCriteria) {
        queryFragment.append("(");
      }
      while (it.hasNext()) {
        CriteriaEntry entry = it.next();
        queryFragment.append(processCriteriaEntry(entry.getKey(), entry.getValue()));
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

  private String processCriteriaEntry(String key, Object value) {
    if (value == null) {
      return null;
    }

    // do not filter espressions
    if (StringUtils.equals("$expression", key)) {
      return value.toString();
    }

    if (StringUtils.equals("$between", key)) {
      Object[] args = (Object[]) value;
      String rangeFragment = "[";
      rangeFragment += args[0] != null ? filterCriteriaValue(args[0]) : WILDCARD;
      rangeFragment += RANGE_OPERATOR;
      rangeFragment += args[1] != null ? filterCriteriaValue(args[1]) : WILDCARD;
      rangeFragment += "]";
      return rangeFragment;
    }

    Object filteredValue = filterCriteriaValue(value);
    if (StringUtils.equals("$contains", key)) {
      return WILDCARD + filteredValue + WILDCARD;
    }
    if (StringUtils.equals("$startsWith", key)) {
      return filteredValue + WILDCARD;
    }
    if (StringUtils.equals("$endsWith", key)) {
      return WILDCARD + filteredValue;
    }
    if (StringUtils.equals("$isNot", key)) {
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
      throw new ApiUsageException("Cannot constructQuery '" + (leadingWildcard ? "*" : "") + "\"" + searchString + "\"" + (trailingWildcard ? "*" : "")
          + "'. Use epxression or mulitple clauses instead.");
    }
  }

  public Field getField() {
    return this.field;
  }

  public String getConjunctionOperator() {
    return AND_OPERATOR;
  }

  List<Criteria> getCriteriaChain() {
    return this.criteriaChain;
  }

  class CriteriaEntry {

    private String key;
    private Object value;

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
