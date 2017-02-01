/*
 * Copyright 2012 - 2017 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * Trivial implementation of {@see QueryParameter}.
 * 
 * @author Christoph Strobl
 */
public class QueryParameterImpl implements QueryParameter {

	private final String name;
	private Object value;

	/**
	 * @param name must not be null
	 * @param value
	 */
	public QueryParameterImpl(String name, Object value) {
		super();
		Assert.notNull(name, "Name must not be null!");

		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Object getValue() {
		return this.value;
	}

	@Override
	public int hashCode() {
		return ((this.name == null) ? 0 : this.name.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		QueryParameterImpl other = (QueryParameterImpl) obj;
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		return true;
	}

}
