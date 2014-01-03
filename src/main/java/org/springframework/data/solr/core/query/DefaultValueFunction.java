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
 * Implementation of {@code def(field|function,defaultValue)}
 * 
 * @author Christoph Strobl
 * @since 1.1
 */
public class DefaultValueFunction extends AbstractFunction {

	private static final String OPERATION = "def";

	private DefaultValueFunction(Object fieldName, Object defaultValue) {
		super(Arrays.asList(fieldName, defaultValue));
	}

	/**
	 * Creates new {@link DefaultValueFunction} representing {@code def(fieldname, defaultValue))}
	 * 
	 * @param fieldName must not be empty
	 * @param defaultValue must not be null
	 * @return
	 */
	public static DefaultValueFunction defaultValue(String fieldName, Object defaultValue) {
		Assert.hasText(fieldName, "Fieldname must not be 'empty' for default value operation.");
		Assert.notNull(defaultValue, "DefaultValue must not be 'null'.");

		return new DefaultValueFunction(fieldName, defaultValue);
	}

	/**
	 * Creates new {@link DefaultValueFunction} representing {@code def(field.getName(), defaultValue))}
	 * 
	 * @param field must not be null
	 * @param defaultValue must not be null
	 * @return
	 */
	public static DefaultValueFunction defaultValue(Field field, Object defaultValue) {
		Assert.notNull(field, "Field must not be 'null' for default value operation.");

		return defaultValue(field.getName(), defaultValue);
	}

	/**
	 * Creates new {@link DefaultValueFunction} representing {@code def(function, defaultValue))}
	 * 
	 * @param function must not be null
	 * @param defaultValue must not be null
	 * @return
	 */
	public static DefaultValueFunction defaultValue(Function function, Object defaultValue) {
		Assert.notNull(function, "Function must not be 'null' for default value operation.");
		Assert.notNull(defaultValue, "DefaultValue must not be 'null'.");

		return new DefaultValueFunction(function, defaultValue);
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

}
