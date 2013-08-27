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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SpatialParams;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Pageable;
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
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.Query.Operator;
import org.springframework.data.solr.core.query.QueryStringHolder;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Base Implementation of {@link QueryParser} providing common functions for creating
 * {@link org.apache.solr.client.solrj.SolrQuery}.
 * 
 * @author Christoph Strobl
 */
public abstract class QueryParserBase<QUERYTPYE extends SolrDataQuery> implements QueryParser {

	protected static final String CRITERIA_VALUE_SEPERATOR = " ";
	protected static final String DELIMINATOR = ":";
	protected static final String NOT = "-";
	protected static final String BOOST = "^";

	protected final GenericConversionService conversionService = new GenericConversionService();
	private final List<CriteriaEntryProcessor> critieraEntryProcessors = new ArrayList<CriteriaEntryProcessor>();
	private final CriteriaEntryProcessor defaultProcessor = new DefaultProcessor();

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
		critieraEntryProcessors.add(new ExpressionProcessor());
		critieraEntryProcessors.add(new BetweenProcessor());
		critieraEntryProcessors.add(new NearProcessor());
		critieraEntryProcessors.add(new WithinProcessor());
		critieraEntryProcessors.add(new FuzzyProcessor());
		critieraEntryProcessors.add(new SloppyProcessor());
		critieraEntryProcessors.add(new WildcardProcessor());
	}

	@Override
	public String getQueryString(SolrDataQuery query) {
		if (query.getCriteria() == null) {
			return null;
		}

		String queryString = createQueryStringFromCriteria(query.getCriteria());
		queryString = prependJoin(queryString, query);
		return queryString;
	}

	@Override
	public void registerConverter(Converter<?, ?> converter) {
		conversionService.addConverter(converter);
	}

	/**
	 * add another {@link CriteriaEntryProcessor}
	 * 
	 * @param processor
	 */
	public void addCriteriaEntryProcessor(CriteriaEntryProcessor processor) {
		this.critieraEntryProcessors.add(processor);
	}

	/**
	 * Iterates criteria list and concats query string fragments to form a valid query string to be used with
	 * {@link org.apache.solr.client.solrj.SolrQuery#setQuery(String)}
	 * 
	 * @param criteria
	 * @return
	 */
	protected String createQueryStringFromCriteria(Criteria criteria) {
		StringBuilder query = new StringBuilder("");

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

	/**
	 * Creates query string representation of a single critiera
	 * 
	 * @param criteria
	 * @return
	 */
	protected String createQueryFragmentForCriteria(Criteria criteria) {
		StringBuilder queryFragment = new StringBuilder();
		boolean singeEntryCriteria = (criteria.getCriteriaEntries().size() == 1);
		if (criteria.getField() != null) {
			String fieldName = criteria.getField().getName();
			if (criteria.isNegating()) {
				fieldName = NOT + fieldName;
			}
			if (!containsFunctionCriteria(criteria.getCriteriaEntries())) {
				queryFragment.append(fieldName);
				queryFragment.append(DELIMINATOR);
			}
			if (!singeEntryCriteria) {
				queryFragment.append("(");
			}

			CriteriaQueryStringValueProvider valueProvider = new CriteriaQueryStringValueProvider(criteria);
			while (valueProvider.hasNext()) {
				queryFragment.append(valueProvider.next());
				if (valueProvider.hasNext()) {
					queryFragment.append(CRITERIA_VALUE_SEPERATOR);
				}
			}

			if (!singeEntryCriteria) {
				queryFragment.append(")");
			}
			if (!Float.isNaN(criteria.getBoost())) {
				queryFragment.append(BOOST + criteria.getBoost());
			}
		} else {
			if (criteria instanceof QueryStringHolder) {
				return ((QueryStringHolder) criteria).getQueryString();
			}
		}
		return queryFragment.toString();
	}

	/**
	 * Prepend {@code !join from= to=} to given queryString
	 * 
	 * @param queryString
	 * @param query
	 * @return
	 */
	protected String prependJoin(String queryString, SolrDataQuery query) {
		if (query == null || query.getJoin() == null) {
			return queryString;
		}
		return "{!join from=" + query.getJoin().getFrom().getName() + " to=" + query.getJoin().getTo().getName() + "}"
				+ queryString;
	}

	/**
	 * Append pagination information {@code start, rows} to {@link SolrQuery}
	 * 
	 * @param query
	 * @param pageable
	 */
	protected void appendPagination(SolrQuery query, Pageable pageable) {
		if (pageable == null) {
			return;
		}
		query.setStart(pageable.getOffset());
		query.setRows(pageable.getPageSize());
	}

	/**
	 * Append field list to {@link SolrQuery}
	 * 
	 * @param solrQuery
	 * @param fields
	 */
	protected void appendProjectionOnFields(SolrQuery solrQuery, List<Field> fields) {
		if (CollectionUtils.isEmpty(fields)) {
			return;
		}
		solrQuery.setParam(CommonParams.FL, StringUtils.join(fields, ","));
	}

	/**
	 * Set {@code q.op} parameter for {@link SolrQuery}
	 * 
	 * @param solrQuery
	 * @param defaultOperator
	 */
	protected void appendDefaultOperator(SolrQuery solrQuery, Operator defaultOperator) {
		if (defaultOperator != null && !Query.Operator.NONE.equals(defaultOperator)) {
			solrQuery.set("q.op", defaultOperator.asQueryStringRepresentation());
		}
	}

	/**
	 * Set {@link SolrQuery#setTimeAllowed(Integer)}
	 * 
	 * @param solrQuery
	 * @param timeAllowed
	 */
	protected void appendTimeAllowed(SolrQuery solrQuery, Integer timeAllowed) {
		if (timeAllowed != null) {
			solrQuery.setTimeAllowed(timeAllowed);
		}
	}

	/**
	 * Set {@code defType} for {@link SolrQuery}
	 * 
	 * @param solrQuery
	 * @param defType
	 */
	protected void appendDefType(SolrQuery solrQuery, String defType) {
		if (StringUtils.isNotBlank(defType)) {
			solrQuery.set("defType", defType);
		}
	}

	/**
	 * Set request handler parameter for {@link SolrQuery}
	 * 
	 * @param solrQuery
	 * @param requestHandler
	 */
	protected void appendRequestHandler(SolrQuery solrQuery, String requestHandler) {
		if (StringUtils.isNotBlank(requestHandler)) {
			solrQuery.add(CommonParams.QT, requestHandler);
		}
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

	@SuppressWarnings("unchecked")
	@Override
	public SolrQuery constructSolrQuery(SolrDataQuery query) {
		return doConstructSolrQuery((QUERYTPYE) query);
	}

	public abstract SolrQuery doConstructSolrQuery(QUERYTPYE query);

	/**
	 * CriteriaEntryProcessor creates a solr reable query string representation for a given {@link CriteriaEntry}
	 * 
	 * @author Christoph Strobl
	 */
	public interface CriteriaEntryProcessor {

		/**
		 * 
		 * @param criteriaEntry
		 * @return true if criteriaEntry can be processed by this parser
		 */
		boolean canProcess(CriteriaEntry criteriaEntry);

		/**
		 * Create query string representation of given {@link CriteriaEntry}
		 * 
		 * @param criteriaEntry
		 * @param field
		 * @return
		 */
		Object process(CriteriaEntry criteriaEntry, Field field);

	}

	/**
	 * @author Christoph Strobl
	 */
	class CriteriaQueryStringValueProvider implements Iterator<String> {

		private final Criteria criteria;
		private Iterator<CriteriaEntry> delegate;

		CriteriaQueryStringValueProvider(Criteria criteria) {
			Assert.notNull(criteria, "Unable to provide values for 'null' criteria");

			this.criteria = criteria;
			this.delegate = criteria.getCriteriaEntries().iterator();
		}

		@SuppressWarnings("unchecked")
		private <T> T getCriteriaEntryValue(CriteriaEntry entry) {
			CriteriaEntryProcessor processor = findMatchingProcessor(entry);
			return (T) processor.process(entry, criteria.getField());
		}

		private CriteriaEntryProcessor findMatchingProcessor(CriteriaEntry criteriaEntry) {
			for (CriteriaEntryProcessor processor : critieraEntryProcessors) {
				if (processor.canProcess(criteriaEntry)) {
					return processor;
				}
			}

			return defaultProcessor;
		}

		@Override
		public boolean hasNext() {
			return this.delegate.hasNext();
		}

		@Override
		public String next() {
			Object o = getCriteriaEntryValue(this.delegate.next());
			String s = o != null ? o.toString() : null;
			return s;
		}

		@Override
		public void remove() {
			this.delegate.remove();
		}

	}

	/**
	 * Base implementation of {@link CriteriaEntryProcessor} handling null values and delegating calls to
	 * {@link BaseCriteriaEntryProcessor#doProcess(CriteriaEntry, Field)}
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	abstract class BaseCriteriaEntryProcessor implements CriteriaEntryProcessor {

		protected static final String DOUBLEQUOTE = "\"";

		protected final String[] RESERVED_CHARS = { DOUBLEQUOTE, "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]",
				"^", "~", "*", "?", ":", "\\" };
		protected String[] RESERVED_CHARS_REPLACEMENT = { "\\" + DOUBLEQUOTE, "\\+", "\\-", "\\&\\&", "\\|\\|", "\\!",
				"\\(", "\\)", "\\{", "\\}", "\\[", "\\]", "\\^", "\\~", "\\*", "\\?", "\\:", "\\\\" };

		@Override
		public Object process(CriteriaEntry criteriaEntry, Field field) {
			if (criteriaEntry == null || criteriaEntry.getValue() == null) {
				return null;
			}
			return doProcess(criteriaEntry, field);
		}

		protected Object filterCriteriaValue(Object criteriaValue) {
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

		protected abstract Object doProcess(CriteriaEntry criteriaEntry, Field field);

	}

	/**
	 * Default implementation of {@link CriteriaEntryProcessor} escaping values accordingly
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	class DefaultProcessor extends BaseCriteriaEntryProcessor {

		@Override
		public boolean canProcess(CriteriaEntry criteriaEntry) {
			return true;
		}

		@Override
		public Object doProcess(CriteriaEntry criteriaEntry, Field field) {
			return filterCriteriaValue(criteriaEntry.getValue());
		}

	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#EXPRESSION}
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	class ExpressionProcessor extends BaseCriteriaEntryProcessor {

		@Override
		public boolean canProcess(CriteriaEntry criteriaEntry) {
			return OperationKey.EXPRESSION.getKey().equals(criteriaEntry.getKey());
		}

		@Override
		public Object doProcess(CriteriaEntry criteriaEntry, Field field) {
			return criteriaEntry.getValue().toString();
		}

	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#BETWEEN}
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	class BetweenProcessor extends BaseCriteriaEntryProcessor {

		private static final String RANGE_OPERATOR = " TO ";

		@Override
		public boolean canProcess(CriteriaEntry criteriaEntry) {
			return OperationKey.BETWEEN.getKey().equals(criteriaEntry.getKey());
		}

		@Override
		public Object doProcess(CriteriaEntry criteriaEntry, Field field) {
			Object[] args = (Object[]) criteriaEntry.getValue();
			String rangeFragment = ((Boolean) args[2]).booleanValue() ? "[" : "{";
			rangeFragment += createRangeFragment(args[0], args[1]);
			rangeFragment += ((Boolean) args[3]).booleanValue() ? "]" : "}";
			return rangeFragment;
		}

		protected String createRangeFragment(Object rangeStart, Object rangeEnd) {
			String rangeFragment = "";
			rangeFragment += (rangeStart != null ? filterCriteriaValue(rangeStart) : Criteria.WILDCARD);
			rangeFragment += RANGE_OPERATOR;
			rangeFragment += (rangeEnd != null ? filterCriteriaValue(rangeEnd) : Criteria.WILDCARD);
			return rangeFragment;
		}

	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#NEAR}
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	class NearProcessor extends BetweenProcessor {

		@Override
		public boolean canProcess(CriteriaEntry criteriaEntry) {
			return OperationKey.NEAR.getKey().equals(criteriaEntry.getKey());
		}

		@Override
		public Object doProcess(CriteriaEntry criteriaEntry, Field field) {
			String nearFragment;
			Object[] args = (Object[]) criteriaEntry.getValue();
			if (args[0] instanceof BoundingBox) {
				BoundingBox box = (BoundingBox) args[0];
				nearFragment = field.getName() + ":[";
				nearFragment += createRangeFragment(box.getGeoLocationStart(), box.getGeoLocationEnd());
				nearFragment += "]";
			} else {
				nearFragment = createSpatialFunctionFragment(field.getName(), (GeoLocation) args[0], (Distance) args[1], "bbox");
			}
			return nearFragment;
		}

		protected String createSpatialFunctionFragment(String fieldName, GeoLocation location, Distance distance,
				String function) {
			String spatialFragment = "{!" + function + " " + SpatialParams.POINT + "=";
			spatialFragment += filterCriteriaValue(location);
			spatialFragment += " " + SpatialParams.FIELD + "=" + fieldName;
			spatialFragment += " " + SpatialParams.DISTANCE + "=" + filterCriteriaValue(distance);
			spatialFragment += "}";
			return spatialFragment;
		}

	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#WITHIN}
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	class WithinProcessor extends NearProcessor {

		@Override
		public boolean canProcess(CriteriaEntry criteriaEntry) {
			return OperationKey.WITHIN.getKey().equals(criteriaEntry.getKey());
		}

		@Override
		public Object doProcess(CriteriaEntry criteriaEntry, Field field) {
			Object[] args = (Object[]) criteriaEntry.getValue();
			return createSpatialFunctionFragment(field.getName(), (GeoLocation) args[0], (Distance) args[1], "geofilt");
		}

	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#FUZZY}
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	class FuzzyProcessor extends BaseCriteriaEntryProcessor {

		@Override
		public boolean canProcess(CriteriaEntry criteriaEntry) {
			return OperationKey.FUZZY.getKey().equals(criteriaEntry.getKey());
		}

		@Override
		protected Object doProcess(CriteriaEntry criteriaEntry, Field field) {
			Object[] args = (Object[]) criteriaEntry.getValue();
			Float distance = (Float) args[1];
			return filterCriteriaValue(args[0]) + "~" + (distance.isNaN() ? "" : distance);
		}

	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#SLOPPY}
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	class SloppyProcessor extends BaseCriteriaEntryProcessor {

		@Override
		public boolean canProcess(CriteriaEntry criteriaEntry) {
			return OperationKey.SLOPPY.getKey().equals(criteriaEntry.getKey());
		}

		@Override
		protected Object doProcess(CriteriaEntry criteriaEntry, Field field) {
			Object[] args = (Object[]) criteriaEntry.getValue();
			Integer distance = (Integer) args[1];
			return filterCriteriaValue(args[0]) + "~" + distance;
		}

	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#CONTAINS}, {@link OperationKey#STARTS_WITH},
	 * {@link OperationKey#ENDS_WITH}
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	class WildcardProcessor extends BaseCriteriaEntryProcessor {

		@Override
		public boolean canProcess(CriteriaEntry criteriaEntry) {
			return OperationKey.CONTAINS.getKey().equals(criteriaEntry.getKey())
					|| OperationKey.STARTS_WITH.getKey().equals(criteriaEntry.getKey())
					|| OperationKey.ENDS_WITH.getKey().equals(criteriaEntry.getKey());
		}

		@Override
		protected Object doProcess(CriteriaEntry criteriaEntry, Field field) {
			Object filteredValue = filterCriteriaValue(criteriaEntry.getValue());
			if (OperationKey.CONTAINS.getKey().equals(criteriaEntry.getKey())) {
				return Criteria.WILDCARD + filteredValue + Criteria.WILDCARD;
			} else if (OperationKey.STARTS_WITH.getKey().equals(criteriaEntry.getKey())) {
				return filteredValue + Criteria.WILDCARD;
			} else if (OperationKey.ENDS_WITH.getKey().equals(criteriaEntry.getKey())) {
				return Criteria.WILDCARD + filteredValue;
			}
			return filteredValue;
		}
	}

}
