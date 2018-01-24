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
package org.springframework.data.solr.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Christoph Strobl
 * @author Andrey Paramonov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Query {

	/**
	 * Solr QueryString to be used when executing query. May contain placeholders eg. {@code ?1}
	 *
	 * @return
	 */
	String value() default "";

	/**
	 * Named Query Named looked up by repository.
	 *
	 * @return
	 */
	String name() default "";

	/**
	 * The fields that should be returned from the store.
	 *
	 * @return
	 */
	String[] fields() default {};

	/**
	 * add query to filter results Corresponds to {@code fq}
	 *
	 * @return
	 */
	String[] filters() default {};

	/**
	 * Specifies the default operator {@code q.op}
	 *
	 * @return
	 */
	org.springframework.data.solr.core.query.Query.Operator defaultOperator() default org.springframework.data.solr.core.query.Query.Operator.NONE;

	/**
	 * Specify the default type of the query. E.g. "lucene", "edismax"
	 *
	 * @return
	 */
	String defType() default "";

	/**
	 * Specifies the request handler {@code qt}
	 *
	 * @return
	 */
	String requestHandler() default "";

	/**
	 * The time in milliseconds allowed for a search to finish. Values <= 0 mean no time restriction.
	 *
	 * @return
	 */
	int timeAllowed() default -1;

	/**
	 * If set to {@code true} matching documents will be removed from index.
	 *
	 * @return
	 * @since 1.2
	 */
	boolean delete() default false;

}
