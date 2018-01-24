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
package org.springframework.data.solr.core.convert;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * CustomConversions holds basically a list of {@link Converter} that can be used for mapping objects to (
 * {@link WritingConverter}) and from ({@link ReadingConverter}) solr representation.
 *
 * @author Christoph Strobl
 * @author Rias A. Sherzad
 * @author Mark Paluch
 * @deprecated since 2.0, use {@link SolrCustomConversions}.
 */
@Deprecated
public class CustomConversions extends SolrCustomConversions {

	/**
	 * Create new instance
	 */
	public CustomConversions() {
		this(new ArrayList<>());
	}

	/**
	 * Create new instance registering given converters
	 *
	 * @param converters
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public CustomConversions(List converters) {
		super(converters);
	}
}
