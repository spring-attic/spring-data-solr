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
package org.springframework.data.solr.repository.query;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.convert.DateTimeConverters;
import org.springframework.data.solr.core.convert.NumberConverters;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.GeoConverters;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.util.Assert;

/**
 * Base implementation of a solr specific {@link RepositoryQuery}
 * 
 * @author Christoph Strobl
 */
public abstract class AbstractSolrQuery implements RepositoryQuery {

	private static final Pattern PARAMETER_PLACEHOLDER = Pattern.compile("\\?(\\d+)");

	private final SolrOperations solrOperations;
	private final SolrQueryMethod solrQueryMethod;

	private final GenericConversionService conversionService = new GenericConversionService();

	{
		if (!conversionService.canConvert(java.util.Date.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JavaDateConverter.INSTANCE);
		}
		if (!conversionService.canConvert(org.joda.time.ReadableInstant.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JodaDateTimeConverter.INSTANCE);
		}
		if (!conversionService.canConvert(org.joda.time.LocalDateTime.class, String.class)) {
			conversionService.addConverter(DateTimeConverters.JodaLocalDateTimeConverter.INSTANCE);
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
	}

	public AbstractSolrQuery(SolrOperations solrOperations, SolrQueryMethod solrQueryMethod) {
		Assert.notNull(solrOperations);
		Assert.notNull(solrQueryMethod);
		this.solrOperations = solrOperations;
		this.solrQueryMethod = solrQueryMethod;
	}

	@Override
	public Object execute(Object[] parameters) {
		SolrParameterAccessor accessor = new SolrParametersParameterAccessor(solrQueryMethod, parameters);

		Query query = createQuery(accessor);
		decorateWithFilterQuery(query, accessor);
		setDefaultQueryOperatorIfDefined(query);

		if (solrQueryMethod.isPageQuery()) {
			if (solrQueryMethod.isFacetQuery()) {
				FacetQuery facetQuery = SimpleFacetQuery.fromQuery(query, new SimpleFacetQuery());
				facetQuery.setFacetOptions(extractFacetOptions(solrQueryMethod, accessor));
				return new FacetPageExecution(accessor.getPageable()).execute(facetQuery);
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
		if (defaultOperator != null && !Query.Operator.NONE.equals(defaultOperator)) {
			query.setDefaultOperator(defaultOperator);
		}
	}

	private void decorateWithFilterQuery(Query query, SolrParameterAccessor parameterAccessor) {
		if (solrQueryMethod.hasFilterQuery()) {
			for (String filterQuery : solrQueryMethod.getFilterQueries()) {
				query.addFilterQuery(createQueryFromString(filterQuery, parameterAccessor));
			}
		}
	}

	protected void appendProjection(Query query) {
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
		options.setFacetLimit(queryMethod.getFacetLimit());
		options.setFacetMinCount(queryMethod.getFacetMinCount());
		return options;
	}

	protected abstract Query createQuery(SolrParameterAccessor parameterAccessor);

	private interface QueryExecution {
		Object execute(Query query);
	}

	abstract class AbstractQueryExecution implements QueryExecution {

		protected Page<?> executeFind(Query query) {
			SolrEntityInformation<?, ?> metadata = solrQueryMethod.getEntityInformation();
			return solrOperations.queryForPage(query, metadata.getJavaType());
		}

	}

	class CollectionExecution extends AbstractQueryExecution {
		private final Pageable pageable;

		public CollectionExecution(Pageable pageable) {
			this.pageable = pageable;
		}

		@Override
		public Object execute(Query query) {
			query.setPageRequest(pageable != null ? pageable : new PageRequest(0, Math.max(1, (int) count(query))));
			return executeFind(query).getContent();
		}

		private long count(Query query) {
			return solrOperations.count(query);
		}

	}

	class PagedExecution extends AbstractQueryExecution {
		private final Pageable pageable;

		public PagedExecution(Pageable pageable) {
			Assert.notNull(pageable);
			this.pageable = pageable;
		}

		@Override
		public Object execute(Query query) {
			query.setPageRequest(getPageable());
			return executeFind(query);
		}

		protected Pageable getPageable() {
			return this.pageable;
		}
	}

	class FacetPageExecution extends PagedExecution {

		public FacetPageExecution(Pageable pageable) {
			super(pageable);
		}

		@Override
		protected FacetPage<?> executeFind(Query query) {
			Assert.isInstanceOf(FacetQuery.class, query);

			SolrEntityInformation<?, ?> metadata = solrQueryMethod.getEntityInformation();
			return solrOperations.queryForFacetPage((FacetQuery) query, metadata.getJavaType());
		}

	}

	class SingleEntityExecution implements QueryExecution {

		@Override
		public Object execute(Query query) {
			SolrEntityInformation<?, ?> metadata = solrQueryMethod.getEntityInformation();
			return solrOperations.queryForObject(query, metadata.getJavaType());
		}
	}

}
