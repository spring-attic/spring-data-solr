/*
 * Copyright 2014-2017 the original author or authors.
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

import org.springframework.lang.Nullable;

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
	@Nullable
	Object getMin();

	/**
	 * @return minimum value as {@link Double}, {@code null} will be returned when not {@link Number}
	 */
	@Nullable
	Double getMinAsDouble();

	/**
	 * @return minimum value as {@link Date}, {@code null} will be returned when not {@link Date}
	 */
	@Nullable
	Date getMinAsDate();

	/**
	 * @return minimum value as {@link String}
	 */
	String getMinAsString();

	/**
	 * @return maximum value
	 */
	@Nullable
	Object getMax();

	/**
	 * @return maximum value as {@link Double}, {@code null} will be returned when not {@link Number}
	 */
	@Nullable
	Double getMaxAsDouble();

	/**
	 * @return maximum value as {@link Date}, {@code null} will be returned when not {@link Date}
	 */
	@Nullable
	Date getMaxAsDate();

	/**
	 * @return maximum value as {@link String}
	 */
	String getMaxAsString();

	/**
	 * @return sum of all values
	 */
	@Nullable
	Object getSum();

	/**
	 * @return average
	 */
	@Nullable
	Object getMean();

	/**
	 * @return mean value as {@link Double}, {@code null} will be returned when not {@link Number}.
	 * @since 3.0
	 */
	@Nullable
	Double getMeanAsDouble();

	/**
	 * @return mean value as {@link Date}, {@code null} will be returned when not {@link Date}.
	 * @since 3.0
	 */
	@Nullable
	Date getMeanAsDate();

	/**
	 * @return number of non-null values
	 */
	@Nullable
	Long getCount();

	/**
	 * @return number of null values
	 */
	@Nullable
	Long getMissing();

	/**
	 * @return standard deviation
	 */
	@Nullable
	Double getStddev();

	/**
	 * @return sum of squares
	 */
	@Nullable
	Double getSumOfSquares();

}
