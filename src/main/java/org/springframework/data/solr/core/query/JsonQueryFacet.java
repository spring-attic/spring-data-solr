/*
 * Copyright 2012 - 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.query;

/**
 * Represents a JSON <a href="https://lucene.apache.org/solr/guide/8_5/json-facet-api.html#query-facet">query facet</a>.
 * 
 * @author Joe Linn
 */
public class JsonQueryFacet extends NestedJsonFacet {
	private Criteria query;

	public JsonQueryFacet() {}

	public JsonQueryFacet(String name, Criteria query) {
		super(name);
		this.query = query;
	}

	public Criteria getQuery() {
		return query;
	}

	public JsonQueryFacet setQuery(Criteria query) {
		this.query = query;
		return this;
	}

	@Override
	public String getType() {
		return "query";
	}
}
