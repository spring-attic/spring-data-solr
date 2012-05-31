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
package at.pagu.soldockr.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.GroupParams;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import at.pagu.soldockr.ApiUsageException;
import at.pagu.soldockr.core.query.FacetOptions;
import at.pagu.soldockr.core.query.FacetQuery;
import at.pagu.soldockr.core.query.Field;
import at.pagu.soldockr.core.query.FilterQuery;
import at.pagu.soldockr.core.query.Query;
import at.pagu.soldockr.core.query.SolDockRQuery;

/**
 * The QueryParser takes a sol-dock-r Query and returns a SolrQuery.
 * All Query parameters are translated into the according SolrQuery fields.
 * <b>Example:</b> <code>
 * Query query = new SimpleQuery(new Criteria("field_1").is("value_1").and("field_2").startsWith("value_2")).addProjection("field_3").setPageRequest(new PageRequest(0, 10));
 * </code> Will be parsed to a SolrQuery that outputs the following <code>
 * q=field_1%3Avalue_1+AND+field_2%3Avalue_2*&fl=field_3&start=0&rows=10
 * </code>
 */
public class QueryParser {

  /**
   * Convert given Query into a SolrQuery executable via {@link SolrServer}
   * 
   * @param query
   * @return
   */
  public final SolrQuery constructSolrQuery(SolDockRQuery query) {
    Assert.notNull(query, "Cannot construct solrQuery from null value.");
    Assert.notNull(query.getCriteria(), "Query has to have a criteria.");

    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setParam(CommonParams.Q, getQueryString(query));
    if(query instanceof Query) {
      processQueryOptions(solrQuery, (Query)query);
    }
    if(query instanceof FacetQuery) {
      processFacetOptions(solrQuery, (FacetQuery)query);
    }
    return solrQuery;
  }
  
  private void processQueryOptions(SolrQuery solrQuery, Query query) {
    appendPagination(solrQuery, query.getPageRequest());
    appendProjectionOnFields(solrQuery, query.getProjectionOnFields());
    appendGroupByFields(solrQuery, query.getGroupByFields());
    appendFilterQuery(solrQuery, query.getFilterQueries());
  }
  
  private void processFacetOptions(SolrQuery solrQuery, FacetQuery query) {
    appendFacetingOnFields(solrQuery, (FacetQuery)query);
  }
  

  /**
   * Get the queryString to use withSolrQuery.setParam(CommonParams.Q, "queryString"}
   * 
   * @param query
   * @return String representation of query without faceting, pagination, projection...
   */
  public String getQueryString(SolDockRQuery query) {
    if(query.getCriteria() == null) {
      return null;
    }
    return query.getCriteria().createQueryString();
  }

  private void appendPagination(SolrQuery query, Pageable pageable) {
    if (pageable == null) {
      return;
    }
    query.setStart(pageable.getOffset());
    query.setRows(pageable.getPageSize());
  }

  private void appendProjectionOnFields(SolrQuery solrQuery, List<Field> fields) {
    if (CollectionUtils.isEmpty(fields)) {
      return;
    }
    solrQuery.setParam(CommonParams.FL, StringUtils.join(fields, ","));
  }

  private void appendFacetingOnFields(SolrQuery solrQuery, FacetQuery query) {
    FacetOptions facetOptions = query.getFacetOptions();
    if (facetOptions == null || !facetOptions.hasFields()) {
      return;
    }
    solrQuery.setFacet(true);
    solrQuery.setParam(FacetParams.FACET_FIELD, StringUtils.join(facetOptions.getFacetOnFields(), ","));
    solrQuery.setFacetMinCount(facetOptions.getFacetMinCount());
    solrQuery.setFacetLimit(facetOptions.getPageable().getPageSize());
    if (facetOptions.getPageable().getPageNumber() > 0) {
      solrQuery.set(FacetParams.FACET_OFFSET, facetOptions.getPageable().getOffset());
    }
    if (FacetOptions.FacetSort.INDEX.equals(facetOptions.getFacetSort())) {
      solrQuery.setFacetSort(FacetParams.FACET_SORT_INDEX);
    }
  }

  private void appendGroupByFields(SolrQuery solrQuery, List<Field> fields) {
    if (CollectionUtils.isEmpty(fields)) {
      return;
    }

    if (fields.size() > 1) {
      // there is a bug in solj which prevents multiple grouping
      // although available via HTTP call
      throw new ApiUsageException("Cannot group on more than one field with current SolrJ API. Group on single field insead");
    }

    solrQuery.set(GroupParams.GROUP, true);
    solrQuery.setParam(GroupParams.GROUP_MAIN, true);

    for (Field field : fields) {
      solrQuery.add(GroupParams.GROUP_FIELD, field.getName());
    }
  }

  private void appendFilterQuery(SolrQuery solrQuery, List<FilterQuery> filterQueries) {
    if (CollectionUtils.isEmpty(filterQueries)) {
      return;
    }
    List<String> filterQueryStrings = getFilterQueryStrings(filterQueries);

    if (!filterQueryStrings.isEmpty()) {
      solrQuery.setFilterQueries(convertFilterQueryStringsToArray(filterQueryStrings));
    }
  }

  private String[] convertFilterQueryStringsToArray(List<String> filterQueryStrings) {
    String[] strResult = new String[filterQueryStrings.size()];
    filterQueryStrings.toArray(strResult);
    return strResult;
  }

  private List<String> getFilterQueryStrings(List<FilterQuery> filterQueries) {
    List<String> filterQueryStrings = new ArrayList<String>(filterQueries.size());

    for (FilterQuery filterQuery : filterQueries) {
      String filterQueryString = getQueryString(filterQuery);
      if (StringUtils.isNotBlank(filterQueryString)) {
        filterQueryStrings.add(filterQueryString);
      }
    }
    return filterQueryStrings;
  }
}
