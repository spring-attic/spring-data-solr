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

import java.util.Date;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * Converts a Date values into a solr readable String that can be directly used within the {@code q} parameter. Note
 * that Dates have to be UTC. <br/>
 * <code>
 *   Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
 *   calendar.set(2012, 7, 23, 6, 10, 0);
 * </code>
 * 
 * will be formatted as
 * 
 * <pre>
 * 2012-08-23T06:10:00.000Z
 * </pre>
 * 
 * @author Christoph Strobl
 */
public final class DateTimeConverters {

	private static final DateTimeFormatter FORMATTER = ISODateTimeFormat.dateTime().withZoneUTC();
	private static final DateTimeFormatter FORMATTER_WITHOUT_MILLIS = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();

	/**
	 * Converter used to parse {@link DateTime} to {@link String} used for setting
	 * {@link org.apache.solr.client.solrj.SolrQuery} query string values
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	@WritingConverter
	public enum JodaDateTimeConverter implements Converter<ReadableInstant, String> {
		INSTANCE;

		@Override
		public String convert(ReadableInstant source) {
			if (source == null) {
				return null;
			}
			return (ClientUtils.escapeQueryChars(FORMATTER.print(source.getMillis())));
		}

	}

	@ReadingConverter
	public enum StringToJodaDateTimeConverter implements Converter<String, DateTime> {
		INSTANCE;

		@Override
		public DateTime convert(String source) {
			if (source == null) {
				return null;
			}
			return DateTime.parse(source, FORMATTER_WITHOUT_MILLIS);
		}
	}

	/**
	 * Reading {@link Converter} parses {@link Date} from {@link org.apache.solr.common.SolrDocument} to {@link DateTime}
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	@ReadingConverter
	public enum DateToJodaDateTimeConverter implements Converter<Date, DateTime> {
		INSTANCE;

		@Override
		public DateTime convert(Date source) {
			if (source == null) {
				return null;
			}
			return new DateTime(source.getTime());
		}
	}

	/**
	 * Writing {@link Converter} converts {@link DateTime} to {@link Date} so it can be used within
	 * {@link org.apache.solr.common.SolrInputDocument}
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	@WritingConverter
	public enum JodaDateTimeToDateConverter implements Converter<DateTime, Date> {
		INSTANCE;

		@Override
		public Date convert(DateTime source) {
			if (source == null) {
				return null;
			}
			return source.toDate();
		}

	}

	/**
	 * Converter used to parse {@link LocalDateTime} to {@link String} used for setting
	 * {@link org.apache.solr.client.solrj.SolrQuery} query string values
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	@WritingConverter
	public enum JodaLocalDateTimeConverter implements Converter<LocalDateTime, String> {
		INSTANCE;

		@Override
		public String convert(LocalDateTime source) {
			if (source == null) {
				return null;
			}
			return ClientUtils.escapeQueryChars(FORMATTER.print(source.toDateTime(DateTimeZone.UTC).getMillis()));
		}

	}

	/**
	 * Writing {@link Converter} converts {@link LocalDateTime} to {@link Date} so it can be used within
	 * {@link org.apache.solr.common.SolrInputDocument}
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	@WritingConverter
	public enum JodaLocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {
		INSTANCE;

		@Override
		public Date convert(LocalDateTime source) {
			if (source == null) {
				return null;
			}
			return source.toDate();
		}

	}

	/**
	 * Reading {@link Converter} parses {@link Date} from {@link org.apache.solr.common.SolrDocument} to
	 * {@link LocalDateTime}
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	@ReadingConverter
	public enum DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {
		INSTANCE;

		@Override
		public LocalDateTime convert(Date source) {
			if (source == null) {
				return null;
			}
			return new LocalDateTime(source.getTime());
		}

	}

	/**
	 * Converter used to parse {@link Date} to {@link String} used for setting
	 * {@link org.apache.solr.client.solrj.SolrQuery} query string values
	 * 
	 * @author Christoph Strobl
	 * 
	 */
	@WritingConverter
	public enum JavaDateConverter implements Converter<Date, String> {
		INSTANCE;

		@Override
		public String convert(Date source) {
			if (source == null) {
				return null;
			}

			return ClientUtils.escapeQueryChars(FORMATTER.print(source.getTime()));
		}

	}

}
