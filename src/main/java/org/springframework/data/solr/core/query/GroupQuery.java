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
 * Query request to get a grouped response.
 * 
 * @author Francisco Spaeth
 * 
 * @since 1.3
 * 
 */
public interface GroupQuery extends FacetQuery {

	int DEFAULT_GROUP_LIMIT = 1;
	int DEFAULT_CACHE_PERCENT = 0;

	/**
	 * Adds a group by function request.
	 * 
	 * @param function
	 * @return
	 */
	<T extends GroupQuery> T addGroupByFunction(Function function);

	/**
	 * @return all group by function request.
	 */
	List<Function> getGroupByFunctions();

	/**
	 * Adds a group by query request.
	 * 
	 * @param query
	 * @return
	 */
	<T extends GroupQuery> T addGroupByQuery(Query query);

	/**
	 * @return all group by query requests.
	 */
	List<Query> getGroupByQueries();

	/**
	 * Defines if response should hold total count of groups for each
	 * {@link org.springframework.data.solr.core.query.result.GroupResult}
	 * 
	 * @param groupCount
	 * @return
	 */
	<T extends GroupQuery> T setGroupTotalCount(boolean groupCount);

	/**
	 * @return if response shall hold total count of groups for each
	 *         {@link org.springframework.data.solr.core.query.result.GroupResult}.
	 */
	boolean isGroupTotalCount();

	/**
	 * Set the number of rows to skip in each {@link org.springframework.data.solr.core.query.result.GroupEntry}
	 * 
	 * @param offset
	 * @return
	 */
	<T extends GroupQuery> T setGroupOffset(Integer offset);

	/**
	 * @return number of rows to skip in each {@link org.springframework.data.solr.core.query.result.GroupEntry}
	 */
	Integer getGroupOffset();

	/**
	 * Set the number of rows to be presented in each {@link org.springframework.data.solr.core.query.result.GroupEntry}
	 * 
	 * @param rows
	 * @return
	 */
	<T extends GroupQuery> T setGroupLimit(Integer rows);

	/**
	 * @return number of rows to be presented in each {@link org.springframework.data.solr.core.query.result.GroupEntry}
	 */
	Integer getGroupRows();

	/**
	 * Add sort constraint to group query to sort the group content.
	 * 
	 * @param sort
	 * @return
	 */
	<T extends GroupQuery> T addGroupSort(Sort sort);

	/**
	 * @return the group query sort constraints.
	 */
	Sort getGroupSort();

	/**
	 * Enables caching for grouping result.
	 * 
	 * @param cachePercent
	 * @return
	 */
	<T extends GroupQuery> T setCachePercent(int cachePercent);

	/**
	 * @return percent of caching for grouping result.
	 */
	int getCachePercent();

	/**
	 * Defines if the facet counts should be grouped by the first field group defined for this query.
	 * 
	 * @param groupFacets
	 * @return
	 */
	<T extends GroupQuery> T setGroupFacets(boolean groupFacets);
	
	/**
	 * @return facet counts should be grouped by the first field group defined for this query
	 */
	boolean isGroupFacets();
	
	/**
	 * Defines if facet counts should be based on the most relevant document of each group matching the query.
	 * 
	 * @param truncateFacets
	 * @return
	 */
	<T extends GroupQuery> T setTruncateFacets(boolean truncateFacets);
	
	/**
	 * @return facet counts should be based on the most relevant document of each group matching the query.
	 */
	boolean isTruncateFacets();
	
	/**
	 * Returns a {@link Pageable} object representing group request.
	 * 
	 * @return
	 */
	@Deprecated
	Pageable getGroupPageRequest();

}