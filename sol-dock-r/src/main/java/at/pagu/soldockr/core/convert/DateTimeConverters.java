/*
 * Copyright (C) 2012 sol-dock-r authors.
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
package at.pagu.soldockr.core.convert;

import java.util.Date;

import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.core.convert.converter.Converter;

final class DateTimeConverters {

  private static DateTimeFormatter formatter = ISODateTimeFormat.dateTime().withZoneUTC();

  public enum JodaDateTimeConverter implements Converter<ReadableInstant, String> {
    INSTANCE;

    @Override
    public String convert(ReadableInstant source) {
      if (source == null) {
        return null;
      }
      return (formatter.print(source.getMillis()));
    }

  }

  public enum JavaDateConverter implements Converter<Date, String> {
    INSTANCE;

    @Override
    public String convert(Date source) {
      if (source == null) {
        return null;
      }

      return formatter.print(source.getTime());
    }

  }

}
