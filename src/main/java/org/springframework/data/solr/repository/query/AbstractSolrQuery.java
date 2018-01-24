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
package org.springframework.data.solr.repository.query;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.common.params.HighlightParams;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.solr.VersionUtil;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTransactionSynchronizationAdapterBuilder;
import org.springframework.data.solr.core.convert.DateTimeConverters;
import org.springframework.data.solr.core.convert.NumberConverters;
import org.springframework.data.solr.core.geo.GeoConverters;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.HighlightOptions.HighlightParameter;
import org.springframework.data.solr.core.query.StatsOptions.FieldStatsOptions;
import org.springframework.data.solr.core.query.result.FacetAndHighlightPage;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Base implementation of a solr specific {@link RepositoryQuery}
 *
 * @author Christoph Strobl
 * @author Luke Corpe
 * @author Andrey Paramonov
 * @author Francisco Spaeth
 * @author David Webb
 */
public abstract class AbstractSolrQuery implements RepositoryQuery {

	private static final Pattern PARAMETER_PLACEHOLDER = Pattern.compile("\\?(\\d+)");

	private final SolrOperations solrOperations;
	private final SolrQueryMethod solrQueryMethod;
	private final String collection;

	public final int UNLIMITED = 1;

	private final GenericConversionService conversionService = new GenericConversionService();

