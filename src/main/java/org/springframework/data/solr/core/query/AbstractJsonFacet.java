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
 * @author Joe Linn
 */
public abstract class AbstractJsonFacet implements JsonFacet {
	private String name;

	public AbstractJsonFacet() {}

	public AbstractJsonFacet(String name) {
		this.name = name;
	}

	/**
	 * @return the type of JSON facet represented by this class. This string will be passed to Solr at request time.
	 */
	public abstract String getType();

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of this JSON facet.
	 * 
	 * @param name facet name. Should be unique within the current request and nesting level.
	 * @return
	 */
	public <T extends AbstractJsonFacet> T setName(String name) {
		this.name = name;
		return (T) this;
	}
}
