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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SpatialParams;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.solr.VersionUtil;
import org.springframework.data.solr.core.convert.DateTimeConverters;
import org.springframework.data.solr.core.convert.NumberConverters;
import org.springframework.data.solr.core.geo.GeoConverters;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.Criteria.OperationKey;
import org.springframework.data.solr.core.query.Criteria.Predicate;
import org.springframework.data.solr.core.query.Query.Operator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Base Implementation of {@link QueryParser} providing common functions for creating
 * {@link org.apache.solr.client.solrj.SolrQuery}.
 *
 * @author Christoph Strobl
 * @author Francisco Spaeth
 * @author Radek Mensik
 * @author David Webb
 * @author Michael Rocke
 */
public abstract class QueryParserBase<QUERYTPYE extends SolrDataQuery> implements QueryParser {

	protected static final String CRITERIA_VALUE_SEPERATOR = " ";
	protected static final String DELIMINATOR = ":";
	protected static final String NOT = "-";
	protected static final String BOOST = "^";

	protected final GenericConversionService conversionService = new GenericConversionService();
	private final List<PredicateProcessor> critieraEntryProcessors = new ArrayList<>();
	private final PredicateProcessor defaultProcessor = new DefaultProcessor();

	private final @Nullable MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext;

	{
		if (!conversionService.canConvert(java.util.Date.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JavaDateConverter.INSTANCE);
		}
		if (!conversionService.canConvert(Number.class, String.class)) {
			conversionService.addConverter(NumberConverters.NumberConverter.INSTANCE);
		}
		if (!conversionService.canConvert(Distance.class, String.class)) {
			conversionService.addConverter(GeoConverters.DistanceToStringConverter.INSTANCE);
		}
		if (!conversionService.canConvert(org.springframework.data.geo.Point.class, String.class)) {
			conversionService.addConverter(GeoConverters.Point3DToStringConverter.INSTANCE);
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
		critieraEntryProcessors.add(new FunctionProcessor());
	}

	/**
	 * @param mappingContext
	 * @since 4.0
	 */
	public QueryParserBase(
			@Nullable MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext) {

		this.mappingContext = mappingContext;
	}

	@Override
	public String getQueryString(SolrDataQuery query, @Nullable Class<?> domainType) {

		if (query.getCriteria() == null) {
			return null;
		}

		String queryString = createQueryStringFromNode(query.getCriteria(), domainType);
		queryString = prependJoin(queryString, query, domainType);
		return queryString;
	}

	@Override
	public void registerConverter(Converter<?, ?> converter) {
		conversionService.addConverter(converter);
	}

	/**
	 * add another {@link PredicateProcessor}
	 *
	 * @param processor
	 */
	public void addPredicateProcessor(PredicateProcessor processor) {
		this.critieraEntryProcessors.add(processor);
	}

	/**
	 * Create the plain query string representation of the given node.
	 *
	 * @param node
	 * @return
	 * @deprecated since 4.0. Use {@link #createQueryStringFromNode(Node, Class)} instead
	 */
	@Deprecated
	public String createQueryStringFromNode(Node node) {
		return createQueryStringFromNode(node, null);
	}

	/**
	 * Create the plain query string representation of the given node using mapping information derived from the domain
	 * type.
	 *
	 * @param node
	 * @param domainType can be {@literal null}.
	 * @return
	 * @since 4.0
	 */
	public String createQueryStringFromNode(Node node, @Nullable Class<?> domainType) {
		return createQueryStringFromNode(node, 0, domainType);
	}

	/**
	 * Create the plain query string representation of the given node.
	 *
	 * @param node
	 * @return
	 * @deprecated since 4.0. Use {@link #createQueryStringFromNode(Node, int, Class)} instead.
	 */
	@Deprecated
	public String createQueryStringFromNode(Node node, int position) {
		return createQueryStringFromNode(node, position, null);
	}

	/**
	 * Create the plain query string representation of the given node using mapping information derived from the domain
	 * type.
	 *
	 * @param node
	 * @param position
	 * @param domainType can be {@literal null}.
	 * @return
	 * @since 4.0
	 */
	public String createQueryStringFromNode(Node node, int position, @Nullable Class<?> domainType) {

		StringBuilder query = new StringBuilder();
		if (position > 0) {
			query.append(node.isOr() ? " OR " : " AND ");
		}

		if (node.hasSiblings()) {
			if (node.isNegating()) {
				query.append("-");
			}
			if (!node.isRoot() || (node.isRoot() && node.isNegating())) {
				query.append('(');
			}

			int i = 0;
			for (Node nested : node.getSiblings()) {
				query.append(createQueryStringFromNode(nested, i++, domainType));
			}

			if (!node.isRoot() || (node.isRoot() && node.isNegating())) {
				query.append(')');
			}
		} else {
			query.append(createQueryFragmentForCriteria((Criteria) node, domainType));
		}
		return query.toString();
	}

	/**
	 * Iterates criteria list and concats query string fragments to form a valid query string to be used with
	 * {@link org.apache.solr.client.solrj.SolrQuery#setQuery(String)}
	 *
	 * @param criteria
	 * @return
	 * @deprecated since 4.0. Use {@link #createQueryStringFromCriteria(Criteria, Class)} instead.
	 */
	@Deprecated
	protected String createQueryStringFromCriteria(Criteria criteria) {
		return createQueryStringFromCriteria(criteria, null);
	}

	/**
	 * Iterates criteria list and concats query string fragments to form a valid query string to be used with
	 * {@link org.apache.solr.client.solrj.SolrQuery#setQuery(String)}
	 *
	 * @param criteria
	 * @param domainType
	 * @return
	 * @since 4.0
	 */
	protected String createQueryStringFromCriteria(Criteria criteria, @Nullable Class<?> domainType) {
		return createQueryStringFromNode(criteria, domainType);
	}

	/**
	 * Creates query string representation of a single critiera.
	 *
	 * @param part
	 * @param domainType
	 * @return
	 */
	protected String createQueryFragmentForCriteria(Criteria part, @Nullable Class<?> domainType) {

		Criteria criteria = part;
		StringBuilder queryFragment = new StringBuilder();
		boolean singeEntryCriteria = (criteria.getPredicates().size() == 1);

		if (criteria instanceof QueryStringHolder) {
			return ((QueryStringHolder) criteria).getQueryString();
		}

		String fieldName = getNullsafeFieldName(criteria.getField(), domainType);
		if (criteria.isNegating()) {
			fieldName = NOT + fieldName;
		}
		if (!StringUtils.isEmpty(fieldName) && !containsFunctionCriteria(criteria.getPredicates())) {
			queryFragment.append(fieldName);
			queryFragment.append(DELIMINATOR);
		}

		// no criteria given is defaulted to not null
		if (criteria.getPredicates().isEmpty()) {
			queryFragment.append("[* TO *]");
			return queryFragment.toString();
		}

		if (!singeEntryCriteria) {
			queryFragment.append("(");
		}

		CriteriaQueryStringValueProvider valueProvider = new CriteriaQueryStringValueProvider(criteria, domainType);
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
			queryFragment.append(BOOST).append(criteria.getBoost());
		}

		return queryFragment.toString();
	}

