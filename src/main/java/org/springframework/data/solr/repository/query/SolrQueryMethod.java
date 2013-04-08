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
package org.springframework.data.solr.repository.query;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.solr.repository.Facet;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Solr specific implementation of {@link QueryMethod} taking care of {@link Query}
 * 
 * @author Christoph Strobl
 * @author Luke Corpe
 * @author Andrey Paramonov
 */
public class SolrQueryMethod extends QueryMethod {

	private final SolrEntityInformation<?, ?> entityInformation;
	private Method method;

	public SolrQueryMethod(Method method, RepositoryMetadata metadata, SolrEntityInformationCreator solrInformationCreator) {
		super(method, metadata);
		this.method = method;
		this.entityInformation = solrInformationCreator.getEntityInformation(metadata.getReturnedDomainClass(method));
	}

	public boolean hasAnnotatedQuery() {
		return getAnnotatedQuery() != null;
	}

	public boolean hasQueryAnnotation() {
		return this.method.getAnnotation(Query.class) != null;
	}

	String getAnnotatedQuery() {
		String query = (String) AnnotationUtils.getValue(getQueryAnnotation(), "value");
		return StringUtils.hasText(query) ? query : null;
	}

	public boolean hasAnnotatedNamedQueryName() {
		return getAnnotatedNamedQueryName() != null;
	}

	String getAnnotatedNamedQueryName() {
		String namedQueryName = (String) AnnotationUtils.getValue(getQueryAnnotation(), "name");
		return StringUtils.hasText(namedQueryName) ? namedQueryName : null;
	}

	private Query getQueryAnnotation() {
		return this.method.getAnnotation(Query.class);
	}

	TypeInformation<?> getReturnType() {
		return ClassTypeInformation.fromReturnTypeOf(method);
	}

	public boolean hasProjectionFields() {
		if (hasQueryAnnotation()) {
			return !CollectionUtils.isEmpty(getProjectionFields());
		}
		return false;
	}

	public List<String> getProjectionFields() {
		return getAnnotationValuesAsStringList(getQueryAnnotation(), "fields");
	}

	public Integer getTimeAllowed() {
		if (hasQueryAnnotation()) {
			Integer timeAllowed = (Integer) AnnotationUtils.getValue(getQueryAnnotation(), "timeAllowed");
			if (timeAllowed != null && timeAllowed.intValue() > 0) {
				return timeAllowed;
			}
		}
		return null;
	}

	public boolean isFacetQuery() {
		return hasFacetFields() || hasFacetQueries();
	}

	public boolean hasFacetFields() {
		if (hasFacetAnnotation()) {
			return !CollectionUtils.isEmpty(getFacetFields());
		}
		return false;
	}

	private boolean hasFacetAnnotation() {
		return getFacetAnnotation() != null;
	}

	List<String> getFacetFields() {
		return getAnnotationValuesAsStringList(getFacetAnnotation(), "fields");
	}

	List<String> getFacetQueries() {
		return getAnnotationValuesAsStringList(getFacetAnnotation(), "queries");
	}

	public boolean hasFacetQueries() {
		if (hasFacetAnnotation()) {
			return !CollectionUtils.isEmpty(getFacetQueries());
		}
		return false;
	}

	private Annotation getFacetAnnotation() {
		return this.method.getAnnotation(Facet.class);
	}

	public Integer getFacetLimit() {
		return (Integer) AnnotationUtils.getValue(getFacetAnnotation(), "limit");
	}

	public Integer getFacetMinCount() {
		return (Integer) AnnotationUtils.getValue(getFacetAnnotation(), "minCount");
	}

	public boolean hasFilterQuery() {
		if (hasQueryAnnotation()) {
			return !CollectionUtils.isEmpty(getFilterQueries());
		}
		return false;
	}

	List<String> getFilterQueries() {
		return getAnnotationValuesAsStringList(getQueryAnnotation(), "filters");
	}

	public org.springframework.data.solr.core.query.Query.Operator getDefaultOperator() {
		if (hasQueryAnnotation()) {
			return getQueryAnnotation().defaultOperator();
		}
		return org.springframework.data.solr.core.query.Query.Operator.NONE;
	}

	public String getDefType() {
		if (hasQueryAnnotation()) {
			return getQueryAnnotation().defType();
		}
		return null;
	}

	public String getRequestHandler() {
		if (hasQueryAnnotation()) {
			return getQueryAnnotation().requestHandler();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<String> getAnnotationValuesAsStringList(Annotation annotation, String attribute) {
		String[] values = (String[]) AnnotationUtils.getValue(annotation, attribute);
		if (values.length > 1 || (values.length == 1 && StringUtils.hasText(values[0]))) {
			return CollectionUtils.arrayToList(values);
		}
		return Collections.emptyList();
	}

	@Override
	public SolrEntityInformation<?, ?> getEntityInformation() {
		return entityInformation;
	}

	@Override
	public String getNamedQueryName() {
		if (!hasAnnotatedNamedQueryName()) {
			return super.getNamedQueryName();
		}
		return getAnnotatedNamedQueryName();
	}

	@Override
	protected SolrParameters createParameters(Method method) {
		return new SolrParameters(method);
	}

	@Override
	public SolrParameters getParameters() {
		return (SolrParameters) super.getParameters();
	}

}
