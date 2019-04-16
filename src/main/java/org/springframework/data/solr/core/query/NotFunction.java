/*
 * Copyright 2012 - 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.query;

import java.util.Collections;

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
		super(Collections.singletonList(condition));
	}

	/**
	 * @param field must not be null
	 * @return
	 */
	public static NotFunction not(Field field) {

		Assert.notNull(field, "Field for not function must not be 'null'");
		return new NotFunction(field);
	}

	/**
	 * @param fieldName must not be empty
	 * @return
	 */
	public static NotFunction not(String fieldName) {

		Assert.notNull(fieldName, "FieldName must not be null!");
		return not(new SimpleField(fieldName));
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
