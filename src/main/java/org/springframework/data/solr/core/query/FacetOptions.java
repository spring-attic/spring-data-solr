/*
 * Copyright 2012 - 2017 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.FacetParams.FacetRangeInclude;
import org.apache.solr.common.params.FacetParams.FacetRangeOther;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Set of options that can be set on a {@link FacetQuery}
 * 
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public class FacetOptions {

	public static final int DEFAULT_FACET_MIN_COUNT = 1;
	public static final int DEFAULT_FACET_LIMIT = 10;
	public static final FacetSort DEFAULT_FACET_SORT = FacetSort.COUNT;

	public enum FacetSort {
		COUNT, INDEX
	}

	private List<Field> facetOnFields = new ArrayList<Field>(1);
	private List<PivotField> facetOnPivotFields = new ArrayList<PivotField>(0);
	private List<FieldWithRangeParameters<?, ?, ?>> facetRangeOnFields = new ArrayList<FieldWithRangeParameters<?, ?, ?>>(
			1);
	private List<SolrDataQuery> facetQueries = new ArrayList<SolrDataQuery>(0);

	private int facetMinCount = DEFAULT_FACET_MIN_COUNT;
	private int facetLimit = DEFAULT_FACET_LIMIT;
	private String facetPrefix;
	private FacetSort facetSort = DEFAULT_FACET_SORT;
	private Pageable pageable;

	public FacetOptions() {}

	/**
	 * Creates new instance faceting on fields with given name
	 * 
	 * @param fieldnames
	 */
	public FacetOptions(String... fieldnames) {
		Assert.notNull(fieldnames, "Fields must not be null.");
		Assert.noNullElements(fieldnames, "Cannot facet on null fieldname.");

		for (String fieldname : fieldnames) {
			addFacetOnField(fieldname);
		}
	}

	/**
	 * Creates new instance faceting on given fields
	 * 
	 * @param fieldnames
	 */
	public FacetOptions(Field... fields) {
		Assert.notNull(fields, "Fields must not be null.");
		Assert.noNullElements(fields, "Cannot facet on null field.");

		for (Field field : fields) {
			addFacetOnField(field);
		}
	}

	/**
	 * Creates new instance faceting on given queries
	 * 
	 * @param facetQueries
	 */
	public FacetOptions(SolrDataQuery... facetQueries) {
		Assert.notNull(facetQueries, "Facet Queries must not be null.");
		Assert.noNullElements(facetQueries, "Cannot facet on null query.");

		this.facetQueries.addAll(Arrays.asList(facetQueries));
	}

	/**
	 * Append additional field for faceting
	 * 
	 * @param field
	 * @return
	 */
	public final FacetOptions addFacetOnField(Field field) {
		Assert.notNull(field, "Cannot facet on null field.");
		Assert.hasText(field.getName(), "Cannot facet on field with null/empty fieldname.");

		this.facetOnFields.add(field);
		return this;
	}

	/**
	 * Append additional field with given name for faceting
	 * 
	 * @param fieldname
	 * @return
	 */
	public final FacetOptions addFacetOnField(String fieldname) {
		addFacetOnField(new SimpleField(fieldname));
		return this;
	}

	/**
	 * Append additional field for range faceting
	 * 
	 * @param field the {@link Field} to be appended to range faceting fields
	 * @return this
	 * @since 1.5
	 */
	public final FacetOptions addFacetByRange(FieldWithRangeParameters<?, ?, ?> field) {
		Assert.notNull(field, "Cannot range facet on null field.");
		Assert.hasText(field.getName(), "Cannot range facet on field with null/empty fieldname.");

		this.facetRangeOnFields.add(field);
		return this;
	}

	/**
	 * Add pivot facet on given {@link Field}s.
	 * 
	 * @param fields
	 * @return
	 */
	public final FacetOptions addFacetOnPivot(Field... fields) {
		Assert.notNull(fields, "Pivot Facets must not be null.");

		for (Field field : fields) {
			Assert.notNull(field, "Cannot facet on null field.");
			Assert.hasText(field.getName(), "Cannot facet on field with null/empty fieldname.");
		}

		List<Field> list = Arrays.asList(fields);
		this.facetOnPivotFields.add(new SimplePivotField(list));
		return this;
	}

	/**
	 * @param fieldName
	 * @return
	 */
	public final FacetOptions addFacetOnPivot(String... fieldnames) {
		Assert.state(fieldnames.length > 1, "2 or more fields required for pivot facets");
		for (String fieldname : fieldnames) {
			Assert.hasText(fieldname, "Fieldnames must not contain null/empty values");
		}

		this.facetOnPivotFields.add(new SimplePivotField(fieldnames));
		return this;
	}

	/**
	 * Append all fieldnames for faceting
	 * 
	 * @param fieldnames
	 * @return
	 */
	public final FacetOptions addFacetOnFlieldnames(Collection<String> fieldnames) {
		Assert.notNull(fieldnames, "Fieldnames must not be null!");

		for (String fieldname : fieldnames) {
			addFacetOnField(fieldname);
		}
		return this;
	}

	/**
	 * Append {@code facet.query}
	 * 
	 * @param query
	 * @return
	 */
	public final FacetOptions addFacetQuery(SolrDataQuery query) {
		Assert.notNull(query, "Facet Query must not be null.");

		this.facetQueries.add(query);
		return this;
	}

	/**
	 * Get the list of facetQueries
	 * 
	 * @return
	 */
	public List<SolrDataQuery> getFacetQueries() {
		return Collections.unmodifiableList(this.facetQueries);
	}

	/**
	 * Set minimum number of hits {@code facet.mincount} for result to be included in response
	 * 
	 * @param minCount Default is 1
	 * @return
	 */
	public FacetOptions setFacetMinCount(int minCount) {
		this.facetMinCount = Math.max(0, minCount);
		return this;
	}

	/**
	 * Set {@code facet.limit}
	 * 
	 * @param rowsToReturn Default is 10
	 * @return
	 */
	public FacetOptions setFacetLimit(int rowsToReturn) {
		this.facetLimit = rowsToReturn;
		return this;
	}

	/**
	 * Set {@code facet.sort} ({@code INDEX} or {@code COUNT})
	 * 
	 * @param facetSort Default is {@code COUNT}
	 * @return
	 */
	public FacetOptions setFacetSort(FacetSort facetSort) {
		Assert.notNull(facetSort, "FacetSort must not be null.");

		this.facetSort = facetSort;
		return this;
	}

	/**
	 * Get the list of Fields to facet on
	 * 
	 * @return
	 */
	public final List<Field> getFacetOnFields() {
		return Collections.unmodifiableList(this.facetOnFields);
	}

	/**
	 * Get the list of pivot Fields to face on
	 * 
	 * @return
	 */
	public final List<PivotField> getFacetOnPivots() {
		return Collections.unmodifiableList(facetOnPivotFields);
	}

	/**
	 * get the min number of hits a result has to have to get listed in result. Default is 1. Zero is not recommended.
	 * 
	 * @return
	 */
	public int getFacetMinCount() {
		return this.facetMinCount;
	}

	/**
	 * Get the max number of results per facet field.
	 * 
	 * @return
	 */
	public int getFacetLimit() {
		return this.facetLimit;
	}

	/**
	 * Get sorting of facet results. Default is COUNT
	 * 
	 * @return
	 */
	public FacetSort getFacetSort() {
		return this.facetSort;
	}

	/**
	 * Get the facet page requested.
	 * 
	 * @return
	 */
	public Pageable getPageable() {
		return this.pageable != null ? this.pageable : new SolrPageRequest(0, facetLimit);
	}

	/**
	 * Set {@code facet.offet} and {@code facet.limit}
	 * 
	 * @param pageable
	 * @return
	 */
	public FacetOptions setPageable(Pageable pageable) {
		this.pageable = pageable;
		return this;
	}

	/**
	 * get value used for {@code facet.prefix}
	 * 
	 * @return
	 */
	public String getFacetPrefix() {
		return facetPrefix;
	}

	/**
	 * Set {@code facet.prefix}
	 * 
	 * @param facetPrefix
	 * @return
	 */
	public FacetOptions setFacetPrefix(String facetPrefix) {
		this.facetPrefix = facetPrefix;
		return this;
	}

	/**
	 * @return true if at least one facet field set
	 */
	public boolean hasFields() {
		return !this.facetOnFields.isEmpty() || !this.facetOnPivotFields.isEmpty();
	}

	/**
	 * @return true if filter queries applied for faceting
	 */
	public boolean hasFacetQueries() {
		return !this.facetQueries.isEmpty();
	}

	/**
	 * @return true if pivot facets apply fo faceting
	 */
	public boolean hasPivotFields() {
		return !facetOnPivotFields.isEmpty();
	}

	private boolean hasFacetRages() {
		return !facetRangeOnFields.isEmpty();
	}

	/**
	 * @return true if any {@code facet.field} or {@code facet.query} set
	 */
	public boolean hasFacets() {
		return hasFields() || hasFacetQueries() || hasPivotFields() || hasFacetRages();
	}

	/**
	 * @return true if non empty prefix available
	 */
	public boolean hasFacetPrefix() {
		return StringUtils.hasText(this.facetPrefix);
	}

	@SuppressWarnings("unchecked")
	public Collection<FieldWithFacetParameters> getFieldsWithParameters() {

		List<FieldWithFacetParameters> result = new ArrayList<FieldWithFacetParameters>();

		for (Field candidate : facetOnFields) {
			if (candidate instanceof FieldWithFacetParameters) {
				result.add((FieldWithFacetParameters) candidate);
			}

		}
		return result;
	}

	public static class FacetParameter extends QueryParameterImpl {

		public FacetParameter(String parameter, Object value) {
			super(parameter, value);
		}

	}

	public static class FieldWithFacetParameters extends FieldWithQueryParameters<FacetParameter> {

		private FacetSort sort;

		public FieldWithFacetParameters(String name) {
			super(name);
		}

		/**
		 * @param prefix
		 */
		public FieldWithFacetParameters setPrefix(String prefix) {
			addFacetParameter(FacetParams.FACET_PREFIX, prefix);
			return this;
		}

		/**
		 * @return null if not set
		 */
		public String getPrefix() {
			return getQueryParameterValue(FacetParams.FACET_PREFIX);
		}

		/**
		 * @param sort
		 */
		public FieldWithFacetParameters setSort(FacetSort sort) {
			this.sort = sort;
			return this;
		}

		/**
		 * @return null if not set
		 */
		public FacetSort getSort() {
			return this.sort;
		}

		/**
		 * @param limit
		 */
		public FieldWithFacetParameters setLimit(Integer limit) {
			addFacetParameter(FacetParams.FACET_LIMIT, Math.max(0, limit), true);
			return this;
		}

		/**
		 * @return null if not set
		 */
		public Integer getLimit() {
			return getQueryParameterValue(FacetParams.FACET_LIMIT);
		}

		/**
		 * @param offset
		 */
		public FieldWithFacetParameters setOffset(Integer offset) {
			addFacetParameter(FacetParams.FACET_OFFSET, offset, true);
			return this;
		}

		/**
		 * @return null if not set
		 */
		public Integer getOffset() {
			return getQueryParameterValue(FacetParams.FACET_OFFSET);
		}

		/**
		 * @param minCount
		 */
		public FieldWithFacetParameters setMinCount(Integer minCount) {
			addFacetParameter(FacetParams.FACET_MINCOUNT, Math.max(0, minCount), true);
			return this;
		}

		/**
		 * @return null if not set
		 */
		public Integer getMinCount() {
			return getQueryParameterValue(FacetParams.FACET_MINCOUNT);
		}

		/**
		 * @param missing
		 * @return
		 */
		public FieldWithFacetParameters setMissing(Boolean missing) {
			addFacetParameter(FacetParams.FACET_MISSING, missing, true);
			return this;
		}

		/**
		 * @return null if not set
		 */
		public Boolean getMissing() {
			return getQueryParameterValue(FacetParams.FACET_MISSING);
		}

		/**
		 * @param method
		 * @return
		 */
		public FieldWithFacetParameters setMethod(String method) {
			addFacetParameter(FacetParams.FACET_METHOD, method, true);
			return this;
		}

		/**
		 * @return null if not set
		 */
		public String getMethod() {
			return getQueryParameterValue(FacetParams.FACET_METHOD);
		}

		/**
		 * Add field specific parameter by name
		 * 
		 * @param parameterName
		 * @param value
		 */
		public FieldWithFacetParameters addFacetParameter(String parameterName, Object value) {
			return addFacetParameter(parameterName, value, false);
		}

		protected FieldWithFacetParameters addFacetParameter(String parameterName, Object value,
				boolean removeIfValueIsNull) {
			if (removeIfValueIsNull && value == null) {
				removeQueryParameter(parameterName);
				return this;
			}
			return this.addFacetParameter(new FacetParameter(parameterName, value));
		}

		/**
		 * Add field specific facet parameter
		 * 
		 * @param parameter
		 * @return
		 */
		public FieldWithFacetParameters addFacetParameter(FacetParameter parameter) {
			this.addQueryParameter(parameter);
			return this;
		}

	}

	/**
	 * @return
	 * @since 1.5
	 */
	public Collection<FieldWithRangeParameters<?, ?, ?>> getFieldsWithRangeParameters() {
		return Collections.unmodifiableCollection(facetRangeOnFields);
	}

	/**
	 * Class representing common facet range parameters.
	 * 
	 * @author Francisco Spaeth
	 * @param <T> range field implementation type
	 * @param <R> type of range
	 * @param <G> type of gap
	 * @since 1.5
	 */
	public abstract static class FieldWithRangeParameters<T extends FieldWithRangeParameters<?, ?, ?>, R, G>
			extends FieldWithQueryParameters<FacetParameter> {

		/**
		 * @param name field name
		 * @param start range facet start
		 * @param end range facet end
		 * @param gap gap to be used for faceting between start and end
		 */
		public FieldWithRangeParameters(String name, R start, R end, G gap) {
			super(name);

			Assert.notNull(start, "date range facet start must not be null for field " + name);
			Assert.notNull(end, "date range facet end must not be null for field " + name);
			Assert.notNull(gap, "date range facet gap must not be null for field" + gap);

			addFacetRangeParameter(FacetParams.FACET_RANGE, name);
			addFacetRangeParameter(FacetParams.FACET_RANGE_START, start);
			addFacetRangeParameter(FacetParams.FACET_RANGE_END, end);
			addFacetRangeParameter(FacetParams.FACET_RANGE_GAP, gap);
		}

		/**
		 * Defines if the last range should be abruptly ended even if the end doesn't satisfies: (start - end) % gap = 0.
		 * 
		 * @param rangeHardEnd whenever <code>false</code> will expect to have the last range with the same size as the
		 *          other ranges entries for the query, otherwise (<code>true</code>), may present the last range smaller
		 *          than the other range entries.
		 * @return this
		 * @see FacetParams#FACET_RANGE_HARD_END
		 */
		@SuppressWarnings("unchecked")
		public T setHardEnd(Boolean rangeHardEnd) {
			addFacetRangeParameter(FacetParams.FACET_RANGE_HARD_END, rangeHardEnd);
			return (T) this;
		}

		/**
		 * If the last range should be abruptly ended even if the end doesn't satisfies: (start - end) % gap = 0.
		 * 
		 * @return if hard end should be used, <code>null</code> will be returned if not set
		 * @see FacetParams#FACET_RANGE_HARD_END
		 */
		public Boolean getHardEnd() {
			return getQueryParameterValue(FacetParams.FACET_RANGE_HARD_END);
		}

		/**
		 * Defines the additional (other) counts for the range facet, i.e. count of documents that are before start of the
		 * range facet, end of range facet or even between start and end.
		 * 
		 * @param rangeOther which other counts shall be added to the facet result
		 * @return this
		 * @see FacetParams.FACET_RANGE_OTHER
		 */
		@SuppressWarnings("unchecked")
		public T setOther(FacetParams.FacetRangeOther rangeOther) {
			addFacetRangeParameter(FacetParams.FACET_RANGE_OTHER, rangeOther);
			return (T) this;
		}

		/**
		 * The definition of additional (other) counts for the range facet.
		 * 
		 * @return null which other counts shall be added to the facet result
		 * @see FacetParams.FACET_RANGE_OTHER
		 */
		public FacetRangeOther getOther() {
			return getQueryParameterValue(FacetParams.FACET_RANGE_OTHER);
		}

		/**
		 * Defines how boundaries (lower and upper) shall be handled (exclusive or inclusive) on range facet requests.
		 * 
		 * @param rangeInclude include option for range
		 * @return this
		 * @see FacetParams.FACET_RANGE_INCLUDE
		 */
		@SuppressWarnings("unchecked")
		public T setInclude(FacetParams.FacetRangeInclude rangeInclude) {
			addFacetRangeParameter(FacetParams.FACET_RANGE_INCLUDE, rangeInclude);
			return (T) this;
		}

		/**
		 * The definition of how boundaries (lower and upper) shall be handled (exclusive or inclusive) on range facet
		 * requests.
		 * 
		 * @return null if not set
		 * @see FacetParams.FACET_RANGE_INCLUDE
		 */
		public FacetRangeInclude getInclude() {
			return getQueryParameterValue(FacetParams.FACET_RANGE_INCLUDE);
		}

		@SuppressWarnings("unchecked")
		protected T addFacetRangeParameter(String parameterName, Object value) {
			if (value == null) {
				removeQueryParameter(parameterName);
			} else {
				addQueryParameter(new FacetParameter(parameterName, value));
			}
			return (T) this;
		}

		/**
		 * The size of the range to be added to the lower bound.
		 * 
		 * @return size of each range.
		 * @see FacetParams#FACET_RANGE_GAP
		 */
		public G getGap() {
			return getQueryParameterValue(FacetParams.FACET_RANGE_GAP);
		}

		/**
		 * Start value configured for this field range facet.
		 * 
		 * @return upper bound for the ranges.
		 * @see FacetParams#FACET_RANGE_START
		 */
		public R getStart() {
			return getQueryParameterValue(FacetParams.FACET_RANGE_START);
		}

		/**
		 * @return lower bound for the ranges.
		 * @see FacetParams#FACET_RANGE_END
		 */
		public R getEnd() {
			return getQueryParameterValue(FacetParams.FACET_RANGE_END);
		}

	}

	/**
	 * Class representing date field specific facet range parameters
	 * 
	 * @author Francisco Spaeth
	 * @since 1.5
	 */
	public static class FieldWithDateRangeParameters
			extends FieldWithRangeParameters<FieldWithDateRangeParameters, Date, String> {

		public FieldWithDateRangeParameters(String name, Date start, Date end, String gap) {
			super(name, start, end, gap);
		}

	}

	/**
	 * Class representing numeric field specific facet range parameters
	 * 
	 * @author Francisco Spaeth
	 * @since 1.5
	 */
	public static class FieldWithNumericRangeParameters
			extends FieldWithRangeParameters<FieldWithNumericRangeParameters, Number, Number> {

		public FieldWithNumericRangeParameters(String name, Number start, Number end, Number gap) {
			super(name, start, end, gap);
		}

	}

}
