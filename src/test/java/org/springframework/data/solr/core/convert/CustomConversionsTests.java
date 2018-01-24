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
package org.springframework.data.solr.core.convert;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.util.NumberUtils;

/**
 * @author Christoph Strobl
 */
public class CustomConversionsTests {

	private CustomConversions conversions;

	@Test
	public void testFindReadingConverter() {
		conversions = new CustomConversions(Arrays.asList(StringToNumberConverter.INSTANCE));
		Assert.assertTrue(conversions.hasCustomReadTarget(String.class, Number.class));
		Assert.assertFalse(conversions.hasCustomReadTarget(Date.class, Number.class));
	}

	@Test
	public void testFindWritingConverter() {
		conversions = new CustomConversions(Arrays.asList(NumberToStringConverter.INSTANCE));
		Assert.assertTrue(conversions.hasCustomWriteTarget(Number.class, String.class));
		Assert.assertFalse(conversions.hasCustomWriteTarget(Date.class, String.class));
	}

	@Test
	public void testFindReadWriteConverter() {
		conversions = new CustomConversions(Arrays.asList(StringToLocaleConverter.INSTANCE));
		Assert.assertTrue(conversions.hasCustomReadTarget(String.class, Locale.class));
		Assert.assertTrue(conversions.hasCustomWriteTarget(String.class, Locale.class));
	}

	@Test
	public void testFindMostRecentConverter() {
		conversions = new CustomConversions(Arrays.asList(NumberToStringConverter.INSTANCE));
		Assert.assertThat(conversions.getCustomWriteTarget(Number.class),
				Matchers.is(Matchers.typeCompatibleWith(String.class)));
	}

	@WritingConverter
	enum NumberToStringConverter implements Converter<Number, String> {
		INSTANCE;

		public String convert(Number source) {
			return source.toString();
		}
	}

	@ReadingConverter
	enum StringToNumberConverter implements Converter<String, Number> {
		INSTANCE;

		public Number convert(String source) {
			return NumberUtils.parseNumber(source, Integer.class);
		}
	}

	enum StringToLocaleConverter implements Converter<String, Locale> {
		INSTANCE;

		@Override
		public Locale convert(String source) {
			return Locale.GERMAN;
		}

	}

}
