/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.solr.core.schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;
import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.common.util.NamedList;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class MappingJacksonResponseParser extends ResponseParser {

	private static final String WRITER = "json";

	private final MimeType responseType;

	public MappingJacksonResponseParser() {
		this(defaultMimeType());
	}

	public MappingJacksonResponseParser(@Nullable MimeType responseType) {
		this.responseType = responseType != null ? responseType : defaultMimeType();
	}

	@Override
	public String getWriterType() {
		return WRITER;
	}

	@Override
	public String getContentType() {
		return responseType.toString();
	}

	@Override
	public NamedList<Object> processResponse(InputStream body, String encoding) {

		NamedList<Object> result = new NamedList<>();
		try {
			result.add("json", StreamUtils.copyToString(body, Charset.forName(encoding)));
		} catch (IOException e) {
			throw new InvalidDataAccessResourceUsageException("Unable to read json from stream", e);
		}
		return result;
	}

	@Override
	public NamedList<Object> processResponse(Reader reader) {
		throw new UnsupportedOperationException();
	}

	private static MimeType defaultMimeType() {
		try {
			return new MimeType("application", "json");
		} catch (final MimeTypeParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
