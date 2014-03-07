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
package org.springframework.data.solr.core.geo;

import org.springframework.data.geo.Box;
import org.springframework.data.geo.Point;
import org.springframework.util.Assert;

/**
 * Implementation of bounding box which is an area defined by two longitudes and two latitudes to be used in spatial
 * queries
 * 
 * @author John Dorman
 * @author Christoph Strobl
 * @deprecated Will be removed in 1.3. Use {@link Box} instead.
 */
public class BoundingBox extends Box {

	public BoundingBox(double[] first, double[] second) {
		super(first, second);
	}

	public BoundingBox(Point first, Point second) {
		super(first, second);
	}

	/**
	 * create bounding box via builder. {@code BoundingBox.startingAt(locationStart).endingAt(locationEnd)}
	 * 
	 * @param start
	 * @return
	 */
	public static Builder startingAt(Point start) {
		return new Builder(start);
	}

	public GeoLocation getGeoLocationStart() {
		return new GeoLocation(getFirst().getX(), getFirst().getY());
	}

	public GeoLocation getGeoLocationEnd() {
		return new GeoLocation(getSecond().getX(), getSecond().getY());
	}

	public static class Builder {

		private Point start;

		/**
		 * @param start must not be null
		 */
		public Builder(Point start) {
			Assert.notNull(start);

			this.start = start;
		}

		/**
		 * @param end must not be null
		 * @return
		 */
		public BoundingBox endingAt(Point end) {
			Assert.notNull(end);

			return new BoundingBox(start, end);
		}

	}
}
