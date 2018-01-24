/*
 * Copyright 2012 - 2015 the original author or authors.
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

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public interface SolrPersistentProperty extends PersistentProperty<SolrPersistentProperty> {

	/**
	 * Get name of field under attention to {@link org.apache.solr.client.solrj.beans.Field} annotation
	 * 
	 * @return
	 */
	String getFieldName();

	/**
	 * @return true if {@link org.apache.solr.client.solrj.beans.Field} is present and not marked
	 *         {@link org.springframework.data.solr.core.mapping.Indexed#readonly()} = {@code true}
	 */
	boolean isReadonly();

	/**
	 * @return true if {@link org.apache.solr.client.solrj.beans.Field#value()} contains {@code *}
	 */
	boolean containsWildcard();

	/**
	 * @return true if property is boosted
	 */
	boolean isBoosted();

	/**
	 * @return property boost value if {@link #isBoosted()}, null otherwise
	 */
	Float getBoost();

	/**
	 * @return true if property shall be indexed in solr.
	 * @since 1.3
	 */
	boolean isSearchable();

	/**
	 * @return true if property shall be stored and returned in result documents.
	 * @since 1.3
	 */
	boolean isStored();

	/**
	 * @see #isCollectionLike()
	 * @return true if property is collection like
	 * @since 1.3
	 */
	boolean isMultiValued();

	/**
	 * @return mapped solr type name
	 * @since 1.3
	 */
	String getSolrTypeName();

	/**
	 * @since 1.3
	 */
	Object getDefaultValue();

	/**
	 * @return list of fields the current fields value shall be copied to
	 * @since 1.3
	 */
	Collection<String> getCopyFields();

	/**
	 * @return
	 * @since 1.3
	 */
	boolean isUnique();

	/**
	 * @return
	 * @since 1.3
	 */
	boolean isRequired();

	/**
	 * Returns whether the property is a <em>potential</em> score property of the owning {@link PersistentEntity}. This
	 * method is mainly used by {@link PersistentEntity} implementation to discover score property candidates on
	 * {@link PersistentEntity} creation you should rather call
	 * {@link PersistentEntity#isScoreProperty(PersistentProperty)} to determine whether the current property is the score
	 * property of that {@link PersistentEntity} under consideration.
	 * 
	 * @return
	 * @since 1.4
	 */
	boolean isScoreProperty();

	/**
	 * Returns whether the property should be handled as dynamic property.
	 * 
	 * @return
	 * @see {@link org.springframework.data.solr.core.mapping.Dynamic}
	 * @since 1.5
	 */
	boolean isDynamicProperty();

	public enum PropertyToFieldNameConverter implements Converter<SolrPersistentProperty, String> {

		INSTANCE;

		public String convert(SolrPersistentProperty source) {
			return source.getFieldName();
		}
	}

}