	private String getNullsafeFieldName(@Nullable Field field, Class<?> domainType) {

		if (field == null || field.getName() == null) {
			return "";
		}

		return getMappedFieldName(field, domainType);
	}

	/**
	 * Get the mapped field name using meta information derived from the given domain type.
	 *
	 * @param field
	 * @param domainType
	 * @return
	 * @since 4.0
	 */
	protected String getMappedFieldName(Field field, @Nullable Class<?> domainType) {
		return getMappedFieldName(field.getName(), domainType);
	}

	/**
	 * Get the mapped field name using meta information derived from the given domain type.
	 *
	 * @param fieldName
	 * @param domainType
	 * @return
	 * @since 4.0
	 */
	protected String getMappedFieldName(String fieldName, @Nullable Class<?> domainType) {

		if (domainType == null || mappingContext == null) {
			return fieldName;
		}

		SolrPersistentEntity entity = mappingContext.getPersistentEntity(domainType);
		if (entity == null) {
			return fieldName;
		}

		SolrPersistentProperty property = entity.getPersistentProperty(fieldName);
		return property != null ? property.getFieldName() : fieldName;
	}

	/**
	 * Create {@link SolrClient} readable String representation for {@link CalculatedField}.
	 *
	 * @param calculatedField
	 * @return
	 * @since 1.1
	 */
	protected String createCalculatedFieldFragment(CalculatedField calculatedField, @Nullable Class<?> domainType) {
		return StringUtils.isNotBlank(calculatedField.getAlias())
				? (calculatedField.getAlias() + ":" + createFunctionFragment(calculatedField.getFunction(), 0, domainType))
				: createFunctionFragment(calculatedField.getFunction(), 0, domainType);
	}

