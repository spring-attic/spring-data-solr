/*
 * Copyright 2012 - 2016 the original author or authors.
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
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * General purpose {@link Query} abstract decorator.
 * 
 * @author Francisco Spaeth
 * @author Christoph Strobl
 * @since 1.4
 */
public abstract class AbstractQueryDecorator implements Query {

	private Query query;

	public AbstractQueryDecorator(Query query) {

		Assert.notNull(query, "query must not be null");
		this.query = query;
	}

	@Override
	public <T extends SolrDataQuery> T addCriteria(Criteria criteria) {
		return this.query.addCriteria(criteria);
	}

	@Override
	public Criteria getCriteria() {
		return this.query.getCriteria();
	}

	@Override
	public void setJoin(Join join) {
		this.query.setJoin(join);
	}

	@Override
	public Join getJoin() {
		return this.query.getJoin();
	}

	@Override
	public <T extends Query> T addProjectionOnField(Field field) {
		return this.query.addProjectionOnField(field);
	}

	@Override
	public <T extends Query> T setPageRequest(Pageable pageable) {
		return this.query.setPageRequest(pageable);
	}

	@Override
	public <T extends Query> T addGroupByField(Field field) {
		return this.query.addGroupByField(field);
	}

	@Override
	public <T extends Query> T addFilterQuery(FilterQuery query) {
		return this.query.addFilterQuery(query);
	}

	@Override
	public <T extends Query> T setTimeAllowed(Integer timeAllowed) {
		return this.query.setTimeAllowed(timeAllowed);
	}

	@Override
	public List<FilterQuery> getFilterQueries() {
		return this.query.getFilterQueries();
	}

	@Deprecated
	@Override
	public Pageable getPageRequest() {
		return this.query.getPageRequest();
	}

	@Override
	public List<Field> getGroupByFields() {
		return this.query.getGroupByFields();
	}

	@Override
	public List<Field> getProjectionOnFields() {
		return this.query.getProjectionOnFields();
	}

	@Override
	public <T extends Query> T addSort(Sort sort) {
		return this.query.addSort(sort);
	}

	@Override
	public Sort getSort() {
		return this.query.getSort();
	}

	@Override
	public Integer getTimeAllowed() {
		return this.query.getTimeAllowed();
	}

	@Override
	public void setDefaultOperator(Operator operator) {
		this.query.setDefaultOperator(operator);
	}

	@Override
	public Operator getDefaultOperator() {
		return this.query.getDefaultOperator();
	}

	@Override
	public String getDefType() {
		return this.query.getDefType();
	}

	@Override
	public void setDefType(String defType) {
		this.query.setDefType(defType);
	}

	@Override
	public String getRequestHandler() {
		return this.query.getRequestHandler();
	}

	@Override
	public void setRequestHandler(String requestHandler) {
		this.query.setRequestHandler(requestHandler);
	}

	@Override
	public <T extends Query> T setOffset(Integer offset) {
		return this.query.setOffset(offset);
	}

	@Override
	public <T extends Query> T setRows(Integer rows) {
		return this.query.setRows(rows);
	}

	@Override
	public Integer getOffset() {
		return this.query.getOffset();
	}

	@Override
	public Integer getRows() {
		return this.query.getRows();
	}

	@Override
	public <T extends Query> T setGroupOptions(GroupOptions groupOptions) {
		return query.setGroupOptions(groupOptions);
	}

	@Override
	public GroupOptions getGroupOptions() {
		return query.getGroupOptions();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.Query#setStatsOptions(org.springframework.data.solr.core.query.StatsOptions)
	 */
	@Override
	public <T extends Query> T setStatsOptions(StatsOptions statsOptions) {
		return query.setStatsOptions(statsOptions);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.Query#getStatsOptions()
	 */
	@Override
	public StatsOptions getStatsOptions() {
		return query.getStatsOptions();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.Query#setSpellcheckOptions(org.springframework.data.solr.core.query.SpellcheckOptions)
	 */
	@Override
	public <T extends Query> T setSpellcheckOptions(SpellcheckOptions spellcheckOptions) {
		return query.setSpellcheckOptions(spellcheckOptions);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.Query#getSpellcheckOptions()
	 */
	@Override
	public SpellcheckOptions getSpellcheckOptions() {
		return query.getSpellcheckOptions();
	}

	/**
	 * Get the {@link Class} of the originally decorated query.
	 *
	 * @return never {@literal null}.
	 * @since 2.1
	 */
	public Class<?> getQueryType() {
		return ClassUtils.getUserClass(query.getClass());
	}

}
