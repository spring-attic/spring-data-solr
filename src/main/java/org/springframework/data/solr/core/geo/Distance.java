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

import org.springframework.data.geo.Metric;
import org.springframework.data.geo.Metrics;

/**
 * Distance implementation to be used for spatial queries taking solr's usage of metric system into account.
 * 
 * @author Christoph Strobl
 * @deprecated Will be removed in 1.3. Use {@link org.springframework.data.geo.Distance} instead.
 */
public class Distance extends org.springframework.data.geo.Distance {

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

	public Distance(double value) {
		super(value);
	}

	public Distance(double value, Metric metric) {
		super(value, metric);
	}

	public Distance(double value, Unit unit) {
		super(value, unit != null ? (Unit.MILES.equals(unit) ? Metrics.MILES : Metrics.KILOMETERS) : Metrics.KILOMETERS);
	}

}
