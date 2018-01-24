/*
 * Copyright 2012 - 2018 the original author or authors.
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
package org.springframework.data.solr.core.geo;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.geo.Metrics;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 */
public final class GeoConverters {

	/**
	 * Converts a {@link Point} to a solrReadable request parameter.
	 */
	@WritingConverter
	public enum Point2DToStringConverter implements Converter<org.springframework.data.geo.Point, String> {
		INSTANCE;

		@Override
		public String convert(org.springframework.data.geo.Point source) {

			if (source instanceof Point) {
				return Point3DToStringConverter.INSTANCE.convert((Point) source);
			}
			String formattedString = StringUtils.stripEnd(String.format(java.util.Locale.ENGLISH, "%f", source.getX()), "0")
					+ "," + StringUtils.stripEnd(String.format(java.util.Locale.ENGLISH, "%f", source.getY()), "0");

			if (formattedString.endsWith(".")) {
				return formattedString.replace(".", ".0");
			}
			return formattedString;
		}
	}

	/**
	 * Converts comma separated string to {@link org.springframework.data.geo.Point}.
	 *
	 * @since 1.2
	 */
	@ReadingConverter
	public enum StringToPointConverter implements Converter<String, org.springframework.data.geo.Point> {
		INSTANCE;

		@Override
		public org.springframework.data.geo.Point convert(String source) {

			String[] coordinates = source.split(",");
			return new org.springframework.data.geo.Point(Double.parseDouble(coordinates[0]),
					Double.parseDouble(coordinates[1]));
		}

	}

	/**
	 * Converts a {@link org.springframework.data.geo.Distance} to a solrReadable request parameter.
	 */
	@WritingConverter
	public enum DistanceToStringConverter implements Converter<org.springframework.data.geo.Distance, String> {
		INSTANCE;

		@Override
		public String convert(org.springframework.data.geo.Distance source) {

			Assert.notNull(source, "Source must not be null!");

			double value = source.getValue();
			if (source.getMetric() == Metrics.MILES) {
				value = source.getValue() * 1.609344D;
			}
			return String.format(java.util.Locale.ENGLISH, "%s", value);
		}
	}

	/**
	 * Converts a {@link Point} to a solrReadable request parameter.
	 *
	 * @since 1.1
	 */
	public enum Point3DToStringConverter implements Converter<org.springframework.data.geo.Point, String> {
		INSTANCE;

		@Override
		public String convert(org.springframework.data.geo.Point source) {

			Assert.notNull(source, "Source must not be null!");

			String formattedString = StringUtils.stripEnd(String.format(java.util.Locale.ENGLISH, "%f", source.getX()), "0")
					+ "," + StringUtils.stripEnd(String.format(java.util.Locale.ENGLISH, "%f", source.getY()), "0");

			if (source instanceof Point) {
				formattedString += (((Point) source).getZ() != null
						? ("," + StringUtils.stripEnd(String.format(java.util.Locale.ENGLISH, "%f", ((Point) source).getZ()), "0"))
						: "");
			}

			return formattedString.replaceAll("\\.,", "\\.0,").replaceFirst("\\.$", ".0");
		}

	}
}
