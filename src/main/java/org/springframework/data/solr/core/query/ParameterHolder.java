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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Generic holder of additional parameters that can be added to a query. eg. per field overrides. <br />
 * The order in which elements are added will be preserved.
 * 
 * @author Christoph Strobl
 */
class ParameterHolder<T extends QueryParameter> implements Iterable<T> {

	private final Map<String, T> parameters = new LinkedHashMap<String, T>(1);

	@SuppressWarnings("unchecked")
	public <S> S getParameterValue(String parameterName) {
		T parameter = this.parameters.get(parameterName);
		if (parameter == null) {
			return null;
		}
		return (S) parameter.getValue();
	}

	/**
	 * add a query parameter
	 * 
	 * @param queryParameter must not be null
	 */
	public void add(T queryParameter) {
		Assert.notNull(queryParameter, "QueryParameter must not be null!");
		this.parameters.put(queryParameter.getName(), queryParameter);
	}

	/**
	 * @param parameterName
	 * @return null if not found
	 */
	public T get(String parameterName) {
		return this.parameters.get(parameterName);
	}

	/**
	 * remove parameter with given name
	 * 
	 * @param parameterName
	 */
	public T remove(String parameterName) {
		return this.parameters.remove(parameterName);
	}

	/**
	 * @return unmodifiable collection of all parameters present
	 */
	public Collection<T> getParameters() {
		return Collections.unmodifiableCollection(this.parameters.values());
	}

	@Override
	public Iterator<T> iterator() {
		return this.parameters.values().iterator();
	}

	/**
	 * @return true if holder does not contain any values
	 */
	public boolean isEmpty() {
		return this.parameters.isEmpty();
	}

}
