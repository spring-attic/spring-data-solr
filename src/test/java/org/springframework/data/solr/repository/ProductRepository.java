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
package org.springframework.data.solr.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.geo.BoundingBox;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.data.solr.core.query.result.FacetPage;

/**
 * @author Christoph Strobl
 * @author John Dorman
 * @author Andrey Paramonov
 */
public interface ProductRepository extends SolrCrudRepository<ProductBean, String> {

	List<ProductBean> findByNamedQuery(Integer popularity);

	@Query(name = "ProductBean.findByNamedQueryUsingAvailable")
	List<ProductBean> findByAvailableWithSort(boolean available, Sort sort);

	@Query(name = "ProductBean.findByNamedQueryUsingAvailable")
	Page<ProductBean> findByAvailableWithSort(boolean available, Pageable pagebale);

	List<ProductBean> findByName(String name);

	List<ProductBean> findByNameNot(String name);

	List<ProductBean> findByNameIsNull();

	List<ProductBean> findByNameIsNotNull();

	ProductBean findById(String id);

	List<ProductBean> findByAvailableTrue();

	List<ProductBean> findByAvailableFalse();

	@Query("inStock:?0")
	List<ProductBean> findByAvailableUsingQueryAnnotation(boolean available);

	List<ProductBean> findByPopularityBetween(Integer low, Integer up);

	List<ProductBean> findByLastModifiedBefore(Date date);

	List<ProductBean> findByPopularityLessThan(Integer up);

	List<ProductBean> findByPopularityLessThanEqual(Integer up);

	List<ProductBean> findByLastModifiedAfter(Date date);

	List<ProductBean> findByPopularityGreaterThan(Integer low);

	List<ProductBean> findByPopularityGreaterThanEqual(Integer low);

	List<ProductBean> findByNameLike(String name);

	List<ProductBean> findByNameStartsWith(String name);

	@Query(value = "name:?0*", fields = { "name", "id" })
	List<ProductBean> findByNameStartsWithProjectionOnNameAndId(String name);

	@Query(fields = { "name", "id" })
	List<ProductBean> findByNameStartingWith(String name);

	List<ProductBean> findByPopularityIn(Collection<Integer> popularities);

	List<ProductBean> findByPopularityNotIn(Collection<Integer> popularities);

	List<ProductBean> findByPopularityAndAvailableTrue(Integer popularity);

	List<ProductBean> findByPopularityOrAvailableFalse(Integer popularity);

	List<ProductBean> findByNameStartsWithOrTitleStartsWith(@Boost(2) String name, String title);

	List<ProductBean> findByLocationWithin(GeoLocation location, Distance distance);

	List<ProductBean> findByLocationNear(GeoLocation location, Distance distance);

	List<ProductBean> findByLocationNear(BoundingBox bbox);

	List<ProductBean> findByAvailableTrueOrderByPopularityDesc();

	@Query("inStock:?0")
	List<ProductBean> findByAvailableWithAnnotatedQueryUsingSort(boolean available, Sort sort);

	@Query("inStock:?0")
	Page<ProductBean> findByAvailableWithAnnotatedQueryUsingSortInPageable(boolean available, Pageable page);

	List<ProductBean> findByAvailableTrueOrderByPopularityAsc();

	ProductBean findByNameAndAvailableTrue(String name);

	List<ProductBean> findByNameRegex(String name);

	Page<ProductBean> findByNameStartingWith(String name, Pageable page);

	@Query(value = "*:*")
	@Facet(fields = { "popularity" })
	FacetPage<ProductBean> findAllFacetOnPopularity(Pageable page);

	@Query(value = "*:*")
	@Facet(fields = { "popularity", "inStock" })
	FacetPage<ProductBean> findAllFacetOnPopularityAndAvailable(Pageable page);

	@Query(value = "*:*")
	@Facet(queries = { "popularity:[* TO 3]" })
	FacetPage<ProductBean> findAllFacetQueryPopularity(Pageable page);

	@Query(value = "*:*")
	@Facet(queries = { "popularity:[* TO ?0]" })
	FacetPage<ProductBean> findAllFacetQueryPopularity(Integer popularity, Pageable page);

	@Query(value = "*:*")
	@Facet(queries = { "inStock:true", "inStock:false" })
	FacetPage<ProductBean> findAllFacetQueryAvailableTrueAndAvailableFalse(Pageable page);

	@Query(value = "*:*")
	@Facet(fields = "name", prefix = "pro")
	FacetPage<ProductBean> findAllFacetOnNameWithStaticPrefix(Pageable page);

	@Query(value = "*:*")
	@Facet(fields = "name", prefix = "?0")
	FacetPage<ProductBean> findAllFacetOnNameWithDynamicPrefix(String prefix, Pageable page);

	@Query(value = "*:*", filters = "inStock:true")
	List<ProductBean> findAllFilterAvailableTrue();

	@Query(filters = "inStock:?1")
	List<ProductBean> findByPopularityLessThan(Integer popularity, boolean filterByInStock);

	@Query(value = "*:*", filters = { "inStock:true", "popularity:[* TO 3]" })
	List<ProductBean> findAllFilterAvailableTrueAndPopularityLessThanEqual3();

	@Query(defaultOperator = org.springframework.data.solr.core.query.Query.Operator.AND)
	List<ProductBean> findByAvailableIn(List<Boolean> values);

	@Query(value = "inStock:(?0)", defaultOperator = org.springframework.data.solr.core.query.Query.Operator.OR)
	List<ProductBean> findByAvailableInWithOrOperator(List<Boolean> values);

	@Query(value = "*:*", timeAllowed = 250)
	List<ProductBean> findAllWithExecutiontimeRestriction();

	@Query(defType = "lucene")
	List<ProductBean> findByNameIn(Collection<String> name);

	@Query(requestHandler = "/instock")
	List<ProductBean> findByText(String text);

}