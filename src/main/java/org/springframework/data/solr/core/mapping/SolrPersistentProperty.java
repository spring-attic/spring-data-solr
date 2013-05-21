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
package org.springframework.data.solr.core.mapping;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentProperty;

/**
 * @author Christoph Strobl
 */
public interface SolrPersistentProperty extends PersistentProperty<SolrPersistentProperty> {

	/**
	 * Get name of field under attention to {@link org.apache.solr.client.solrj.beans.Field} annotation
	 * 
	 * @return
	 */
	String getFieldName();

	public enum PropertyToFieldNameConverter implements Converter<SolrPersistentProperty, String> {

		INSTANCE;

		public String convert(SolrPersistentProperty source) {
			return source.getFieldName();
		}
	}
}
