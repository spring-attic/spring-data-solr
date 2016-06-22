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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.query.Update;

/**
 * Trivial implementation of {@link SolrConverter} delegating conversion to {@link DocumentObjectBinder}
 * 
 * @author Christoph Strobl
 */
public class SolrJConverter extends SolrConverterBase implements SolrConverter {

	private final MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext;

	public SolrJConverter() {
		this.mappingContext = new SimpleSolrMappingContext();
		initializeConverters();
	}

	@Override
	public MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> getMappingContext() {
		return mappingContext;
	}

	@Override
	public <S, R> List<R> read(SolrDocumentList source, Class<R> type) {
		if (source == null) {
			return Collections.emptyList();
		}

		List<R> resultList = new ArrayList<R>(source.size());
		for (Map<String, ?> item : source) {
			resultList.add(read(type, item));
		}

		return resultList;
	}

	@Override
	public <R> R read(Class<R> type, Map<String, ?> source) {
		if (!canConvert(SolrDocument.class, type)) {
			initializeTypedConverter(source, type);
		}
		return convert(source, type);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void write(Object source, Map sink) {
		if (source == null) {
			return;
		}

		SolrInputDocument convertedDocument = convert(source, SolrInputDocument.class);
		sink.putAll(convertedDocument);
	}

	private void initializeConverters() {
		if (!canConvert(Update.class, SolrInputDocument.class)) {
			getConversionService().addConverter(new SolrjConverters.UpdateToSolrInputDocumentConverter());
		}
		if (!canConvert(Object.class, SolrInputDocument.class)) {
			getConversionService().addConverter(
					new SolrjConverters.ObjectToSolrInputDocumentConverter(new DocumentObjectBinder()));
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <S> void initializeTypedConverter(Map<String, ?> source, Class<? extends S> rawType) {
		getConversionService().addConverter((Class) source.getClass(), (Class) rawType,
				new SolrjConverters.SolrInputDocumentToObjectConverter<S>((Class<S>) rawType));
	}

}
