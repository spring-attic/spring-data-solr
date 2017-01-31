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
package org.springframework.data.solr.repository.query;

/**
 * Used to provide additional information on parameters used in query definition. This allows to access per parameter
 * metadata such as Boost values.
 *
 * @author Christoph Strobl
 */
public class BindableSolrParameter {

	private final int index;
	private final Object value;
	private float boost;

	public BindableSolrParameter(int index, Object value) {
		super();
		this.index = index;
		this.value = value;
	}

	public float getBoost() {
		return boost;
	}

	public void setBoost(float boost) {
		this.boost = boost;
	}

	public int getIndex() {
		return index;
	}

	public Object getValue() {
		return value;
	}

}
