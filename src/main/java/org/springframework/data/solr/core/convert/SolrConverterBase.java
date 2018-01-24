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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * @author Christoph Strobl
 */
public abstract class SolrConverterBase implements SolrConverter, InitializingBean {

	private final GenericConversionService conversionService = new DefaultConversionService();
	private CustomConversions customConversions = new CustomConversions();

	@Override
	public Collection<SolrInputDocument> write(Iterable<?> source) {
		if (source == null) {
			return Collections.emptyList();
		}

		List<SolrInputDocument> resultList = new ArrayList<SolrInputDocument>();
		for (Object bean : source) {
			if (bean instanceof SolrInputDocument) {
				resultList.add((SolrInputDocument) bean);
			} else {
				resultList.add(createAndWrite(bean));
			}
		}

		return resultList;
	}

	/**
	 * create a new {@link SolrInputDocument} for given source and write values to it
	 * 
	 * @param source
	 * @return
	 */
	protected SolrInputDocument createAndWrite(Object source) {
		SolrInputDocument document = new SolrInputDocument();
		write(source, document);
		return document;
	}

	/**
	 * @return
	 */
	public CustomConversions getCustomConversions() {
		return this.customConversions;
	}

	/**
	 * @param sourceType
	 * @param targetType
	 * @return true if custom read target defined in {@link #customConversions}
	 */
	protected boolean hasCustomReadTarget(Class<?> sourceType, Class<?> targetType) {
		return this.customConversions.hasCustomReadTarget(sourceType, targetType);
	}

	/**
	 * @param sourceType
	 * @return true if custom write target defined in {@link #customConversions}
	 */
	protected boolean hasCustomWriteTarget(Class<?> sourceType) {
		return this.customConversions.hasCustomWriteTarget(sourceType, null);
	}

	/**
	 * @param sourceType
	 * @param targetType
	 * @return true if custom write target defined in {@link #customConversions}
	 */
	protected boolean hasCustomWriteTarget(Class<?> sourceType, Class<?> targetType) {
		return this.customConversions.hasCustomWriteTarget(sourceType, targetType);
	}

	/**
	 * @param type
	 * @return true if is simple type as defined in {@link #customConversions}
	 */
	protected boolean isSimpleType(Class<?> type) {
		return customConversions.isSimpleType(type);
	}

	/**
	 * get the target conversion type
	 * 
	 * @param type
	 * @return
	 */
	protected Class<?> getCustomWriteTargetType(Class<?> type) {
		return customConversions.getCustomWriteTarget(type);
	}

	/**
	 * register {@link #customConversions} with {@link #conversionService}
	 * 
	 * @param conversionService
	 */
	protected void registerCutomConverters(GenericConversionService conversionService) {
		if (customConversions != null) {
			customConversions.registerConvertersIn(conversionService);
		}
	}

	/**
	 * @param customConversions
	 */
	public void setCustomConversions(CustomConversions customConversions) {
		this.customConversions = customConversions != null ? customConversions : new CustomConversions();
	}

	@Override
	public GenericConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * @param sourceType
	 * @param targetType
	 * @return true if sourceType can be converted into targetType
	 */
	protected boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return this.conversionService.canConvert(sourceType, targetType);
	}

	/**
	 * Convert given object into target type
	 * 
	 * @param source
	 * @param targetType
	 * @return
	 */
	protected <T> T convert(Object source, Class<T> targetType) {
		return this.conversionService.convert(source, targetType);
	}

	@Override
	public void afterPropertiesSet() {
		registerCutomConverters(this.conversionService);
	}

}
