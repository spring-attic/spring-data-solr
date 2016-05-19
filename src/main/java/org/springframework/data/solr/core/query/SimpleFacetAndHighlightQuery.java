/*
 * Copyright 2012 - 2016 the original author or authors.
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
package org.springframework.data.solr.core.query;

import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * Trivial implementation of {@link FacetAndHighlightQuery}
 * 
 * @author David Webb
 * @since 2.1.0
 */
public class SimpleFacetAndHighlightQuery extends SimpleQuery implements FacetAndHighlightQuery {

	private FacetOptions facetOptions;
	private HighlightOptions highlightOptions;

	public SimpleFacetAndHighlightQuery() {
		super();
	}

	public SimpleFacetAndHighlightQuery(Criteria criteria) {
		this(criteria, null);
	}

	public SimpleFacetAndHighlightQuery(Criteria criteria, Pageable pageable) {
		super(criteria, pageable);
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <T extends SolrDataQuery> T setFacetOptions(FacetOptions facetOptions) {
		if (facetOptions != null) {
			Assert.isTrue(facetOptions.hasFacets(), "Cannot set facet options having neither fields nor queries.");
		}
		this.facetOptions = facetOptions;
		return (T) this;
	}

	@Override
	public FacetOptions getFacetOptions() {
		return this.facetOptions;
	}

	@Override
	public boolean hasFacetOptions() {
		return this.getFacetOptions() != null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends SolrDataQuery> T setHighlightOptions(HighlightOptions highlightOptions) {
		this.highlightOptions = highlightOptions;
		return (T) this;
	}

	@Override
	public HighlightOptions getHighlightOptions() {
		return this.highlightOptions;
	}

	@Override
	public boolean hasHighlightOptions() {
		return this.highlightOptions != null;
	}

}
