/*
 * Copyright 2012 - 2013 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * Implementation of bounding box which is an area defined by two longitudes and two latitudes to be used in spatial
 * queries
 * 
 * @author John Dorman
 * @author Christoph Strobl
 */
public class BoundingBox {

	private GeoLocation geoLocationStart;
	private GeoLocation geoLocationEnd;

	private BoundingBox() {
		// hide default constructor
	}

	/**
	 * Create new BoundingBox for given locations
	 * 
	 * @param geoLocationStart must not be null
	 * @param geoLocationEnd must not be null
	 */
	public BoundingBox(GeoLocation geoLocationStart, GeoLocation geoLocationEnd) {
		Assert.notNull(geoLocationStart);
		Assert.notNull(geoLocationEnd);

		this.geoLocationStart = geoLocationStart;
		this.geoLocationEnd = geoLocationEnd;
	}

	/**
	 * create bounding box via builder. {@code BoundingBox.startingAt(locationStart).endingAt(locationEnd)}
	 * 
	 * @param start
	 * @return
	 */
	public static Builder startingAt(GeoLocation start) {
		return new Builder(start);
	}

	public GeoLocation getGeoLocationStart() {
		return geoLocationStart;
	}

	public void setGeoLocationStart(GeoLocation geoLocationStart) {
		this.geoLocationStart = geoLocationStart;
	}

	public GeoLocation getGeoLocationEnd() {
		return geoLocationEnd;
	}

	public void setGeoLocationEnd(GeoLocation geoLocationEnd) {
		this.geoLocationEnd = geoLocationEnd;
	}

	public static class Builder {

		private BoundingBox bbox;

		/**
		 * @param start must not be null
		 */
		public Builder(GeoLocation start) {
			Assert.notNull(start);

			bbox = new BoundingBox();
			bbox.geoLocationStart = start;
		}

		/**
		 * @param end must not be null
		 * @return
		 */
		public BoundingBox endingAt(GeoLocation end) {
			Assert.notNull(end);

			bbox.geoLocationEnd = end;
			return bbox;
		}

	}
}
