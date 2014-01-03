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
import org.springframework.util.StringUtils;

/**
 * Implementation of {@code currency(field_name,[CODE])}
 * 
 * @author Christoph Strobl
 * @since 1.1
 */
public class CurrencyFunction extends AbstractFunction {

	private static final String OPERATION = "currency";

	private CurrencyFunction(String fieldname) {
		super(Arrays.asList(fieldname));
	}

	/**
	 * Create new {@link CurrencyFunction} representing {@code currency(fieldname)}
	 * 
	 * @param fieldname
	 * @return
	 */
	public static CurrencyFunction currency(String fieldname) {
		return currency(fieldname, null);
	}

	/**
	 * Create new {@link CurrencyFunction} representing {@code currency(field.getName())}
	 * 
	 * @param field
	 * @return
	 */
	public static CurrencyFunction currency(Field field) {
		return currency(field, null);
	}

	/**
	 * Create new {@link CurrencyFunction} using ISO-4217 currencyCode representing
	 * {@code currency(fiel.getName(),currencyCode)}
	 * 
	 * @param field
	 * @param currencyCode
	 * @return
	 */
	public static CurrencyFunction currency(Field field, String currencyCode) {
		Assert.notNull(field, "Field for currency function must not be 'null'.");

		return currency(field.getName(), currencyCode);
	}

	/**
	 * Create new {@link CurrencyFunction} using ISO-4217 currencyCode representing
	 * {@code currency(fieldname,currencyCode)}
	 * 
	 * @param fieldname
	 * @param currencyCode
	 * @return
	 */
	public static CurrencyFunction currency(String fieldname, String currencyCode) {
		Assert.hasText(fieldname, "Fieldname for currency function must not be 'empty'.");

		CurrencyFunction function = new CurrencyFunction(fieldname);
		if (StringUtils.hasText(currencyCode)) {
			function.addArgument(currencyCode);
		}
		return function;
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

}
