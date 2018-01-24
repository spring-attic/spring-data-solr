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
import java.util.Iterator;

import org.springframework.util.Assert;

/**
 * Field that holds additional parameters to provide query hints to solr.
 * 
 * @author Christoph Strobl
 * 
 * @param <T>
 */
public class FieldWithQueryParameters<T extends QueryParameter> extends SimpleField implements Iterable<T> {

	private final ParameterHolder<T> parameterHolder = new ParameterHolder<T>();

	/**
	 * @param fieldname must not be null
	 */
	public FieldWithQueryParameters(String fieldname) {
		super(fieldname);
		Assert.hasText(fieldname, "Fieldname must not be null nor empty!");
	}

	/**
	 * Get the parameter for given name.
	 * 
	 * @param parameterName
	 * @return null if parameter does not exist
	 */
	public T getQueryParameter(String parameterName) {
		return this.parameterHolder.get(parameterName);
	}

	/**
	 * @param parameter must not be null
	 */
	public void addQueryParameter(T parameter) {
		Assert.notNull(parameter, "Parameter must not be null!");
		this.parameterHolder.add(parameter);
	}

	/**
	 * remove parameter with given name
	 * 
	 * @param parameterName
	 */
	public T removeQueryParameter(String parameterName) {
		return this.parameterHolder.remove(parameterName);
	}

	/**
	 * Get Collection of all parameters
	 * 
	 * @return
	 */
	public Collection<T> getQueryParameters() {
		return this.parameterHolder.getParameters();
	}

	/**
	 * @return true if parameters present
	 */
	public boolean hasQueryParameters() {
		return !this.parameterHolder.isEmpty();
	}

	/**
	 * Get value of given parameter
	 * 
	 * @param parameterName
	 * @return null if no parameter with given name present
	 */
	public <S> S getQueryParameterValue(String parameterName) {
		return this.parameterHolder.getParameterValue(parameterName);
	}

	@Override
	public Iterator<T> iterator() {
		return this.parameterHolder.iterator();
	}

}
