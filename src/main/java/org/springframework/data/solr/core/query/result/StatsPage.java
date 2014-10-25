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

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.StatsOptions;

/**
 * Representation of a Stats result page, holding one {@link FieldStatsResult} for each field statistic requested on a
 * {@link org.springframework.data.solr.core.query.Query} through {@link StatsOptions}.
 * 
 * @author Francisco Spaeth
 * @param <T>
 * @since 1.4
 */
public interface StatsPage<T> extends Page<T> {

	/**
	 * Get the stats result done for the given {@link Field}.
	 * 
	 * @param field
	 * @return
	 */
	FieldStatsResult getFieldStatsResult(Field field);

	/**
	 * Get the stats result done for the field with the given fieldName.
	 * 
	 * @param fieldName
	 * @return
	 */
	FieldStatsResult getFieldStatsResult(String fieldName);

	/**
	 * Get all field stats results for this page.
	 * 
	 * @return
	 */
	Map<String, FieldStatsResult> getFieldStatsResults();

}