	/**
	 * Create {@link SolrClient} readable String representation for {@link Function}
	 *
	 * @param function
	 * @return
	 * @since 1.1
	 */
	protected String createFunctionFragment(Function function, int level, @Nullable Class<?> domainType) {

		StringBuilder sb = new StringBuilder();
		if (level <= 0) {
			sb.append("{!func}");
		}

		sb.append(function.getOperation());
		sb.append('(');
		if (function.hasArguments()) {
			List<String> solrReadableArguments = new ArrayList<>();
			for (Object arg : function.getArguments()) {
				Assert.notNull(arg, "Unable to parse 'null' within function arguments.");
				if (arg instanceof Function) {
					solrReadableArguments.add(createFunctionFragment((Function) arg, level + 1, domainType));
				} else if (arg instanceof Criteria) {
					solrReadableArguments.add(createQueryStringFromNode((Criteria) arg, domainType));
				} else if (arg instanceof Field) {
					solrReadableArguments.add(getMappedFieldName((Field) arg, domainType));
				} else if (arg instanceof Query) {
					solrReadableArguments.add(getQueryString((Query) arg, domainType));
				} else if (arg instanceof String || !conversionService.canConvert(arg.getClass(), String.class)) {
					solrReadableArguments.add(arg.toString());
				} else {
					solrReadableArguments.add(conversionService.convert(arg, String.class));
				}
			}
			sb.append(StringUtils.join(solrReadableArguments, ','));
		}
		sb.append(')');
		return sb.toString();
	}

	/**
	 * Prepend {@code !join from= to=} to given queryString
	 *
	 * @param queryString
	 * @param query
	 * @param domainType
	 * @return
	 */
	protected String prependJoin(String queryString, @Nullable SolrDataQuery query, @Nullable Class<?> domainType) {
		if (query == null || query.getJoin() == null) {
			return queryString;
		}

		String fromIndex = query.getJoin().getFromIndex() != null ? " fromIndex=" + query.getJoin().getFromIndex() : "";
		return "{!join from=" + getMappedFieldName(query.getJoin().getFrom(), domainType) + " to="
				+ getMappedFieldName(query.getJoin().getTo(), domainType) + fromIndex + "}" + queryString;
	}

	/**
	 * Append pagination information {@code start, rows} to {@link SolrQuery}
	 *
	 * @param query
	 * @param offset
	 * @param rows
	 */
	protected void appendPagination(SolrQuery query, @Nullable Long offset, @Nullable Integer rows) {

		if (offset != null && offset.intValue() >= 0) {
			query.setStart(offset.intValue());
		}
		if (rows != null && rows >= 0) {
			query.setRows(rows);
		}
	}

	@Deprecated
	protected void appendProjectionOnFields(SolrQuery solrQuery, List<Field> fields) {
		appendProjectionOnFields(solrQuery, fields, null);
	}

