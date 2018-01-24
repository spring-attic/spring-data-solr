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
package org.springframework.data.solr.core.query.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.Function;
import org.springframework.data.solr.core.query.PivotField;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimplePivotField;
import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Base implementation of page holding solr response entities.
 *
 * @author Christoph Strobl
 * @author Francisco Spaeth
 * @author David Webb
 * @author Petar Tahchiev
 */
public class SolrResultPage<T> extends PageImpl<T> implements FacetPage<T>, HighlightPage<T>, FacetAndHighlightPage<T>,
		ScoredPage<T>, GroupPage<T>, StatsPage<T>, SpellcheckedPage<T> {

	private static final long serialVersionUID = -4199560685036530258L;

	private Map<PageKey, Page<FacetFieldEntry>> facetResultPages = new LinkedHashMap<>(1);
	private Map<PageKey, List<FacetPivotFieldEntry>> facetPivotResultPages = new LinkedHashMap<>();
	private Map<PageKey, Page<FacetFieldEntry>> facetRangeResultPages = new LinkedHashMap<>(1);
	private @Nullable Page<FacetQueryEntry> facetQueryResult;
	private List<HighlightEntry<T>> highlighted = Collections.emptyList();
	private @Nullable Float maxScore;
	private Map<Object, GroupResult<T>> groupResults = Collections.emptyMap();
	private Map<String, FieldStatsResult> fieldStatsResults = Collections.emptyMap();
	private Map<String, List<Alternative>> suggestions = new LinkedHashMap<>();

	public SolrResultPage(List<T> content) {
		super(content);
	}

	public SolrResultPage(List<T> content, Pageable pageable, long total, @Nullable Float maxScore) {
		super(content, pageable, total);
		this.maxScore = maxScore;
	}

	private Page<FacetFieldEntry> getResultPage(String fieldname, Map<PageKey, Page<FacetFieldEntry>> resultPages) {
		Page<FacetFieldEntry> page = resultPages.get(new StringPageKey(fieldname));
		return page != null ? page : new PageImpl<>(Collections.<FacetFieldEntry> emptyList());
	}

	@Override
	public final Page<FacetFieldEntry> getFacetResultPage(String fieldname) {
		return getResultPage(fieldname, this.facetResultPages);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.FacetPage#getRangeFacetResultPage(java.lang.String)
	 */
	@Override
	public final Page<FacetFieldEntry> getRangeFacetResultPage(String fieldname) {
		return getResultPage(fieldname, this.facetRangeResultPages);
	}

	@Override
	public final Page<FacetFieldEntry> getFacetResultPage(Field field) {
		return this.getFacetResultPage(field.getName());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.FacetPage#getRangeFacetResultPage(org.springframework.data.solr.core.query.Field)
	 */
	@Override
	public final Page<FacetFieldEntry> getRangeFacetResultPage(Field field) {
		return getRangeFacetResultPage(field.getName());
	}

	@Override
	public List<FacetPivotFieldEntry> getPivot(String fieldName) {
		return facetPivotResultPages.get(new StringPageKey(fieldName));
	}

	@Override
	public List<FacetPivotFieldEntry> getPivot(PivotField field) {
		return facetPivotResultPages.get(new StringPageKey(field.getName()));
	}

	public final void addFacetResultPage(Page<FacetFieldEntry> page, Field field) {
		this.facetResultPages.put(new StringPageKey(field.getName()), page);
	}

	/**
	 * @param page
	 * @param field
	 * @since 1.5
	 */
	public final void addRangeFacetResultPage(Page<FacetFieldEntry> page, Field field) {
		this.facetRangeResultPages.put(new StringPageKey(field.getName()), page);
	}

	public final void addFacetPivotResultPage(List<FacetPivotFieldEntry> result, PivotField field) {
		this.facetPivotResultPages.put(new StringPageKey(field.getName()), result);
	}

	public void addAllFacetFieldResultPages(Map<Field, Page<FacetFieldEntry>> pageMap) {
		for (Map.Entry<Field, Page<FacetFieldEntry>> entry : pageMap.entrySet()) {
			addFacetResultPage(entry.getValue(), entry.getKey());
		}
	}

	/**
	 * @param pageMap
	 * @since 1.5
	 */
	public void addAllRangeFacetFieldResultPages(Map<Field, Page<FacetFieldEntry>> pageMap) {
		for (Map.Entry<Field, Page<FacetFieldEntry>> entry : pageMap.entrySet()) {
			addRangeFacetResultPage(entry.getValue(), entry.getKey());
		}
	}

	public void addAllFacetPivotFieldResult(Map<PivotField, List<FacetPivotFieldEntry>> resultMap) {
		for (Map.Entry<PivotField, List<FacetPivotFieldEntry>> entry : resultMap.entrySet()) {
			addFacetPivotResultPage(entry.getValue(), entry.getKey());
		}
	}

	@Override
	public Collection<Page<FacetFieldEntry>> getFacetResultPages() {
		return Collections.unmodifiableCollection(this.facetResultPages.values());
	}

	public final void setFacetQueryResultPage(List<FacetQueryEntry> facetQueryResult) {
		this.facetQueryResult = new PageImpl<>(facetQueryResult);
	}

	@Override
	public Page<FacetQueryEntry> getFacetQueryResult() {
		return this.facetQueryResult != null ? this.facetQueryResult
				: new PageImpl<>(Collections.<FacetQueryEntry> emptyList());
	}

	@Override
	public Collection<Field> getFacetFields() {
		if (this.facetResultPages.isEmpty()) {
			return Collections.emptyList();
		}
		List<Field> fields = new ArrayList<>(this.facetResultPages.size());
		for (PageKey pageKey : this.facetResultPages.keySet()) {
			fields.add(new SimpleField(pageKey.getKey().toString()));
		}
		return fields;
	}

	@Override
	public Collection<PivotField> getFacetPivotFields() {
		if (this.facetPivotResultPages.isEmpty()) {
			return Collections.emptyList();
		}
		List<PivotField> fields = new ArrayList<>(this.facetPivotResultPages.size());

		for (PageKey pageKey : this.facetPivotResultPages.keySet()) {
			fields.add(new SimplePivotField(pageKey.getKey().toString()));
		}

		return fields;
	}

	@Override
	public Collection<Page<? extends FacetEntry>> getAllFacets() {
		List<Page<? extends FacetEntry>> entries = new ArrayList<>(this.facetResultPages.size() + 1);
		entries.addAll(this.facetResultPages.values());
		entries.add(this.facetQueryResult);
		return entries;
	}

	@Override
	public List<HighlightEntry<T>> getHighlighted() {
		return this.highlighted;
	}

	public void setHighlighted(List<HighlightEntry<T>> highlighted) {
		this.highlighted = highlighted;
	}

	@Override
	public List<Highlight> getHighlights(T entity) {
		if (entity != null) {
			for (HighlightEntry<T> highlightEntry : this.highlighted) {
				if (highlightEntry != null && ObjectUtils.nullSafeEquals(highlightEntry.getEntity(), entity)) {
					return highlightEntry.getHighlights();
				}
			}
		}
		return Collections.emptyList();
	}

	/**
	 * @param groupResults
	 * @since 1.4
	 */
	public void setGroupResults(Map<Object, GroupResult<T>> groupResults) {
		this.groupResults = groupResults;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.ScoredPage#getMaxScore()
	 */
	@Nullable
	@Override
	public Float getMaxScore() {
		return maxScore;
	}

	@Override
	public GroupResult<T> getGroupResult(Field field) {
		Assert.notNull(field, "group result field must not be null");
		return groupResults.get(field.getName());
	}

	@Override
	public GroupResult<T> getGroupResult(Function function) {
		Assert.notNull(function, "group result function must not be null");
		return groupResults.get(function);
	}

	@Override
	public GroupResult<T> getGroupResult(Query query) {
		Assert.notNull(query, "group result query must not be null");
		return groupResults.get(query);
	}

	@Override
	public GroupResult<T> getGroupResult(String name) {
		Assert.notNull(name, "group result name must not be null");
		return groupResults.get(name);
	}

	/**
	 * @since 1.4
	 */
	public void setFieldStatsResults(Map<String, FieldStatsResult> fieldStatsResults) {
		this.fieldStatsResults = fieldStatsResults;
	}

	@Override
	public FieldStatsResult getFieldStatsResult(Field field) {
		return getFieldStatsResult(field.getName());
	}

	@Override
	public FieldStatsResult getFieldStatsResult(String fieldName) {
		return this.fieldStatsResults.get(fieldName);
	}

	@Override
	public Map<String, FieldStatsResult> getFieldStatsResults() {
		return this.fieldStatsResults;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.SpellcheckQueryResult#getSuggestions(java.lang.String)
	 */
	@Override
	public Collection<String> getSuggestions(String term) {

		List<String> suggestions = new ArrayList<>();
		for (Alternative alternative : getAlternatives(term)) {
			suggestions.add(alternative.getSuggestion());
		}
		return suggestions;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.SpellcheckQueryResult#getSuggestions()
	 */
	@Override
	public Collection<String> getSuggestions() {

		List<String> suggestions = new ArrayList<>();
		for (Alternative alternative : getAlternatives()) {
			suggestions.add(alternative.getSuggestion());
		}
		return suggestions;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.SpellcheckQueryResult#addSuggestions(java.lang.String, java.util.List)
	 */
	@Override
	public void addSuggestions(String term, List<Alternative> suggestions) {
		this.suggestions.put(term, suggestions);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.SpellcheckQueryResult#getAlternatives()
	 */
	@Override
	public Collection<Alternative> getAlternatives() {

		List<Alternative> allSuggestions = new ArrayList<>();
		for (List<Alternative> suggestions : this.suggestions.values()) {
			allSuggestions.addAll(suggestions);
		}
		return allSuggestions;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.SpellcheckQueryResult#getAlternatives(java.lang.String)
	 */
	@Override
	public Collection<Alternative> getAlternatives(String term) {
		return suggestions.containsKey(term) ? Collections.<Alternative> unmodifiableList(this.suggestions.get(term))
				: Collections.<Alternative> emptyList();
	}

}
