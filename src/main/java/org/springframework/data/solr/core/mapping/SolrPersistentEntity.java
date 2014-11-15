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

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;

/**
 * @param <T>
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public interface SolrPersistentEntity<T> extends PersistentEntity<T, SolrPersistentProperty> {

	/**
	 * Get the core's name for this entity.
	 * 
	 * @return
	 */
	String getSolrCoreName();

	/**
	 * @return true if this entity is boosted
	 */
	boolean isBoosted();

	/**
	 * @return entity's boost value if {@link #isBoosted()}, null otherwise
	 */
	Float getBoost();

	/**
	 * Returns whether the {@link SolrPersistentEntity} has an score property. If this call returns {@literal true},
	 * {@link #getScoreProperty()} will return a non-{@literal null} value.
	 * 
	 * @return
	 * @since 1.4
	 */
	boolean hasScoreProperty();

	/**
	 * Returns the score property of the {@link SolrPersistentEntity}. Can be {@literal null} in case no score property is
	 * available on the entity.
	 * 
	 * @return the score property of the {@link PersistentEntity}.
	 * @since 1.4
	 */
	SolrPersistentProperty getScoreProperty();

	/**
	 * Returns whether the given {@link PersistentProperty} is the score property of the entity.
	 * 
	 * @param property
	 * @return
	 * @since 1.4
	 */
	boolean isScoreProperty(SolrPersistentProperty property);

}
