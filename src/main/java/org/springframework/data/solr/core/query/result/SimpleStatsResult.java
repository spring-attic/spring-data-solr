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

import org.springframework.util.ObjectUtils;

/**
 * Trivial implementation of {@link StatsResult}.
 * 
 * @author Francisco Spaeth
 * @author Christoph Strobl
 * @since 1.4
 */
public class SimpleStatsResult implements StatsResult {

	private Object min;
	private Object max;
	private Object sum;
	private Object mean;
	private Long count;
	private Long missing;
	private Double stddev;
	private Double sumOfSquares;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getMin()
	 */
	@Override
	public Object getMin() {
		return min;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getMinAsDate()
	 */
	@Override
	public Date getMinAsDate() {

		if (min instanceof Date) {
			return (Date) min;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getMinAsDouble()
	 */
	@Override
	public Double getMinAsDouble() {

		if (min instanceof Number) {
			return ((Number) min).doubleValue();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getMinAsString()
	 */
	@Override
	public String getMinAsString() {
		return ObjectUtils.nullSafeToString(min);
	}

	public void setMin(Object min) {
		this.min = min;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getMax()
	 */
	@Override
	public Object getMax() {
		return max;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getMaxAsDate()
	 */
	@Override
	public Date getMaxAsDate() {

		if (max instanceof Date) {
			return (Date) max;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getMaxAsDouble()
	 */
	@Override
	public Double getMaxAsDouble() {

		if (max instanceof Number) {
			return ((Number) max).doubleValue();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getMaxAsString()
	 */
	@Override
	public String getMaxAsString() {
		return ObjectUtils.nullSafeToString(max);
	}

	public void setMax(Object max) {
		this.max = max;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getSum()
	 */
	@Override
	public Object getSum() {
		return sum;
	}

	public void setSum(Object sum) {
		this.sum = sum;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getMean()
	 */
	@Override
	public Object getMean() {
		return mean;
	}

	public void setMean(Object mean) {
		this.mean = mean;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getCount()
	 */
	@Override
	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getMissing()
	 */
	@Override
	public Long getMissing() {
		return missing;
	}

	public void setMissing(Long missing) {
		this.missing = missing;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getStddev()
	 */
	@Override
	public Double getStddev() {
		return stddev;
	}

	public void setStddev(Double stddev) {
		this.stddev = stddev;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.StatsResult#getSumOfSquares()
	 */
	@Override
	public Double getSumOfSquares() {
		return this.sumOfSquares;
	}

	public void setSumOfSquares(Double sumOfSquares) {
		this.sumOfSquares = sumOfSquares;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SimpleStatsResult [min=" + min + ", max=" + max + ", sum=" + sum + ", mean=" + mean + ", count=" + count
				+ ", missing=" + missing + ", stddev=" + stddev + "]";
	}

}
