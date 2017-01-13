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
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.solr.core.convert.GeoConverterTests.DistanceConverterTests;
import org.springframework.data.solr.core.convert.GeoConverterTests.PointConverterTests;
import org.springframework.data.solr.core.geo.GeoConverters;
import org.springframework.data.solr.core.geo.Point;

/**
 * @author Christoph Strobl
 */
@RunWith(Suite.class)
@SuiteClasses({ DistanceConverterTests.class, PointConverterTests.class })
public class GeoConverterTests {

	public static class DistanceConverterTests {

		@Test
		public void testConvertDistanceToStringWithNull() {
			Assert.assertNull(GeoConverters.DistanceToStringConverter.INSTANCE.convert(null));
		}

		@Test
		public void testConvertDistanceToString() {
			Assert.assertEquals("5.0", GeoConverters.DistanceToStringConverter.INSTANCE.convert(new Distance(5)));
		}

		@Test
		public void testConvertMilesDistanceToString() {
			Assert.assertEquals("1.609344",
					GeoConverters.DistanceToStringConverter.INSTANCE.convert(new Distance(1, Metrics.MILES)));
		}

		@Test
		public void testConvertDistanceWithNullUnitToString() {
			Assert.assertEquals("1.0", GeoConverters.DistanceToStringConverter.INSTANCE.convert(new Distance(1, null)));
		}
	}

	public static class PointConverterTests {

		@Test
		public void testConvertPointToStringWithNull() {
			Assert.assertNull(GeoConverters.Point3DToStringConverter.INSTANCE.convert(null));
		}

		@Test
		public void testConvertPointXYToString() {
			Assert.assertEquals("48.303056,14.290556",
					GeoConverters.Point3DToStringConverter.INSTANCE.convert(new Point(48.303056, 14.290556)));
		}

		@Test
		public void testConvertPointXYToStringWithNegativeValue() {
			Assert.assertEquals("45.17614,-93.87341",
					GeoConverters.Point3DToStringConverter.INSTANCE.convert(new Point(45.17614, -93.87341)));
		}

		@Test
		public void testConvertPointXYZToString() {
			Assert.assertEquals("48.303056,14.290556,12.78",
					GeoConverters.Point3DToStringConverter.INSTANCE.convert(new Point(48.303056, 14.290556, 12.78)));
		}

		@Test
		public void testConvertPointXYZToStringWithNegativeValue() {
			Assert.assertEquals("45.17614,-93.87341,-12.78",
					GeoConverters.Point3DToStringConverter.INSTANCE.convert(new Point(45.17614, -93.87341, -12.78)));
		}

		@Test // DATASOLR-307
		public void shouldConvertYCoordWithZeroScaleCorrectly() {

			Assert.assertEquals("53.549999,10.0",
					GeoConverters.Point2DToStringConverter.INSTANCE.convert(new Point(53.549999, 10.000000)));
		}

		@Test // DATASOLR-307
		public void shouldConvertXCoordWithZeroScaleCorrectly() {

			Assert.assertEquals("10.0,53.549999",
					GeoConverters.Point2DToStringConverter.INSTANCE.convert(new Point(10.000000, 53.549999)));
		}

		@Test // DATASOLR-307
		public void shouldConvertZCoordWithZeroScaleCorrectly() {

			Assert.assertEquals("123.456,53.549999,10.0", GeoConverters.Point3DToStringConverter.INSTANCE
					.convert(new org.springframework.data.solr.core.geo.Point(123.456, 53.549999, 10.00)));
		}

		@Test // DATASOLR-307
		public void shouldConvertXYZCoordWithZeroScaleCorrectly() {
			Assert.assertEquals("123.0,456.0,789.0", GeoConverters.Point3DToStringConverter.INSTANCE
					.convert(new org.springframework.data.solr.core.geo.Point(123, 456, 789)));
		}

	}

}
