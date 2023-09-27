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
package org.springframework.data.solr.core.query.result;

import java.util.Map;

/**
 * Represents a single bucket in a multi-bucketed (terms, range) JSON facet result.
 * 
 * @author Joe Linn
 */
public class BucketFacetEntry extends ValueCountEntry implements NestableFacetEntry {
	private Map<String, JsonFacetResult> facets;
	private Object value;

	public BucketFacetEntry(Object value, long valueCount) {
		super(value.toString(), valueCount);
		this.value = value;
	}

	public BucketFacetEntry(Object value, long valueCount, Map<String, JsonFacetResult> facets) {
		this(value, valueCount);
		this.facets = facets;
	}

	@Override
	public Map<String, JsonFacetResult> getFacets() {
		return facets;
	}

	@Override
	public Object getKey() {
		return value;
	}

	/**
	 * Attempts to return the {@link #value} of this bucket as a number if possible.
	 * 
	 * @return this bucket's value in number form
	 */
	public Number getValueAsNumber() {
		if (value instanceof Number) {
			return (Number) value;
		} else {
			return Double.parseDouble((String) value);
		}
	}
}