	{
		if (!conversionService.canConvert(java.util.Date.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JavaDateConverter.INSTANCE);
		}
		if (!conversionService.canConvert(Number.class, String.class)) {
			conversionService.addConverter(NumberConverters.NumberConverter.INSTANCE);
		}
		if (!conversionService.canConvert(Point.class, String.class)) {
			conversionService.addConverter(GeoConverters.Point3DToStringConverter.INSTANCE);
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
	 * @param solrOperations must not be null
	 * @param solrQueryMethod must not be null
	 */
	protected AbstractSolrQuery(@Nullable String collection, SolrOperations solrOperations,
			SolrQueryMethod solrQueryMethod) {
		Assert.notNull(solrOperations, "SolrOperations must not be null!");
		Assert.notNull(solrQueryMethod, "SolrQueryMethod must not be null!");
		this.solrOperations = solrOperations;
		this.solrQueryMethod = solrQueryMethod;
		this.collection = collection;
	}

	@Override
	public Object execute(Object[] parameters) {
		SolrParameterAccessor accessor = new SolrParametersParameterAccessor(solrQueryMethod, parameters);

		Query query = createQuery(accessor);
		decorateWithFilterQuery(query, accessor);
		setDefaultQueryOperatorIfDefined(query);
		setAllowedQueryExeutionTime(query);
		setDefTypeIfDefined(query);
		setRequestHandlerIfDefined(query);
		setSpellecheckOptionsWhenDefined(query);

		if (solrQueryMethod.hasStatsDefinition()) {
			query.setStatsOptions(extractStatsOptions(solrQueryMethod, accessor));
		}

		if (isCountQuery() && isDeleteQuery()) {
			throw new InvalidDataAccessApiUsageException("Cannot execute 'delete' and 'count' at the same time.");
		}

		if (isCountQuery()) {
			return new CountExecution().execute(query);
		}
		if (isDeleteQuery()) {
			return new DeleteExecution().execute(query);
		}

		if (solrQueryMethod.isPageQuery() || solrQueryMethod.isSliceQuery()) {
			if (solrQueryMethod.isFacetQuery() && solrQueryMethod.isHighlightQuery()) {
				FacetAndHighlightQuery facetAndHighlightQuery = SimpleFacetAndHighlightQuery.fromQuery(query,
						new SimpleFacetAndHighlightQuery());
				facetAndHighlightQuery.setFacetOptions(extractFacetOptions(solrQueryMethod, accessor));
				facetAndHighlightQuery.setHighlightOptions(extractHighlightOptions(solrQueryMethod, accessor));
				return new FacetAndHighlightPageExecution(accessor.getPageable()).execute(facetAndHighlightQuery);
			}
			if (solrQueryMethod.isFacetQuery()) {
				FacetQuery facetQuery = SimpleFacetQuery.fromQuery(query, new SimpleFacetQuery());
				facetQuery.setFacetOptions(extractFacetOptions(solrQueryMethod, accessor));
				return new FacetPageExecution(accessor.getPageable()).execute(facetQuery);
			}
			if (solrQueryMethod.isHighlightQuery()) {
				HighlightQuery highlightQuery = SimpleHighlightQuery.fromQuery(query, new SimpleHighlightQuery());
				highlightQuery.setHighlightOptions(extractHighlightOptions(solrQueryMethod, accessor));
				return new HighlightPageExecution(accessor.getPageable()).execute(highlightQuery);
			}
			return new PagedExecution(accessor.getPageable()).execute(query);
		} else if (solrQueryMethod.isCollectionQuery()) {
			return new CollectionExecution(accessor.getPageable()).execute(query);
		}

		return new SingleEntityExecution().execute(query);
	}

	@Override
	public SolrQueryMethod getQueryMethod() {
		return this.solrQueryMethod;
	}

	private void setDefaultQueryOperatorIfDefined(Query query) {
		Query.Operator defaultOperator = solrQueryMethod.getDefaultOperator();
		if (!Query.Operator.NONE.equals(defaultOperator)) {
			query.setDefaultOperator(defaultOperator);
		}
	}

	private void setAllowedQueryExeutionTime(Query query) {
		Integer timeAllowed = solrQueryMethod.getTimeAllowed();
		if (timeAllowed != null) {
			query.setTimeAllowed(timeAllowed);
		}
	}

	private void setDefTypeIfDefined(Query query) {
		String defType = solrQueryMethod.getDefType();
		if (StringUtils.hasText(defType)) {
			query.setDefType(defType);
		}
	}

	private void setRequestHandlerIfDefined(Query query) {
		String requestHandler = solrQueryMethod.getRequestHandler();
		if (StringUtils.hasText(requestHandler)) {
			query.setRequestHandler(requestHandler);
		}
	}

	private void setSpellecheckOptionsWhenDefined(Query query) {

		if (solrQueryMethod.hasSpellcheck()) {
			query.setSpellcheckOptions(solrQueryMethod.getSpellcheckOptions());
		}
	}

	private void decorateWithFilterQuery(Query query, SolrParameterAccessor parameterAccessor) {
		if (solrQueryMethod.hasFilterQuery()) {
			for (String filterQuery : solrQueryMethod.getFilterQueries()) {
				query.addFilterQuery(createQueryFromString(filterQuery, parameterAccessor));
			}
		}
	}

	protected void appendProjection(@Nullable Query query) {
		if (query != null && this.getQueryMethod().hasProjectionFields()) {
			for (String fieldname : this.getQueryMethod().getProjectionFields()) {
				query.addProjectionOnField(new SimpleField(fieldname));
			}
		}
	}

	protected SimpleQuery createQueryFromString(String queryString, SolrParameterAccessor parameterAccessor) {
		String parsedQueryString = replacePlaceholders(queryString, parameterAccessor);
		return new SimpleQuery(new SimpleStringCriteria(parsedQueryString));
	}

	private String replacePlaceholders(String input, SolrParameterAccessor accessor) {
		if (!StringUtils.hasText(input)) {
			return input;
		}

		Matcher matcher = PARAMETER_PLACEHOLDER.matcher(input);
		String result = input;

		while (matcher.find()) {
			String group = matcher.group();
			int index = Integer.parseInt(matcher.group(1));
			result = result.replace(group, getParameterWithIndex(accessor, index));
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Nullable
	private String getParameterWithIndex(SolrParameterAccessor accessor, int index) {

		Object parameter = accessor.getBindableValue(index);
		if (parameter == null) {
			return "null";
		}
		if (conversionService.canConvert(parameter.getClass(), String.class)) {
			return conversionService.convert(parameter, String.class);
		}

		if (parameter instanceof Collection) {
			StringBuilder sb = new StringBuilder();
			for (Object o : (Collection) parameter) {
				if (conversionService.canConvert(o.getClass(), String.class)) {
					sb.append(conversionService.convert(o, String.class));
				} else {
					sb.append(o.toString());
				}
				sb.append(" ");
			}
			return sb.toString().trim();
		}

		return parameter.toString();
	}

	@Nullable
	private StatsOptions extractStatsOptions(SolrQueryMethod queryMethod, SolrParameterAccessor accessor) {

		if (!queryMethod.hasStatsDefinition()) {
			return null;
		}

		StatsOptions options = new StatsOptions();

		for (String fieldName : queryMethod.getFieldStats()) {
			options.addField(fieldName);
		}

		for (String facetFieldName : queryMethod.getStatsFacets()) {
			options.addFacet(facetFieldName);
		}

		options.setCalcDistinct(queryMethod.isFieldStatsCountDistinctEnable());

		Collection<String> selectiveCountDistinct = queryMethod.getStatsSelectiveCountDistinctFields();

		for (Entry<String, String[]> selectiveFacet : queryMethod.getStatsSelectiveFacets().entrySet()) {
			FieldStatsOptions fieldStatsOptions = options.addField(selectiveFacet.getKey());
			for (String facetFieldName : selectiveFacet.getValue()) {
				fieldStatsOptions.addSelectiveFacet(facetFieldName);
			}

			fieldStatsOptions.setSelectiveCalcDistinct(selectiveCountDistinct.contains(selectiveFacet.getKey()));
		}

		return options;
	}

	private FacetOptions extractFacetOptions(SolrQueryMethod queryMethod, SolrParameterAccessor parameterAccessor) {
		FacetOptions options = new FacetOptions();
		if (queryMethod.hasFacetFields()) {
			options.addFacetOnFlieldnames(queryMethod.getFacetFields());
		}
		if (queryMethod.hasFacetQueries()) {
			for (String queryString : queryMethod.getFacetQueries()) {
				options.addFacetQuery(createQueryFromString(queryString, parameterAccessor));
			}
		}
		if (queryMethod.hasPivotFields()) {
			for (String[] pivot : queryMethod.getPivotFields()) {
				options.addFacetOnPivot(pivot);
			}
		}
		options.setFacetLimit(queryMethod.getFacetLimit());
		options.setFacetMinCount(queryMethod.getFacetMinCount());
		options.setFacetPrefix(replacePlaceholders(queryMethod.getFacetPrefix(), parameterAccessor));
		return options;
	}

	private HighlightOptions extractHighlightOptions(SolrQueryMethod queryMethod, SolrParameterAccessor accessor) {
		HighlightOptions options = new HighlightOptions();
		if (queryMethod.hasHighlightFields()) {
			options.addFields(queryMethod.getHighlightFieldNames());
		}
		Integer fragsize = queryMethod.getHighlightFragsize();
		if (fragsize != null) {
			options.setFragsize(fragsize);
		}
		Integer snipplets = queryMethod.getHighlighSnipplets();
		if (snipplets != null) {
			options.setNrSnipplets(snipplets);
		}
		String queryString = queryMethod.getHighlightQuery();
		if (queryString != null) {
			options.setQuery(createQueryFromString(queryString, accessor));
		}
		appendHighlightFormatOptions(options, solrQueryMethod);
		return options;
	}

	private void appendHighlightFormatOptions(HighlightOptions options, SolrQueryMethod queryMethod) {
		String formatter = queryMethod.getHighlightFormatter();
		if (formatter != null) {
			options.setFormatter(formatter);
		}
		String highlightPrefix = queryMethod.getHighlightPrefix();
		if (highlightPrefix != null) {
			if (isSimpleHighlightingOption(formatter)) {
				options.setSimplePrefix(highlightPrefix);
			} else {
				options.addHighlightParameter(new HighlightParameter(HighlightParams.TAG_PRE, highlightPrefix));
			}
		}
		String highlightPostfix = queryMethod.getHighlightPostfix();
		if (highlightPostfix != null) {
			if (isSimpleHighlightingOption(formatter)) {
				options.setSimplePostfix(highlightPostfix);
			} else {
				options.addHighlightParameter(new HighlightParameter(HighlightParams.TAG_POST, highlightPostfix));
			}
		}
	}

	private boolean isSimpleHighlightingOption(@Nullable String formatter) {
		return formatter == null || HighlightParams.SIMPLE.equalsIgnoreCase(formatter);
	}

	protected abstract Query createQuery(SolrParameterAccessor parameterAccessor);

	/**
	 * @since 1.2
	 */
	public boolean isCountQuery() {
		return false;
	}

	/**
	 * @since 1.2
	 */
	public boolean isDeleteQuery() {
		return solrQueryMethod.isDeleteQuery();
	}

	/**
	 * @return
	 * @since 1.3
	 */
	public boolean isLimiting() {
		return false;
	}

	/**
	 * @return
	 * @since 1.3
	 */
	public int getLimit() {
		return UNLIMITED;
	}

	protected Pageable getLimitingPageable(@Nullable Pageable source, final int limit) {

		if (source == null) {
			return new SolrPageRequest(0, limit);
		}

		return new PageRequest(source.getPageNumber(), source.getPageSize(), source.getSort()) {

			private static final long serialVersionUID = 8100166028148948968L;

			@Override
			public long getOffset() {
				return source.getOffset();
			}

			@Override
			public int getPageSize() {
				return limit;
			}

		};
	}

	private interface QueryExecution {
		Object execute(Query query);
	}

	/**
	 * Base class for query execution implementing {@link QueryExecution}
	 *
	 * @author Christoph Strobl
	 */
	abstract class AbstractQueryExecution implements QueryExecution {

		protected Page<?> executeFind(Query query) {
			EntityMetadata<?> metadata = solrQueryMethod.getEntityInformation();
			return solrOperations.queryForPage(collection, query, metadata.getJavaType());
		}
	}

	/**
	 * Implementation to query solr returning list of data without metadata. <br />
	 * If not pageable argument is set count operation will be executed to determine total number of entities to be
	 * fetched
	 *
	 * @author Christoph Strobl
	 */
	class CollectionExecution extends AbstractQueryExecution {

		private final Pageable pageable;

		public CollectionExecution(Pageable pageable) {
			this.pageable = pageable;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Object execute(Query query) {

			if (!isLimiting()) {

				query.setPageRequest(pageable.isPaged() ? pageable : new SolrPageRequest(0, (int) count(query)));
				return executeFind(query).getContent();
			}

			if (pageable.isUnpaged() && isLimiting()) {
				return executeFind(query.setPageRequest(new SolrPageRequest(0, getLimit()))).getContent();
			}

			if (getLimit() > 0) {
				if (pageable.getOffset() > getLimit()) {
					return new PageImpl(java.util.Collections.emptyList(), pageable, getLimit());
				}
				if (pageable.getOffset() + pageable.getPageSize() > getLimit()) {
					query.setPageRequest(getLimitingPageable(pageable, getLimit() - (int) pageable.getOffset()));
				}
			}
			return executeFind(query).getContent();
		}

		private long count(Query query) {
			return solrOperations.count(collection, query);
		}

	}

	/**
	 * Implementation to query solr returning requested {@link Page}
	 *
	 * @author Christoph Strobl
	 */
	class PagedExecution extends AbstractQueryExecution {
		private final Pageable pageable;

		public PagedExecution(Pageable pageable) {
			Assert.notNull(pageable, "Pageable must not be null!");
			this.pageable = pageable;
		}

		@Override
		public Object execute(Query query) {

			Pageable pageToUse = getPageable();

			if (isLimiting()) {

				int limit = getLimit();
				if (pageToUse == null) {
					pageToUse = new SolrPageRequest(0, limit);
				}

				if (limit > 0) {
					if (pageToUse.getOffset() > limit) {
						return new PageImpl(java.util.Collections.emptyList(), pageToUse, limit);
					}
					if (pageToUse.getOffset() + pageToUse.getPageSize() > limit) {
						pageToUse = getLimitingPageable(pageToUse, limit - (int) pageToUse.getOffset());
					}
				}
			}

			query.setPageRequest(pageToUse);
			return executeFind(query);
		}

		protected Pageable getPageable() {
			return this.pageable;
		}
	}

	/**
	 * Implementation to query solr retuning {@link FacetPage}
	 *
	 * @author Christoph Strobl
	 */
	class FacetPageExecution extends PagedExecution {

		public FacetPageExecution(Pageable pageable) {
			super(pageable);
		}

		@Override
		protected FacetPage<?> executeFind(Query query) {
			Assert.isInstanceOf(FacetQuery.class, query, "Query must be instance of FacetQuery!");

			EntityMetadata<?> metadata = solrQueryMethod.getEntityInformation();
			return solrOperations.queryForFacetPage(collection, (FacetQuery) query, metadata.getJavaType());
		}

	}

	/**
	 * Implementation to execute query returning {@link HighlightPage}
	 *
	 * @author Christoph Strobl
	 */
	class HighlightPageExecution extends PagedExecution {

		public HighlightPageExecution(Pageable pageable) {
			super(pageable);
		}

		@Override
		protected HighlightPage<?> executeFind(Query query) {
			Assert.isInstanceOf(HighlightQuery.class, query, "Query must be instanceof HighlightQuery!");

			EntityMetadata<?> metadata = solrQueryMethod.getEntityInformation();
			return solrOperations.queryForHighlightPage(collection, (HighlightQuery) query, metadata.getJavaType());
		}
	}

	/**
	 * Implementation to query solr returning {@link FacetAndHighlightPage}
	 *
	 * @author David Webb
	 * @since 2.1
	 */
	class FacetAndHighlightPageExecution extends PagedExecution {

		public FacetAndHighlightPageExecution(Pageable pageable) {
			super(pageable);
		}

		@Override
		protected FacetAndHighlightPage<?> executeFind(Query query) {

			Assert.isInstanceOf(FacetAndHighlightQuery.class, query, "Query must be instance of FacetAndHighlightQuery!");

			EntityMetadata<?> metadata = solrQueryMethod.getEntityInformation();
			return solrOperations.queryForFacetAndHighlightPage(collection, (FacetAndHighlightQuery) query,
					metadata.getJavaType());
		}
	}

	/**
	 * Implementation to query solr returning one single entity
	 *
	 * @author Christoph Strobl
	 */
	class SingleEntityExecution implements QueryExecution {

		@Override
		public Object execute(Query query) {

			EntityMetadata<?> metadata = solrQueryMethod.getEntityInformation();

			Optional<?> result = solrOperations.queryForObject(collection, query, metadata.getJavaType());
			return solrQueryMethod.returnsOptional() ? result : result.orElse(null);
		}
	}

	/**
	 * @since 1.2
	 */
	class CountExecution implements QueryExecution {

		@Override
		public Object execute(Query query) {
			return solrOperations.count(collection, query);
		}

	}

	/**
	 * @since 1.2
	 */
	class DeleteExecution implements QueryExecution {

		@Override
		public Object execute(Query query) {

			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				SolrTransactionSynchronizationAdapterBuilder.forOperations(solrOperations).onCollection(collection)
						.withDefaultBehaviour().register();
			}

			Object result = countOrGetDocumentsForDelete(query);

			solrOperations.delete(collection, query);
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				solrOperations.commit(collection);
			}

			return result;
		}

		@Nullable
		private Object countOrGetDocumentsForDelete(Query query) {

			Object result = null;

			if (solrQueryMethod.isCollectionQuery()) {
				Query clone = SimpleQuery.fromQuery(query);
				result = solrOperations
						.queryForPage(collection, clone.setPageRequest(new SolrPageRequest(0, Integer.MAX_VALUE)),
								solrQueryMethod.getEntityInformation().getJavaType())
						.getContent();
			}

			if (ClassUtils.isAssignable(Number.class, solrQueryMethod.getReturnedObjectType())) {
				result = solrOperations.count(collection, query);
			}
			return result;
		}
	}

}
