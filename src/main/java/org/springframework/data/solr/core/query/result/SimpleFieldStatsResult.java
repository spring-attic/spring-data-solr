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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.data.solr.core.query.Field;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Trivial implementation of {@link FieldStatsResult}.
 * 
 * @author Francisco Spaeth
 * @since 1.4
 */
public class SimpleFieldStatsResult extends SimpleStatsResult implements FieldStatsResult {

	private Map<String, Map<String, StatsResult>> facetStatsResult = Collections.emptyMap();
	private @Nullable Long distinctCount;
	private Collection<Object> distinctValues = Collections.emptyList();

	@Override
	public Map<String, Map<String, StatsResult>> getFacetStatsResults() {
		return Collections.unmodifiableMap(facetStatsResult);
	}

	public void setStatsResults(Map<String, Map<String, StatsResult>> statsResults) {
		this.facetStatsResult = statsResults;
	}

	@Override
	public Map<String, StatsResult> getFacetStatsResult(Field field) {
		Assert.notNull(field, "field must not be null");
		return getFacetStatsResult(field.getName());
	}

	@Override
	public Map<String, StatsResult> getFacetStatsResult(String fieldName) {
		Assert.notNull("fieldName must be not null", fieldName);
		return facetStatsResult.get(fieldName);
	}

	@Nullable
	@Override
	public Long getDistinctCount() {
		return distinctCount;
	}

	public void setCountDistinct(Long distinctCount) {
		this.distinctCount = distinctCount;
	}

	@Override
	public Collection<Object> getDistinctValues() {
		return Collections.unmodifiableCollection(this.distinctValues);
	}

	public void setDistinctValues(@Nullable  Collection<Object> distinctValues) {
		if (distinctValues == null) {
			this.distinctValues = Collections.emptyList();
		} else {
			this.distinctValues = new ArrayList<>();
			this.distinctValues.addAll(distinctValues);
		}
	}

	@Override
	public String toString() {
		return "SimpleFieldStatsResult [min=" + getMin() + ", max=" + getMax() + ", sum=" + getSum() + ", mean=" + getMean()
				+ ", count=" + getCount() + ", missing=" + getMissing() + ", stddev=" + getStddev() + ", statsResults="
				+ facetStatsResult + "]";
	}

}
