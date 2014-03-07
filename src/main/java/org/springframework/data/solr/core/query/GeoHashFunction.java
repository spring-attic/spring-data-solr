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
 * Implementation of {@code geohash(latitude, longitude)}
 * 
 * @author Christoph Strobl
 * @since 1.1
 */
public class GeoHashFunction extends AbstractFunction {

	private static final String OPERATION = "geohash";

	private GeoHashFunction(Point location) {
		super(Arrays.asList(location));
	}

	/**
	 * @param location must not be null
	 * @return
	 */
	public static GeoHashFunction geohash(Point location) {
		Assert.notNull(location, "Location for geohash function must not be 'null'");

		return new GeoHashFunction(location);
	}

	/**
	 * @param latitude
	 * @param longitude
	 * @return
	 */
	public static GeoHashFunction geohash(double latitude, double longitude) {
		return geohash(new Point(latitude, longitude));
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

}
