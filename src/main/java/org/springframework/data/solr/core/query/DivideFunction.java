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
 * Implementation of {@code div(x,y)}
 * 
 * @author Christoph Strobl
 * @since 1.1
 */
public class DivideFunction extends AbstractFunction {

	private static final String OPERATION = "div";

	private DivideFunction(Object dividend, Object divisor) {
		super(Arrays.asList(dividend, divisor));
	}

	/**
	 * creates new {@link Builder} for dividing value in field with given name
	 * 
	 * @param field must not be null
	 * @return
	 */
	public static Builder divide(Field field) {
		Assert.notNull(field, "Field cannot be 'null' for divide function.");

		return divide(field.getName());
	}

	/**
	 * creates new {@link Builder} for dividing value in field with given name
	 * 
	 * @param fieldname must not be empty
	 * @return
	 */
	public static Builder divide(String fieldname) {
		Assert.hasText(fieldname, "Fieldname cannot be 'empty' for divide function.");

		return new Builder(fieldname);
	}

	/**
	 * creates new {@link Builder} for dividing given value
	 * 
	 * @param dividend
	 * @return
	 */
	public static Builder divide(Number dividend) {
		return new Builder(dividend);
	}

	/**
	 * creates new {@link Builder} for dividing value calculated by given {@link Function}
	 * 
	 * @param dividend
	 * @return
	 */
	public static Builder divide(Function dividend) {
		return new Builder(dividend);
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

	public static class Builder {

		private Object dividend;

		public Builder(Object dividend) {
			Assert.notNull(dividend, "Dividend must not be 'null'");

			this.dividend = dividend;
		}

		/**
		 * @param divisor must not be null
		 * @return
		 */
		public DivideFunction by(Number divisor) {
			return by((Object) divisor);
		}

		/**
		 * @param divisor must not be null
		 * @return
		 */
		public DivideFunction by(Function divisor) {
			return by((Object) divisor);
		}

		/**
		 * @param fieldname must not be empty
		 * @return
		 */
		public DivideFunction by(String fieldname) {
			Assert.hasText(fieldname, "Fieldname for devide function must not be 'empty'.");

			return by((Object) fieldname);
		}

		/**
		 * @param field must not be null
		 * @return
		 */
		public DivideFunction by(Field field) {
			Assert.notNull(field, "Field must not be 'null'.");

			return by(field.getName());
		}

		private DivideFunction by(Object divisor) {
			Assert.notNull(divisor, "Cannot divide by 'null'.");

			return new DivideFunction(dividend, divisor);
		}
	}

}
