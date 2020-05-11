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
 * Represents a JSON facet that operates on a single field
 * 
 * @author Joe Linn
 */
public abstract class JsonFieldFacet extends NestedJsonFacet {
	private String field;

	public JsonFieldFacet() {}

	public JsonFieldFacet(String name, String field) {
		super(name);
		this.field = field;
	}

	/**
	 * @return the name of the field on which this JSON facet will operate
	 */
	public String getField() {
		return field;
	}

	public <T extends JsonFieldFacet> T setField(String field) {
		this.field = field;
		return (T) this;
	}
}
