/*
 * Copyright (C) 2012 sol-dock-r authors.
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
package com.google.soldockr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface SolrRepository <T> extends Repository<T, String> {
  
  /**
   * Returns the single instance with matching id
   * 
   * @param id The unique identifier for retrieving the entity
   * @return null if not found
   */
  T findOne(String id);

  /**
   * Returns a {@link Page} of entities meeting the paging restriction
   * provided in the {@code Pageable} object.
   * 
   * @param pageable
   * @return a page of entities
   */
  Page<T> findAll(Pageable pageable);

  /**
   * Returns the number of entities available.
   * 
   * @return the number of entities
   */
  long count();

}
