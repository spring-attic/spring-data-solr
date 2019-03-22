/*
 * Copyright 2012 - 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.query;

import org.springframework.lang.Nullable;

/**
 * Defines a Field that can be used within {@link Criteria}.
 *
 * @author Christoph Strobl
 */
public interface Field {

	/**
	 * Get the name of the field used in {@code schema.xml} of solr server
	 *
	 * @return
	 */
	@Nullable
	String getName();

	/**
	 * Create a {@link Field} with given name.
	 *
	 * @param name must not be {@literal null}.
	 * @return new instance of {@link Field}.
	 * @since 4.1
	 */
	static Field of(String name) {
		return new SimpleField(name);
	}

	/**
	 * Create a {@link Field} for the given names.
	 *
	 * @param names must not be {@literal null}.
	 * @return new instance of {@link Field}.
	 * @since 4.1
	 */
	static Field pivot(String... names) {
		return new SimplePivotField(names);
	}

	/**
	 * Create a {@link Field} with given alias for the calculated distance.
	 *
	 * @param alias the alias to use. must not be {@literal null}.
	 * @return new instance of {@link Field}.
	 * @since 4.1
	 */
	static Field distance(String alias) {
		return DistanceField.distanceAs(alias);
	}

}
