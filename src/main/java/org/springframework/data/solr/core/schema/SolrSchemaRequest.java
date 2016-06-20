/*
 * Copyright 2014-2016 the original author or authors.
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

import java.text.MessageFormat;
import java.util.Collection;

import org.springframework.data.solr.core.schema.SchemaDefinition.FieldDefinition;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 * @since 1.3
 * @deprecated since 2.1. Use {@link org.apache.solr.client.solrj.request.schema.SchemaRequest} instead.
 */
@Deprecated
public class SolrSchemaRequest extends SolrJsonRequest {

	private static final long serialVersionUID = 483080361035195746L;

	public SolrSchemaRequest(METHOD method, String path) {
		super(method, path);
	}

	public static SolrSchemaRequest version() {
		return new SolrSchemaRequestBuilder().forVersion().build();
	}

	public static SolrSchemaRequest schema() {
		return new SolrSchemaRequestBuilder().schema().build();
	}

	public static SolrSchemaRequestBuilder create() {
		return new SolrSchemaRequestBuilder().create();
	}

	public static SolrSchemaRequest name() {
		return new SolrSchemaRequestBuilder().forName().build();
	}

	// -- static
	public static class SolrSchemaRequestBuilder {

		private static final String PATH_PATTERN = "/schema/{0}";
		private String command;
		private METHOD method;
		private Collection<FieldDefinition> newFields;

		public SolrSchemaRequestBuilder schema() {

			method = METHOD.GET;
			command = "";
			return this;
		}

		public SolrSchemaRequestBuilder forName() {
			schema();
			command = "name";
			return this;
		}

		public SolrSchemaRequestBuilder forVersion() {

			method = METHOD.GET;
			command = "version";
			return this;
		}

		public SolrSchemaRequestBuilder create() {
			method = METHOD.POST;
			return this;
		}

		public SolrSchemaRequestBuilder fields(Collection<FieldDefinition> fields) {
			command = "fields";
			newFields = fields;
			return this;
		}

		public SolrSchemaRequest build() {

			String path = buildRequestPath();
			SolrSchemaRequest request = new SolrSchemaRequest(method, path);

			if (!CollectionUtils.isEmpty(newFields)) {
				request.addContentToStream(newFields);
			}
			return request;
		}

		private String buildRequestPath() {
			return MessageFormat.format(PATH_PATTERN, command);
		}

	}

}
