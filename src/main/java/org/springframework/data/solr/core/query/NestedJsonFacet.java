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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON facet which can contain nested facets.
 * 
 * @author Joe Linn
 */
public abstract class NestedJsonFacet extends AbstractJsonFacet {
	@JsonProperty("facet") private Map<String, JsonFacet> facets = new HashMap<>();

	public NestedJsonFacet() {}

	public NestedJsonFacet(String name) {
		super(name);
	}

	public Map<String, JsonFacet> getFacets() {
		return facets;
	}

	public void setFacets(Map<String, JsonFacet> facets) {
		this.facets = facets;
	}

	/**
	 * Adds a {@link JsonFacet} as a nested facet of this facet.
	 * 
	 * @param facet the facet to be added
	 * @return
	 */
	public <T extends NestedJsonFacet> T addFacet(JsonFacet facet) {
		this.facets.put(facet.getName(), facet);
		return (T) this;
	}
}
