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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import at.pagu.soldockr.ApiUsageException;

public class Criteria {

  public static final String WILDCARD = "*";
  public static final String CRITERIA_VALUE_SEPERATOR = " ";

  private static final String OR_OPERATOR = " OR ";
  private static final String DELIMINATOR = ":";
  private static final String AND_OPERATOR = " AND ";
  private static final String DOUBLEQUOTE = "\"";
  private static final String[] RESERVED_CHARS = {DOUBLEQUOTE, "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "~", "*", "?",
      ":", "\\"};
  private static final String[] RESERVED_CHARS_REPLACEMENT = {"\\" + DOUBLEQUOTE, "\\+", "\\-", "\\&\\&", "\\|\\|", "\\!", "\\(", "\\)",
      "\\{", "\\}", "\\[", "\\]", "\\^", "\\~", "\\*", "\\?", "\\:", "\\\\"};

  private Field field;
  private ArrayList<Criteria> criteriaChain = new ArrayList<Criteria>(1);

  private LinkedHashSet<CriteriaEntry> criteria = new LinkedHashSet<CriteriaEntry>();

  public Criteria() {}

  public Criteria(String fieldname) {
    this(new SimpleField(fieldname));
  }

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
    this.criteriaChain.addAll(criteriaChain);
    this.criteriaChain.add(this);
    this.field = field;
  }

  public static Criteria where(String field) {
    return where(new SimpleField(field));
  }

  public static Criteria where(Field field) {
    return new Criteria(field);
  }

  public Criteria and(Field field) {
    return new Criteria(this.criteriaChain, field);
  }

  public Criteria and(String fieldname) {
    return new Criteria(this.criteriaChain, fieldname);
  }

  public Criteria and(Criteria criteria) {
    this.criteriaChain.add(criteria);
    return this;
  }

  public Criteria and(Criteria... criterias) {
    this.criteriaChain.addAll(Arrays.asList(criterias));
    return this;
  }

  public Criteria or(Field field) {
    return new Criteria(this.criteriaChain, field) {
      @Override
      public String getConjunctionOperator() {
        return OR_OPERATOR;
      }
    };
  }

  public Criteria or(String fieldname) {
    return or(new SimpleField(fieldname));
  }

  public Criteria is(Object o) {
    criteria.add(new CriteriaEntry("$equals", o));
    return this;
  }

  public Criteria contains(String s) {
    assertNoBlankInWildcardedQuery(s, true, true);
    criteria.add(new CriteriaEntry("$contains", s));
    return this;
  }

  public Criteria startsWith(String s) {
    assertNoBlankInWildcardedQuery(s, true, false);
    criteria.add(new CriteriaEntry("$startsWith", s));
    return this;
  }

  public Criteria endsWith(String s) {
    assertNoBlankInWildcardedQuery(s, false, true);
    criteria.add(new CriteriaEntry("$endsWith", s));
    return this;
  }

  public Criteria expression(String s) {
    criteria.add(new CriteriaEntry("$expression", s));
    return this;
  }
  
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

  private String createQueryFragmentForCriteria(Criteria chainedCriteria) {
    StringBuilder queryFragment = new StringBuilder();
    Iterator<CriteriaEntry> it = chainedCriteria.criteria.iterator();
    boolean singeEntryCriteria = (chainedCriteria.criteria.size() == 1);
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
    return queryFragment.toString();
  }


  private String processCriteriaEntry(String key, Object value) {
    if(value == null) {
      return null;
    }
    
    // do not filter espressions
    if (StringUtils.equals("$expression", key)) {
      return value.toString();
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

    return filteredValue.toString();
  }

  private Object filterCriteriaValue(Object criteriaValue) {
    if (criteriaValue == null || !(criteriaValue instanceof String)) {
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
      throw new ApiUsageException("Cannot constructQuery '" + (leadingWildcard ? "*" : "") + "\"" + searchString + "\""
          + (trailingWildcard ? "*" : "") + "'. Use epxression or mulitple clauses instead.");
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
