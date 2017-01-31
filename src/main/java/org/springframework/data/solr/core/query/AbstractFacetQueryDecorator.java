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
package org.springframework.data.solr.core.query;

/**
 * General purpose {@link FacetQuery} decorator.
 *
 * @author Francisco Spaeth
 * @since 1.4
 */
public abstract class AbstractFacetQueryDecorator extends AbstractQueryDecorator implements FacetQuery {

	private FacetQuery query;

	public AbstractFacetQueryDecorator(FacetQuery query) {
		super(query);
		this.query = query;
	}

	@Override
	public <T extends SolrDataQuery> T setFacetOptions(FacetOptions facetOptions) {
		return query.setFacetOptions(facetOptions);
	}

	@Override
	public FacetOptions getFacetOptions() {
		return query.getFacetOptions();
	}

	@Override
	public boolean hasFacetOptions() {
		return query.hasFacetOptions();
	}

}
