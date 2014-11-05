/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.solr.core.query.result;

import java.util.Date;

/**
 * Contract to define representation of statistic information requested using
 * {@link org.springframework.data.solr.core.query.StatsOptions}.
 * 
 * @author Francisco Spaeth
 * @since 1.4
 */
public interface StatsResult {

	/**
	 * @return minimum value
	 */
	Object getMin();

	/**
	 * @return minimum value as {@link Double}, {@code null} will be returned when not {@link Number}
	 */
	Double getMinAsDouble();

	/**
	 * @return minimum value as {@link Date}, {@code null} will be returned when not {@link Date}
	 */
	Date getMinAsDate();

	/**
	 * @return minimum value as {@link String}
	 */
	String getMinAsString();

	/**
	 * @return maximum value
	 */
	Object getMax();

	/**
	 * @return maximum value as {@link Double}, {@code null} will be returned when not {@link Number}
	 */
	Double getMaxAsDouble();

	/**
	 * @return maximum value as {@link Date}, {@code null} will be returned when not {@link Date}
	 */
	Date getMaxAsDate();

	/**
	 * @return maximum value as {@link String}
	 */
	String getMaxAsString();

	/**
	 * @return sum of all values
	 */
	Object getSum();

	/**
	 * @return average
	 */
	Object getMean();

	/**
	 * @return number of non-null values
	 */
	Long getCount();

	/**
	 * @return number of null values
	 */
	Long getMissing();

	/**
	 * @return standard deviation
	 */
	Double getStddev();

	/**
	 * @return sum of squares
	 */
	Double getSumOfSquares();

}
