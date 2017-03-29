/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Christoph Strobl
 */
public class NumberConvertersTests {

	@Test
	public void testConvertPositiveLong() {
		Assert.assertEquals("100", NumberConverters.NumberConverter.INSTANCE.convert(100l));
	}

	@Test
	public void testConvertNegativeLong() {
		Assert.assertEquals("\\-100", NumberConverters.NumberConverter.INSTANCE.convert(-100l));
	}

	@Test
	public void testConvertPositiveInteger() {
		Assert.assertEquals("100", NumberConverters.NumberConverter.INSTANCE.convert(100));
	}

	@Test
	public void testConvertNegativeInteger() {
		Assert.assertEquals("\\-100", NumberConverters.NumberConverter.INSTANCE.convert(-100));
	}

	@Test
	public void testConvertPositiveFloat() {
		Assert.assertEquals("100.0", NumberConverters.NumberConverter.INSTANCE.convert(100f));
	}

	@Test
	public void testConvertNegativeFloat() {
		Assert.assertEquals("\\-100.0", NumberConverters.NumberConverter.INSTANCE.convert((float) -100));
	}

	@Test
	public void testConvertNaNFloat() {
		Assert.assertEquals("NaN", NumberConverters.NumberConverter.INSTANCE.convert(Float.NaN));
	}

	@Test
	public void testConvertPositiveInfiniteFloat() {
		Assert.assertEquals("Infinity", NumberConverters.NumberConverter.INSTANCE.convert(Float.POSITIVE_INFINITY));
	}

	@Test
	public void testConvertNegativeInfiniteFloat() {
		Assert.assertEquals("\\-Infinity", NumberConverters.NumberConverter.INSTANCE.convert(Float.NEGATIVE_INFINITY));
	}

	@Test
	public void testConvertPositiveDouble() {
		Assert.assertEquals("100.0", NumberConverters.NumberConverter.INSTANCE.convert(100d));
	}

	@Test
	public void testConvertNegativeDouble() {
		Assert.assertEquals("\\-100.0", NumberConverters.NumberConverter.INSTANCE.convert((double) -100));
	}

	@Test
	public void testConvertNaNDouble() {
		Assert.assertEquals("NaN", NumberConverters.NumberConverter.INSTANCE.convert(Double.NaN));
	}

	@Test
	public void testConvertPositiveInfiniteDouble() {
		Assert.assertEquals("Infinity", NumberConverters.NumberConverter.INSTANCE.convert(Double.POSITIVE_INFINITY));
	}

	@Test
	public void testConvertNegativeInfiniteDouble() {
		Assert.assertEquals("\\-Infinity", NumberConverters.NumberConverter.INSTANCE.convert(Double.NEGATIVE_INFINITY));
	}

}
