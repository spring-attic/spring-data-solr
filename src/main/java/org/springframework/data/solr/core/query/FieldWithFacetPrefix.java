/*
 * Copyright 2012 - 2013 the original author or authors.
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
 * Faceted Search allows per field prefix. Use this one for adding such a field along with the prefix to use.
 * 
 * @author Christoph Strobl
 */
public class FieldWithFacetPrefix extends SimpleField {

	private String facetPrefix;

	public FieldWithFacetPrefix(String name) {
		super(name);
	}

	public FieldWithFacetPrefix(String name, String prefix) {
		this(name);
		this.facetPrefix = prefix;
	}

	/**
	 * @param facetPrefix
	 */
	public void setFacetPrefix(String facetPrefix) {
		this.facetPrefix = facetPrefix;
	}

	/**
	 * @return null if not set
	 */
	public String getFacetPrefix() {
		return facetPrefix;
	}

}
