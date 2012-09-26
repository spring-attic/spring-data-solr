/*
 * Copyright 2012 the original author or authors.
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

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.data.solr.core.geo.GeoLocation;

/**
 * @author Christoph Strobl
 */
public class GeoConverterTest {

	@Test
	public void testConvertWithNull() {
		Assert.assertNull(new GeoConverter().convert(null));
	}

	@Test
	public void testConvert() {
		Assert.assertEquals("48.303056,14.290556", new GeoConverter().convert(new GeoLocation(48.303056, 14.290556)));
	}

	@Test
	public void testConvertWithNegativeValue() {
		Assert.assertEquals("45.17614,-93.87341", new GeoConverter().convert(new GeoLocation(45.17614, -93.87341)));
	}

}
