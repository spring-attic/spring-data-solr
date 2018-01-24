/*
 * Copyright 2012 - 2016 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.repository.Score;
import org.springframework.util.CollectionUtils;
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
		return this.name;
	}

	private String readAnnotatedFieldName() {
		String fieldName = null;
		if (isAnnotationPresent(org.apache.solr.client.solrj.beans.Field.class)) {
			fieldName = findAnnotation(org.apache.solr.client.solrj.beans.Field.class).value();
		} else if (isAnnotationPresent(Indexed.class)) {
			Indexed indexedAnnotation = findAnnotation(Indexed.class);
			fieldName = indexedAnnotation.value();
			if (!StringUtils.hasText(fieldName)) {
				fieldName = indexedAnnotation.name();
			}
		}
		return fieldName;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#isReadonly()
	 */
	@Override
	public boolean isReadonly() {

		if (isIdProperty() || isVersionProperty()) {
			return false;
		}

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
	public boolean isAssociation() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#isIdProperty()
	 */
	@Override
	public boolean isIdProperty() {
		if (super.isIdProperty()) {
			return true;
		}

		return SUPPORTED_ID_PROPERTY_NAMES.contains(getFieldName());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AbstractPersistentProperty#createAssociation()
	 */
	@Override
	protected Association<SolrPersistentProperty> createAssociation() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#containsWildcard()
	 */
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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#isBoosted()
	 */
	@Override
	public boolean isBoosted() {

		Float boost = getBoost();
		return boost != null && !Float.isNaN(boost);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#getBoost()
	 */
	@Override
	public Float getBoost() {

		Float boost = Float.NaN;
		Indexed indexedAnnotation = getIndexAnnotation();
		if (indexedAnnotation != null) {
			boost = indexedAnnotation.boost();
		}
		return Float.isNaN(boost) ? null : boost;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#isSearchable()
	 */
	@Override
	public boolean isSearchable() {

		if (isIdProperty()) {
			return true;
		}

		Indexed indexedAnnotation = getIndexAnnotation();
		return indexedAnnotation != null && indexedAnnotation.searchable();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#isStored()
	 */
	@Override
	public boolean isStored() {

		if (isIdProperty()) {
			return true;
		}

		Indexed indexedAnnotation = getIndexAnnotation();
		return indexedAnnotation != null && indexedAnnotation.stored();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#isMultiValued()
	 */
	@Override
	public boolean isMultiValued() {
		return isCollectionLike();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#getSolrTypeName()
	 */
	@Override
	public String getSolrTypeName() {
		Indexed indexedAnnotation = getIndexAnnotation();
		if (indexedAnnotation != null) {
			if (StringUtils.hasText(indexedAnnotation.type())) {
				return indexedAnnotation.type();
			}
		}
		return getActualType().getSimpleName().toLowerCase();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#getDefaultValue()
	 */
	@Override
	public Object getDefaultValue() {

		Indexed indexedAnnotation = getIndexAnnotation();
		if (indexedAnnotation != null) {
			if (StringUtils.hasText(indexedAnnotation.defaultValue())) {
				return indexedAnnotation.defaultValue();
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#getCopyFields()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Collection<String> getCopyFields() {
		Indexed indexedAnnotation = getIndexAnnotation();
		if (indexedAnnotation != null) {
			if (indexedAnnotation.copyTo().length > 0) {
				return CollectionUtils.arrayToList(indexedAnnotation.copyTo());
			}
		}
		return Collections.emptyList();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#isUnique()
	 */
	@Override
	public boolean isUnique() {
		return isIdProperty();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#isRequired()
	 */
	@Override
	public boolean isRequired() {

		Indexed indexedAnnotation = getIndexAnnotation();
		return indexedAnnotation != null && indexedAnnotation.required();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#isScoreProperty()
	 */
	@Override
	public boolean isScoreProperty() {
		return findAnnotation(Score.class) != null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#isDynamicProperty()
	 */
	@Override
	public boolean isDynamicProperty() {
		return findAnnotation(Dynamic.class) != null;
	}

}
