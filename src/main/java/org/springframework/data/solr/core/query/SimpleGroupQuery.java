/*
 * Copyright 2012 - 2014 the original author or authors.
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
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * Trivial implementation of {@link GroupQuery}.
 * 
 * @author Francisco Spaeth
 *
 * @since 1.3
 * 
 */
public class SimpleGroupQuery extends SimpleFacetQuery implements GroupQuery {

	private List<Function> groupByFunctions = new ArrayList<Function>(0);
	private List<Query> groupByQuery = new ArrayList<Query>(0);

	private Integer offset = null;
	private Integer limit = null;

	private Sort sort;

	private boolean truncateFacets = false;	
	private boolean groupFacets = false;
	private boolean groupCount = false;
	private int cachePercent = DEFAULT_CACHE_PERCENT;

	public SimpleGroupQuery() {
		super();
	}

	public SimpleGroupQuery(Criteria criteria, Pageable pageable) {
		super(criteria, pageable);
	}

	public SimpleGroupQuery(Criteria criteria) {
		super(criteria);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends GroupQuery> T addGroupByFunction(Function function) {
		Assert.notNull(function, "Function for grouping must not be null.");
		groupByFunctions.add(function);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends GroupQuery> T addGroupByQuery(Query query) {
		Assert.notNull(query, "Query for grouping must not be null.");
		groupByQuery.add(query);
		return (T) this;
	}

	@Override
	public List<Function> getGroupByFunctions() {
		return groupByFunctions;
	}

	@Override
	public List<Query> getGroupByQueries() {
		return groupByQuery;
	}

	@Override
	public Integer getGroupOffset() {
		return offset;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends GroupQuery> T setGroupOffset(Integer offset) {
		this.offset = offset;
		return (T) this;
	}

	@Override
	public Integer getGroupRows() {
		return limit;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends GroupQuery> T setGroupLimit(Integer limit) {
		this.limit = limit;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends GroupQuery> T addGroupSort(Sort sort) {
		if (sort == null) {
			return (T) this;
		}

		if (this.sort == null) {
			this.sort = sort;
		} else {
			this.sort = this.sort.and(sort);
		}

		return (T) this;
	}
	
	@Override
	public Sort getGroupSort() {
		return sort;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends GroupQuery> T setGroupTotalCount(boolean groupCount) {
		this.groupCount = groupCount;
		return (T) this;
	}

	@Override
	public boolean isGroupTotalCount() {
		return groupCount;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends GroupQuery> T setCachePercent(int cachePercent) {
		this.cachePercent = cachePercent;
		return (T) this;
	}

	@Override
	public int getCachePercent() {
		return cachePercent;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends GroupQuery> T setTruncateFacets(boolean truncateFacets) {
		this.truncateFacets = truncateFacets;
		return (T) this;
	}
	
	@Override
	public boolean isTruncateFacets() {
		return truncateFacets;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends GroupQuery> T setGroupFacets(boolean groupFacets) {
		this.groupFacets = groupFacets;
		return (T) this;
	}
	
	@Override
	public boolean isGroupFacets() {
		return groupFacets;
	}
	
	@Override
	@Deprecated
	public Pageable getGroupPageRequest() {

		if (this.limit == null && this.offset == null) {
			return null;
		}

		int rows = this.limit != null ? this.limit : DEFAULT_GROUP_LIMIT;
		int offset = this.offset != null ? this.offset : 0;

		return new SolrPageRequest(rows != 0 ? offset / rows : 0, rows, this.sort);
	}

}
