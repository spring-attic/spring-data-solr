/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.repository;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @param <T>
 * @param <ID>
 * @author Christoph Strobl
 * @author Mayank Kumar
 */
public interface SolrCrudRepository<T, ID extends Serializable>
		extends SolrRepository<T, ID>, PagingAndSortingRepository<T, ID> {

	Optional<T> findById(ID id);

	Iterable<T> findAll();

	Iterable<T> findAllById(Iterable<ID> ids);

	<S extends T> S save(S entity);

	/**
	 * Saves a given entity and commits withing given {@link Duration}.
	 *
	 * @param entity must not be {@literal null}.
	 * @return the saved entity will never be {@literal null}.
	 * @since 4.0
	 */
	<S extends T> S save(S entity, Duration commitWithin);

	<S extends T> Iterable<S> saveAll(Iterable<S> entities);

	/**
	 * Saves all given entities and commits withing given {@link Duration}.
	 *
	 * @param entities must not be {@literal null}.
	 * @return the saved entity will never be {@literal null}.
	 * @since 4.0
	 */
	<S extends T> Iterable<S> saveAll(Iterable<S> entities, Duration commitWithin);

	boolean existsById(ID id);

	void deleteById(ID id);

	void delete(T entity);

	void deleteAll(Iterable<? extends T> entities);

	void deleteAll();
}
