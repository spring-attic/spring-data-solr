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
package org.springframework.data.solr.core.query;

import java.util.Arrays;

import org.springframework.data.geo.Point;
import org.springframework.util.Assert;

/**
 * Implementation of {@code geodist(sfield, latitude, longitude)}
 * 
 * @author Christoph Stobl
 * @since 1.1
 */
public class GeoDistanceFunction extends AbstractFunction {

	private static final String OPERATION = "geodist";

	private GeoDistanceFunction(String fieldName, Point location) {
		super(Arrays.asList(fieldName, location));
	}

	/**
	 * Creates new {@link Builder}
	 * 
	 * @param fieldname
	 * @return
	 */
	public static Builder distanceFrom(String fieldname) {
		return new Builder(fieldname);
	}

	/**
	 * Creates new {@link Builder}
	 * 
	 * @param field must not be null
	 * @return
	 */
	public static Builder distanceFrom(Field field) {
		Assert.notNull(field, "Field cannot be 'null' for geodistance function.");

		return distanceFrom(field.getName());
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

	public static class Builder {

		private final String fieldname;

		/**
		 * @param fieldname must not be empty
		 */
		public Builder(String fieldname) {
			Assert.hasText(fieldname, "Fieldname must not be an empty.");

			this.fieldname = fieldname;
		}

		/**
		 * @param location must not be null
		 * @return
		 */
		public GeoDistanceFunction to(Point location) {
			Assert.notNull(location, "Location for geodist function must not be 'null'");

			return new GeoDistanceFunction(this.fieldname, location);
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
