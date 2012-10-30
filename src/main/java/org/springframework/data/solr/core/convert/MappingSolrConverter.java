/*
 * Copyright 2012 the original author or authors.
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

import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.server.SolrServerFactory;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 */
public class MappingSolrConverter implements SolrConverter, ApplicationContextAware, InitializingBean {

	private final MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext;
	private final GenericConversionService conversionService;

	@SuppressWarnings("unused")
	private ApplicationContext applicationContext;
	private SolrServerFactory solrServerFactory;

	public MappingSolrConverter(SolrServerFactory solrServerFactory,
			MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext) {
		Assert.notNull(solrServerFactory);
		Assert.notNull(mappingContext);

		this.solrServerFactory = solrServerFactory;
		this.mappingContext = mappingContext;
		conversionService = new DefaultConversionService();
	}

	@Override
	public MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> getMappingContext() {
		return mappingContext;
	}

	@Override
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	@Override
	public <R> R read(Class<R> type, Map<String, ?> source) {
		return read(ClassTypeInformation.from(type), source);
	}

	protected <S extends Object> S read(TypeInformation<S> targetTypeInformation, Map<String, ?> source) {
		Class<S> rawType = targetTypeInformation.getType();

		if (!conversionService.canConvert(SolrDocument.class, rawType)) {
			initializeTypedConverter(source, rawType);
		}

		return conversionService.convert(source, rawType);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void write(Object source, @SuppressWarnings("rawtypes") Map target) {
		if (source == null) {
			return;
		}

		SolrInputDocument convertedDocument = conversionService.convert(source, SolrInputDocument.class);
		target.putAll(convertedDocument);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void afterPropertiesSet() {
		initializeConverters();
	}

	private void initializeConverters() {
		if (!conversionService.canConvert(Object.class, SolrInputDocument.class)) {
			conversionService.addConverter(new SolrjConverters.ObjectToSolrInputDocumentConverter(solrServerFactory
					.getSolrServer().getBinder()));
		}
	}

	@SuppressWarnings("unchecked")
	private <S> void initializeTypedConverter(Map<String, ?> source, Class<? extends S> rawType) {
		conversionService.addConverter(source.getClass(), rawType,
				new SolrjConverters.SolrInputDocumentToObjectConverter<S>((Class<S>) rawType));
	}

}
