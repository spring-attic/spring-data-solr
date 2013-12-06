/*
 * Copyright 2012 - 2013 the original author or authors.
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
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.solr.common.params.FacetParams;
import org.springframework.data.domain.PageRequest;
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
	private List<SolrDataQuery> facetQueries = new ArrayList<SolrDataQuery>(0);
	private int facetMinCount = DEFAULT_FACET_MIN_COUNT;
	private int facetLimit = DEFAULT_FACET_LIMIT;
	private String facetPrefix;
	private FacetSort facetSort = DEFAULT_FACET_SORT;
	private Pageable pageable;

	public FacetOptions() {
	}

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
	 * 
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
		Assert.notNull(fieldnames);

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
		this.facetLimit = Math.max(1, rowsToReturn);
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
		return this.pageable != null ? this.pageable : new PageRequest(0, facetLimit);
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

	/**
	 * @return true if any {@code facet.field} or {@code facet.query} set
	 */
	public boolean hasFacets() {
		return hasFields() || hasFacetQueries() || hasPivotFields();
	}

	/**
	 * @return true if non empty prefix available
	 */
	public boolean hasFacetPrefix() {
		return StringUtils.hasText(this.facetPrefix);
	}

	@SuppressWarnings("unchecked")
	public Collection<FieldWithFacetParameters> getFieldsWithParameters() {
		return (Collection<FieldWithFacetParameters>) CollectionUtils.select(this.facetOnFields,
				new IsFieldWithFacetParametersInstancePredicate());
	}

	private static class IsFieldWithFacetParametersInstancePredicate implements Predicate {

		@Override
		public boolean evaluate(Object object) {
			return object instanceof FieldWithFacetParameters;
		}
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

		protected FieldWithFacetParameters addFacetParameter(String parameterName, Object value, boolean removeIfValueIsNull) {
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

}
