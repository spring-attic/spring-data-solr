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
package org.springframework.data.solr.repository.query;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.solr.repository.Boost;

/**
 * Solr specific {@link Parameter} implementation
 * 
 * @author Christoph Strobl
 */
class SolrParameter extends Parameter {

	private final MethodParameter parameter;

	protected SolrParameter(MethodParameter parameter) {
		super(parameter);
		this.parameter = parameter;
	}

	private Boost getBoostAnnotation() {
		return parameter.getParameterAnnotation(Boost.class);
	}

	private boolean hasBoostAnnotation() {
		return getBoostAnnotation() != null;
	}

	/**
	 * if method parameter has {@link Boost} use it
	 * 
	 * @return Float.NaN by default
	 */
	public float getBoost() {
		if (hasBoostAnnotation()) {
			return getBoostAnnotation().value();
		}
		return Float.NaN;
	}
}
