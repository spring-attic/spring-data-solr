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
package org.springframework.data.solr.core.query;

import java.util.Arrays;

import org.springframework.util.Assert;

/**
 * Implementation of {@code query(x)}
 * 
 * @author Christoph Strobl
 * @since 1.1
 */
public class QueryFunction extends AbstractFunction {

	private static final String OPERATION = "query";

	private QueryFunction(Query query) {
		super(Arrays.asList(query));
	}

	/**
	 * @param query
	 * @return
	 */
	public static QueryFunction query(Query query) {
		Assert.notNull(query, "Cannot create query function for 'null' query.");

		return new QueryFunction(query);
	}

	/**
	 * @param queryString
	 * @return
	 */
	public static QueryFunction query(String queryString) {
		Assert.hasText(queryString, "Cannot create query function for 'empty' queryString.");

		return query(new SimpleStringCriteria(queryString));
	}

	/**
	 * @param criteria
	 * @return
	 */
	public static QueryFunction query(Criteria criteria) {
		Assert.notNull(criteria, "Cannot create query function for 'null' criteria.");

		return query(new SimpleQuery(criteria));
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

}
