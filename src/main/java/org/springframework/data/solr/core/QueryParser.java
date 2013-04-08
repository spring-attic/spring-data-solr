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
package org.springframework.data.solr.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.SpatialParams;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.solr.VersionUtil;
import org.springframework.data.solr.core.convert.DateTimeConverters;
import org.springframework.data.solr.core.convert.NumberConverters;
import org.springframework.data.solr.core.geo.BoundingBox;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.GeoConverters;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Criteria.CriteriaEntry;
import org.springframework.data.solr.core.query.Criteria.OperationKey;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.Query.Operator;
import org.springframework.data.solr.core.query.QueryStringHolder;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * The QueryParser takes a spring-data-solr Query and returns a SolrQuery. All Query parameters are translated into the
 * according SolrQuery fields. <b>Example:</b> <code>
 *  Query query = new SimpleQuery(new Criteria("field_1").is("value_1").and("field_2").startsWith("value_2")).addProjection("field_3").setPageRequest(new PageRequest(0, 10));
 * </code> Will be parsed to a SolrQuery that outputs the following <code>
 *  q=field_1%3Avalue_1+AND+field_2%3Avalue_2*&fl=field_3&start=0&rows=10
 * </code>
 * 
 * @author Christoph Strobl
 * @author John Dorman
 * @author Rosty Kerei
 * @author Luke Corpe
 * @author Andrey Paramonov
 */
public class QueryParser {

	public static final String WILDCARD = "*";
	private static final String DELIMINATOR = ":";
	public static final String CRITERIA_VALUE_SEPERATOR = " ";
	private static final String RANGE_OPERATOR = " TO ";
	private static final String DOUBLEQUOTE = "\"";

	private static final String[] RESERVED_CHARS = { DOUBLEQUOTE, "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[",
			"]", "^", "~", "*", "?", ":", "\\" };
	private static final String[] RESERVED_CHARS_REPLACEMENT = { "\\" + DOUBLEQUOTE, "\\+", "\\-", "\\&\\&", "\\|\\|",
			"\\!", "\\(", "\\)", "\\{", "\\}", "\\[", "\\]", "\\^", "\\~", "\\*", "\\?", "\\:", "\\\\" };

	private final GenericConversionService conversionService = new GenericConversionService();

	{
		if (!conversionService.canConvert(java.util.Date.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JavaDateConverter.INSTANCE);
		}
		if (!conversionService.canConvert(Number.class, String.class)) {
			conversionService.addConverter(NumberConverters.NumberConverter.INSTANCE);
		}
		if (!conversionService.canConvert(GeoLocation.class, String.class)) {
			conversionService.addConverter(GeoConverters.GeoLocationToStringConverter.INSTANCE);
		}
		if (!conversionService.canConvert(Distance.class, String.class)) {
			conversionService.addConverter(GeoConverters.DistanceToStringConverter.INSTANCE);
		}
		if (VersionUtil.isJodaTimeAvailable()) {
			if (!conversionService.canConvert(org.joda.time.ReadableInstant.class, String.class)) {
				conversionService.addConverter(DateTimeConverters.JodaDateTimeConverter.INSTANCE);
			}
			if (!conversionService.canConvert(org.joda.time.LocalDateTime.class, String.class)) {
				conversionService.addConverter(DateTimeConverters.JodaLocalDateTimeConverter.INSTANCE);
			}
		}
	}

	/**
	 * Convert given Query into a SolrQuery executable via {@link SolrServer}
	 * 
	 * @param query
	 * @return
	 */
	public final SolrQuery constructSolrQuery(SolrDataQuery query) {
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
		appendRequestHandler(solrQuery, query.getRequestHandler());
	}

	private void processFacetOptions(SolrQuery solrQuery, FacetQuery query) {
		if (enableFaceting(solrQuery, query)) {
			appendFacetingOnFields(solrQuery, (FacetQuery) query);
			appendFacetingQueries(solrQuery, (FacetQuery) query);
		}
	}

	/**
	 * Get the queryString to use withSolrQuery.setParam(CommonParams.Q, "queryString"}
	 * 
	 * @param query
	 * @return String representation of query without faceting, pagination, projection...
	 */
	public String getQueryString(SolrDataQuery query) {
		if (query.getCriteria() == null) {
			return null;
		}
		return createQueryStringFromCriteria(query.getCriteria());
	}

