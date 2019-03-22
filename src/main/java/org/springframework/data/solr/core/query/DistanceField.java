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

import org.springframework.data.geo.Point;
import org.springframework.lang.Nullable;

/**
 * Implementation of {@link CalculatedField} for {@code geodist}
 *
 * @author Christoph Strobl
 * @since 1.1
 */
public class DistanceField extends SimpleCalculatedField {

	/**
	 * As of 4.1 please use {@link #distanceAs(String)} along with a {@link FilterQuery}.
	 *
	 * <pre>
	 *     <code>
	 *
	 * Query q = new SimpleQuery("*:*");
	 * q.addFilterQuery(FilterQuery.geoFilter("store", new Point(45.15, -93.85)));
	 * q.projectAllFields();
	 * q.addProjectionOnField(Field.distance("distance"));
	 *     </code>
	 * </pre>
	 *
	 * @param geoFieldName
	 * @param location
	 * @deprecated since 4.1. Replace with: {@link #distanceAs(String)} and add an explicit {@link FilterQuery} with a
	 *             {@link DistanceFunction}.
	 */
	@Deprecated
	public DistanceField(String geoFieldName, Point location) {
		this(null, geoFieldName, location);
	}

	/**
	 * As of 4.1 please use {@link #distanceAs(String)} along with a {@link FilterQuery}.
	 * 
	 * <pre>
	 *     <code>
	 *
	 * Query q = new SimpleQuery("*:*");
	 * q.addFilterQuery(FilterQuery.geoFilter("store", new Point(45.15, -93.85)));
	 * q.projectAllFields();
	 * q.addProjectionOnField(Field.distance("distance"));
	 *     </code>
	 * </pre>
	 * 
	 * @param alias
	 * @param geoFieldName
	 * @param location
	 * @deprecated since 4.1. Replace with: {@link #distanceAs(String)} and add an explicit {@link FilterQuery} with a
	 *             {@link DistanceFunction}.
	 */
	@Deprecated
	public DistanceField(@Nullable String alias, String geoFieldName, Point location) {
		super(alias, GeoDistanceFunction.distanceFrom(geoFieldName).to(location));
	}

	/**
	 * Create a new {@link DistanceField}.
	 *
	 * @param alias the field alias to use.
	 * @since 4.1
	 */
	public DistanceField(String alias) {
		super(alias, GeoDistanceFunction.geodist());
	}

	/**
	 * @param alias the field alias to use.
	 * @since 4.1
	 */
	public static DistanceField distanceAs(String alias) {
		return new DistanceField(alias);
	}

}
