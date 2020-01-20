/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.solr.core.convert;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * @author Christoph Strobl
 */
public class NumberConvertersTests {

	@Test
	public void testConvertPositiveLong() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert(100l)).isEqualTo("100");
	}

	@Test
	public void testConvertNegativeLong() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert(-100l)).isEqualTo("\\-100");
	}

	@Test
	public void testConvertPositiveInteger() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert(100)).isEqualTo("100");
	}

	@Test
	public void testConvertNegativeInteger() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert(-100)).isEqualTo("\\-100");
	}

	@Test
	public void testConvertPositiveFloat() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert(100f)).isEqualTo("100.0");
	}

	@Test
	public void testConvertNegativeFloat() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert((float) -100)).isEqualTo("\\-100.0");
	}

	@Test
	public void testConvertNaNFloat() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert(Float.NaN)).isEqualTo("NaN");
	}

	@Test
	public void testConvertPositiveInfiniteFloat() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert(Float.POSITIVE_INFINITY)).isEqualTo("Infinity");
	}

	@Test
	public void testConvertNegativeInfiniteFloat() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert(Float.NEGATIVE_INFINITY)).isEqualTo("\\-Infinity");
	}

	@Test
	public void testConvertPositiveDouble() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert(100d)).isEqualTo("100.0");
	}

	@Test
	public void testConvertNegativeDouble() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert((double) -100)).isEqualTo("\\-100.0");
	}

	@Test
	public void testConvertNaNDouble() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert(Double.NaN)).isEqualTo("NaN");
	}

	@Test
	public void testConvertPositiveInfiniteDouble() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert(Double.POSITIVE_INFINITY)).isEqualTo("Infinity");
	}

	@Test
	public void testConvertNegativeInfiniteDouble() {
		assertThat(NumberConverters.NumberConverter.INSTANCE.convert(Double.NEGATIVE_INFINITY)).isEqualTo("\\-Infinity");
	}

}
