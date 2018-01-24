/*
 * Copyright 2012 - 2018 the original author or authors.
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
 * Full implementation of {@link Query} that allows multiple options like pagination, grouping,...
 *
 * @author Christoph Strobl
 * @author Rosty Kerei
 * @author Luke Corpe
 * @author Andrey Paramonov
 * @author Francisco Spaeth
 */
public class SimpleQuery extends AbstractQuery implements Query, FilterQuery {

	private List<Field> projectionOnFields = new ArrayList<>(0);
	private List<FilterQuery> filterQueries = new ArrayList<>(0);

	private @Nullable Long offset = null;
	private @Nullable Integer rows = null;

	private Sort sort = Sort.unsorted();

	private @Nullable Operator defaultOperator;
	private @Nullable Integer timeAllowed;
	private @Nullable String defType;

	private @Nullable GroupOptions groupOptions;
	private @Nullable StatsOptions statsOptions;
	private @Nullable SpellcheckOptions spellcheckOptions;

	public SimpleQuery() {}

	/**
	 * @param criteria
	 */
	public SimpleQuery(Criteria criteria) {
		this(criteria, Pageable.unpaged());
	}

	/**
	 * @param queryString
	 * @since 1.1
	 */
	public SimpleQuery(String queryString) {
		this(new SimpleStringCriteria(queryString));
	}

	/**
	 * @param criteria
	 * @param pageable
	 */
	public SimpleQuery(Criteria criteria, @Nullable Pageable pageable) {
		super(criteria);

		if (pageable != null && !pageable.isUnpaged()) {
			this.offset = pageable.getOffset();
			this.rows = pageable.getPageSize();
			this.addSort(pageable.getSort());
		}
	}

	/**
	 * @param queryString
	 * @param pageable
	 * @since 1.1
	 */
	public SimpleQuery(String queryString, Pageable pageable) {
		this(new SimpleStringCriteria(queryString), pageable);
	}

	@Nullable
	public static final Query fromQuery(Query source) {
		return fromQuery(source, new SimpleQuery());
	}

