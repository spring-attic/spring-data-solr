/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.query;

import org.springframework.data.geo.Point;

/**
 * Filter Queries are simple solr Queries applied after executing the original query. This corresponds to the {@code fq}
 * Parameter within solr.
 * 
 * @author Christoph Strobl
 */
public interface FilterQuery extends SolrDataQuery {

	/**
	 * Create a new {@link FilterQuery} with the given {@link Criteria}.
	 *
	 * @param criteria must not be {@literal null}.
	 * @return new instance of {@link FilterQuery}.
	 * @since 4.1
	 */
	static FilterQuery filter(Criteria criteria) {
		return new SimpleFilterQuery(criteria);
	}

	/**
	 * Create a new {@link FilterQuery} applying a {@code geodist} function.
	 * 
	 * @param from must not be {@literal null}.
	 * @param to must not be {@literal null}.
	 * @return new instance of {@link FilterQuery}.
	 * @since 4.1
	 */
	static FilterQuery geoFilter(String from, Point to) {
		return filter(Criteria.where(GeoDistanceFunction.distanceFrom(from).to(to)));
	}

}
