/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.solr.repository;

/**
 * Definition of field selective statistical that shall be executed within the request.
 * 
 * @author Francisco Spaeth
 * @since 1.4
 */
public @interface SelectiveStats {

	/**
	 * @return field name to which the selective stats are associated to
	 */
	String field();

	/**
	 * @return fields to be facet within this stats field request
	 */
	String[] facets();

	/**
	 * @return if distinct elements for this field shall be calculated
	 */
	boolean distinct() default false;

}
