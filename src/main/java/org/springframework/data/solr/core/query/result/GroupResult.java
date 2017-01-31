/*
 * Copyright 2012 - 2018 the original author or authors.
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

import org.springframework.data.domain.Page;

/**
 * Representation of a group in response to group request (i.e. , , ) will have a {@link GroupResult} representation.
 *
 * @author Francisco Spaeth
 * @param <T> grouped content type
 * @since 1.4
 */
public interface GroupResult<T> {

	/**
	 * Matched documents for this group.
	 *
	 * @return
	 */
	int getMatches();

	/**
	 * Groups count.
	 *
	 * @return
	 */
	Integer getGroupsCount();

	/**
	 * Grouping result name.
	 *
	 * @return
	 */
	String getName();

	/**
	 * Group entries.
	 *
	 * @see GroupEntry
	 * @return
	 */
	Page<GroupEntry<T>> getGroupEntries();

}
