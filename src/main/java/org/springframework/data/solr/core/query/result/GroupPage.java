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
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.Function;
import org.springframework.data.solr.core.query.Query;

/**
 * Representation of a Group result page, holding one {@link GroupResult} for each grouping requested on a
 * {@link org.springframework.data.solr.core.query.GroupQuery}.
 * 
 * @author Francisco Spaeth
 * @param <T>
 * @since 1.4
 */
public interface GroupPage<T> extends Page<T> {

	/**
	 * Get a group result done for the given {@link Field}.
	 * 
	 * @param field
	 * @return
	 */
	public GroupResult<T> getGroupResult(Field field);

	/**
	 * Get a group result done for the given {@link Function}.
	 * 
	 * @param function
	 * @return
	 */
	public GroupResult<T> getGroupResult(Function function);

	/**
	 * Get a group result done for the given {@link Query}.
	 * 
	 * @param query
	 * @return
	 */
	public GroupResult<T> getGroupResult(Query query);

	/**
	 * Get a group result with the given name.
	 * 
	 * @param name
	 * @return
	 */
	public GroupResult<T> getGroupResult(String name);

}
