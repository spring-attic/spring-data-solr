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
package org.springframework.data.solr.core.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.solr.client.solrj.beans.Field;

/**
 * @author Christoph Strobl
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Indexed {

	/**
	 * if set to false, field will not be transfered to solr, but can be read from there
	 * 
	 * @return
	 */
	boolean readonly() default false;

	/**
	 * @return
	 * @since 1.3
	 */
	boolean stored() default true;

	/**
	 * @return
	 * @since 1.3
	 */
	boolean searchable() default true;

	/**
	 * @return
	 * @since 1.3
	 */
	String type() default "";

	/**
	 * @return
	 * @since 1.3
	 */
	String[] copyTo() default {};

	/**
	 * @return
	 * @since 1.3
	 */
	String defaultValue() default "";

	/**
	 * @return
	 * @since 1.3
	 */
	boolean required() default false;

	/**
	 * If not set the fields name or the one defined via {@link Field} will be used.
	 * 
	 * @return
	 * @since 1.3
	 * @see Indexed#value()
	 */
	String name() default "";

	/**
	 * if not set the fields name or the one defined via {@link Field} will be used
	 * 
	 * @return
	 */
	String value() default "";

	/**
	 * Boost Field by value. Default is {@code Float.NaN}.
	 * 
	 * @return
	 * @since 1.2
	 */
	float boost() default Float.NaN;

}
