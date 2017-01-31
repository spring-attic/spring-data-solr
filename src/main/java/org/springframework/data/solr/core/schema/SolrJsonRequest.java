/*
 * Copyright 2014 - 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class SolrJsonRequest extends SolrRequest<SolrJsonResponse> {

	private static final long serialVersionUID = 5786008418321490550L;

	private ModifiableSolrParams params = new ModifiableSolrParams();
	private List<ContentStream> contentStream = Collections.emptyList();
	private @Nullable ContentParser contentParser;

	public SolrJsonRequest(METHOD method, String path) {
		super(method, path);
		setResponseParser(new MappingJacksonResponseParser());
		setContentParser(new MappingJacksonRequestContentParser());
	}

	private void setContentParser(@Nullable ContentParser requestParser) {
		this.contentParser = requestParser != null ? requestParser : new MappingJacksonRequestContentParser();
	}

	@Nullable
	public ContentParser getContentParser() {
		return this.contentParser;
	}

	@Override
	public SolrParams getParams() {
		return this.params;
	}

	@Override
	public Collection<ContentStream> getContentStreams() throws IOException {
		return Collections.unmodifiableCollection(contentStream);
	}

	public void addContentToStream(Object content) {

		contentStream = new ArrayList<>(contentStream);
		contentStream.add(getContentParser().parse(content));
	}

	@Override
	public String toString() {
		return getMethod().toString() + " " + getPath() + "\r\n" + quietlyReadContentStreams();
	}

	private String quietlyReadContentStreams() {
		StringBuilder sb = new StringBuilder();
		if (contentStream != null) {
			for (ContentStream stream : this.contentStream) {
				InputStream ioStream = null;
				try {
					ioStream = stream.getStream();
					sb.append(StreamUtils.copyToString(ioStream, Charset.forName("UTF-8")));
				} catch (IOException e) {} finally {
					if (ioStream != null) {
						try {
							ioStream.close();
						} catch (IOException e) {}
					}
				}
			}
		}
		return sb.toString();
	}

	@Override
	protected SolrJsonResponse createResponse(SolrClient client) {
		return new SolrJsonResponse();
	}

}
