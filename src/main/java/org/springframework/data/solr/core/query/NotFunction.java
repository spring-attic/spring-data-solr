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
 * Implementation of {@code not(field|function)}
 * 
 * @author Christoph Strobl
 * @since 1.1
 */
public class NotFunction extends AbstractFunction {

	private static final String OPERATION = "not";

	private NotFunction(Object condition) {
		super(Arrays.asList(condition));
	}

	/**
	 * @param field must not be null
	 * @return
	 */
	public static NotFunction not(Field field) {
		Assert.notNull(field, "Field for not function must not be 'null'");

		return not(field.getName());
	}

	/**
	 * @param fieldname must not be empty
	 * @return
	 */
	public static NotFunction not(String fieldname) {
		Assert.hasText(fieldname, "Fieldname for not function must not be 'empty'.");

		return new NotFunction(fieldname);
	}

	/**
	 * @param condition must not be null
	 * @return
	 */
	public static NotFunction not(Function condition) {
		Assert.notNull(condition, "Condition for not function must not be 'null'");

		return new NotFunction(condition);
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}
}
