/*
 * Copyright 2012 - 2013 the original author or authors.
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
package org.springframework.data.solr.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.HighlightParams;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.solr.VersionUtil;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetOptions.FacetParameter;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithFacetParameters;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightOptions.FieldWithHighlightParameters;
import org.springframework.data.solr.core.query.HighlightOptions.HighlightParameter;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.QueryParameter;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of {@link QueryParser}. <br/>
 * Creates executable {@link SolrQuery} from {@link Query} by traversing {@link Criteria}. Reserved characters like
 * {@code +} or {@code -} will be escaped to form a valid query.
 * 
 * @author Christoph Strobl
 * @author John Dorman
 * @author Rosty Kerei
 * @author Luke Corpe
 * @author Andrey Paramonov
 * @author Philipp Jardas
 * @author Francisco Spaeth
 */
public class DefaultQueryParser extends QueryParserBase<SolrDataQuery> {

	/**
	 * Convert given Query into a SolrQuery executable via {@link org.apache.solr.client.solrj.SolrServer}
	 * 
	 * @param query
	 * @return
	 */
	@Override
	public final SolrQuery doConstructSolrQuery(SolrDataQuery query) {
		Assert.notNull(query, "Cannot construct solrQuery from null value.");
		Assert.notNull(query.getCriteria(), "Query has to have a criteria.");

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setParam(CommonParams.Q, getQueryString(query));
		if (query instanceof Query) {
			processQueryOptions(solrQuery, (Query) query);
		}
		if (query instanceof FacetQuery) {
			processFacetOptions(solrQuery, (FacetQuery) query);
		}
		if (query instanceof HighlightQuery) {
			processHighlightOptions(solrQuery, (HighlightQuery) query);
		}
		return solrQuery;
	}

	private void processQueryOptions(SolrQuery solrQuery, Query query) {
		appendPagination(solrQuery, query.getPageRequest());
		appendProjectionOnFields(solrQuery, query.getProjectionOnFields());
		appendGroupByFields(solrQuery, query.getGroupByFields());
		appendFilterQuery(solrQuery, query.getFilterQueries());
		appendSort(solrQuery, query.getSort());
		appendDefaultOperator(solrQuery, query.getDefaultOperator());
		appendTimeAllowed(solrQuery, query.getTimeAllowed());
		appendDefType(solrQuery, query.getDefType());
		appendQueryFields(solrQuery, query.getQueryFields());
		appendRequestHandler(solrQuery, query.getRequestHandler());
	}

	private void processFacetOptions(SolrQuery solrQuery, FacetQuery query) {
		if (enableFaceting(solrQuery, query)) {
			appendFacetingOnFields(solrQuery, (FacetQuery) query);
			appendFacetingQueries(solrQuery, (FacetQuery) query);
			appendFacetingOnPivot(solrQuery, (FacetQuery) query);
		}
	}

	/**
	 * Append highlighting parameters to {@link SolrQuery}
	 * 
	 * @param solrQuery
	 * @param query
	 */
	protected void processHighlightOptions(SolrQuery solrQuery, HighlightQuery query) {
		if (query.hasHighlightOptions()) {
			HighlightOptions highlightOptions = query.getHighlightOptions();
			solrQuery.setHighlight(true);
			if (!highlightOptions.hasFields()) {
				solrQuery.addHighlightField(HighlightOptions.ALL_FIELDS.getName());
			} else {
				for (Field field : highlightOptions.getFields()) {
					solrQuery.addHighlightField(field.getName());
				}
				for (FieldWithHighlightParameters fieldWithHighlightParameters : highlightOptions
						.getFieldsWithHighlightParameters()) {
					addPerFieldHighlightParameters(solrQuery, fieldWithHighlightParameters);
				}
			}
			for (HighlightParameter option : highlightOptions.getHighlightParameters()) {
				addOptionToSolrQuery(solrQuery, option);
			}
			if (highlightOptions.hasQuery()) {
				solrQuery.add(HighlightParams.Q, getQueryString(highlightOptions.getQuery()));
			}
		}
	}

	private void addOptionToSolrQuery(SolrQuery solrQuery, QueryParameter option) {
		if (option != null && StringUtils.isNotBlank(option.getName())) {
			solrQuery.add(option.getName(), conversionService.convert(option.getValue(), String.class));
		}
	}

	private void addFieldSpecificParameterToSolrQuery(SolrQuery solrQuery, Field field, QueryParameter option) {
		if (option != null && field != null && StringUtils.isNotBlank(option.getName())) {
			if (option.getValue() == null) {
				solrQuery.add(createPerFieldOverrideParameterName(field, option.getName()), (String) null);
			} else {
				String value = option.getValue().toString();
				if (conversionService.canConvert(option.getValue().getClass(), String.class)) {
					value = conversionService.convert(option.getValue(), String.class);
				}
				solrQuery.add(createPerFieldOverrideParameterName(field, option.getName()), value);
			}
		}
	}

	private void addPerFieldHighlightParameters(SolrQuery solrQuery, FieldWithHighlightParameters field) {
		for (HighlightParameter option : field) {
			addFieldSpecificParameterToSolrQuery(solrQuery, field, option);
		}
	}

	protected String createPerFieldOverrideParameterName(Field field, String parameterName) {
		return "f." + field.getName() + "." + parameterName;
	}

