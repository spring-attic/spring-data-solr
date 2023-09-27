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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON <a href="https://lucene.apache.org/solr/guide/8_5/json-facet-api.html#stat-facet-functions">stats
 * facet</a>.
 * 
 * @author Joe Linn
 */
public class JsonStatFacet extends AbstractJsonFacet {
	@JsonProperty("func") private StatFacetFunction function;
	private Map<String, Object> params = new HashMap<>();

	public JsonStatFacet() {}

	public JsonStatFacet(String name, StatFacetFunction function) {
		super(name);
		this.function = function;
	}

	@Override
	public String getType() {
		return "func";
	}

	public StatFacetFunction getFunction() {
		return function;
	}

	public JsonStatFacet setFunction(StatFacetFunction function) {
		this.function = function;
		return this;
	}

	@JsonAnyGetter
	public Map<String, Object> getParams() {
		return params;
	}

	public JsonStatFacet setParams(Map<String, Object> params) {
		this.params = params;
		return this;
	}

	public JsonStatFacet addParam(String name, Object value) {
		this.params.put(name, value);
		return this;
	}
}
