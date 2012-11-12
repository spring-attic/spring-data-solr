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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.convert.DateTimeConverters;
import org.springframework.data.solr.core.convert.NumberConverters;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.GeoConverters;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;

/**
 * Solr specific implementation of {@link RepositoryQuery} that can handle string based queries
 * 
 * @author Christoph Strobl
 */
public class StringBasedSolrQuery extends AbstractSolrQuery {

	private static final Pattern PARAMETER_PLACEHOLDER = Pattern.compile("\\?(\\d+)");

	private final String rawQueryString;
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

	public StringBasedSolrQuery(SolrQueryMethod method, SolrOperations solrOperations) {
		this(method.getAnnotatedQuery(), method, solrOperations);
	}

	public StringBasedSolrQuery(String query, SolrQueryMethod queryMethod, SolrOperations solrOperations) {
		super(solrOperations, queryMethod);
		this.rawQueryString = query;
	}

	@Override
	protected Query createQuery(SolrParameterAccessor parameterAccessor) {
		String queryString = replacePlaceholders(this.rawQueryString, parameterAccessor);

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria(queryString));
		appendProjection(query);
		return query;
	}

	private void appendProjection(SimpleQuery query) {
		if (this.getQueryMethod().hasProjectionFields()) {
			for (String fieldname : this.getQueryMethod().getProjectionFields()) {
				query.addProjectionOnField(fieldname);
			}
		}
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

	private String getParameterWithIndex(SolrParameterAccessor accessor, int index) {

		Object parameter = accessor.getBindableValue(index);

		if (parameter == null) {
			return "null";
		}

		if (conversionService.canConvert(parameter.getClass(), String.class)) {
			return conversionService.convert(parameter, String.class);
		}

		return parameter.toString();
	}
}
