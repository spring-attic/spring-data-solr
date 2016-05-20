/*
 * Copyright 2016 the original author or authors.
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

/**
 * General purpose {@link FacetAndHighlightQuery} decorator.
 *
 * @author David Webb
 * @since 2.1.0
 */
public class AbstractFacetAndHighlightQueryDecorator extends AbstractQueryDecorator
		implements FacetQuery, HighlightQuery {

	private FacetAndHighlightQuery query;

	public AbstractFacetAndHighlightQueryDecorator(FacetAndHighlightQuery query) {

		super(query);
		this.query = query;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.HighlightQuery#setHighlightOptions(org.springframework.data.solr.core.query.HighlightOptions)
	 */
	@Override
	public <T extends SolrDataQuery> T setHighlightOptions(HighlightOptions highlightOptions) {
		return query.setHighlightOptions(highlightOptions);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.HighlightQuery#getHighlightOptions()
	 */
	@Override
	public HighlightOptions getHighlightOptions() {
		return query.getHighlightOptions();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.HighlightQuery#hasHighlightOptions()
	 */
	@Override
	public boolean hasHighlightOptions() {
		return query.hasHighlightOptions();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.FacetQuery#setFacetOptions(org.springframework.data.solr.core.query.FacetOptions)
	 */
	@Override
	public <T extends SolrDataQuery> T setFacetOptions(FacetOptions facetOptions) {
		return query.setFacetOptions(facetOptions);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.FacetQuery#getFacetOptions()
	 */
	@Override
	public FacetOptions getFacetOptions() {
		return query.getFacetOptions();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.FacetQuery#hasFacetOptions()
	 */
	@Override
	public boolean hasFacetOptions() {
		return query.hasFacetOptions();
	}
}
