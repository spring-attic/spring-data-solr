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
import org.springframework.data.solr.core.query.SimpleField;

/**
 * FacetPage holds a page for each field targeted by the facet query as well as the page values returned by facet.query
 * 
 * @param <T>
 * 
 * @author Christoph Strobl
 */
public class FacetPage<T> extends PageImpl<T> {

	private static final long serialVersionUID = 9024455741261109788L;

	private Map<PageKey, Page<FacetFieldEntry>> facetResultPages = new LinkedHashMap<PageKey, Page<FacetFieldEntry>>(1);
	private Page<FacetQueryEntry> facetQueryResult;

	public FacetPage(List<T> content) {
		super(content);
	}

	public FacetPage(List<T> content, Pageable pageable, long total) {
		super(content, pageable, total);
	}

	public final Page<FacetFieldEntry> getFacetResultPage(Field field) {
		Page<FacetFieldEntry> page = facetResultPages.get(new StringPageKey(field.getName()));
		return page != null ? page : new PageImpl<FacetFieldEntry>(Collections.<FacetFieldEntry> emptyList());
	}

	public final void addFacetResultPage(Page<FacetFieldEntry> page, Field field) {
		facetResultPages.put(new StringPageKey(field.getName()), page);
	}

	public void addAllFacetFieldResultPages(Map<Field, Page<FacetFieldEntry>> pageMap) {
		for (Map.Entry<Field, Page<FacetFieldEntry>> entry : pageMap.entrySet()) {
			addFacetResultPage(entry.getValue(), entry.getKey());
		}
	}

	/**
	 * @return Collection holding faceting result pages
	 */
	public Collection<Page<FacetFieldEntry>> getFacetResultPages() {
		return Collections.unmodifiableCollection(this.facetResultPages.values());
	}

	public final void setFacetQueryResultPage(List<FacetQueryEntry> facetQueryResult) {
		this.facetQueryResult = new PageImpl<FacetQueryEntry>(facetQueryResult);
	}

	public Page<FacetQueryEntry> getFacetQueryResult() {
		return this.facetQueryResult != null ? this.facetQueryResult : new PageImpl<FacetQueryEntry>(
				Collections.<FacetQueryEntry> emptyList());
	}

	/**
	 * Get Fields contained in Result.
	 * 
	 * @return
	 */
	public Collection<Field> getFacetFields() {
		if (facetResultPages.isEmpty()) {
			return Collections.emptyList();
		}
		List<Field> fields = new ArrayList<Field>(facetResultPages.size());
		for (PageKey pageKey : this.facetResultPages.keySet()) {
			fields.add(new SimpleField(pageKey.getKey().toString()));
		}
		return fields;
	}

	public Collection<Page<? extends FacetEntry>> getAllFacets() {
		List<Page<? extends FacetEntry>> entries = new ArrayList<Page<? extends FacetEntry>>(facetResultPages.size() + 1);
		entries.addAll(facetResultPages.values());
		entries.add(this.facetQueryResult);
		return entries;
	}

}
