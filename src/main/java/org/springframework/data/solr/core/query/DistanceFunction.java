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
package org.springframework.data.solr.core.query;

import java.util.Arrays;

import org.springframework.data.solr.core.geo.Point;
import org.springframework.util.Assert;

/**
 * Implementation of {@code dist(power, pointA, pointB)}
 * 
 * @author Christoph Strobl
 * @since 1.1
 * 
 */
public class DistanceFunction extends AbstractFunction {

	private static final String OPERATION = "dist";

	public enum Power {
		SPARSENESS_CALCULATION("0"), MANHATTAN_DISTANCE("1"), EUCLIDEAN_DISTANCE("2"), INFINITE_NORM("Infinite");

		private String value;

		private Power(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}

	private DistanceFunction(Power power) {
		super(Arrays.asList(power.getValue()));
	}

	/**
	 * creates new {@link Builder} for sparseness distance {@link Power#SPARSENESS_CALCULATION}
	 * 
	 * @return
	 */
	public static Builder sparsenessDistance() {
		return new Builder(Power.SPARSENESS_CALCULATION);
	}

	/**
	 * creates new {@link Builder} for Manhattan (taxicab) Distance {@link Power#MANHATTAN_DISTANCE}
	 * 
	 * @return
	 */
	public static Builder manhattanDistance() {
		return new Builder(Power.MANHATTAN_DISTANCE);
	}

	/**
	 * creates new {@link Builder} for Euclidean Distance {@link Power#EUCLIDEAN_DISTANCE}
	 * 
	 * @return
	 */
	public static Builder euclideanDistance() {
		return new Builder(Power.EUCLIDEAN_DISTANCE);
	}

	/**
	 * creates new {@link Builder} for Infinite norm distance - maximum value in the vector {@link Power#INFINITE_NORM}
	 * 
	 * @return
	 */
	public static Builder infiniteNormDistance() {
		return new Builder(Power.INFINITE_NORM);
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

	public static class Builder {

		private DistanceFunction function;

		public Builder(Power power) {
			Assert.notNull(power, "Calculation type must not be 'null'.");

			function = new DistanceFunction(power);
		}

		public DistanceFunction between(Point point1, Point point2) {
			Assert.notNull(point1, "Parameter 'point1' must not be null");
			Assert.notNull(point2, "Parameter 'point2' must not be null");

			function.addArgument(point1);
			function.addArgument(point2);
			return function;
		}

	}

}
