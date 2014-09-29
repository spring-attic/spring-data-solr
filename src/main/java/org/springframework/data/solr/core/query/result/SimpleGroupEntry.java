/*
 * Copyright 2012 - 2014 the original author or authors.
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
 * Represents a group holding the group value and all beans belonging to the group.
 * 
 * @author Francisco Spaeth
 * @param <T>
 * @since 1.4
 */
public class SimpleGroupEntry<T> implements GroupEntry<T> {

	private String groupValue;
	private Page<T> result;

	public SimpleGroupEntry(String groupValue, Page<T> result) {
		super();
		this.groupValue = groupValue;
		this.result = result;
	}

	@Override
	public String getGroupValue() {
		return groupValue;
	}

	@Override
	public Page<T> getResult() {
		return result;
	}

	@Override
	public String toString() {
		return "SimpleGroupEntry [groupValue=" + groupValue + ", result=" + result + "]";
	}

}
