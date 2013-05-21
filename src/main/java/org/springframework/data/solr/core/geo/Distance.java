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

/**
 * Distance implementation to be used for spatial queries taking solr's usage of metric system into account.
 * 
 * @author Christoph Strobl
 */
public class Distance {

	public static enum Unit {
		KILOMETERS(1.0), MILES(1.609344);

		private final double multiplier;

		Unit(double multiplier) {
			this.multiplier = multiplier;
		}

		public double getMultiplier() {
			return multiplier;
		}
	}

	private final double value;
	private final Unit unit;

	/**
	 * Create new Distance with {@link Distance.Unit.KILOMETERS} as default unit
	 * 
	 * @param value
	 */
	public Distance(double value) {
		this(value, Unit.KILOMETERS);
	}

	/**
	 * Create new Distance
	 * 
	 * @param value
	 * @param unit
	 */
	public Distance(double value, Unit unit) {
		this.value = value;
		this.unit = unit;
	}

	public double getValue() {
		return value;
	}

	public double getNormalizedValue() {
		return unit != null ? (unit.getMultiplier() * value) : (value * Unit.KILOMETERS.getMultiplier());
	}

}
