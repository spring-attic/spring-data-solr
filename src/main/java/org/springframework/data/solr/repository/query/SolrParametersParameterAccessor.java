/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.Iterator;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParametersParameterAccessor;

/**
 * Implementation of {@link SolrParameterAccessor}
 * 
 * @author Christoph Strobl
 */
public class SolrParametersParameterAccessor implements SolrParameterAccessor {

	private final SolrParameters parameters;
	private final ParametersParameterAccessor parametersParameterAccessorDelegate;

	public SolrParametersParameterAccessor(SolrQueryMethod solrQueryMethod, Object[] values) {
		this.parameters = solrQueryMethod.getParameters();
		this.parametersParameterAccessorDelegate = new ParametersParameterAccessor(this.parameters, values.clone());
	}

	@Override
	public float getBoost(int index) {
		return parameters.getBindableParameter(index).getBoost();
	}

	@Override
	public Pageable getPageable() {
		return parametersParameterAccessorDelegate.getPageable();
	}

	@Override
	public Sort getSort() {
		return parametersParameterAccessorDelegate.getSort();
	}

	@Override
	public Object getBindableValue(int index) {
		return parametersParameterAccessorDelegate.getBindableValue(index);
	}

	@Override
	public boolean hasBindableNullValue() {
		return parametersParameterAccessorDelegate.hasBindableNullValue();
	}

	@Override
	public Iterator<Object> iterator() {
		return new BindableSolrParameterIterator(parametersParameterAccessorDelegate.iterator());
	}

	@Override
	public Class<?> getDynamicProjection() {
		return parametersParameterAccessorDelegate.getDynamicProjection();
	}

	public class BindableSolrParameterIterator implements Iterator<Object> {

		private final Iterator<Object> delegate;
		private int currentIndex = 0;

		public BindableSolrParameterIterator(Iterator<Object> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean hasNext() {
			return delegate.hasNext();
		}

		@Override
		public BindableSolrParameter next() {
			BindableSolrParameter solrParameter = new BindableSolrParameter(currentIndex, delegate.next());
			solrParameter.setBoost(parameters.getBindableParameter(currentIndex).getBoost());
			currentIndex++;
			return solrParameter;
		}

		@Override
		public void remove() {
			delegate.remove();
		}

	}

}