	@Nullable
	public static <T extends SimpleQuery> T fromQuery(@Nullable Query source, @Nullable T destination) {
		if (source == null || destination == null) {
			return null;
		}

		if (source.getCriteria() != null) {
			destination.addCriteria(source.getCriteria());
		}
		if (!source.getFilterQueries().isEmpty()) {
			for (FilterQuery fq : source.getFilterQueries()) {
				destination.addFilterQuery(fq);
			}
		}
		if (!source.getProjectionOnFields().isEmpty()) {
			for (Field projectionField : source.getProjectionOnFields()) {
				destination.addProjectionOnField(projectionField);
			}
		}
		if (!source.getGroupByFields().isEmpty()) {
			for (Field groupByField : source.getGroupByFields()) {
				destination.addGroupByField(groupByField);
			}
		}
		if (source.getSort() != null) {
			destination.addSort(source.getSort());
		}

		if (source.getDefType() != null) {
			destination.setDefType(source.getDefType());
		}

		if (source.getDefaultOperator() != null) {
			destination.setDefaultOperator(source.getDefaultOperator());
		}

		if (source.getTimeAllowed() != null) {
			destination.setTimeAllowed(source.getTimeAllowed());
		}

		if (source.getRequestHandler() != null) {
			destination.setRequestHandler(source.getRequestHandler());
		}

		return destination;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <T extends Query> T addProjectionOnField(Field field) {
		Assert.notNull(field, "Field for projection must not be null.");
		Assert.hasText(field.getName(), "Field.name for projection must not be null/empty.");

		this.projectionOnFields.add(field);
		return (T) this;
	}

	public final <T extends Query> T addProjectionOnField(String fieldname) {
		return this.addProjectionOnField(new SimpleField(fieldname));
	}

	@SuppressWarnings("unchecked")
	public final <T extends Query> T addProjectionOnFields(Field... fields) {
		Assert.notEmpty(fields, "Cannot add projection on null/empty field list.");
		for (Field field : fields) {
			addProjectionOnField(field);
		}
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public final <T extends Query> T addProjectionOnFields(String... fieldnames) {
		Assert.notEmpty(fieldnames, "Cannot add projection on null/empty field list.");
		for (String fieldname : fieldnames) {
			addProjectionOnField(fieldname);
		}
		return (T) this;
	}

	@Override
	public final <T extends Query> T setPageRequest(Pageable pageable) {
		Assert.notNull(pageable, "Pageable must not be null!");

		this.offset = pageable.getOffset();
		this.rows = pageable.getPageSize();
		return this.addSort(pageable.getSort());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Query> T setOffset(Long offset) {
		this.offset = offset;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Query> T setRows(Integer rows) {
		this.rows = rows;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Deprecated
	public final <T extends Query> T addGroupByField(Field field) {
		Assert.notNull(field, "Field for grouping must not be null.");
		Assert.hasText(field.getName(), "Field.name for grouping must not be null/empty.");

		if (this.groupOptions == null) {
			this.groupOptions = new GroupOptions();
		}

		this.groupOptions.addGroupByField(field).setGroupMain(true);
		return (T) this;
	}

	/**
	 * add grouping on field name
	 *
	 * @param fieldname must not be null
	 * @return
	 * @deprecated in favor of {@link GroupOptions}
	 * @see GroupOptions
	 */
	@Deprecated
	public final <T extends Query> T addGroupByField(String fieldname) {
		return addGroupByField(new SimpleField(fieldname));
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <T extends Query> T addSort(@Nullable Sort sort) {
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
	public Sort getSort() {
		return this.sort;
	}

	@Override
	public Pageable getPageRequest() {

		if (this.rows == null && this.offset == null) {
			return Pageable.unpaged();
		}

		int rows = this.rows != null ? this.rows : DEFAULT_PAGE_SIZE;
		long offset = this.offset != null ? this.offset : 0;

		return new SolrPageRequest(rows != 0 ? (int) (offset / rows) : 0, rows, this.sort);
	}

	@Nullable
	@Override
	public Long getOffset() {
		return this.offset;
	}

	@Nullable
	@Override
	public Integer getRows() {
		return this.rows;
	}

	@Override
	@Deprecated
	public List<Field> getGroupByFields() {
		if (this.groupOptions == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(this.groupOptions.getGroupByFields());
	}

	@Override
	public List<Field> getProjectionOnFields() {
		return Collections.unmodifiableList(this.projectionOnFields);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Query> T addFilterQuery(FilterQuery filterQuery) {
		this.filterQueries.add(filterQuery);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Query> T setTimeAllowed(Integer timeAllowed) {
		this.timeAllowed = timeAllowed;
		return (T) this;
	}

	@Nullable
	@Override
	public Integer getTimeAllowed() {
		return this.timeAllowed;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Query> T setGroupOptions(GroupOptions groupOptions) {
		this.groupOptions = groupOptions;
		return (T) this;
	}

	@Nullable
	@Override
	public GroupOptions getGroupOptions() {
		return groupOptions;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.Query#getStatsOptions()
	 */
	@Nullable
	@Override
	public StatsOptions getStatsOptions() {
		return statsOptions;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.Query#setStatsOptions(org.springframework.data.solr.core.query.StatsOptions)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Query> T setStatsOptions(StatsOptions statsOptions) {

		this.statsOptions = statsOptions;
		return (T) this;
	}

	@Override
	public List<FilterQuery> getFilterQueries() {
		return Collections.unmodifiableList(this.filterQueries);
	}

	@Override
	public Operator getDefaultOperator() {
		return this.defaultOperator != null ? this.defaultOperator : Operator.NONE;
	}

	/**
	 * @return true if current operator does not equal {@link Operator#NONE}
	 */
	public boolean hasDefaultOperatorDefined() {
		return !Operator.NONE.equals(getDefaultOperator());
	}

	@Override
	public void setDefaultOperator(Operator operator) {
		this.defaultOperator = operator;
	}

	@Override
	public String getDefType() {
		return this.defType != null ? this.defType : "";
	}

	@Override
	public void setDefType(String defType) {
		this.defType = defType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.Query#setSpellcheckOptions(org.springframework.data.solr.core.query.SpellcheckOptions)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends Query> T setSpellcheckOptions(SpellcheckOptions spellcheckOptions) {

		this.spellcheckOptions = spellcheckOptions;
		return (T) this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.Query#getSpellcheckOptions()
	 */
	@Nullable
	@Override
	public SpellcheckOptions getSpellcheckOptions() {
		return this.spellcheckOptions;
	}
}
