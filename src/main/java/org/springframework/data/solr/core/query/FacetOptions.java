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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Set of options that can be set on a {@link FacetQuery}
 * 
 * @author Christoph Strobl
 */
public class FacetOptions {

	public static final int DEFAULT_FACET_MIN_COUNT = 1;
	public static final int DEFAULT_FACET_LIMIT = 10;
	public static final FacetSort DEFAULT_FACET_SORT = FacetSort.COUNT;

	public enum FacetSort {
		COUNT, INDEX
	}

	private List<Field> facetOnFields = new ArrayList<Field>(1);
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
	 * @param filterQuery
	 * @return
	 */
	public final FacetOptions addFacetQuery(SolrDataQuery facetQuery) {
		Assert.notNull(facetQuery, "Facet Query must not be null.");

		this.facetQueries.add(facetQuery);
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
		this.facetMinCount = java.lang.Math.max(0, minCount);
		return this;
	}

	/**
	 * Set {@code facet.limit}
	 * 
	 * @param rowsToReturn Default is 10
	 * @return
	 */
	public FacetOptions setFacetLimit(int rowsToReturn) {
		this.facetLimit = java.lang.Math.max(1, rowsToReturn);
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
	 * true if at least one facet field set
	 * 
	 * @return
	 */
	public boolean hasFields() {
		return !this.facetOnFields.isEmpty();
	}

	/**
	 * true if filter queries applied for faceting
	 * 
	 * @return
	 */
	public boolean hasFacetQueries() {
		return !this.facetQueries.isEmpty();
	}

	/**
	 * @return true if either {@code facet.field} or {@code facet.query} set
	 */
	public boolean hasFacets() {
		return hasFields() || hasFacetQueries();
	}

	/**
	 * @return true if non empty prefix available
	 */
	public boolean hasFacetPrefix() {
		return StringUtils.hasText(this.facetPrefix);
	}

	@SuppressWarnings("unchecked")
	public Collection<FieldWithFacetPrefix> getFieldsWithPrefix() {
		return (Collection<FieldWithFacetPrefix>) CollectionUtils.select(this.facetOnFields,
				new IsFieldWithFacetPrefixInstancePredicate());
	}

	private static class IsFieldWithFacetPrefixInstancePredicate implements Predicate {

		@Override
		public boolean evaluate(Object object) {
			return object instanceof FieldWithFacetPrefix;
		}
	}

}
