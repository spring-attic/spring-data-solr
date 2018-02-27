/*
 * Copyright 2012 - 2016 the original author or authors.
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SpellingParams;
import org.apache.solr.common.params.StatsParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.solr.VersionUtil;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetOptions.FacetParameter;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithDateRangeParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithFacetParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithNumericRangeParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithRangeParameters;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.Function;
import org.springframework.data.solr.core.query.GroupOptions;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightOptions.FieldWithHighlightParameters;
import org.springframework.data.solr.core.query.HighlightOptions.HighlightParameter;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.QueryParameter;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.core.query.SpellcheckOptions;
import org.springframework.data.solr.core.query.StatsOptions;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

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
 * @author Joachim Uhrlaß
 * @author Petar Tahchiev
 * @author Juan Manuel de Blas
 */
public class DefaultQueryParser extends QueryParserBase<SolrDataQuery> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQueryParser.class);

	/**
	 * Convert given Query into a SolrQuery executable via {@link org.apache.solr.client.solrj.SolrClient}
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
		appendPagination(solrQuery, query.getOffset(), query.getRows());
		appendProjectionOnFields(solrQuery, query.getProjectionOnFields());
		appendFilterQuery(solrQuery, query.getFilterQueries());
		appendSort(solrQuery, query.getSort());
		appendDefaultOperator(solrQuery, query.getDefaultOperator());
		appendTimeAllowed(solrQuery, query.getTimeAllowed());
		appendDefType(solrQuery, query.getDefType());
		appendRequestHandler(solrQuery, query.getRequestHandler());

		processGroupOptions(solrQuery, query);
		processStatsOptions(solrQuery, query);
		processSpellcheckOptions(solrQuery, query);

		LOGGER.debug("Constructed SolrQuery:\r\n {}", solrQuery);
	}

	private void processFacetOptions(SolrQuery solrQuery, FacetQuery query) {
		if (enableFaceting(solrQuery, query)) {
			appendFacetingOnFields(solrQuery, (FacetQuery) query);
			appendFacetingQueries(solrQuery, (FacetQuery) query);
			appendFacetingOnPivot(solrQuery, (FacetQuery) query);
			appendRangeFacetingOnFields(solrQuery, (FacetQuery) query);
		}
	}

	private void setObjectNameOnGroupQuery(Query query, Object object, String name) {

		if (query instanceof NamedObjectsQuery) {
			((NamedObjectsQuery) query).setName(object, name);
		}
	}

	private void processStatsOptions(SolrQuery solrQuery, Query query) {
		StatsOptions statsOptions = query.getStatsOptions();

		if (statsOptions == null
				|| (CollectionUtils.isEmpty(statsOptions.getFields()) && CollectionUtils.isEmpty(statsOptions.getFacets())
						&& CollectionUtils.isEmpty(statsOptions.getSelectiveFacets()))) {
			return;
		}

		solrQuery.set(StatsParams.STATS, true);

		for (Field field : statsOptions.getFields()) {
			solrQuery.add(StatsParams.STATS_FIELD, field.getName());

			String selectiveCalcDistinctParam = CommonParams.FIELD + "." + field.getName() + "."
					+ StatsParams.STATS_CALC_DISTINCT;
			Boolean selectiveCountDistincts = statsOptions.isSelectiveCalcDistincts(field);

			if (selectiveCountDistincts != null) {
				solrQuery.add(selectiveCalcDistinctParam, String.valueOf(selectiveCountDistincts.booleanValue()));
			}

		}

		for (Field field : statsOptions.getFacets()) {
			solrQuery.add(StatsParams.STATS_FACET, field.getName());
		}

		for (Entry<Field, Collection<Field>> entry : statsOptions.getSelectiveFacets().entrySet()) {

			Field field = entry.getKey();
			String prefix = CommonParams.FIELD + "." + field.getName() + ".";

			String paramName = prefix + StatsParams.STATS_FACET;
			for (Field facetField : entry.getValue()) {
				solrQuery.add(paramName, facetField.getName());
			}

		}

	}

	private void processGroupOptions(SolrQuery solrQuery, Query query) {

		GroupOptions groupOptions = query.getGroupOptions();

		if (groupOptions == null || (CollectionUtils.isEmpty(groupOptions.getGroupByFields())
				&& CollectionUtils.isEmpty(groupOptions.getGroupByFunctions())
				&& CollectionUtils.isEmpty(groupOptions.getGroupByQueries()))) {
			return;
		}

		solrQuery.set(GroupParams.GROUP, true);
		solrQuery.set(GroupParams.GROUP_MAIN, groupOptions.isGroupMain());
		solrQuery.set(GroupParams.GROUP_FORMAT, "grouped");

		if (!CollectionUtils.isEmpty(groupOptions.getGroupByFields())) {
			for (Field field : groupOptions.getGroupByFields()) {
				solrQuery.add(GroupParams.GROUP_FIELD, field.getName());
			}
		}

		if (!CollectionUtils.isEmpty(groupOptions.getGroupByFunctions())) {
			for (Function function : groupOptions.getGroupByFunctions()) {
				String functionFragment = createFunctionFragment(function, 0);
				setObjectNameOnGroupQuery(query, function, functionFragment);
				solrQuery.add(GroupParams.GROUP_FUNC, functionFragment);
			}
		}

		if (!CollectionUtils.isEmpty(groupOptions.getGroupByQueries())) {
			for (Query groupQuery : groupOptions.getGroupByQueries()) {
				String queryFragment = getQueryString(groupQuery);
				setObjectNameOnGroupQuery(query, groupQuery, queryFragment);
				solrQuery.add(GroupParams.GROUP_QUERY, queryFragment);
			}
		}

		if (groupOptions.getSort() != null) {

			for (Order order : groupOptions.getSort()) {
				solrQuery.add(GroupParams.GROUP_SORT,
						order.getProperty().trim() + " " + (order.isAscending() ? ORDER.asc : ORDER.desc));
			}
		}

		if (groupOptions.getCachePercent() > 0) {
			solrQuery.add(GroupParams.GROUP_CACHE_PERCENTAGE, String.valueOf(groupOptions.getCachePercent()));
		}

		if (groupOptions.getLimit() != null) {
			solrQuery.set(GroupParams.GROUP_LIMIT, groupOptions.getLimit());
		}

		if (groupOptions.getOffset() != null && groupOptions.getOffset() >= 0) {
			solrQuery.set(GroupParams.GROUP_OFFSET, groupOptions.getOffset());
		}

		solrQuery.set(GroupParams.GROUP_TOTAL_COUNT, groupOptions.isTotalCount());
		solrQuery.set(GroupParams.GROUP_FACET, groupOptions.isGroupFacets());
		solrQuery.set(GroupParams.GROUP_TRUNCATE, groupOptions.isTruncateFacets());
	}

	private void processSpellcheckOptions(SolrQuery solrQuery, Query query) {

		if (query.getSpellcheckOptions() == null) {
			return;
		}

		SpellcheckOptions options = query.getSpellcheckOptions();

		if (options.getQuery() != null) {
			solrQuery.set(SpellingParams.SPELLCHECK_Q, createQueryStringFromCriteria(options.getQuery().getCriteria()));
		}

		ModifiableSolrParams params = new ModifiableSolrParams();
		params.add("spellcheck", "on");
		for (Entry<String, Object> entry : options.getParams().entrySet()) {

			if (entry.getValue() instanceof Iterable<?>) {
				Iterator<?> it = ((Iterable<?>) entry.getValue()).iterator();
				while (it.hasNext()) {
					params.add(entry.getKey(), it.next().toString());
				}
			} else if (ObjectUtils.isArray(entry.getValue())) {
				for (Object o : ObjectUtils.toObjectArray(entry.getValue())) {
					params.add(entry.getKey(), o.toString());
				}
			} else {
				params.add(entry.getKey(), entry.getValue().toString());
			}
		}
		solrQuery.add(params);
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
		if (facetOptions == null || !facetOptions.hasFacets()) {
			return false;
		}
		solrQuery.setFacet(true);
		solrQuery.setFacetMinCount(facetOptions.getFacetMinCount());
		solrQuery.setFacetLimit(facetOptions.getPageable().getPageSize());
		if (facetOptions.getPageable().getPageNumber() > 0) {
			int offset = Math.max(0, facetOptions.getPageable().getOffset());
			solrQuery.set(FacetParams.FACET_OFFSET, offset);
		}
		if (FacetOptions.FacetSort.INDEX.equals(facetOptions.getFacetSort())) {
			solrQuery.setFacetSort(FacetParams.FACET_SORT_INDEX);
		}
		return true;
	}

	private void appendFacetingOnFields(SolrQuery solrQuery, FacetQuery query) {
		FacetOptions facetOptions = query.getFacetOptions();
		solrQuery.addFacetField(convertFieldListToStringArray(facetOptions.getFacetOnFields()));
		if (facetOptions.hasFacetPrefix()) {
			solrQuery.setFacetPrefix(facetOptions.getFacetPrefix());
		}
		for (FieldWithFacetParameters parametrizedField : facetOptions.getFieldsWithParameters()) {
			addPerFieldFacetParameters(solrQuery, parametrizedField);
			if (parametrizedField.getSort() != null && FacetOptions.FacetSort.INDEX.equals(parametrizedField.getSort())) {
				addFieldSpecificParameterToSolrQuery(solrQuery, parametrizedField,
						new FacetParameter(FacetParams.FACET_SORT, FacetParams.FACET_SORT_INDEX));
			}

		}
	}

	private void addPerFieldFacetParameters(SolrQuery solrQuery, FieldWithFacetParameters field) {
		for (FacetParameter parameter : field) {
			addFieldSpecificParameterToSolrQuery(solrQuery, field, parameter);
		}
	}

	private void appendRangeFacetingOnFields(SolrQuery solrQuery, FacetQuery query) {
		FacetOptions facetRangeOptions = query.getFacetOptions();

		if (facetRangeOptions == null) {
			return;
		}

		for (FieldWithRangeParameters<?, ?, ?> rangeField : facetRangeOptions.getFieldsWithRangeParameters()) {

			if (rangeField instanceof FieldWithDateRangeParameters) {
				appendFieldFacetingByDateRange(solrQuery, (FieldWithDateRangeParameters) rangeField);
			} else if (rangeField instanceof FieldWithNumericRangeParameters) {
				appendFieldFacetingByNumberRange(solrQuery, (FieldWithNumericRangeParameters) rangeField);
			}

			if (rangeField.getHardEnd() != null && rangeField.getHardEnd()) {
				FacetParameter param = new FacetParameter(FacetParams.FACET_RANGE_HARD_END, true);
				addFieldSpecificParameterToSolrQuery(solrQuery, rangeField, param);
			}
			if (rangeField.getOther() != null) {
				FacetParameter param = new FacetParameter(FacetParams.FACET_RANGE_OTHER, rangeField.getOther());
				addFieldSpecificParameterToSolrQuery(solrQuery, rangeField, param);
			}

			if (rangeField.getInclude() != null) {
				FacetParameter param = new FacetParameter(FacetParams.FACET_RANGE_INCLUDE, rangeField.getInclude());
				addFieldSpecificParameterToSolrQuery(solrQuery, rangeField, param);
			}

		}
	}

	private void appendFieldFacetingByNumberRange(SolrQuery solrQuery, FieldWithNumericRangeParameters field) {
		solrQuery.addNumericRangeFacet( //
				field.getName(), //
				field.getStart(), //
				field.getEnd(), //
				field.getGap());
	}

	private void appendFieldFacetingByDateRange(SolrQuery solrQuery, FieldWithDateRangeParameters field) {
		solrQuery.addDateRangeFacet( //
				field.getName(), //
				field.getStart(), //
				field.getEnd(), //
				field.getGap());
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
	protected void appendSort(SolrQuery solrQuery, Sort sort) {
		if (sort == null) {
			return;
		}

		for (Order order : sort) {
			solrQuery.addSort(order.getProperty(), order.isAscending() ? ORDER.asc : ORDER.desc);
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
