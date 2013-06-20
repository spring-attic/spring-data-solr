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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.data.solr.core.convert.GeoConverterTests.DistanceConverterTest;
import org.springframework.data.solr.core.convert.GeoConverterTests.GeoLocationConverterTest;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.Distance.Unit;
import org.springframework.data.solr.core.geo.GeoConverters;
import org.springframework.data.solr.core.geo.GeoLocation;

/**
 * @author Christoph Strobl
 */
@RunWith(Suite.class)
@SuiteClasses({ GeoLocationConverterTest.class, DistanceConverterTest.class })
public class GeoConverterTests {

	public static class GeoLocationConverterTest {

		@Test
		public void testConvertGeoLocationToStringWithNull() {
			Assert.assertNull(GeoConverters.GeoLocationToStringConverter.INSTANCE.convert(null));
		}

		@Test
		public void testConvertGeoLocationToString() {
			Assert.assertEquals("48.303056,14.290556",
					GeoConverters.GeoLocationToStringConverter.INSTANCE.convert(new GeoLocation(48.303056, 14.290556)));
		}

		@Test
		public void testConvertGeoLocationToStringWithNegativeValue() {
			Assert.assertEquals("45.17614,-93.87341",
					GeoConverters.GeoLocationToStringConverter.INSTANCE.convert(new GeoLocation(45.17614, -93.87341)));
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

	public static class DistanceConverterTest {

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
					GeoConverters.DistanceToStringConverter.INSTANCE.convert(new Distance(1, Unit.MILES)));
		}

		@Test
		public void testConvertDistanceWithNullUnitToString() {
			Assert.assertEquals("1.0", GeoConverters.DistanceToStringConverter.INSTANCE.convert(new Distance(1, null)));
		}
	}

}
