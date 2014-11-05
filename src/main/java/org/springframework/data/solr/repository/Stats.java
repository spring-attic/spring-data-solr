/*
 * Copyright 2014 the original author or authors.
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
 * Statistics definition to be performed within a {@link Query}.
 * 
 * @author Francisco Spaeth
 * @since 1.4
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Stats {

	/**
	 * @return fields that shall have its statistics returned.
	 */
	public String[] value() default {};

	/**
	 * @return faceting that shall be returned within statistics result.
	 */
	public String[] facets() default {};

	/**
	 * @return if distinct elements shall be calculated
	 */
	public boolean distinct() default false;

	/**
	 * @return field selective stats parameters.
	 */
	public SelectiveStats[] selective() default {};

}
