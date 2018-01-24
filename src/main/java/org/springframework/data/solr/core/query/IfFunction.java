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
package org.springframework.data.solr.core.query;

import org.springframework.util.Assert;

/**
 * Implementation of {@code if(value|field|function,trueValue,falseValue)}
 *
 * @author Christoph Strobl
 * @since 1.1
 */
public class IfFunction extends AbstractFunction {

	private static final String OPERATION = "if";

	private IfFunction() {
		super();
	}

	/**
	 * Creates new {@link Builder} for creating {@link IfFunction}
	 *
	 * @param condition
	 * @return
	 */
	public static Builder when(Object condition) {
		return new Builder(condition);
	}

	public static Builder when(Field field) {
		Assert.notNull(field, "Field cannot be 'null' in if clause.");

		return when(field.getName());
	}

	public static Builder when(String fieldname) {
		Assert.hasText(fieldname, "Fieldname cannot be 'null' for if clause.");

		return new Builder(fieldname);
	}

	public static Builder when(Function function) {
		Assert.notNull(function, "Function cannot be 'null' for if clause.");

		return new Builder(function);
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

	public static class Builder {

		private IfFunction function;

		public Builder(Object condition) {
			Assert.notNull(condition, "Condition cannot be 'null' for if operation");

			function = new IfFunction();
			function.addArgument(condition);
		}

		/**
		 * @param value
		 * @return
		 */
		public Builder then(Object value) {
			Assert.notNull(value, "True value cannot be 'null' for if operation.");

			function.addArgument(value);
			return this;
		}

		/**
		 * @param value
		 * @return
		 */
		public IfFunction otherwise(Object value) {
			Assert.notNull(value, "False value cannot be 'null' for if operation.");

			function.addArgument(value);
			return function;
		}
	}

}
