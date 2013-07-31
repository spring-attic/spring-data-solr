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

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameters;

/**
 * @author Christoph Strobl
 */
public class SolrParameters extends Parameters<SolrParameters, SolrParameter> {

	public SolrParameters(Method method) {
		super(method);
	}

	public SolrParameters(List<SolrParameter> parameters) {
		super(parameters);
	}

	@Override
	protected SolrParameter createParameter(MethodParameter parameter) {
		return new SolrParameter(parameter);
	}

	@Override
	protected SolrParameters createFrom(List<SolrParameter> parameters) {
		return new SolrParameters(parameters);
	}

}
