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

import org.apache.commons.lang.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.solr.core.geo.GeoLocation;

/**
 * @author Christoph Strobl
 */
public final class GeoConverter implements Converter<GeoLocation, String> {

	@Override
	public String convert(GeoLocation source) {
		if (source == null) {
			return null;
		}
		return StringUtils.stripEnd(String.format(java.util.Locale.ENGLISH, "%f", source.getLatitude()), "0") + ","
				+ StringUtils.stripEnd(String.format(java.util.Locale.ENGLISH, "%f", source.getLongitude()), "0");
	}
}
