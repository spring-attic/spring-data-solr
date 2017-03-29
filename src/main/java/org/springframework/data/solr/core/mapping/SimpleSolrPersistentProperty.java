/*
 * Copyright 2012 - 2017 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
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
public class SimpleSolrPersistentProperty extends AnnotationBasedPersistentProperty<SolrPersistentProperty>
		implements SolrPersistentProperty {

	private static final String SOLRJ_FIELD_ANNOTATION_DEFAULT_VALUE = "#default";
	private static final Set<Class<?>> SUPPORTED_ID_TYPES = new HashSet<>(3);
	private static final Set<String> SUPPORTED_ID_PROPERTY_NAMES = new HashSet<>(1);

	static {
		SUPPORTED_ID_TYPES.add(String.class);
		SUPPORTED_ID_TYPES.add(Long.class);
		SUPPORTED_ID_TYPES.add(Integer.class);

		SUPPORTED_ID_PROPERTY_NAMES.add("id");
	}

	public SimpleSolrPersistentProperty(Property property, PersistentEntity<?, SolrPersistentProperty> owner,
			SimpleTypeHolder simpleTypeHolder) {
		super(property, owner, simpleTypeHolder);
	}

	@Override
	public String getFieldName() {
		String fieldName = readAnnotatedFieldName();

		if (StringUtils.hasText(fieldName) && !SOLRJ_FIELD_ANNOTATION_DEFAULT_VALUE.equals(fieldName)) {
			return fieldName;
		}
		return getName();
	}

	private String readAnnotatedFieldName() {
		String fieldName = null;
		if (isAnnotationPresent(org.apache.solr.client.solrj.beans.Field.class)) {
			fieldName = findAnnotation(org.apache.solr.client.solrj.beans.Field.class).get().value();
		} else if (isAnnotationPresent(Indexed.class)) {
			Optional<Indexed> indexedAnnotation = findAnnotation(Indexed.class);

			if (indexedAnnotation.isPresent()) {
				fieldName = indexedAnnotation.get().value();
				if (!StringUtils.hasText(fieldName)) {
					fieldName = indexedAnnotation.get().name();
				}
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

		Optional<Indexed> indexedAnnotation = getIndexAnnotation();
		if (indexedAnnotation.isPresent() && indexedAnnotation.get().readonly()) {
			return true;
		}
		if (!indexedAnnotation.isPresent() && !getFieldAnnotation().isPresent()) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#isIdProperty()
	 */
	@Override
	public boolean isIdProperty() {
		return super.isIdProperty() || SUPPORTED_ID_PROPERTY_NAMES.contains(getFieldName());
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

	private Optional<org.apache.solr.client.solrj.beans.Field> getFieldAnnotation() {
		return findAnnotation(org.apache.solr.client.solrj.beans.Field.class);
	}

	private Optional<Indexed> getIndexAnnotation() {
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
		Optional<Indexed> indexedAnnotation = getIndexAnnotation();
		if (indexedAnnotation.isPresent()) {
			boost = indexedAnnotation.get().boost();
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

		Optional<Indexed> indexedAnnotation = getIndexAnnotation();
		return indexedAnnotation.isPresent() && indexedAnnotation.get().searchable();
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

		Optional<Indexed> indexedAnnotation = getIndexAnnotation();
		return indexedAnnotation.isPresent() && indexedAnnotation.get().stored();
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

		Optional<Indexed> indexedAnnotation = getIndexAnnotation();
		if (indexedAnnotation.isPresent()) {
			if (StringUtils.hasText(indexedAnnotation.get().type())) {
				return indexedAnnotation.get().type();
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

		Optional<Indexed> indexedAnnotation = getIndexAnnotation();
		if (indexedAnnotation.isPresent()) {
			if (StringUtils.hasText(indexedAnnotation.get().defaultValue())) {
				return indexedAnnotation.get().defaultValue();
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

		Optional<Indexed> indexedAnnotation = getIndexAnnotation();
		if (indexedAnnotation.isPresent()) {
			if (indexedAnnotation.get().copyTo().length > 0) {
				return CollectionUtils.arrayToList(indexedAnnotation.get().copyTo());
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

		Optional<Indexed> indexedAnnotation = getIndexAnnotation();
		return indexedAnnotation.isPresent() && indexedAnnotation.get().required();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#isScoreProperty()
	 */
	@Override
	public boolean isScoreProperty() {
		return findAnnotation(Score.class).isPresent();
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentProperty#isDynamicProperty()
	 */
	@Override
	public boolean isDynamicProperty() {
		return findAnnotation(Dynamic.class).isPresent();
	}

}
