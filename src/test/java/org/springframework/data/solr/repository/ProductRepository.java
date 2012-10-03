/*
 * Copyright 2012 the original author or authors.
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
import java.util.List;

/**
 * @author Christoph Strobl
 */
public interface ProductRepository extends SolrCrudRepository<ProductBean, String> {

	List<ProductBean> findByNamedQuery(Integer popularity);
	
	List<ProductBean> findByName(String name);
	
	ProductBean findById(String id);
	
	List<ProductBean> findByAvailableTrue();
	
	List<ProductBean> findByAvailableFalse();
	
	@Query("inStock:?0")
	List<ProductBean> findByAvailableUsingQueryAnnotation(boolean available);
	
	List<ProductBean> findByPopularityBetween(Integer low, Integer up);
	
	List<ProductBean> findByPopularityLessThan(Integer up);
	
	List<ProductBean> findByPopularityGreaterThan(Integer low);

	List<ProductBean> findByNameLike(String name);
	
	List<ProductBean> findByNameStartsWith(String name);
	
	List<ProductBean> findByPopularityIn(Collection<Integer> popularities);
	
	List<ProductBean> findByPopularityAndAvailableTrue(Integer popularity);
}