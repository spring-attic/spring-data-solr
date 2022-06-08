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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.geo.Point;
import org.springframework.data.solr.core.query.Function.Context.Target;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of {@code geodist(sfield, latitude, longitude)}
 *
 * @author Christoph Stobl
 * @since 1.1
 */
public class GeoDistanceFunction extends AbstractFunction {

	private static final String OPERATION = "geodist";

	private GeoDistanceFunction(@Nullable Field field, @Nullable Point location) {
		super(toArgs(field, location));
	}

	public static GeoDistanceFunction geodist() {
		return new GeoDistanceFunction(null, null);
	}

	private static List<Object> toArgs(@Nullable Field field, @Nullable Point location) {

		if (field == null || location == null) {
			return Collections.emptyList();
		}

		return Arrays.asList(field, location);
	}

	/**
	 * Creates new {@link Builder}
	 *
	 * @param fieldName
	 * @return
	 */
	public static Builder distanceFrom(String fieldName) {

		Assert.hasText(fieldName, "FieldName must not be empty");
		return distanceFrom(new SimpleField(fieldName));
	}

	/**
	 * Creates new {@link Builder}
	 *
	 * @param field must not be null
	 * @return
	 */
	public static Builder distanceFrom(Field field) {

		Assert.notNull(field, "Field cannot be 'null' for geodistance function");
		return new Builder(field);
	}

	@Override
	public String toSolrFunction(Context context) {

		if (Target.PROJECTION.equals(context.getTarget())) {
			return "geodist()";
		}

		StringBuilder sb = new StringBuilder("{!func}geodist()");
		return sb.toString();
	}

	@Override
	public Map<String, String> getArgumentMap(Context context) {

		if (CollectionUtils.isEmpty(getArguments())) {
			return super.getArgumentMap(context);
		}

		HashMap<String, String> argumentMap = new LinkedHashMap<>();
		argumentMap.put("pt", context.convert(getArguments().get(1)));
		argumentMap.put("sfield", context.convert(getArguments().get(0)));
		return argumentMap;
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

	public static class Builder {

		private final Field field;

		/**
		 * @param field must not be empty
		 */
		public Builder(Field field) {

			Assert.notNull(field, "field must not be an null");
			this.field = field;
		}

		/**
		 * @param location must not be null
		 * @return
		 */
		public GeoDistanceFunction to(Point location) {

			Assert.notNull(location, "Location for geodist function must not be 'null'");
			return new GeoDistanceFunction(this.field, location);
		}

		/**
		 * @param latitude
		 * @param longitude
		 * @return
		 */
		public GeoDistanceFunction to(double latitude, double longitude) {
			return to(new Point(latitude, longitude));
		}

	}

}
