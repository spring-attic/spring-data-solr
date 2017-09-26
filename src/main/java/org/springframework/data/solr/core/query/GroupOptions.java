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
package org.springframework.data.solr.core.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Set of options that could be set for a {@link Query} in order to have grouped results.
 * 
 * @author Francisco Spaeth
 * @author Christoph Strobl
 * @since 1.4
 */
public class GroupOptions {

	int DEFAULT_GROUP_LIMIT = 1;
	int DEFAULT_CACHE_PERCENT = 0;

	private List<Field> groupByFields = new ArrayList<>(0);
	private List<Function> groupByFunctions = new ArrayList<>(0);
	private List<Query> groupByQuery = new ArrayList<>(0);

	private @Nullable Integer offset = null;
	private @Nullable Integer limit = null;

	private @Nullable Sort sort;

	private boolean truncateFacets = false;
	private boolean groupFacets = false;
	private boolean totalCount = false;
	private boolean groupMain = false;
	private int cachePercent = DEFAULT_CACHE_PERCENT;

	/**
	 * Adds a group request for a {@link Field}.
	 * 
	 * @param field
	 * @return
	 */
	public GroupOptions addGroupByField(Field field) {

		Assert.notNull(field, "Field for grouping must not be null.");
		Assert.hasText(field.getName(), "Field.name for grouping must not be null/empty.");
		groupByFields.add(field);
		return this;
	}

	/**
	 * List of {@link Field}s to perform grouping by.
	 * 
	 * @return
	 */
	public List<Field> getGroupByFields() {
		return Collections.unmodifiableList(this.groupByFields);
	}

	/**
	 * Adds a group request for a {@link Field} using its name.
	 * 
	 * @param fieldName
	 * @return
	 */
	public GroupOptions addGroupByField(String fieldName) {

		Assert.hasText(fieldName, "Field.name for grouping must not be null/empty.");
		groupByFields.add(new SimpleField(fieldName));
		return this;
	}

	/**
	 * Adds a group request for a {@link Function} result.
	 * 
	 * @param function
	 * @return
	 */
	public GroupOptions addGroupByFunction(Function function) {

		Assert.notNull(function, "Function for grouping must not be null.");
		groupByFunctions.add(function);
		return this;
	}

	/**
	 * List of {@link Function}s to perform grouping by.
	 * 
	 * @return
	 */
	public List<Function> getGroupByFunctions() {
		return Collections.unmodifiableList(groupByFunctions);
	}

	/**
	 * Adds a group request for a {@link Query} result.
	 * 
	 * @param query
	 * @return
	 */
	public GroupOptions addGroupByQuery(Query query) {

		Assert.notNull(query, "Query for grouping must not be null.");
		groupByQuery.add(query);
		return this;
	}

	/**
	 * List of {@link Query}s to perform grouping by.
	 * 
	 * @return
	 */
	public List<Query> getGroupByQueries() {
		return Collections.unmodifiableList(groupByQuery);
	}

	/**
	 * Sets the initial offset of each group.
	 * 
	 * @param offset
	 * @return
	 */
	public GroupOptions setOffset(@Nullable Integer offset) {

		this.offset = offset == null ? null : Math.max(0, offset);
		return this;
	}

	/**
	 * @return initial offset of each group
	 */
	@Nullable
	public Integer getOffset() {
		return offset;
	}

	/**
	 * @return the number of rows to return for each group.
	 */
	@Nullable
	public Integer getLimit() {
		return limit;
	}

	/**
	 * Sets the number of rows to return for each group.
	 * 
	 * @param limit
	 * @return
	 */
	public GroupOptions setLimit(Integer limit) {

		this.limit = limit;
		return this;
	}

	/**
	 * Adds {@link Sort} to instruct how to sort elements within a single group.
	 * 
	 * @param sort
	 * @return
	 */
	public GroupOptions addSort(@Nullable Sort sort) {

		if (sort == null) {
			return this;
		}

		if (this.sort == null) {
			this.sort = sort;
		} else {
			this.sort = this.sort.and(sort);
		}

		return this;
	}

	/**
	 * @return sort instruction on how to sort elements within a single group.
	 */
	@Nullable
	public Sort getSort() {
		return sort;
	}

	/**
	 * Defines whether the group count should be included in the response.
	 * 
	 * @param groupCount
	 * @return
	 */
	public GroupOptions setTotalCount(boolean groupCount) {

		this.totalCount = groupCount;
		return this;
	}

	/**
	 * @return whether the group count should be included in the response.
	 */
	public boolean isTotalCount() {
		return totalCount;
	}

	/**
	 * Sets the caching for grouping results.
	 * 
	 * @param cachePercent
	 * @return
	 */
	public GroupOptions setCachePercent(int cachePercent) {

		this.cachePercent = Math.max(0, Math.min(100, cachePercent));
		return this;
	}

	/**
	 * @return caching for grouping results.
	 */
	public int getCachePercent() {
		return cachePercent;
	}

	/**
	 * Defines the maximum size of the group cache.
	 * 
	 * @param truncateFacets
	 * @return
	 */
	public GroupOptions setTruncateFacets(boolean truncateFacets) {

		this.truncateFacets = truncateFacets;
		return this;
	}

	/**
	 * @return the maximum size of the group cache.
	 */
	public boolean isTruncateFacets() {
		return truncateFacets;
	}

	/**
	 * Defines whether field facet shall be computed in grouped fashion.
	 * 
	 * @param groupFacets
	 * @return
	 */
	public GroupOptions setGroupFacets(boolean groupFacets) {

		this.groupFacets = groupFacets;
		return this;
	}

	/**
	 * @return whether field facet shall be computed in grouped fashion.
	 */
	public boolean isGroupFacets() {
		return groupFacets;
	}

	/**
	 * Defines whether or not the first field group result shall be used as main result.
	 * 
	 * @param groupMain
	 * @return
	 */
	public GroupOptions setGroupMain(boolean groupMain) {

		this.groupMain = groupMain;
		return this;
	}

	/**
	 * @return whether or not the first field group result shall be used as main result.
	 */
	public boolean isGroupMain() {
		return groupMain;
	}

	@Nullable
	public Pageable getPageRequest() {

		if (this.limit == null && this.offset == null) {
			return null;
		}

		int limit = this.limit != null ? this.limit : DEFAULT_GROUP_LIMIT;
		int offset = this.offset != null ? this.offset : 0;

		return new SolrPageRequest(limit != 0 ? offset / limit : 0, limit, this.sort);
	}

}
