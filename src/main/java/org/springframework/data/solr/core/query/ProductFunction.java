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
 * Implementation of {@code product(x,y,...)}
 * 
 * @author Christoph Strobl
 * @since 1.1
 */
public class ProductFunction extends AbstractFunction {

	private static final String OPERATION = "product";

	private ProductFunction(Object value) {
		super(Arrays.asList(value));
	}

	/**
	 * @param value
	 * @return
	 */
	public static Builder product(Number value) {
		return new Builder(value);
	}

	/**
	 * @param field
	 * @return
	 */
	public static Builder product(Field field) {
		Assert.notNull(field, "Field must not be 'null'");

		return product(field.getName());
	}

	/**
	 * @param fieldname
	 * @return
	 */
	public static Builder product(String fieldname) {
		return new Builder(fieldname);
	}

	/**
	 * @param function
	 * @return
	 */
	public static Builder product(Function function) {
		return new Builder(function);
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

	public static class Builder {

		private ProductFunction function;

		/**
		 * @param fieldname
		 */
		public Builder(String fieldname) {
			Assert.hasText(fieldname, "Fieldname must not be 'empty'.");

			this.function = new ProductFunction(fieldname);
		}

		/**
		 * @param value
		 */
		public Builder(Number value) {
			Assert.notNull(value, "Argument 'value' must not be 'null'.");

			this.function = new ProductFunction(value);
		}

		/**
		 * @param function
		 */
		public Builder(Function function) {
			Assert.notNull(function, "Argument 'function' must not be 'null'.");

			this.function = new ProductFunction(function);
		}

		/**
		 * @param field must not be null
		 * @return
		 */
		public Builder times(Field field) {
			Assert.notNull(field, "Argument 'field' must not be 'null'.");

			return times(field.getName());
		}

		/**
		 * @param fieldname must not be null
		 * @return
		 */
		public Builder times(String fieldname) {
			Assert.hasText(fieldname, "Fieldname must not be 'empty'.");

			this.function.addArgument(fieldname);
			return this;
		}

		/**
		 * @param value must not be null
		 * @return
		 */
		public Builder times(Number value) {
			Assert.notNull(value, "Argument 'value' must not be 'null'.");

			this.function.addArgument(value);
			return this;
		}

		/**
		 * @param function must not be null
		 * @return
		 */
		public Builder times(Function function) {
			Assert.notNull(function, "Argument 'function' must not be 'null'.");

			this.function.addArgument(function);
			return this;
		}

		public ProductFunction build() {
			return this.function;
		}

	}

}
