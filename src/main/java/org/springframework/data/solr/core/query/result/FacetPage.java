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
package org.springframework.data.solr.core.query.result;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.PivotField;

/**
 * FacetPage holds a page for each field targeted by the facet query as well as the page values returned by facet.query
 * 
 * @param <T>
 * 
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public interface FacetPage<T> extends Page<T> {

	/**
	 * Get Facet results for field with given name
	 * 
	 * @param fieldname must not be null
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
