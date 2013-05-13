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
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Highlight {

	/**
	 * Fieldnames to be used for {@code hl.fl}.
	 * 
	 * @return
	 */
	String[] fields() default {};

	/**
	 * Query to be used for {@code hl.q}
	 * 
	 * @return
	 */
	String query() default "";

	/**
	 * sets {@code hl.fragsize}
	 * 
	 * @return
	 */
	int fragsize() default -1;

	/**
	 * set {@code hl.snipplets}
	 * 
	 * @return
	 */
	int snipplets() default -1;

	/**
	 * set {@code hl.formatter}
	 * 
	 * @return
	 */
	String formatter() default "";

	/**
	 * set {@code hl.simple.pre} in case formatter not set or 'simple' otherwise {@code hl.tag.pre}
	 * 
	 * @return
	 */
	String prefix() default "";

	/**
	 * set {@code hl.simple.post} in case formatter not set or 'simple' otherwise {@code hl.tag.post}
	 * 
	 * @return
	 */
	String postfix() default "";

}
