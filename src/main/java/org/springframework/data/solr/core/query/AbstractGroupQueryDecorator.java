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

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * General purpose {@link GroupQuery} abstract decorator.
 * 
 * @author Francisco Spaeth
 *
 * @since 1.3
 * 
 */
public abstract class AbstractGroupQueryDecorator extends AbstractFacetQueryDecorator implements GroupQuery {

	private GroupQuery query;

	public AbstractGroupQueryDecorator(GroupQuery query) {
		super(query);
		this.query = query;
	}

	@Override
	public <T extends GroupQuery> T addGroupByFunction(Function function) {
		return this.query.addGroupByFunction(function);
	}

	@Override
	public List<Function> getGroupByFunctions() {
		return this.query.getGroupByFunctions();
	}

	@Override
	public <T extends GroupQuery> T addGroupByQuery(Query query) {
		return this.query.addGroupByQuery(query);
	}

	@Override
	public List<Query> getGroupByQueries() {
		return this.query.getGroupByQueries();
	}

	@Override
	public <T extends GroupQuery> T setGroupTotalCount(boolean groupCount) {
		return this.query.setGroupTotalCount(groupCount);
	}

	@Override
	public boolean isGroupTotalCount() {
		return this.query.isGroupTotalCount();
	}

	@Override
	public Integer getGroupOffset() {
		return query.getGroupOffset();
	}

	@Override
	public <T extends GroupQuery> T setGroupOffset(Integer offset) {
		return query.setGroupOffset(offset);
	}

	@Override
	public Integer getGroupRows() {
		return query.getGroupRows();
	}

	@Override
	public <T extends GroupQuery> T setGroupLimit(Integer rows) {
		return query.setGroupLimit(rows);
	}

	@Override
	public <T extends GroupQuery> T addGroupSort(Sort sort) {
		return query.addGroupSort(sort);
	}

	@Override
	public Sort getGroupSort() {
		return query.getGroupSort();
	}
	
	@Override
	public <T extends GroupQuery> T setCachePercent(int cachePercent) {
		return query.setCachePercent(cachePercent);
	}
	
	@Override
	public int getCachePercent() {
		return query.getCachePercent();
	}
	
	@Override
	public <T extends GroupQuery> T setTruncateFacets(boolean truncateFacets) {
		return query.setTruncateFacets(truncateFacets);
	}
	
	@Override
	public boolean isTruncateFacets() {
		return query.isTruncateFacets();
	}
	
	@Override
	public <T extends GroupQuery> T setGroupFacets(boolean groupFacets) {
		return query.setGroupFacets(groupFacets);
	}
	
	@Override
	public boolean isGroupFacets() {
		return query.isGroupFacets();
	}

	@Deprecated
	@Override
	public Pageable getGroupPageRequest() {
		return query.getGroupPageRequest();
	}
	
}
