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
package org.springframework.data.solr.core.convert;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.data.convert.EntityConverter;
import org.springframework.data.convert.EntityReader;
import org.springframework.data.convert.EntityWriter;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;

/**
 * @author Christoph Strobl
 */
public interface SolrConverter extends

EntityConverter<SolrPersistentEntity<?>, SolrPersistentProperty, Object, Map<String, ?>>,
		EntityWriter<Object, Map<String, ?>>, EntityReader<Object, Map<String, ?>> {

	/**
	 * Read {@link SolrDocumentList} and convert to {@link List} of given type
	 * 
	 * @param source
	 * @param type
	 * @return empty list if {@code source == null || source.isEmpty()}
	 */
	<S, R> List<R> read(SolrDocumentList source, Class<R> type);

	/**
	 * Write values to {@link List} of {@link SolrInputDocument}
	 * 
	 * @param values
	 * @return empty list if values is {@code null}-
	 */
	Iterable<SolrInputDocument> write(Iterable<?> values);

}
