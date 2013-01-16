/*
 * Copyright 2013 the original author or authors.
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

/**
 * @author John Dorman
 */
public class BoundingBox {

	private GeoLocation geoLocationStart;
	private GeoLocation geoLocationEnd;

	public BoundingBox(GeoLocation geoLocationStart, GeoLocation geoLocationEnd) {
		this.geoLocationStart = geoLocationStart;
		this.geoLocationEnd = geoLocationEnd;
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
}
