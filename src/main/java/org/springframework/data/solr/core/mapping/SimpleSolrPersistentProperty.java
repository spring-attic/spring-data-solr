/*
 * Copyright 2012 - 2014 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.util.StringUtils;

/**
 * Solr specific {@link org.springframework.data.mapping.PersistentProperty} implementation processing taking
 * {@link org.apache.solr.client.solrj.beans.Field} into account
 * 
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public class SimpleSolrPersistentProperty extends AnnotationBasedPersistentProperty<SolrPersistentProperty> implements
		SolrPersistentProperty {

	private static final String SOLRJ_FIELD_ANNOTATION_DEFAULT_VALUE = "#default";
	private static final Set<Class<?>> SUPPORTED_ID_TYPES = new HashSet<Class<?>>(3);
	private static final Set<String> SUPPORTED_ID_PROPERTY_NAMES = new HashSet<String>(1);

	static {
		SUPPORTED_ID_TYPES.add(String.class);
		SUPPORTED_ID_TYPES.add(Long.class);
		SUPPORTED_ID_TYPES.add(Integer.class);

		SUPPORTED_ID_PROPERTY_NAMES.add("id");
	}

	public SimpleSolrPersistentProperty(Field field, PropertyDescriptor propertyDescriptor,
			PersistentEntity<?, SolrPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
		super(field, propertyDescriptor, owner, simpleTypeHolder);
	}

	@Override
	public String getFieldName() {
		String fieldName = readAnnotatedFieldName();

		if (StringUtils.hasText(fieldName) && !SOLRJ_FIELD_ANNOTATION_DEFAULT_VALUE.equals(fieldName)) {
			return fieldName;
		}
		return field.getName();
	}

	private String readAnnotatedFieldName() {
		String fieldName = null;
		if (isAnnotationPresent(org.apache.solr.client.solrj.beans.Field.class)) {
			fieldName = findAnnotation(org.apache.solr.client.solrj.beans.Field.class).value();
		} else if (isAnnotationPresent(Indexed.class)) {
			fieldName = findAnnotation(Indexed.class).value();
		}
		return fieldName;
	}

	@Override
	public boolean isReadonly() {
		Indexed indexedAnnotation = getIndexAnnotation();
		if (indexedAnnotation != null && indexedAnnotation.readonly()) {
			return true;
		}
		if (indexedAnnotation == null && getFieldAnnotation() == null) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isIdProperty() {
		if (super.isIdProperty()) {
			return true;
		}

		return SUPPORTED_ID_PROPERTY_NAMES.contains(getFieldName());
	}

	@Override
	protected Association<SolrPersistentProperty> createAssociation() {
		return null;
	}

	@Override
	public boolean containsWildcard() {
		String fieldName = getFieldName();
		return fieldName != null ? (fieldName.startsWith(Criteria.WILDCARD) || fieldName.endsWith(Criteria.WILDCARD))
				: false;
	}

	private org.apache.solr.client.solrj.beans.Field getFieldAnnotation() {
		return findAnnotation(org.apache.solr.client.solrj.beans.Field.class);
	}

	private Indexed getIndexAnnotation() {
		return findAnnotation(Indexed.class);
	}

	@Override
	public boolean isBoosted() {

		Float boost = getBoost();
		return boost != null && !Float.isNaN(boost);
	}

	@Override
	public Float getBoost() {

		Float boost = Float.NaN;
		Indexed indexedAnnotation = getIndexAnnotation();
		if (indexedAnnotation != null) {
			boost = indexedAnnotation.boost();
		}
		return Float.isNaN(boost) ? null : boost;
	}

}
