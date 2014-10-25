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

import org.apache.commons.lang3.ObjectUtils;


/**
 * Trivial implementation of {@link StatsResult}.
 * 
 * @author Francisco Spaeth
 * @since 1.4
 */
public class SimpleStatsResult implements StatsResult {

	private Object min;
	private Object max;
	private Double sum;
	private Double mean;
	private Long count;
	private Long missing;
	private Double stddev;
	private Double sumOfSquares;

	@Override
	public Object getMin() {
		return min;
	}

	@Override
	public Date getMinAsDate() {
		if (min instanceof Date) {
			return (Date) min;
		}
		return null;
	}

	@Override
	public Double getMinAsDouble() {
		if (min instanceof Number) {
			return ((Number) min).doubleValue();
		}
		return null;
	}

	@Override
	public String getMinAsString() {
		return ObjectUtils.toString(min, null);
	}

	public void setMin(Object min) {
		this.min = min;
	}

	@Override
	public Object getMax() {
		return max;
	}

	@Override
	public Date getMaxAsDate() {
		if (max instanceof Date) {
			return (Date) max;
		}
		return null;
	}

	@Override
	public Double getMaxAsDouble() {
		if (max instanceof Number) {
			return ((Number) max).doubleValue();
		}
		return null;
	}

	@Override
	public String getMaxAsString() {
		return ObjectUtils.toString(max, null);
	}

	public void setMax(Object max) {
		this.max = max;
	}

	@Override
	public Double getSum() {
		return sum;
	}

	public void setSum(Double sum) {
		this.sum = sum;
	}

	@Override
	public Double getMean() {
		return mean;
	}

	public void setMean(Double mean) {
		this.mean = mean;
	}

	@Override
	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	@Override
	public Long getMissing() {
		return missing;
	}

	public void setMissing(Long missing) {
		this.missing = missing;
	}

	@Override
	public Double getStddev() {
		return stddev;
	}

	public void setStddev(Double stddev) {
		this.stddev = stddev;
	}

	@Override
	public Double getSumOfSquares() {
		return this.sumOfSquares;
	}

	public void setSumOfSquares(Double sumOfSquares) {
		this.sumOfSquares = sumOfSquares;
	}

	@Override
	public String toString() {
		return "SimpleStatsResult [min=" + min + ", max=" + max + ", sum=" + sum + ", mean=" + mean + ", count=" + count
				+ ", missing=" + missing + ", stddev=" + stddev + "]";
	}

}
