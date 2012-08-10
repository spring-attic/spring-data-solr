/*
 * Copyright (C) 2012 j73x73r@gmail.com.
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
package at.pagu.data.solr.example.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.repository.CrudRepository;

import at.pagu.data.solr.example.model.Product;
import at.pagu.soldockr.core.query.result.FacetPage;
import at.pagu.soldockr.repository.Query;

public interface ProductRepository extends CrudRepository<Product, String> {

  @Query(Product.POPULARITY_FIELD + ":?0") // Query annotation will be resolved in v.0.2.0
  Page<Product> findByPopularity(Integer popularity);

  FacetPage<Product> findByNameStartingWithAndFacetOnAvailable(String namePrefix);

  Page<Product> findByAvailableTrue();

}
