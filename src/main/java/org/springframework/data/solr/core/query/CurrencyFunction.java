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

import org.springframework.lang.Nullable;
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

	private CurrencyFunction(Field field) {
		super(Collections.singletonList(field));
	}

	/**
	 * Create new {@link CurrencyFunction} representing {@code currency(fieldname)}
	 *
	 * @param fieldName
	 * @return
	 */
	public static CurrencyFunction currency(String fieldName) {

		Assert.hasText(fieldName, "FieldName must not be empty");
		return currency(fieldName, null);
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
	public static CurrencyFunction currency(Field field, @Nullable String currencyCode) {

		Assert.notNull(field, "Field for currency function must not be 'null'");

		CurrencyFunction function = new CurrencyFunction(field);
		if (StringUtils.hasText(currencyCode)) {
			function.addArgument(currencyCode);
		}
		return function;
	}

	/**
	 * Create new {@link CurrencyFunction} using ISO-4217 currencyCode representing
	 * {@code currency(fieldname,currencyCode)}
	 *
	 * @param fieldName
	 * @param currencyCode
	 * @return
	 */
	public static CurrencyFunction currency(String fieldName, @Nullable String currencyCode) {

		Assert.hasText(fieldName, "FieldName for currency function must not be 'empty'");
		return currency(new SimpleField(fieldName), currencyCode);
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

}
