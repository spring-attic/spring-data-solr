/*
 * Copyright 2012 - 2018 the original author or authors.
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
package org.springframework.data.solr.core;

import static org.apache.solr.common.params.CommonParams.*;
import static org.apache.solr.common.params.DisMaxParams.*;
import static org.apache.solr.common.params.SimpleParams.QF;

import java.util.ArrayList;
import java.util.Collection;
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
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.FacetOptions.FacetParameter;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithDateRangeParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithFacetParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithNumericRangeParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithRangeParameters;
import org.springframework.data.solr.core.query.Function.Context.Target;
import org.springframework.data.solr.core.query.HighlightOptions.FieldWithHighlightParameters;
import org.springframework.data.solr.core.query.HighlightOptions.HighlightParameter;
import org.springframework.lang.Nullable;
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
 * @author Matthew Hall
 */
public class DefaultQueryParser extends QueryParserBase<SolrDataQuery> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQueryParser.class);

	/**
	 * Create a new {@link DefaultQueryParser} using the provided {@link MappingContext} to map {@link Field fields} to
	 * domain domain type {@link org.springframework.data.mapping.PersistentProperty properties}.
	 *
	 * @param mappingContext can be {@literal null}.
	 * @since 4.0
	 */
	public DefaultQueryParser(@Nullable MappingContext mappingContext) {
		super(mappingContext);
	}

	/**
	 * Convert given Query into a SolrQuery executable via {@link org.apache.solr.client.solrj.SolrClient}
	 *
	 * @param query the source query to turn into a {@link SolrQuery}.
	 * @param domainType can be {@literal null}.
	 * @return
	 */
	@Override
	public final SolrQuery doConstructSolrQuery(SolrDataQuery query, @Nullable Class<?> domainType) {

		Assert.notNull(query, "Cannot construct solrQuery from null value.");
		Assert.notNull(query.getCriteria(), "Query has to have a criteria.");

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setParam(CommonParams.Q, getQueryString(query, domainType));

		if (query instanceof Query) {
			processQueryOptions(solrQuery, (Query) query, domainType);
		}

		if (query instanceof FacetQuery) {
			processFacetOptions(solrQuery, (FacetQuery) query, domainType);
		}

		if (query instanceof HighlightQuery) {
			processHighlightOptions(solrQuery, (HighlightQuery) query, domainType);
		}

		if (query instanceof DisMaxQuery) {
			processDisMaxOptions(solrQuery, (DisMaxQuery) query);
		}

		return solrQuery;
	}

	private void processQueryOptions(SolrQuery solrQuery, Query query, @Nullable Class<?> domainType) {

		appendPagination(solrQuery, query.getOffset(), query.getRows());
		appendProjectionOnFields(solrQuery, query.getProjectionOnFields(), domainType);
		appendFilterQuery(solrQuery, query.getFilterQueries(), domainType);
		appendSort(solrQuery, query.getSort(), domainType);
		appendDefaultOperator(solrQuery, query.getDefaultOperator());
		appendTimeAllowed(solrQuery, query.getTimeAllowed());
		appendDefType(solrQuery, query.getDefType());
		appendRequestHandler(solrQuery, query.getRequestHandler());

		processGroupOptions(solrQuery, query, domainType);
		processStatsOptions(solrQuery, query, domainType);
		processSpellcheckOptions(solrQuery, query, domainType);

		appendGeoParametersIfRequired(solrQuery, query, domainType);

		LOGGER.debug("Constructed SolrQuery:\r\n {}", solrQuery);
	}

	protected void processDisMaxOptions(SolrQuery solrQuery, DisMaxQuery disMaxQuery) {

		if (disMaxQuery == null || disMaxQuery.getDisMaxOptions() == null) {
			return;
		}

		DisMaxOptions disMaxOptions = disMaxQuery.getDisMaxOptions();

		solrQuery.set("defType", "dismax");

		setSolrParamIfPresent(solrQuery, DF, disMaxOptions.getDefaultField());

		setSolrParamIfPresent(solrQuery, ALTQ, disMaxOptions.getAltQuery());
		setSolrParamIfPresent(solrQuery, QF, disMaxOptions.getQueryFunction());
		setSolrParamIfPresent(solrQuery, MM, disMaxOptions.getMinimumMatch());

		setSolrParamIfPresent(solrQuery, BQ, disMaxOptions.getBoostQuery());
		setSolrParamIfPresent(solrQuery, BF, disMaxOptions.getBoostFunction());
		setSolrParamIfPresent(solrQuery, PF, disMaxOptions.getPhraseFunction());

		setSolrParamIfPresent(solrQuery, PS, disMaxOptions.getPhraseSlop() == null ? null :
				String.valueOf(disMaxOptions.getPhraseSlop()));
		setSolrParamIfPresent(solrQuery, QS, disMaxOptions.getQuerySlop() == null ? null : String.valueOf(disMaxOptions.getQuerySlop()));
		setSolrParamIfPresent(solrQuery, TIE, disMaxOptions.getTie() == null ? null : String.valueOf(disMaxOptions.getTie()));
	}

	private static void setSolrParamIfPresent(SolrQuery solrQuery, String param, String value) {
		if (!org.springframework.util.StringUtils.isEmpty(value)) {
			solrQuery.setParam(param, value);
		}
	}

	private void processFacetOptions(SolrQuery solrQuery, FacetQuery query, @Nullable Class<?> domainType) {

		if (enableFaceting(solrQuery, query)) {
			appendFacetingOnFields(solrQuery, query, domainType);
			appendFacetingQueries(solrQuery, query, domainType);
			appendFacetingOnPivot(solrQuery, query, domainType);
			appendRangeFacetingOnFields(solrQuery, query, domainType);
		}
	}

	private void setObjectNameOnGroupQuery(Query query, Object object, String name) {

		if (query instanceof NamedObjectsQuery) {
			((NamedObjectsQuery) query).setName(object, name);
		}
	}

	private void processStatsOptions(SolrQuery solrQuery, Query query, @Nullable Class<?> domainType) {

		StatsOptions statsOptions = query.getStatsOptions();

		if (statsOptions == null
				|| (CollectionUtils.isEmpty(statsOptions.getFields()) && CollectionUtils.isEmpty(statsOptions.getFacets())
						&& CollectionUtils.isEmpty(statsOptions.getSelectiveFacets()))) {
			return;
		}

		solrQuery.set(StatsParams.STATS, true);

		for (Field field : statsOptions.getFields()) {

			String mappedFieldName = getMappedFieldName(field, domainType);
			solrQuery.add(StatsParams.STATS_FIELD, mappedFieldName);

			String selectiveCalcDistinctParam = CommonParams.FIELD + "." + mappedFieldName + "."
					+ StatsParams.STATS_CALC_DISTINCT;
			Boolean selectiveCountDistincts = statsOptions.isSelectiveCalcDistincts(field);

			if (selectiveCountDistincts != null) {
				solrQuery.add(selectiveCalcDistinctParam, String.valueOf(selectiveCountDistincts.booleanValue()));
			}
		}

		for (Field field : statsOptions.getFacets()) {
			solrQuery.add(StatsParams.STATS_FACET, getMappedFieldName(field, domainType));
		}

		for (Entry<Field, Collection<Field>> entry : statsOptions.getSelectiveFacets().entrySet()) {

			Field field = entry.getKey();
			String prefix = CommonParams.FIELD + "." + getMappedFieldName(field, domainType) + ".";

			String paramName = prefix + StatsParams.STATS_FACET;
			for (Field facetField : entry.getValue()) {
				solrQuery.add(paramName, getMappedFieldName(facetField, domainType));
			}
		}
	}

	private void processGroupOptions(SolrQuery solrQuery, Query query, Class<?> domainType) {

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
				solrQuery.add(GroupParams.GROUP_FIELD, getMappedFieldName(field, domainType));
			}
		}

		if (!CollectionUtils.isEmpty(groupOptions.getGroupByFunctions())) {
			for (Function function : groupOptions.getGroupByFunctions()) {
				String functionFragment = createFunctionFragment(function, 0, domainType, Target.QUERY);
				setObjectNameOnGroupQuery(query, function, functionFragment);
				solrQuery.add(GroupParams.GROUP_FUNC, functionFragment);
			}
		}

		if (!CollectionUtils.isEmpty(groupOptions.getGroupByQueries())) {
			for (Query groupQuery : groupOptions.getGroupByQueries()) {
				String queryFragment = getQueryString(groupQuery, domainType);
				setObjectNameOnGroupQuery(query, groupQuery, queryFragment);
				solrQuery.add(GroupParams.GROUP_QUERY, queryFragment);
			}
		}

		if (groupOptions.getSort() != null) {

			for (Order order : groupOptions.getSort()) {
				solrQuery.add(GroupParams.GROUP_SORT, getMappedFieldName(order.getProperty().trim(), domainType) + " "
						+ (order.isAscending() ? ORDER.asc : ORDER.desc));
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

	private void processSpellcheckOptions(SolrQuery solrQuery, Query query, @Nullable Class<?> domainType) {

		if (query.getSpellcheckOptions() == null) {
			return;
		}

		SpellcheckOptions options = query.getSpellcheckOptions();

		if (options.getQuery() != null && options.getQuery().getCriteria() != null) {
			solrQuery.set(SpellingParams.SPELLCHECK_Q,
					createQueryStringFromCriteria(options.getQuery().getCriteria(), domainType));
		}

		ModifiableSolrParams params = new ModifiableSolrParams();
		params.add("spellcheck", "on");
		for (Entry<String, Object> entry : options.getParams().entrySet()) {

			if (entry.getValue() instanceof Iterable<?>) {
				for (Object o : ((Iterable<?>) entry.getValue())) {
					params.add(entry.getKey(), o.toString());
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
	 * @param solrQuery the target {@link SolrQuery}
	 * @param query the source query.
	 * @param domainType used for mapping fields to properties. Can be {@literal null}.
	 */
	protected void processHighlightOptions(SolrQuery solrQuery, HighlightQuery query, @Nullable Class<?> domainType) {

		if (query.hasHighlightOptions()) {

			HighlightOptions highlightOptions = query.getHighlightOptions();
			solrQuery.setHighlight(true);

			if (!highlightOptions.hasFields()) {
				solrQuery.addHighlightField(HighlightOptions.ALL_FIELDS.getName());
			} else {
				for (Field field : highlightOptions.getFields()) {
					solrQuery.addHighlightField(getMappedFieldName(field, domainType));
				}
				for (FieldWithHighlightParameters fieldWithHighlightParameters : highlightOptions
						.getFieldsWithHighlightParameters()) {
					addPerFieldHighlightParameters(solrQuery, fieldWithHighlightParameters, domainType);
				}
			}

			for (HighlightParameter option : highlightOptions.getHighlightParameters()) {
				addOptionToSolrQuery(solrQuery, option);
			}

			if (highlightOptions.hasQuery()) {
				solrQuery.add(HighlightParams.Q, getQueryString(highlightOptions.getQuery(), domainType));
			}
		}
	}

	private void addOptionToSolrQuery(SolrQuery solrQuery, QueryParameter option) {

		if (option != null && StringUtils.isNotBlank(option.getName())) {
			solrQuery.add(option.getName(), conversionService.convert(option.getValue(), String.class));
		}
	}

	private void addFieldSpecificParameterToSolrQuery(SolrQuery solrQuery, Field field, QueryParameter option,
			@Nullable Class<?> domainType) {

		if (option != null && field != null && StringUtils.isNotBlank(option.getName())) {
			if (option.getValue() == null) {
				solrQuery.add(createPerFieldOverrideParameterName(field, option.getName(), domainType), (String) null);
			} else {
				String value = option.getValue().toString();
				if (conversionService.canConvert(option.getValue().getClass(), String.class)) {
					value = conversionService.convert(option.getValue(), String.class);
				}
				solrQuery.add(createPerFieldOverrideParameterName(field, option.getName(), domainType), value);
			}
		}
	}

	private void addPerFieldHighlightParameters(SolrQuery solrQuery, FieldWithHighlightParameters field,
			@Nullable Class<?> domainType) {

		for (HighlightParameter option : field) {
			addFieldSpecificParameterToSolrQuery(solrQuery, field, option, domainType);
		}
	}

	/**
	 * @param field the source field.
	 * @param parameterName the parameter name to append
	 * @param domainType used for mapping fields to properties. Can be {@literal null}.
	 * @return
	 */
	protected String createPerFieldOverrideParameterName(Field field, String parameterName,
			@Nullable Class<?> domainType) {
		return "f." + getMappedFieldName(field, domainType) + "." + parameterName;
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
			long offset = Math.max(0, facetOptions.getPageable().getOffset());
			solrQuery.set(FacetParams.FACET_OFFSET, "" + offset);
		}
		if (FacetOptions.FacetSort.INDEX.equals(facetOptions.getFacetSort())) {
			solrQuery.setFacetSort(FacetParams.FACET_SORT_INDEX);
		}
		return true;
	}

	private void appendFacetingOnFields(SolrQuery solrQuery, FacetQuery query, @Nullable Class<?> domainType) {

		FacetOptions facetOptions = query.getFacetOptions();
		solrQuery.addFacetField(convertFieldListToStringArray(facetOptions.getFacetOnFields(), domainType));

		if (facetOptions.hasFacetPrefix()) {
			solrQuery.setFacetPrefix(facetOptions.getFacetPrefix());
		}
		for (FieldWithFacetParameters parametrizedField : facetOptions.getFieldsWithParameters()) {
			addPerFieldFacetParameters(solrQuery, parametrizedField, domainType);
			if (parametrizedField.getSort() != null && FacetOptions.FacetSort.INDEX.equals(parametrizedField.getSort())) {
				addFieldSpecificParameterToSolrQuery(solrQuery, parametrizedField,
						new FacetParameter(FacetParams.FACET_SORT, FacetParams.FACET_SORT_INDEX), domainType);
			}

		}
	}

	private void addPerFieldFacetParameters(SolrQuery solrQuery, FieldWithFacetParameters field,
			@Nullable Class<?> domainType) {

		for (FacetParameter parameter : field) {
			addFieldSpecificParameterToSolrQuery(solrQuery, field, parameter, domainType);
		}
	}

	private void appendRangeFacetingOnFields(SolrQuery solrQuery, FacetQuery query, @Nullable Class<?> domainType) {

		FacetOptions facetRangeOptions = query.getFacetOptions();

		if (facetRangeOptions == null) {
			return;
		}

		for (FieldWithRangeParameters<?, ?, ?> rangeField : facetRangeOptions.getFieldsWithRangeParameters()) {

			if (rangeField instanceof FieldWithDateRangeParameters) {
				appendFieldFacetingByDateRange(solrQuery, (FieldWithDateRangeParameters) rangeField, domainType);
			} else if (rangeField instanceof FieldWithNumericRangeParameters) {
				appendFieldFacetingByNumberRange(solrQuery, (FieldWithNumericRangeParameters) rangeField, domainType);
			}

			if (rangeField.getHardEnd() != null && rangeField.getHardEnd()) {
				FacetParameter param = new FacetParameter(FacetParams.FACET_RANGE_HARD_END, true);
				addFieldSpecificParameterToSolrQuery(solrQuery, rangeField, param, domainType);
			}

			if (rangeField.getOther() != null) {
				FacetParameter param = new FacetParameter(FacetParams.FACET_RANGE_OTHER, rangeField.getOther());
				addFieldSpecificParameterToSolrQuery(solrQuery, rangeField, param, domainType);
			}

			if (rangeField.getInclude() != null) {
				FacetParameter param = new FacetParameter(FacetParams.FACET_RANGE_INCLUDE, rangeField.getInclude());
				addFieldSpecificParameterToSolrQuery(solrQuery, rangeField, param, domainType);
			}

		}
	}

	private void appendFieldFacetingByNumberRange(SolrQuery solrQuery, FieldWithNumericRangeParameters field,
			@Nullable Class<?> domainType) {

		solrQuery.addNumericRangeFacet( //
				getMappedFieldName(field, domainType), //
				field.getStart(), //
				field.getEnd(), //
				field.getGap());
	}

	private void appendFieldFacetingByDateRange(SolrQuery solrQuery, FieldWithDateRangeParameters field,
			@Nullable Class<?> domainType) {

		solrQuery.addDateRangeFacet( //
				getMappedFieldName(field, domainType), //
				field.getStart(), //
				field.getEnd(), //
				field.getGap());
	}

	private void appendFacetingQueries(SolrQuery solrQuery, FacetQuery query, @Nullable Class<?> domainType) {

		FacetOptions facetOptions = query.getFacetOptions();
		for (SolrDataQuery fq : facetOptions.getFacetQueries()) {
			String facetQueryString = getQueryString(fq, domainType);
			if (StringUtils.isNotBlank(facetQueryString)) {
				solrQuery.addFacetQuery(facetQueryString);
			}
		}
	}

	private void appendFacetingOnPivot(SolrQuery solrQuery, FacetQuery query, @Nullable Class<?> domainType) {

		FacetOptions facetOptions = query.getFacetOptions();
		String[] pivotFields = convertFieldListToStringArray(facetOptions.getFacetOnPivots(), domainType);
		solrQuery.addFacetPivotField(pivotFields);
	}

	/**
	 * Set filter filter queries for {@link SolrQuery}
	 *
	 * @param solrQuery
	 * @param filterQueries
	 * @param domainType used for mapping fields to properties. Can be {@literal null}.
	 */
	protected void appendFilterQuery(SolrQuery solrQuery, List<FilterQuery> filterQueries,
			@Nullable Class<?> domainType) {

		if (CollectionUtils.isEmpty(filterQueries)) {
			return;
		}

		List<String> filterQueryStrings = getFilterQueryStrings(filterQueries, domainType);

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
	protected void appendSort(SolrQuery solrQuery, @Nullable Sort sort, @Nullable Class<?> domainType) {

		if (sort == null) {
			return;
		}

		for (Order order : sort) {
			solrQuery.addSort(getMappedFieldName(order.getProperty(), domainType),
					order.isAscending() ? ORDER.asc : ORDER.desc);
		}
	}

	private String[] convertFieldListToStringArray(List<? extends Field> fields, @Nullable Class<?> domainType) {

		String[] strResult = new String[fields.size()];
		for (int i = 0; i < fields.size(); i++) {

			Field field = fields.get(i);

			if (field instanceof PivotField) {

				if (field.getName().contains(",")) {

					String[] args = field.getName().split(",");
					String[] mapped = new String[args.length];

					for (int j = 0; j < args.length; j++) {
						mapped[j] = getMappedFieldName(args[j], domainType);
					}

					strResult[i] = org.springframework.util.StringUtils.arrayToCommaDelimitedString(mapped);
				} else {
					strResult[i] = field.getName();
				}

			} else {
				strResult[i] = getMappedFieldName(field, domainType);
			}
		}
		return strResult;
	}

	private String[] convertStringListToArray(List<String> listOfString) {

		String[] strResult = new String[listOfString.size()];
		listOfString.toArray(strResult);
		return strResult;
	}

	private List<String> getFilterQueryStrings(List<FilterQuery> filterQueries, @Nullable Class<?> domainType) {
		List<String> filterQueryStrings = new ArrayList<>(filterQueries.size());

		for (FilterQuery filterQuery : filterQueries) {
			String filterQueryString = getQueryString(filterQuery, domainType);
			if (StringUtils.isNotBlank(filterQueryString)) {
				filterQueryStrings.add(filterQueryString);
			}
		}
		return filterQueryStrings;
	}
}