	String createQueryStringFromCriteria(Criteria criteria) {
		StringBuilder query = new StringBuilder(StringUtils.EMPTY);

		ListIterator<Criteria> chainIterator = criteria.getCriteriaChain().listIterator();
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
		Iterator<CriteriaEntry> it = chainedCriteria.getCriteriaEntries().iterator();
		boolean singeEntryCriteria = (chainedCriteria.getCriteriaEntries().size() == 1);
		if (chainedCriteria.getField() != null) {
			String fieldName = chainedCriteria.getField().getName();
			if (chainedCriteria.isNegating()) {
				fieldName = "-" + fieldName;
			}
			if (!containsFunctionCriteria(chainedCriteria.getCriteriaEntries())) {
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
			if (!Float.isNaN(chainedCriteria.getBoost())) {
				queryFragment.append("^" + chainedCriteria.getBoost());
			}
		} else {
			if (chainedCriteria instanceof QueryStringHolder) {
				return ((QueryStringHolder) chainedCriteria).getQueryString();
			}
		}
		return queryFragment.toString();
	}

	private boolean containsFunctionCriteria(Set<CriteriaEntry> chainedCriterias) {
		for (CriteriaEntry entry : chainedCriterias) {
			if (StringUtils.equals(OperationKey.WITHIN.getKey(), entry.getKey())) {
				return true;
			} else if (StringUtils.equals(OperationKey.NEAR.getKey(), entry.getKey())) {
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
			String rangeFragment = ((Boolean) args[2]).booleanValue() ? "[" : "{";
			rangeFragment += createRangeFragment(args[0], args[1]);
			rangeFragment += ((Boolean) args[3]).booleanValue() ? "]" : "}";
			return rangeFragment;
		}

		if (StringUtils.equals(OperationKey.WITHIN.getKey(), key)) {
			Object[] args = (Object[]) value;
			return createSpatialFunctionFragment(fieldName, (GeoLocation) args[0], (Distance) args[1], "geofilt");
		}

		if (StringUtils.equals(OperationKey.NEAR.getKey(), key)) {
			String nearFragment;
			Object[] args = (Object[]) value;
			if (args[0] instanceof BoundingBox) {
				BoundingBox box = (BoundingBox) args[0];
				nearFragment = fieldName + ":[";
				nearFragment += createRangeFragment(box.getGeoLocationStart(), box.getGeoLocationEnd());
				nearFragment += "]";
			} else {
				nearFragment = createSpatialFunctionFragment(fieldName, (GeoLocation) args[0], (Distance) args[1], "bbox");
			}
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

	private String createRangeFragment(Object rangeStart, Object rangeEnd) {
		String rangeFragment = "";
		rangeFragment += (rangeStart != null ? filterCriteriaValue(rangeStart) : WILDCARD);
		rangeFragment += RANGE_OPERATOR;
		rangeFragment += (rangeEnd != null ? filterCriteriaValue(rangeEnd) : WILDCARD);
		return rangeFragment;
	}

	private String createSpatialFunctionFragment(String fieldName, GeoLocation location, Distance distance,
			String function) {
		String spatialFragment = "{!" + function + " " + SpatialParams.POINT + "=";
		spatialFragment += filterCriteriaValue(location);
		spatialFragment += " " + SpatialParams.FIELD + "=" + fieldName;
		spatialFragment += " " + SpatialParams.DISTANCE + "=" + filterCriteriaValue(distance);
		spatialFragment += "}";
		return spatialFragment;
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

	private boolean enableFaceting(SolrQuery solrQuery, FacetQuery query) {
		FacetOptions facetOptions = query.getFacetOptions();
		if (facetOptions == null || (!facetOptions.hasFields() && !facetOptions.hasFacetQueries())) {
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

	private void appendGroupByFields(SolrQuery solrQuery, List<Field> fields) {
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

	private void appendFilterQuery(SolrQuery solrQuery, List<FilterQuery> filterQueries) {
		if (CollectionUtils.isEmpty(filterQueries)) {
			return;
		}
		List<String> filterQueryStrings = getFilterQueryStrings(filterQueries);

		if (!filterQueryStrings.isEmpty()) {
			solrQuery.setFilterQueries(convertStringListToArray(filterQueryStrings));
		}
	}

	private void appendSort(SolrQuery solrQuery, Sort sort) {
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

	private void appendDefaultOperator(SolrQuery solrQuery, Operator defaultOperator) {
		if (defaultOperator != null && !Query.Operator.NONE.equals(defaultOperator)) {
			solrQuery.set("q.op", defaultOperator.asQueryStringRepresentation());
		}
	}

	private void appendTimeAllowed(SolrQuery solrQuery, Integer timeAllowed) {
		if (timeAllowed != null) {
			solrQuery.setTimeAllowed(timeAllowed);
		}
	}

	private void appendDefType(SolrQuery solrQuery, String defType) {
		if (!StringUtils.isEmpty(defType)) {
			solrQuery.set("defType", defType);
		}
	}

	private void appendRequestHandler(SolrQuery solrQuery, String requestHandler) {
		if (!StringUtils.isEmpty(requestHandler)) {
			solrQuery.add(CommonParams.QT, requestHandler);
		}
	}

	private String[] convertFieldListToStringArray(List<Field> fields) {
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

	/**
	 * Register an additional converter for transforming object values to solr readable format
	 * 
	 * @param converter
	 */
	public void registerConverter(Converter<?, ?> converter) {
		conversionService.addConverter(converter);
	}

}
