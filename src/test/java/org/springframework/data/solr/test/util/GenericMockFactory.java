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
package org.springframework.data.solr.test.util;

import org.mockito.Mockito;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author Christoph Strobl
 */
public class GenericMockFactory<T> implements FactoryBean<T> {

	private Class<T> type;

	@Override
	public T getObject() throws Exception {
		return Mockito.mock(type);
	}

	@Override
	public Class<?> getObjectType() {
		return type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public void setType(Class<T> type) {
		this.type = type;
	}

}
