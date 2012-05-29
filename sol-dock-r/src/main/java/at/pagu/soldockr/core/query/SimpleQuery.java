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
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

public class SimpleQuery implements Query {

  public static final Pageable DEFAULT_PAGE = new PageRequest(0, DEFAULT_PAGE_SIZE);

  private Criteria criteria;
  private List<Field> projectionOnFields = new ArrayList<Field>(0);
  private List<Field> groupByFields = new ArrayList<Field>(0);
  private List<FilterQuery> filterQueries  = new ArrayList<FilterQuery>(0);;
  private FacetOptions facetOptions;
  private Pageable pageable = DEFAULT_PAGE;

  public SimpleQuery() {}

  public SimpleQuery(Criteria criteria) {
    this(criteria, null);
  }

  public SimpleQuery(Criteria criteria, Pageable pageable) {
    this.addCriteria(criteria);
    this.pageable = pageable;
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <T extends FilterQuery> T addCriteria(Criteria criteria) {
    Assert.notNull(criteria, "Cannot add null criteria.");
    Assert.notNull(criteria.getField(), "Cannot add criteria for null field.");
    Assert.hasText(criteria.getField().getName(), "Criteria.field.name must not be null/empty.");

    if (this.criteria == null) {
      this.criteria = criteria;
    } else {
      this.criteria.and(criteria);
    }
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <T extends Query> T addProjectionOnField(Field field) {
    Assert.notNull(field, "Field for projection must not be null.");
    Assert.hasText(field.getName(), "Field.name for projection must not be null/empty.");

    this.projectionOnFields.add(field);
    return (T) this;
  }

  public final <T extends Query> T addProjectionOnField(String fieldname) {
    return this.addProjectionOnField(new SimpleField(fieldname));
  }

  @SuppressWarnings("unchecked")
  public final <T extends Query> T addProjectionOnFields(Field... fields) {
    Assert.notEmpty(fields, "Cannot add projection on null/empty field list.");
    for (Field field : fields) {
      addProjectionOnField(field);
    }
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public final <T extends Query> T addProjectionOnFields(String... fieldnames) {
    Assert.notEmpty(fieldnames, "Cannot add projection on null/empty field list.");
    for (String fieldname : fieldnames) {
      addProjectionOnField(fieldname);
    }
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <T extends Query> T setPageRequest(Pageable pageable) {
    Assert.notNull(pageable);

    this.pageable = pageable;
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <T extends Query> T addGroupByField(Field field) {
    Assert.notNull(field, "Field for grouping must not be null.");
    Assert.hasText(field.getName(), "Field.name for grouping must not be null/empty.");

    this.groupByFields.add(field);
    return (T) this;
  }

  public final <T extends Query> T addGroupByField(String fieldname) {
    return addGroupByField(new SimpleField(fieldname));
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <T extends Query> T setFacetOptions(FacetOptions facetOptions) {
    if (facetOptions != null) {
      Assert.isTrue(facetOptions.hasFields(), "Cannot set facet options having no fields.");
    }
    this.facetOptions = facetOptions;
    return (T) this;
  }

  @Override
  public Pageable getPageRequest() {
    return this.pageable;
  }

  @Override
  public List<Field> getGroupByFields() {
    return Collections.unmodifiableList(this.groupByFields);
  }

  @Override
  public List<Field> getProjectionOnFields() {
    return Collections.unmodifiableList(this.projectionOnFields);
  }

  @Override
  public FacetOptions getFacetOptions() {
    return this.facetOptions;
  }

  public Criteria getCriteria() {
    return this.criteria;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Query> T addFilterQuery(Query filterQuery) {
    this.filterQueries.add(filterQuery);
    return (T) this;
  }

  @Override
  public List<FilterQuery> getFilterQueries() {
    return Collections.unmodifiableList(this.filterQueries);
  }

}
