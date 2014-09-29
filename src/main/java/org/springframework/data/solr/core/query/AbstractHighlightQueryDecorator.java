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
package org.springframework.data.solr.core.query;

/**
 * General purpose {@link FacetQuery} decorator.
 * 
 * @author Francisco Spaeth
 *
 * @since 1.4
 * 
 */
public class AbstractHighlightQueryDecorator extends AbstractQueryDecorator implements HighlightQuery {

	private HighlightQuery query;

	public AbstractHighlightQueryDecorator(HighlightQuery query) {
		super(query);
		this.query = query;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends SolrDataQuery> T setHighlightOptions(HighlightOptions highlightOptions) {
		return (T) this;
	}

	@Override
	public HighlightOptions getHighlightOptions() {
		return query.getHighlightOptions();
	}

	@Override
	public boolean hasHighlightOptions() {
		return query.hasHighlightOptions();
	}

}
