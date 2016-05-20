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

import java.util.List;

import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;

/**
 * Hold the results of a solr highlight query.
 *
 * @param <T>
 * @author David Webb
 * @since 2.1
 */
public interface HighlightQueryResult<T> {

	/**
	 * @return empty list of not set
	 */
	List<HighlightEntry<T>> getHighlighted();

	/**
	 * @param entity
	 * @return empty list if none found
	 */
	List<Highlight> getHighlights(T entity);

}
