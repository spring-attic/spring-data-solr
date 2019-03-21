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
 * Implementation of {@code exists(field|function)}
 *
 * @author Christoph Strobl
 * @since 1.1
 */
public class ExistsFunction extends AbstractFunction {

	private static final String OPERATION = "exists";

	private ExistsFunction(Object candidate) {
		super(Collections.singletonList(candidate));
	}

	/**
	 * Creates new {@link ExistsFunction} representing {@code exists(field)}
	 *
	 * @param field
	 * @return
	 */
	public static ExistsFunction exists(Field field) {

		Assert.notNull(field, "Field cannot be 'null' for exists operation.");
		return new ExistsFunction(field);
	}

	/**
	 * Creates new {@link ExistsFunction} representing {@code exists(fieldname)}
	 *
	 * @param fieldName
	 * @return
	 */
	public static ExistsFunction exists(String fieldName) {

		Assert.hasText(fieldName, "fieldName cannot be 'empty' for exists operation.");
		return exists(new SimpleField(fieldName));
	}

	/**
	 * Creates new {@link ExistsFunction} representing {@code exists(function())}
	 *
	 * @param function
	 * @return
	 */
	public static ExistsFunction exists(Function function) {

		Assert.notNull(function, "Function cannot be 'null' for exists operation.");
		return new ExistsFunction(function);
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

}
