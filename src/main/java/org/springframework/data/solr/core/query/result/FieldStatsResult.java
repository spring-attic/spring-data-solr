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

import java.util.Collection;
import java.util.Map;

import org.springframework.data.solr.core.query.Field;
import org.springframework.lang.Nullable;

/**
 * Specialization of {@link StatsResult} to represent statistic information for a field.
 * 
 * @author Francisco Spaeth
 * @since 1.4
 * @see StatsResult
 */
public interface FieldStatsResult extends StatsResult {

	/**
	 * Return a map of fieldName associated to value facets for the given {@link FieldStatsResult}.
	 * 
	 * @return map of field faceting statistics
	 */
	Map<String, Map<String, StatsResult>> getFacetStatsResults();

	/**
	 * Return a map of value associated to its statistics for a given field.
	 * 
	 * @return map of values statistics
	 */
	Map<String, StatsResult> getFacetStatsResult(Field field);

	/**
	 * Return a map of value associated to its statistics for a given field name.
	 * 
	 * @return map of values statistics
	 */
	Map<String, StatsResult> getFacetStatsResult(String fieldName);

	/**
	 * Return the count of distinct values for this field.
	 * 
	 * @return distinct count, {@literal null} when not requested
	 */
	@Nullable
	Long getDistinctCount();

	/**
	 * Return the distinct values for this field.
	 * 
	 * @return
	 */
	Collection<Object> getDistinctValues();

}
