/*
 * Copyright 2012 - 2014 the original author or authors.
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
import org.springframework.data.solr.core.convert.GeoConverterTests.GeoLocationConverterTests;
import org.springframework.data.solr.core.convert.GeoConverterTests.PointConverterTests;
import org.springframework.data.solr.core.geo.Distance.Unit;
import org.springframework.data.solr.core.geo.GeoConverters;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.data.solr.core.geo.Point;

/**
 * @author Christoph Strobl
 */
@RunWith(Suite.class)
@SuiteClasses({ GeoLocationConverterTests.class, DistanceConverterTests.class, PointConverterTests.class })
public class GeoConverterTests {

	public static class GeoLocationConverterTests {

		@Test
		public void testConvertGeoLocationToStringWithNull() {
			Assert.assertNull(GeoConverters.GeoLocationToStringConverter.INSTANCE.convert(null));
		}

		@Test
		public void testConvertPointToString() {
			Assert.assertEquals("48.303056,14.290556", GeoConverters.GeoLocationToStringConverter.INSTANCE
					.convert(new org.springframework.data.geo.Point(48.303056, 14.290556)));
		}

		/**
		 * @see DATASOLR-142
		 */
		@Test
		public void testConvertGeoLocationToString() {
			Assert.assertEquals("48.303056,14.290556",
					GeoConverters.GeoLocationToStringConverter.INSTANCE.convert(new GeoLocation(48.303056, 14.290556)));
		}

		@Test
		public void testConvertGeoLocationToStringWithNegativeValue() {
			Assert.assertEquals("45.17614,-93.87341", GeoConverters.GeoLocationToStringConverter.INSTANCE
					.convert(new org.springframework.data.geo.Point(45.17614, -93.87341)));
		}

		@Test
		public void testConvertStringToGeoLocationWithNull() {
			Assert.assertNull(GeoConverters.StringToGeoLocationConverter.INSTANCE.convert(null));
		}

		@Test
		public void testConvertStringToGeoLocation() {
			GeoLocation geoLocation = GeoConverters.StringToGeoLocationConverter.INSTANCE.convert("48.303056,14.290556");

			Assert.assertEquals(geoLocation.getLatitude(), 48.303056D, 0F);
			Assert.assertEquals(geoLocation.getLongitude(), 14.290556D, 0F);
		}

		@Test
		public void testConvertStringToGeoLocationWithNegativeValue() {
			GeoLocation geoLocation = GeoConverters.StringToGeoLocationConverter.INSTANCE.convert("45.17614,-93.87341");

			Assert.assertEquals(geoLocation.getLatitude(), 45.17614D, 0F);
			Assert.assertEquals(geoLocation.getLongitude(), -93.87341D, 0F);
		}

	}

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

		/**
		 * @see DATASOLR-142
		 */
		@Test
		public void testConvertMilesDistanceUnitToString() {
			Assert.assertEquals("1.609344", GeoConverters.DistanceToStringConverter.INSTANCE
					.convert(new org.springframework.data.solr.core.geo.Distance(1, Unit.MILES)));
		}

		@Test
		public void testConvertDistanceWithNullUnitToString() {
			Assert.assertEquals("1.0", GeoConverters.DistanceToStringConverter.INSTANCE.convert(new Distance(1, null)));
		}
	}

	public static class PointConverterTests {

		@Test
		public void testConvertPointToStringWithNull() {
			Assert.assertNull(GeoConverters.PointToStringConverter.INSTANCE.convert(null));
		}

		@Test
		public void testConvertPointXYToString() {
			Assert.assertEquals("48.303056,14.290556",
					GeoConverters.PointToStringConverter.INSTANCE.convert(new Point(48.303056, 14.290556)));
		}

		@Test
		public void testConvertPointXYToStringWithNegativeValue() {
			Assert.assertEquals("45.17614,-93.87341",
					GeoConverters.PointToStringConverter.INSTANCE.convert(new Point(45.17614, -93.87341)));
		}

		@Test
		public void testConvertPointXYZToString() {
			Assert.assertEquals("48.303056,14.290556,12.78",
					GeoConverters.PointToStringConverter.INSTANCE.convert(new Point(48.303056, 14.290556, 12.78)));
		}

		@Test
		public void testConvertPointXYZToStringWithNegativeValue() {
			Assert.assertEquals("45.17614,-93.87341,-12.78",
					GeoConverters.PointToStringConverter.INSTANCE.convert(new Point(45.17614, -93.87341, -12.78)));
		}

	}

}
