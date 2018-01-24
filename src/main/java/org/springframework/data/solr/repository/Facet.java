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
package org.springframework.data.solr.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Facet {

	/**
	 * {@code facet.field} fields to facet on
	 * 
	 * @return
	 */
	String[] fields() default {};

	/**
	 * {@code facet.query} queries to facet on
	 * 
	 * @return
	 */
	String[] queries() default {};

	/**
	 * {@code facet.mincount} minimum number of hits for result to be included in response
	 * 
	 * @return
	 */
	int minCount() default 1;

	/**
	 * {@code facet.limit} limit number results returned
	 * 
	 * @return
	 */
	int limit() default 10;

	/**
	 * {@code facet.prefix}
	 * 
	 * @return prefix
	 */
	String prefix() default "";

	/**
	 * {@code facet.pivot} fields to pivot on
	 * 
	 * @return
	 * @since 1.2
	 */
	Pivot[] pivots() default {};

	/**
	 * {@code facet.pivot.mincount} minimum number of hits for result to be included in pivot response
	 * 
	 * @return
	 */
	int pivotMinCount() default 1;

}