	/**
	 * Append field list to {@link SolrQuery}
	 *
	 * @param solrQuery
	 * @param fields
	 */
	protected void appendProjectionOnFields(SolrQuery solrQuery, List<Field> fields, @Nullable Class<?> domainType) {

		if (CollectionUtils.isEmpty(fields)) {
			return;
		}
		List<String> solrReadableFields = new ArrayList<>();
		for (Field field : fields) {
			if (field instanceof CalculatedField) {
				solrReadableFields.add(createCalculatedFieldFragment((CalculatedField) field, domainType));
			} else {
				solrReadableFields.add(getMappedFieldName(field, domainType));
			}
		}
		solrQuery.setParam(CommonParams.FL, StringUtils.join(solrReadableFields, ","));
	}

	/**
	 * Set {@code q.op} parameter for {@link SolrQuery}
	 *
	 * @param solrQuery
	 * @param defaultOperator
	 */
	protected void appendDefaultOperator(SolrQuery solrQuery, @Nullable Operator defaultOperator) {
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
	protected void appendTimeAllowed(SolrQuery solrQuery, @Nullable Integer timeAllowed) {
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
	protected void appendDefType(SolrQuery solrQuery, @Nullable String defType) {
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
	protected void appendRequestHandler(SolrQuery solrQuery, @Nullable String requestHandler) {
		if (StringUtils.isNotBlank(requestHandler)) {
			solrQuery.add(CommonParams.QT, requestHandler);
		}
	}

	private boolean containsFunctionCriteria(Set<Predicate> chainedCriterias) {
		for (Predicate entry : chainedCriterias) {
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
	public SolrQuery constructSolrQuery(SolrDataQuery query, @Nullable Class<?> domainType) {
		return doConstructSolrQuery((QUERYTPYE) query, domainType);
	}

	public abstract SolrQuery doConstructSolrQuery(QUERYTPYE query, @Nullable Class<?> domainType);

	/**
	 * {@link PredicateProcessor} creates a solr reable query string representation for a given {@link Predicate}
	 *
	 * @author Christoph Strobl
	 */
	public interface PredicateProcessor {

		/**
		 * @param predicate
		 * @return true if predicate can be processed by this parser
		 */
		boolean canProcess(@Nullable Predicate predicate);

		/**
		 * Create query string representation of given {@link Predicate}
		 *
		 * @param predicate
		 * @param field
		 * @return
		 */
		Object process(@Nullable Predicate predicate, @Nullable Field field, Class<?> domainType);

	}

	/**
	 * @author Christoph Strobl
	 */
	class CriteriaQueryStringValueProvider implements Iterator<String> {

		private final Criteria criteria;
		private Iterator<Predicate> delegate;
		private @Nullable Class<?> domainType;

		CriteriaQueryStringValueProvider(Criteria criteria, @Nullable Class<?> domainType) {
			Assert.notNull(criteria, "Unable to provide values for 'null' criteria");

			this.criteria = criteria;
			this.delegate = criteria.getPredicates().iterator();
			this.domainType = domainType;
		}

		@SuppressWarnings("unchecked")
		@Nullable
		private <T> T getPredicateValue(Predicate predicate) {
			PredicateProcessor processor = findMatchingProcessor(predicate);
			return (T) processor.process(predicate, criteria.getField(), domainType);
		}

		private PredicateProcessor findMatchingProcessor(Predicate predicate) {
			for (PredicateProcessor processor : critieraEntryProcessors) {
				if (processor.canProcess(predicate)) {
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
			Object o = getPredicateValue(this.delegate.next());
			return o != null ? o.toString() : null;
		}

		@Override
		public void remove() {
			this.delegate.remove();
		}

	}

	/**
	 * Base implementation of {@link PredicateProcessor} handling null values and delegating calls to
	 * {@link BasePredicateProcessor#doProcess(Predicate, Field, Class)}
	 *
	 * @author Christoph Strobl
	 */
	abstract class BasePredicateProcessor implements PredicateProcessor {

		protected static final String DOUBLEQUOTE = "\"";

		protected final Set<String> BOOLEAN_OPERATORS = new HashSet<>(Arrays.asList("NOT", "AND", "OR"));

		protected final String[] RESERVED_CHARS = { DOUBLEQUOTE, "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]",
				"^", "~", "*", "?", ":", "\\" };
		protected String[] RESERVED_CHARS_REPLACEMENT = { "\\" + DOUBLEQUOTE, "\\+", "\\-", "\\&\\&", "\\|\\|", "\\!",
				"\\(", "\\)", "\\{", "\\}", "\\[", "\\]", "\\^", "\\~", "\\*", "\\?", "\\:", "\\\\" };

		@Override
		public Object process(@Nullable Predicate predicate, @Nullable Field field, @Nullable Class<?> domainType) {

			if (predicate == null || predicate.getValue() == null) {
				return null;
			}
			return doProcess(predicate, field, domainType);
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
			if (StringUtils.contains(criteriaValue, CRITERIA_VALUE_SEPERATOR) || BOOLEAN_OPERATORS.contains(criteriaValue)) {
				return DOUBLEQUOTE + criteriaValue + DOUBLEQUOTE;
			}
			return criteriaValue;
		}

		@Nullable
		protected abstract Object doProcess(@Nullable Predicate predicate, Field field, @Nullable Class<?> domainType);

	}

	/**
	 * Default implementation of {@link PredicateProcessor} escaping values accordingly
	 *
	 * @author Christoph Strobl
	 */
	class DefaultProcessor extends BasePredicateProcessor {

		@Override
		public boolean canProcess(@Nullable Predicate predicate) {
			return true;
		}

		@Override
		public Object doProcess(@Nullable Predicate predicate, Field field, @Nullable Class<?> domainType) {

			Assert.notNull(predicate, "Predicate must not be null!");

			return filterCriteriaValue(predicate.getValue());
		}
	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#EXPRESSION}
	 *
	 * @author Christoph Strobl
	 */
	class ExpressionProcessor extends BasePredicateProcessor {

		@Override
		public boolean canProcess(@Nullable Predicate predicate) {
			return predicate != null && OperationKey.EXPRESSION.getKey().equals(predicate.getKey());
		}

		@Override
		public Object doProcess(@Nullable Predicate predicate, Field field, @Nullable Class<?> domainType) {
			Assert.notNull(predicate, "Predicate must not be null!");

			return predicate.getValue().toString();
		}
	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#BETWEEN}
	 *
	 * @author Christoph Strobl
	 */
	class BetweenProcessor extends BasePredicateProcessor {

		private static final String RANGE_OPERATOR = " TO ";

		@Override
		public boolean canProcess(@Nullable Predicate predicate) {
			return predicate != null && OperationKey.BETWEEN.getKey().equals(predicate.getKey());
		}

		@Override
		public Object doProcess(@Nullable Predicate predicate, Field field, @Nullable Class<?> domainType) {
			Object[] args = (Object[]) predicate.getValue();
			String rangeFragment = (Boolean) args[2] ? "[" : "{";
			rangeFragment += createRangeFragment(args[0], args[1]);
			rangeFragment += (Boolean) args[3] ? "]" : "}";
			return rangeFragment;
		}

		protected String createRangeFragment(@Nullable Object rangeStart, @Nullable Object rangeEnd) {
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
	 */
	class NearProcessor extends BetweenProcessor {

		@Override
		public boolean canProcess(@Nullable Predicate predicate) {
			return predicate != null && OperationKey.NEAR.getKey().equals(predicate.getKey());
		}

		@Override
		public Object doProcess(@Nullable Predicate predicate, Field field, @Nullable Class<?> domainType) {

			Assert.notNull(predicate, "Predicate must not be null!");

			String nearFragment;
			Object[] args = (Object[]) predicate.getValue();
			if (args[0] instanceof Box) {
				Box box = (Box) args[0];
				nearFragment = getMappedFieldName(field, domainType) + ":[";
				nearFragment += createRangeFragment(box.getFirst(), box.getSecond());
				nearFragment += "]";
			} else {
				nearFragment = createSpatialFunctionFragment(getMappedFieldName(field, domainType),
						(org.springframework.data.geo.Point) args[0], (Distance) args[1], "bbox");
			}
			return nearFragment;
		}

		protected String createSpatialFunctionFragment(@Nullable String fieldName,
				org.springframework.data.geo.Point location, Distance distance, String function) {
			String spatialFragment = "{!" + function + " " + SpatialParams.POINT + "=";
			spatialFragment += filterCriteriaValue(location);
			spatialFragment += " " + SpatialParams.FIELD + "=" + fieldName;
			spatialFragment += " " + SpatialParams.DISTANCE + "=" + filterCriteriaValue(distance);

			if (Metrics.KILOMETERS.equals(distance.getMetric())) {
				spatialFragment += " " + "score=kilometers";
			} else if (Metrics.MILES.equals(distance.getMetric())) {
				spatialFragment += " " + "score=miles";
			} else {
				if (!Metrics.NEUTRAL.equals(distance.getMetric())) {
					spatialFragment += " " + "score=" + distance.getUnit();
				}
			}

			spatialFragment += "}";
			return spatialFragment;
		}
	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#WITHIN}
	 *
	 * @author Christoph Strobl
	 */
	class WithinProcessor extends NearProcessor {

		@Override
		public boolean canProcess(@Nullable Predicate predicate) {
			return OperationKey.WITHIN.getKey().equals(predicate.getKey());
		}

		@Nullable
		@Override
		public Object doProcess(@Nullable Predicate predicate, Field field, @Nullable Class<?> domainType) {

			Assert.notNull(predicate, "Predicate must not be null!");

			Object[] args = (Object[]) predicate.getValue();
			return createSpatialFunctionFragment(getMappedFieldName(field, domainType),
					(org.springframework.data.geo.Point) args[0], (Distance) args[1], "geofilt");
		}
	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#FUZZY}
	 *
	 * @author Christoph Strobl
	 */
	class FuzzyProcessor extends BasePredicateProcessor {

		@Override
		public boolean canProcess(@Nullable Predicate predicate) {
			return predicate != null && OperationKey.FUZZY.getKey().equals(predicate.getKey());
		}

		@Nullable
		@Override
		protected Object doProcess(@Nullable Predicate predicate, Field field, @Nullable Class<?> domainType) {

			Assert.notNull(predicate, "Predicate must not be null!");

			Object[] args = (Object[]) predicate.getValue();
			Float distance = (Float) args[1];
			return filterCriteriaValue(args[0]) + "~" + (distance.isNaN() ? "" : distance);
		}
	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#SLOPPY}
	 *
	 * @author Christoph Strobl
	 */
	class SloppyProcessor extends BasePredicateProcessor {

		@Override
		public boolean canProcess(@Nullable Predicate predicate) {
			return predicate != null && OperationKey.SLOPPY.getKey().equals(predicate.getKey());
		}

		@Nullable
		@Override
		protected Object doProcess(@Nullable Predicate predicate, Field field, @Nullable Class<?> domainType) {

			Assert.notNull(predicate, "Predicate must not be null!");

			Object[] args = (Object[]) predicate.getValue();
			Integer distance = (Integer) args[1];
			return filterCriteriaValue(args[0]) + "~" + distance;
		}
	}

	/**
	 * Handles {@link Criteria}s with {@link OperationKey#CONTAINS}, {@link OperationKey#STARTS_WITH},
	 * {@link OperationKey#ENDS_WITH}
	 *
	 * @author Christoph Strobl
	 */
	class WildcardProcessor extends BasePredicateProcessor {

		@Override
		public boolean canProcess(@Nullable Predicate predicate) {
			return predicate != null && (OperationKey.CONTAINS.getKey().equals(predicate.getKey())
					|| OperationKey.STARTS_WITH.getKey().equals(predicate.getKey())
					|| OperationKey.ENDS_WITH.getKey().equals(predicate.getKey()));
		}

		@Override
		protected Object doProcess(@Nullable Predicate predicate, Field field, @Nullable Class<?> domainType) {

			Assert.notNull(predicate, "Predicate must not be null!");

			Object filteredValue = filterCriteriaValue(predicate.getValue());
			if (OperationKey.CONTAINS.getKey().equals(predicate.getKey())) {
				return Criteria.WILDCARD + filteredValue + Criteria.WILDCARD;
			} else if (OperationKey.STARTS_WITH.getKey().equals(predicate.getKey())) {
				return filteredValue + Criteria.WILDCARD;
			} else if (OperationKey.ENDS_WITH.getKey().equals(predicate.getKey())) {
				return Criteria.WILDCARD + filteredValue;
			}
			return filteredValue;
		}
	}

	/**
	 * Handles {@link Criteria} with {@link OperationKey#FUNCTION}
	 *
	 * @since 1.1
	 */
	class FunctionProcessor extends BasePredicateProcessor {

		@Override
		public boolean canProcess(@Nullable Predicate predicate) {
			return predicate != null && OperationKey.FUNCTION.getKey().equals(predicate.getKey());
		}

		@Override
		@Nullable
		protected Object doProcess(@Nullable Predicate predicate, Field field, @Nullable Class<?> domainType) {

			Assert.notNull(predicate, "Predicate must not be null!");

			return createFunctionFragment((Function) predicate.getValue(), 0, domainType);
		}
	}

	private static void setObjectName(Map<String, Object> namesAssociation, Object object, String name) {
		namesAssociation.put(name, object);
	}

	/**
	 * @author Francisco Spaeth
	 * @since 1.4
	 */
	interface NamedObjects {

		void setName(Object object, String name);

		Map<String, Object> getNamesAssociation();

	}

	/**
	 * @author Francisco Spaeth
	 * @since 1.4
	 */
	static class NamedObjectsQuery extends AbstractQueryDecorator implements NamedObjects {

		private Map<String, Object> namesAssociation = new HashMap<>();

		public NamedObjectsQuery(Query query) {
			super(query);
			Assert.notNull(query, "group query shall not be null");
		}

		@Override
		public void setName(Object object, String name) {
			setObjectName(namesAssociation, object, name);
		}

		@Override
		public Map<String, Object> getNamesAssociation() {
			return Collections.unmodifiableMap(namesAssociation);
		}

	}

	/**
	 * @author Francisco Spaeth
	 * @since 1.4
	 */
	static class NamedObjectsFacetQuery extends AbstractFacetQueryDecorator implements NamedObjects {

		private Map<String, Object> namesAssociation = new HashMap<>();

		public NamedObjectsFacetQuery(FacetQuery query) {
			super(query);
		}

		@Override
		public void setName(Object object, String name) {
			setObjectName(namesAssociation, object, name);
		}

		@Override
		public Map<String, Object> getNamesAssociation() {
			return Collections.unmodifiableMap(namesAssociation);
		}

	}

	/**
	 * @author Francisco Spaeth
	 * @since 1.4
	 */
	static class NamedObjectsHighlightQuery extends AbstractHighlightQueryDecorator implements NamedObjects {

		private Map<String, Object> namesAssociation = new HashMap<>();

		public NamedObjectsHighlightQuery(HighlightQuery query) {
			super(query);
		}

		@Override
		public void setName(Object object, String name) {
			setObjectName(namesAssociation, object, name);
		}

		@Override
		public Map<String, Object> getNamesAssociation() {
			return Collections.unmodifiableMap(namesAssociation);
		}

	}

	/**
	 * @author David Webb
	 * @since 2.1
	 */
	static class NamedObjectsFacetAndHighlightQuery extends AbstractFacetAndHighlightQueryDecorator
			implements NamedObjects {

		private Map<String, Object> namesAssociation = new HashMap<>();

		public NamedObjectsFacetAndHighlightQuery(FacetAndHighlightQuery query) {
			super(query);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.solr.core.QueryParserBase.NamedObjects#setName(java.lang.Object, java.lang.String)
		 */
		@Override
		public void setName(Object object, String name) {
			setObjectName(namesAssociation, object, name);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.solr.core.QueryParserBase.NamedObjects#getNamesAssociation()
		 */
		@Override
		public Map<String, Object> getNamesAssociation() {
			return Collections.unmodifiableMap(namesAssociation);
		}
	}
}
