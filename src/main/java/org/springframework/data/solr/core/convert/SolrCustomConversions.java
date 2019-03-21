/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.convert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.solr.core.geo.GeoConverters.Point3DToStringConverter;
import org.springframework.data.solr.core.geo.GeoConverters.StringToPointConverter;
import org.springframework.data.solr.core.mapping.SolrSimpleTypes;

/**
 * Value object to capture custom conversion. {@link SolrCustomConversions} also act as factory for
 * {@link org.springframework.data.mapping.model.SimpleTypeHolder}
 *
 * @author Mark Paluch
 * @since 2.0
 * @see org.springframework.data.convert.CustomConversions
 * @see org.springframework.data.mapping.model.SimpleTypeHolder
 */
public class SolrCustomConversions extends org.springframework.data.convert.CustomConversions {

	private static final StoreConversions STORE_CONVERSIONS;

	private static final List<Object> STORE_CONVERTERS;

	static {

		List<Object> converters = new ArrayList<>();

		converters.add(StringToPointConverter.INSTANCE);
		converters.add(Point3DToStringConverter.INSTANCE);
		converters.add(new SolrjConverters.UpdateToSolrInputDocumentConverter());

		STORE_CONVERTERS = Collections.unmodifiableList(converters);
		STORE_CONVERSIONS = StoreConversions.of(SolrSimpleTypes.HOLDER, STORE_CONVERTERS);
	}

	/**
	 * Create a new {@link SolrCustomConversions} instance registering the given converters.
	 *
	 * @param converters must not be {@literal null}.
	 */
	public SolrCustomConversions(List<?> converters) {
		super(STORE_CONVERSIONS, converters);
	}
}
