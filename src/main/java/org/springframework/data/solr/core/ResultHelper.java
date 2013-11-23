/*
 * Copyright 2012 - 2014 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.TermsResponse;
import org.apache.solr.client.solrj.response.TermsResponse.Term;
import org.apache.solr.common.util.NamedList;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.repository.util.ClassUtils;
import org.springframework.data.solr.VersionUtil;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimplePivotField;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPivotFieldEntry;
import org.springframework.data.solr.core.query.result.FacetQueryEntry;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.SimpleFacetFieldEntry;
import org.springframework.data.solr.core.query.result.SimpleFacetPivotEntry;
import org.springframework.data.solr.core.query.result.SimpleFacetQueryEntry;
import org.springframework.data.solr.core.query.result.SimpleTermsFieldEntry;
import org.springframework.data.solr.core.query.result.SolrResultPage;
import org.springframework.data.solr.core.query.result.TermsFieldEntry;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Use Result Helper to extract various parameters from the QueryResponse and convert it into a proper Format taking
 * care of non existent and null elements with the response.
 * 
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
final class ResultHelper {

	private ResultHelper() {}

	static Map<String, List<TermsFieldEntry>> convertTermsQueryResponseToTermsMap(QueryResponse response) {
		if (response == null || response.getTermsResponse() == null || response.getTermsResponse().getTermMap() == null) {
			return Collections.emptyMap();
		}

		TermsResponse termsResponse = response.getTermsResponse();
		Map<String, List<TermsFieldEntry>> result = new LinkedHashMap<String, List<TermsFieldEntry>>(termsResponse
				.getTermMap().size());

		for (Map.Entry<String, List<Term>> entry : termsResponse.getTermMap().entrySet()) {
			List<TermsFieldEntry> terms = new ArrayList<TermsFieldEntry>(entry.getValue().size());
			for (Term term : entry.getValue()) {
				SimpleTermsFieldEntry termsEntry = new SimpleTermsFieldEntry(term.getTerm(), term.getFrequency());
				termsEntry.setField(entry.getKey());
				terms.add(termsEntry);
			}
			result.put(entry.getKey(), terms);
		}

		return result;
	}

	static Map<Field, Page<FacetFieldEntry>> convertFacetQueryResponseToFacetPageMap(FacetQuery query,
			QueryResponse response) {
		Assert.notNull(query, "Cannot convert response for 'null', query");

		if (!hasFacets(query, response)) {
			return Collections.emptyMap();
		}
		Map<Field, Page<FacetFieldEntry>> facetResult = new HashMap<Field, Page<FacetFieldEntry>>();

		if (CollectionUtils.isNotEmpty(response.getFacetFields())) {
			int initalPageSize = query.getFacetOptions().getPageable().getPageSize();
			for (FacetField facetField : response.getFacetFields()) {
				if (facetField != null && StringUtils.hasText(facetField.getName())) {
					Field field = new SimpleField(facetField.getName());
					if (CollectionUtils.isNotEmpty(facetField.getValues())) {
						List<FacetFieldEntry> pageEntries = new ArrayList<FacetFieldEntry>(initalPageSize);
						for (Count count : facetField.getValues()) {
							if (count != null) {
								pageEntries.add(new SimpleFacetFieldEntry(field, count.getName(), count.getCount()));
							}
						}
						facetResult.put(field, new SolrResultPage<FacetFieldEntry>(pageEntries, query.getFacetOptions()
								.getPageable(), facetField.getValueCount(), null));
					} else {
						facetResult.put(field, new SolrResultPage<FacetFieldEntry>(Collections.<FacetFieldEntry> emptyList(), query
								.getFacetOptions().getPageable(), 0, null));
					}
				}
			}
		}
		return facetResult;
	}

	static Map<org.springframework.data.solr.core.query.PivotField, List<FacetPivotFieldEntry>> convertFacetQueryResponseToFacetPivotMap(
			FacetQuery query, QueryResponse response) {

		if (VersionUtil.isSolr3XAvailable()) {
			// pivot facets are a solr 4+ Feature
			return Collections.emptyMap();
		}

		Map<org.springframework.data.solr.core.query.PivotField, List<FacetPivotFieldEntry>> facetResult = new HashMap<org.springframework.data.solr.core.query.PivotField, List<FacetPivotFieldEntry>>();
		NamedList<List<PivotField>> facetPivot = response.getFacetPivot();
		if (facetPivot != null && facetPivot.size() > 0) {
			for (int i = 0; i < facetPivot.size(); i++) {
				String name = facetPivot.getName(i);
				List<PivotField> pivotResult = facetPivot.get(name);
				facetResult.put(new SimplePivotField(name), convertPivotResult(pivotResult));
			}
		}

		return facetResult;
	}

	private static List<FacetPivotFieldEntry> convertPivotResult(List<PivotField> pivotResult) {
		if (CollectionUtils.isEmpty(pivotResult)) {
			return Collections.emptyList();
		}

		ArrayList<FacetPivotFieldEntry> pivotFieldEntries = new ArrayList<FacetPivotFieldEntry>();

		for (PivotField pivotField : pivotResult) {
			SimpleFacetPivotEntry pivotFieldEntry = new SimpleFacetPivotEntry(new SimpleField(pivotField.getField()),
					String.valueOf(pivotField.getValue()), pivotField.getCount());

			List<PivotField> pivot = pivotField.getPivot();
			if (pivot != null) {
				pivotFieldEntry.setPivot(convertPivotResult(pivot));
			}

			pivotFieldEntries.add(pivotFieldEntry);
		}

		return pivotFieldEntries;
	}

	static List<FacetQueryEntry> convertFacetQueryResponseToFacetQueryResult(FacetQuery query, QueryResponse response) {
		Assert.notNull(query, "Cannot convert response for 'null', query");

		if (!hasFacets(query, response)) {
			return Collections.emptyList();
		}

		List<FacetQueryEntry> facetResult = new ArrayList<FacetQueryEntry>();

		if (MapUtils.isNotEmpty(response.getFacetQuery())) {
			for (Entry<String, Integer> entry : response.getFacetQuery().entrySet()) {
				facetResult.add(new SimpleFacetQueryEntry(entry.getKey(), entry.getValue()));
			}
		}
		return facetResult;
	}

	static <T> List<HighlightEntry<T>> convertAndAddHighlightQueryResponseToResultPage(QueryResponse response,
			SolrResultPage<T> page) {
		if (response == null || MapUtils.isEmpty(response.getHighlighting()) || page == null) {
			return Collections.emptyList();
		}

		List<HighlightEntry<T>> mappedHighlights = new ArrayList<HighlightEntry<T>>(page.getSize());
		Map<String, Map<String, List<String>>> highlighting = response.getHighlighting();

		for (T item : page) {
			HighlightEntry<T> highlightEntry = processHighlightingForPageEntry(highlighting, item);
			mappedHighlights.add(highlightEntry);
		}
		page.setHighlighted(mappedHighlights);
		return mappedHighlights;
	}

	private static <T> HighlightEntry<T> processHighlightingForPageEntry(
			Map<String, Map<String, List<String>>> highlighting, T pageEntry) {
		HighlightEntry<T> highlightEntry = new HighlightEntry<T>(pageEntry);
		Object itemId = getMappedId(pageEntry);

		Map<String, List<String>> highlights = highlighting.get(itemId.toString());
		if (MapUtils.isNotEmpty(highlights)) {
			for (Map.Entry<String, List<String>> entry : highlights.entrySet()) {
				highlightEntry.addSnipplets(entry.getKey(), entry.getValue());
			}
		}
		return highlightEntry;
	}

	private static Object getMappedId(Object o) {
		if (ClassUtils.hasProperty(o.getClass(), "id")) {
			try {
				return FieldUtils.readDeclaredField(o, "id", true);
			} catch (IllegalAccessException e) {
				throw new MappingException("Id property could not be accessed!", e);
			}
		}

		for (java.lang.reflect.Field field : o.getClass().getDeclaredFields()) {
			Annotation annotation = AnnotationUtils.getAnnotation(field, Id.class);
			if (annotation != null) {
				try {
					return FieldUtils.readField(field, o, true);
				} catch (IllegalArgumentException e) {
					throw new MappingException("Id property could not be accessed!", e);
				} catch (IllegalAccessException e) {
					throw new MappingException("Id property could not be accessed!", e);
				}
			}
		}
		throw new MappingException("Id property could not be found!");
	}

	private static boolean hasFacets(FacetQuery query, QueryResponse response) {
		return query.hasFacetOptions() && response != null;
	}

}
