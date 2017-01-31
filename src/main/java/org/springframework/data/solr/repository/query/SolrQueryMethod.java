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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.solr.core.query.SpellcheckOptions;
import org.springframework.data.solr.repository.Facet;
import org.springframework.data.solr.repository.Highlight;
import org.springframework.data.solr.repository.Pivot;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.SelectiveStats;
import org.springframework.data.solr.repository.Spellcheck;
import org.springframework.data.solr.repository.Stats;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Solr specific implementation of {@link QueryMethod} taking care of {@link Query}
 *
 * @author Christoph Strobl
 * @author Luke Corpe
 * @author Andrey Paramonov
 * @author Francisco Spaeth
 * @author Mark Paluch
 */
public class SolrQueryMethod extends QueryMethod {

	private final Method method;

	public SolrQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
			SolrEntityInformationCreator solrInformationCreator) {
		super(method, metadata, factory);
		this.method = method;
	}

	/**
	 * @return true if {@link Query#value()} is not blank
	 */
	public boolean hasAnnotatedQuery() {
		return getAnnotatedQuery() != null;
	}

	/**
	 * @return true if {@link Query} is not blank
	 */
	public boolean hasQueryAnnotation() {
		return this.method.getAnnotation(Query.class) != null;
	}

	@Nullable
	String getAnnotatedQuery() {
		return getAnnotationValueAsStringOrNullIfBlank(getQueryAnnotation(), "value");
	}

	/**
	 * @return true if {@link Query#name()} is not blank
	 */
	public boolean hasAnnotatedNamedQueryName() {
		return getAnnotatedNamedQueryName() != null;
	}

	@Nullable
	String getAnnotatedNamedQueryName() {
		return getAnnotationValueAsStringOrNullIfBlank(getQueryAnnotation(), "name");
	}

	private Query getQueryAnnotation() {
		return this.method.getAnnotation(Query.class);
	}

	TypeInformation<?> getReturnType() {
		return ClassTypeInformation.fromReturnTypeOf(method);
	}

	/**
	 * @return true if {@link Query#fields()} is not empty
	 */
	public boolean hasProjectionFields() {
		return hasQueryAnnotation() && !CollectionUtils.isEmpty(getProjectionFields());
	}

	/**
	 * @return empty collection if {@link Query#fields()} is empty
	 */
	public List<String> getProjectionFields() {
		return getAnnotationValuesAsStringList(getQueryAnnotation(), "fields");
	}

	/**
	 * @return null if {@link Query#timeAllowed()} is null or negative
	 */
	@Nullable
	public Integer getTimeAllowed() {
		if (hasQueryAnnotation()) {
			return getAnnotationValueAsIntOrNullIfNegative(getQueryAnnotation(), "timeAllowed");
		}
		return null;
	}

	/**
	 * @return true if {@link #hasFacetFields()} or {@link #hasFacetQueries()}
	 */
	public boolean isFacetQuery() {
		return hasFacetFields() || hasFacetQueries() || hasPivotFields();
	}

	/**
	 * @return true if {@link Facet#fields()} is not empty
	 */
	public boolean hasFacetFields() {
		return hasFacetAnnotation() && !CollectionUtils.isEmpty(getFacetFields());
	}

	/**
	 * @return true if is not empty
	 */
	public boolean hasPivotFields() {
		return hasFacetAnnotation() && !CollectionUtils.isEmpty(getPivotFields());
	}

	private boolean hasFacetAnnotation() {
		return getFacetAnnotation() != null;
	}

	/**
	 * @return empty collection if {@link Facet#fields()} is empty
	 */
	public List<String> getFacetFields() {
		return getAnnotationValuesAsStringList(getFacetAnnotation(), "fields");
	}

	/**
	 * @return empty collection if {@link Facet#queries()} is empty
	 */
	public List<String> getFacetQueries() {
		return getAnnotationValuesAsStringList(getFacetAnnotation(), "queries");
	}

	public List<String[]> getPivotFields() {
		List<Pivot> pivotFields = getAnnotationValuesList(getFacetAnnotation(), "pivots", Pivot.class);
		ArrayList<String[]> result = new ArrayList<>();

		for (Pivot pivot : pivotFields) {
			result.add(pivot.value());
		}

		return result;
	}

	/**
	 * @return true if {@link Facet#queries()} is not empty
	 */
	public boolean hasFacetQueries() {
		return hasFacetAnnotation() && !CollectionUtils.isEmpty(getFacetQueries());
	}

	@Nullable
	private Facet getFacetAnnotation() {
		return this.method.getAnnotation(Facet.class);
	}

	/**
	 * @return value of {@link Facet#limit()}
	 */
	@Nullable
	public Integer getFacetLimit() {

		Facet facetAnnotation = getFacetAnnotation();

		if (facetAnnotation != null) {
			return (Integer) AnnotationUtils.getValue(getFacetAnnotation(), "limit");
		}

		return null;
	}

	/**
	 * @return value of {@link Facet#minCount()}
	 */
	@Nullable
	public Integer getFacetMinCount() {

		Facet facetAnnotation = getFacetAnnotation();

		if (facetAnnotation != null) {
			return (Integer) AnnotationUtils.getValue(getFacetAnnotation(), "minCount");
		}

		return null;
	}

	/**
	 * @return value of {@link Facet#prefix()}
	 */
	@Nullable
	public String getFacetPrefix() {
		return getAnnotationValueAsStringOrNullIfBlank(getFacetAnnotation(), "prefix");
	}

	/**
	 * @return the {@link Stats} annotation, null if there is none
	 */
	@Nullable
	private Stats getStatsAnnotation() {
		return this.method.getAnnotation(Stats.class);
	}

	/**
	 * @return if something was configured within {@link Stats}
	 * @since 1.4
	 */
	public boolean hasStatsDefinition() {
		return (//
		!(getStatsAnnotation() == null) && (//
		!getFieldStats().isEmpty() || //
				!getStatsFacets().isEmpty() || //
				!getStatsSelectiveFacets().isEmpty() || //
				!getStatsSelectiveCountDistinctFields().isEmpty())//
		);
	}

	/**
	 * @return true if stats is distinct
	 * @since 1.4
	 */
	public boolean isFieldStatsCountDistinctEnable() {

		Stats stats = getStatsAnnotation();
		return stats != null && stats.distinct();
	}

	/**
	 * @return value of {@link Stats#value()}
	 * @since 1.4
	 */
	public List<String> getFieldStats() {
		return getAnnotationValuesAsStringList(getStatsAnnotation(), "value");
	}

	/**
	 * @return value of {@link Stats#facets()}
	 * @since 1.4
	 */
	public List<String> getStatsFacets() {
		return getAnnotationValuesAsStringList(getStatsAnnotation(), "facets");
	}

	/**
	 * @return value of facets used in {@link Stats#selective()}
	 * @since 1.4
	 */
	public Map<String, String[]> getStatsSelectiveFacets() {

		List<SelectiveStats> selective = getAnnotationValuesList(getStatsAnnotation(), "selective", SelectiveStats.class);

		Map<String, String[]> result = new LinkedHashMap<>();
		for (SelectiveStats selectiveFacet : selective) {
			result.put(selectiveFacet.field(), selectiveFacet.facets());
		}

		return result;
	}

	/**
	 * @return value of facets used in {@link Stats#selective()}
	 * @since 1.4
	 */
	public Collection<String> getStatsSelectiveCountDistinctFields() {

		List<SelectiveStats> selective = getAnnotationValuesList(getStatsAnnotation(), "selective", SelectiveStats.class);

		Collection<String> result = new LinkedHashSet<>();
		for (SelectiveStats selectiveFacet : selective) {
			if (selectiveFacet.distinct()) {
				result.add(selectiveFacet.field());
			}
		}

		return result;
	}

	/**
	 * @return true if {@link Query#filters()} is not empty
	 */
	public boolean hasFilterQuery() {
		return hasQueryAnnotation() && !CollectionUtils.isEmpty(getFilterQueries());
	}

	/**
	 * @return value of {@link Query#delete()}
	 * @since 1.2
	 */
	public boolean isDeleteQuery() {
		return hasQueryAnnotation() && (Boolean) AnnotationUtils.getValue(getQueryAnnotation(), "delete");
	}

	@Nullable
	private Annotation getHighlightAnnotation() {
		return this.method.getAnnotation(Highlight.class);
	}

	private boolean hasHighlightAnnotation() {
		return this.getHighlightAnnotation() != null;
	}

	/**
	 * @return if {@link Highlight} is present
	 */
	public boolean isHighlightQuery() {
		return this.hasHighlightAnnotation();
	}

	/**
	 * @return empty collection if {@link Highlight#fields()} is empty
	 */
	public List<String> getHighlightFieldNames() {
		if (hasHighlightAnnotation()) {
			return this.getAnnotationValuesAsStringList(getHighlightAnnotation(), "fields");
		}
		return Collections.emptyList();
	}

	/**
	 * @return null if {@link Highlight#query()} is blank
	 */
	@Nullable
	public String getHighlightQuery() {
		if (hasHighlightAnnotation()) {
			return getAnnotationValueAsStringOrNullIfBlank(getHighlightAnnotation(), "query");
		}
		return null;
	}

	/**
	 * @return value of {@link Highlight#snipplets()} or null if negative
	 */
	@Nullable
	public Integer getHighlighSnipplets() {
		if (hasHighlightAnnotation()) {
			return getAnnotationValueAsIntOrNullIfNegative(getHighlightAnnotation(), "snipplets");
		}
		return null;
	}

	/**
	 * @return value of {@link Highlight#fragsize()} or null if negative
	 */
	@Nullable
	public Integer getHighlightFragsize() {
		if (hasHighlightAnnotation()) {
			return getAnnotationValueAsIntOrNullIfNegative(getHighlightAnnotation(), "fragsize");
		}
		return null;
	}

	/**
	 * @return value of {@link Highlight#formatter()} or null if blank
	 */
	@Nullable
	public String getHighlightFormatter() {
		if (hasHighlightAnnotation()) {
			return getAnnotationValueAsStringOrNullIfBlank(getHighlightAnnotation(), "formatter");
		}
		return null;
	}

	/**
	 * @return value of {@link Highlight#prefix()} or null if blank
	 */
	@Nullable
	public String getHighlightPrefix() {
		if (hasHighlightAnnotation()) {
			return getAnnotationValueAsStringOrNullIfBlank(getHighlightAnnotation(), "prefix");
		}
		return null;
	}

	/**
	 * @return value of {@link Highlight#postfix()} or null if blank
	 */
	@Nullable
	public String getHighlightPostfix() {
		if (hasHighlightAnnotation()) {
			return getAnnotationValueAsStringOrNullIfBlank(getHighlightAnnotation(), "postfix");
		}
		return null;
	}

	/**
	 * @return true if {@link Highlight#fields()} is not empty
	 */
	public boolean hasHighlightFields() {
		return !getHighlightFieldNames().isEmpty();
	}

	List<String> getFilterQueries() {
		return getAnnotationValuesAsStringList(getQueryAnnotation(), "filters");
	}

	/**
	 * @return value of {@link Query#defaultOperator()} or
	 *         {@link org.springframework.data.solr.core.query.Query.Operator#NONE} if not set
	 */
	public org.springframework.data.solr.core.query.Query.Operator getDefaultOperator() {

		if (hasQueryAnnotation()) {
			return getQueryAnnotation().defaultOperator();
		}
		return org.springframework.data.solr.core.query.Query.Operator.NONE;
	}

	/**
	 * @return null if {@link Query#defType()} not set
	 */
	@Nullable
	public String getDefType() {
		if (hasQueryAnnotation()) {
			return getQueryAnnotation().defType();
		}
		return null;
	}

	/**
	 * @return null if {@link Query#requestHandler()} not set
	 */
	@Nullable
	public String getRequestHandler() {
		if (hasQueryAnnotation()) {
			return getQueryAnnotation().requestHandler();
		}
		return null;
	}

	/**
	 * @return
	 * @since 2.1
	 */
	@Nullable
	public Spellcheck getSpellcheckAnnotation() {
		return AnnotatedElementUtils.findMergedAnnotation(this.method, Spellcheck.class);
	}

	/**
	 * @return
	 * @since 2.1
	 */
	public boolean hasSpellcheck() {
		return getSpellcheckAnnotation() != null;
	}

	/**
	 * @return
	 * @since 2.1
	 */
	@Nullable
	public SpellcheckOptions getSpellcheckOptions() {

		Spellcheck spellcheck = getSpellcheckAnnotation();
		if (spellcheck == null) {
			return null;
		}

		SpellcheckOptions sc = SpellcheckOptions.spellcheck();
		if (spellcheck.accuracy() >= 0F) {
			sc = sc.accuracy(spellcheck.accuracy());
		}
		if (spellcheck.buildDictionary()) {
			sc = sc.buildDictionary();
		}
		if (spellcheck.collate()) {
			sc = sc.collate();
		}
		if (spellcheck.collateExtendedResults()) {
			sc = sc.collateExtendedResults();
		}
		if (spellcheck.onlyMorePopular()) {
			sc = sc.onlyMorePopular();
		}
		if (spellcheck.alternativeTermCount() >= 0) {
			sc = sc.alternativeTermCount(spellcheck.alternativeTermCount());
		}
		if (spellcheck.count() >= 0) {
			sc = sc.count(spellcheck.count());
		}
		if (!ObjectUtils.isEmpty(spellcheck.dictionaries())) {
			sc = sc.dictionaries(spellcheck.dictionaries());
		}
		if (spellcheck.maxCollationEvaluations() >= 0) {
			sc = sc.maxCollationEvaluations(spellcheck.maxCollationEvaluations());
		}
		if (spellcheck.maxCollations() >= 0) {
			sc = sc.maxCollations(spellcheck.maxCollations());
		}
		if (spellcheck.maxCollationsTries() >= 0) {
			sc = sc.maxCollationTries(spellcheck.maxCollationsTries());
		}
		if (spellcheck.maxResultsForSuggest() >= 0) {
			sc = sc.maxResultsForSuggest(spellcheck.maxResultsForSuggest());
		}
		if (spellcheck.maxCollationCollectDocs() >= 0) {
			sc = sc.maxCollationCollectDocs(spellcheck.maxCollationCollectDocs());
		}
		if (spellcheck.extendedResults()) {
			sc = sc.extendedResults();
		}
		return sc;
	}

	private String getAnnotationValueAsStringOrNullIfBlank(@Nullable Annotation annotation, String attributeName) {

		if (annotation == null) {
			return null;
		}

		String value = (String) AnnotationUtils.getValue(annotation, attributeName);

		return StringUtils.hasText(value) ? value : null;
	}

	@Nullable
	private Integer getAnnotationValueAsIntOrNullIfNegative(@Nullable Annotation annotation, String attributeName) {

		if (annotation == null) {
			return null;
		}

		Integer timeAllowed = (Integer) AnnotationUtils.getValue(annotation, attributeName);

		if (timeAllowed != null && timeAllowed > 0) {
			return timeAllowed;
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private List<String> getAnnotationValuesAsStringList(@Nullable Annotation annotation, String attribute) {

		if (annotation == null) {
			return Collections.emptyList();
		}

		String[] values = (String[]) AnnotationUtils.getValue(annotation, attribute);
		if (values.length > 1 || (values.length == 1 && StringUtils.hasText(values[0]))) {
			return CollectionUtils.arrayToList(values);
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> getAnnotationValuesList(@Nullable Annotation annotation, String attribute, Class<T> clazz) {

		if (annotation == null) {
			return Collections.emptyList();
		}

		T[] values = (T[]) AnnotationUtils.getValue(annotation, attribute);

		return CollectionUtils.arrayToList(values);
	}

	@Override
	public String getNamedQueryName() {
		if (!hasAnnotatedNamedQueryName()) {
			return super.getNamedQueryName();
		}
		return getAnnotatedNamedQueryName();
	}

	/**
	 * @return {@literal true} if the method return type is {@link Optional}.
	 * @since 2.0
	 */
	public boolean returnsOptional() {
		return ClassUtils.isAssignable(Optional.class, getReturnedObjectType());
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