	private boolean enableFaceting(SolrQuery solrQuery, FacetQuery query) {
		FacetOptions facetOptions = query.getFacetOptions();
		if (facetOptions == null
				|| (!facetOptions.hasFields() && !facetOptions.hasFacetQueries() && !facetOptions.hasPivotFields())) {
			return false;
		}
		solrQuery.setFacet(true);
		solrQuery.setFacetMinCount(facetOptions.getFacetMinCount());
		solrQuery.setFacetLimit(facetOptions.getPageable().getPageSize());
		if (facetOptions.getPageable().getPageNumber() > 0) {
			solrQuery.set(FacetParams.FACET_OFFSET, facetOptions.getPageable().getOffset());
		}
		if (FacetOptions.FacetSort.INDEX.equals(facetOptions.getFacetSort())) {
			solrQuery.setFacetSort(FacetParams.FACET_SORT_INDEX);
		}
		return true;
	}

	private void appendFacetingOnFields(SolrQuery solrQuery, FacetQuery query) {
		FacetOptions facetOptions = query.getFacetOptions();
		if (facetOptions.getPageable().getPageNumber() > 0) {
			solrQuery.set(FacetParams.FACET_OFFSET, facetOptions.getPageable().getOffset());
		}
		solrQuery.addFacetField(convertFieldListToStringArray(facetOptions.getFacetOnFields()));
		if (facetOptions.hasFacetPrefix()) {
			solrQuery.setFacetPrefix(facetOptions.getFacetPrefix());
		}
		for (FieldWithFacetParameters parametrizedField : facetOptions.getFieldsWithParameters()) {
			addPerFieldFacetParameters(solrQuery, parametrizedField);
			if (parametrizedField.getSort() != null && FacetOptions.FacetSort.INDEX.equals(parametrizedField.getSort())) {
				addFieldSpecificParameterToSolrQuery(solrQuery, parametrizedField, new FacetParameter(FacetParams.FACET_SORT,
						FacetParams.FACET_SORT_INDEX));
			}

		}
	}

	private void addPerFieldFacetParameters(SolrQuery solrQuery, FieldWithFacetParameters field) {
		for (FacetParameter parameter : field) {
			addFieldSpecificParameterToSolrQuery(solrQuery, field, parameter);
		}
	}

	private void appendFacetingQueries(SolrQuery solrQuery, FacetQuery query) {
		FacetOptions facetOptions = query.getFacetOptions();
		for (SolrDataQuery fq : facetOptions.getFacetQueries()) {
			String facetQueryString = getQueryString(fq);
			if (StringUtils.isNotBlank(facetQueryString)) {
				solrQuery.addFacetQuery(facetQueryString);
			}
		}
	}

	private void appendFacetingOnPivot(SolrQuery solrQuery, FacetQuery query) {
		if (VersionUtil.isSolr3XAvailable()) {
			throw new UnsupportedOperationException(
					"Pivot Facets are not available for solr version lower than 4.x - Please check your depdendencies.");
		}

		FacetOptions facetOptions = query.getFacetOptions();
		String[] pivotFields = convertFieldListToStringArray(facetOptions.getFacetOnPivots());
		solrQuery.addFacetPivotField(pivotFields);
	}

	/**
	 * Append grouping parameters to {@link SolrQuery}
	 * 
	 * @param solrQuery
	 * @param fields
	 */
	protected void appendGroupByFields(SolrQuery solrQuery, List<Field> fields) {
		if (CollectionUtils.isEmpty(fields)) {
			return;
		}

		if (fields.size() > 1) {
			// there is a bug in solj which prevents multiple grouping
			// although available via HTTP call
			throw new InvalidDataAccessApiUsageException(
					"Cannot group on more than one field with current SolrJ API. Group on single field insead");
		}

		solrQuery.set(GroupParams.GROUP, true);
		solrQuery.setParam(GroupParams.GROUP_MAIN, true);

		for (Field field : fields) {
			solrQuery.add(GroupParams.GROUP_FIELD, field.getName());
		}
	}

	/**
	 * Set filter filter queries for {@link SolrQuery}
	 * 
	 * @param solrQuery
	 * @param filterQueries
	 */
	protected void appendFilterQuery(SolrQuery solrQuery, List<FilterQuery> filterQueries) {
		if (CollectionUtils.isEmpty(filterQueries)) {
			return;
		}
		List<String> filterQueryStrings = getFilterQueryStrings(filterQueries);

		if (!filterQueryStrings.isEmpty()) {
			solrQuery.setFilterQueries(convertStringListToArray(filterQueryStrings));
		}
	}

	/**
	 * Append sorting parameters to {@link SolrQuery}
	 * 
	 * @param solrQuery
	 * @param sort
	 */
	@SuppressWarnings("deprecation")
	protected void appendSort(SolrQuery solrQuery, Sort sort) {
		if (sort == null) {
			return;
		}

		for (Order order : sort) {
			// addSort which is to be used instead of addSortField is not available in versions below 4.2.0
			if (VersionUtil.isSolr420Available()) {
				solrQuery.addSort(order.getProperty(), order.isAscending() ? ORDER.asc : ORDER.desc);
			} else {
				solrQuery.addSortField(order.getProperty(), order.isAscending() ? ORDER.asc : ORDER.desc);
			}
		}
	}

	private String[] convertFieldListToStringArray(List<? extends Field> fields) {
		String[] strResult = new String[fields.size()];
		for (int i = 0; i < fields.size(); i++) {
			strResult[i] = fields.get(i).getName();
		}
		return strResult;
	}

	private String[] convertStringListToArray(List<String> listOfString) {
		String[] strResult = new String[listOfString.size()];
		listOfString.toArray(strResult);
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
