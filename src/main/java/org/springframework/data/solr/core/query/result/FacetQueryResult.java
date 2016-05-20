/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.solr.core.query.result;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.PivotField;

/**
 * Hold the results of a solr facet query.
 *
 * @param <T>
 * @author David Webb
 * @since 2.1.0
 */
public interface FacetQueryResult<T> {

	/**
	 * Get Facet results for field with given name
	 *
	 * @param fieldname must not be {@literal null}.
	 * @return
	 */
	Page<FacetFieldEntry> getFacetResultPage(String fieldname);

	/**
	 * Get Facet results for field with given field
	 *
	 * @param field
	 * @return
	 */
	Page<FacetFieldEntry> getFacetResultPage(Field field);

	/**
	 * Get Range Facet results for field with given name
	 *
	 * @param fieldname field name (must not be {@literal null})
	 * @return facet range for the field name provided
	 * @since 1.5
	 */
	Page<FacetFieldEntry> getRangeFacetResultPage(String fieldname);

	/**
	 * Get Range Facet results for a given field with given name
	 *
	 * @param field
	 * @return facet range page for the field provided
	 * @since 1.5
	 */
	Page<FacetFieldEntry> getRangeFacetResultPage(Field field);

	/**
	 * Get Facet Pivot results for fields with given fields.
	 *
	 * @param fields pivot field name
	 * @return
	 */
	List<FacetPivotFieldEntry> getPivot(String fieldName);

	/**
	 * Get Facet Pivot results for fields with given fields.
	 *
	 * @param fields pivot field
	 * @return
	 */
	List<FacetPivotFieldEntry> getPivot(PivotField field);

	/**
	 * @return Collection holding faceting result pages
	 */
	Collection<Page<FacetFieldEntry>> getFacetResultPages();

	/**
	 * @return empty collection if not set
	 */
	Page<FacetQueryEntry> getFacetQueryResult();

	/**
	 * Get Fields contained in Result.
	 *
	 * @return
	 */
	Collection<Field> getFacetFields();

	/**
	 * Get Pivot Fields contained in Result.
	 *
	 * @return
	 */
	Collection<PivotField> getFacetPivotFields();

	/**
	 * @return empty collection if not available
	 */
	Collection<Page<? extends FacetEntry>> getAllFacets();

}
