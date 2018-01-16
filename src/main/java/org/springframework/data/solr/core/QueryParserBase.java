/*
 * Copyright 2012 - 2018 the original author or authors.
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
import org.springframework.data.solr.VersionUtil;
import org.springframework.data.solr.core.convert.DateTimeConverters;
import org.springframework.data.solr.core.convert.NumberConverters;
import org.springframework.data.solr.core.geo.GeoConverters;
import org.springframework.data.solr.core.query.AbstractFacetAndHighlightQueryDecorator;
import org.springframework.data.solr.core.query.AbstractFacetQueryDecorator;
import org.springframework.data.solr.core.query.AbstractHighlightQueryDecorator;
import org.springframework.data.solr.core.query.AbstractQueryDecorator;
import org.springframework.data.solr.core.query.CalculatedField;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Criteria.OperationKey;
import org.springframework.data.solr.core.query.Criteria.Predicate;
import org.springframework.data.solr.core.query.FacetAndHighlightQuery;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.Function;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Node;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.Query.Operator;
import org.springframework.data.solr.core.query.QueryStringHolder;
import org.springframework.data.solr.core.query.SolrDataQuery;
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

	@Override
	public String getQueryString(SolrDataQuery query) {
		if (query.getCriteria() == null) {
			return null;
		}

		String queryString = createQueryStringFromNode(query.getCriteria());
		queryString = prependJoin(queryString, query);
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

	public String createQueryStringFromNode(Node node) {
		return createQueryStringFromNode(node, 0);
	}

	public String createQueryStringFromNode(Node node, int position) {

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
				query.append(createQueryStringFromNode(nested, i++));
			}

			if (!node.isRoot() || (node.isRoot() && node.isNegating())) {
				query.append(')');
			}
		} else {
			query.append(createQueryFragmentForCriteria((Criteria) node));
		}
		return query.toString();
	}

	/**
	 * Iterates criteria list and concats query string fragments to form a valid query string to be used with
	 * {@link org.apache.solr.client.solrj.SolrQuery#setQuery(String)}
	 *
	 * @param criteria
	 * @return
	 */
	protected String createQueryStringFromCriteria(Criteria criteria) {
		return createQueryStringFromNode(criteria);
	}

	/**
	 * Creates query string representation of a single critiera
	 *
	 * @return
	 */
	protected String createQueryFragmentForCriteria(Criteria part) {
		Criteria criteria = part;
		StringBuilder queryFragment = new StringBuilder();
		boolean singeEntryCriteria = (criteria.getPredicates().size() == 1);
		if (criteria instanceof QueryStringHolder) {
			return ((QueryStringHolder) criteria).getQueryString();
		}

		String fieldName = getNullsafeFieldName(criteria.getField());
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
			queryFragment.append(BOOST).append(criteria.getBoost());
		}

		return queryFragment.toString();
	}

	private String getNullsafeFieldName(@Nullable Field field) {
		if (field == null || field.getName() == null) {
			return "";
		}
		return field.getName();
	}

	/**
	 * Create {@link SolrClient} readable String representation for {@link CalculatedField}.
	 *
	 * @param calculatedField
	 * @return
	 * @since 1.1
	 */
	protected String createCalculatedFieldFragment(CalculatedField calculatedField) {
		return StringUtils.isNotBlank(calculatedField.getAlias())
				? (calculatedField.getAlias() + ":" + createFunctionFragment(calculatedField.getFunction(), 0))
				: createFunctionFragment(calculatedField.getFunction(), 0);
	}

	/**
	 * Create {@link SolrClient} readable String representation for {@link Function}
	 *
	 * @param function
	 * @return
	 * @since 1.1
	 */
	protected String createFunctionFragment(Function function, int level) {

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
					solrReadableArguments.add(createFunctionFragment((Function) arg, level + 1));
				} else if (arg instanceof Criteria) {
					solrReadableArguments.add(createQueryStringFromNode((Criteria) arg));
				} else if (arg instanceof Field) {
					solrReadableArguments.add(((Field) arg).getName());
				} else if (arg instanceof Query) {
					solrReadableArguments.add(getQueryString((Query) arg));
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
	 * @return
	 */
	protected String prependJoin(String queryString, @Nullable SolrDataQuery query) {
		if (query == null || query.getJoin() == null) {
			return queryString;
		}

		String fromIndex = query.getJoin().getFromIndex() != null ? " fromIndex=" + query.getJoin().getFromIndex() : "";
		return "{!join from=" + query.getJoin().getFrom().getName() + " to=" + query.getJoin().getTo().getName() + fromIndex
				+ "}" + queryString;
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
		List<String> solrReadableFields = new ArrayList<>();
		for (Field field : fields) {
			if (field instanceof CalculatedField) {
				solrReadableFields.add(createCalculatedFieldFragment((CalculatedField) field));
			} else {
				solrReadableFields.add(field.getName());
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
	public SolrQuery constructSolrQuery(SolrDataQuery query) {
		return doConstructSolrQuery((QUERYTPYE) query);
	}

	public abstract SolrQuery doConstructSolrQuery(QUERYTPYE query);

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
		Object process(@Nullable Predicate predicate, @Nullable Field field);

	}

	/**
	 * @author Christoph Strobl
	 */
	class CriteriaQueryStringValueProvider implements Iterator<String> {

		private final Criteria criteria;
		private Iterator<Predicate> delegate;

		CriteriaQueryStringValueProvider(Criteria criteria) {
			Assert.notNull(criteria, "Unable to provide values for 'null' criteria");

			this.criteria = criteria;
			this.delegate = criteria.getPredicates().iterator();
		}

		@SuppressWarnings("unchecked")
		@Nullable
		private <T> T getPredicateValue(Predicate predicate) {
			PredicateProcessor processor = findMatchingProcessor(predicate);
			return (T) processor.process(predicate, criteria.getField());
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
	 * {@link BasePredicateProcessor#doProcess(Predicate, Field)}
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
		public Object process(@Nullable Predicate predicate, @Nullable Field field) {
			if (predicate == null || predicate.getValue() == null) {
				return null;
			}
			return doProcess(predicate, field);
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
		protected abstract Object doProcess(@Nullable Predicate predicate, Field field);

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
		public Object doProcess(@Nullable Predicate predicate, Field field) {

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
		public Object doProcess(@Nullable Predicate predicate, Field field) {
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
		public Object doProcess(@Nullable Predicate predicate, Field field) {
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
		public Object doProcess(@Nullable Predicate predicate, Field field) {

			Assert.notNull(predicate, "Predicate must not be null!");

			String nearFragment;
			Object[] args = (Object[]) predicate.getValue();
			if (args[0] instanceof Box) {
				Box box = (Box) args[0];
				nearFragment = field.getName() + ":[";
				nearFragment += createRangeFragment(box.getFirst(), box.getSecond());
				nearFragment += "]";
			} else {
				nearFragment = createSpatialFunctionFragment(field.getName(), (org.springframework.data.geo.Point) args[0],
						(Distance) args[1], "bbox");
			}
			return nearFragment;
		}

		protected String createSpatialFunctionFragment(@Nullable String fieldName,
				org.springframework.data.geo.Point location, Distance distance, String function) {
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
	 */
	class WithinProcessor extends NearProcessor {

		@Override
		public boolean canProcess(@Nullable Predicate predicate) {
			return OperationKey.WITHIN.getKey().equals(predicate.getKey());
		}

		@Nullable
		@Override
		public Object doProcess(@Nullable Predicate predicate, Field field) {

			Assert.notNull(predicate, "Predicate must not be null!");

			Object[] args = (Object[]) predicate.getValue();
			return createSpatialFunctionFragment(field.getName(), (org.springframework.data.geo.Point) args[0],
					(Distance) args[1], "geofilt");
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
		protected Object doProcess(@Nullable Predicate predicate, Field field) {

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
		protected Object doProcess(@Nullable Predicate predicate, Field field) {

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
		protected Object doProcess(@Nullable Predicate predicate, Field field) {

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
		protected Object doProcess(@Nullable Predicate predicate, Field field) {

			Assert.notNull(predicate, "Predicate must not be null!");

			return createFunctionFragment((Function) predicate.getValue(), 0);
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
