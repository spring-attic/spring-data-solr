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
package org.springframework.data.solr.core.query;

/**
 * The most basic criteria holding an already formatted QueryString that can be executed 'as is' against the solr server
 * 
 * @author Christoph Strobl
 */
public class SimpleStringCriteria extends Criteria implements QueryStringHolder {

	private final String queryString;

	/**
	 * @param queryString
	 */
	public SimpleStringCriteria(String queryString) {
		this.queryString = queryString;
	}

	@Override
	public String getQueryString() {
		return this.queryString;
	}

	@Override
	public String toString() {
		return getQueryString();
	}

}
