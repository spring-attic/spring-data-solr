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
import org.springframework.util.Assert;

/**
 * This represents the result of a group command. This can be the result of the following parameter: field, function or
 * query.
 *
 * @author Francisco Spaeth
 * @param <T>
 * @since 1.4
 */
public class SimpleGroupResult<T> implements GroupResult<T> {

	private int matches;
	private Integer groupsCount;
	private String name;
	private Page<GroupEntry<T>> groupEntries;

	public SimpleGroupResult(int matches, Integer groupsCount, String name, Page<GroupEntry<T>> groupEntries) {
		Assert.isTrue(matches >= 0, "matches must be >= 0");
		Assert.hasLength(name, "group result name must be not empty");
		Assert.notNull(groupEntries, "groupEntries must be not null");
		this.matches = matches;
		this.groupsCount = groupsCount;
		this.name = name;
		this.groupEntries = groupEntries;
	}

	@Override
	public int getMatches() {
		return matches;
	}

	@Override
	public Integer getGroupsCount() {
		return groupsCount;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Page<GroupEntry<T>> getGroupEntries() {
		return groupEntries;
	}

	@Override
	public String toString() {
		return //
				"SimpleGroupResult [name=" + name + //
						", matches=" + matches + //
						", groupsCount=" + groupsCount + //
						", groupsEntries.total=" + groupEntries.getTotalElements() + //
						"]";
	}

}
