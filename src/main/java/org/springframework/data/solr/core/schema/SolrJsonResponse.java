/*
 * Copyright 2014-2017 the original author or authors.
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

import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.common.util.NamedList;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class SolrJsonResponse extends SolrResponseBase {

	private static final long serialVersionUID = 5727953031460362404L;
	private @Nullable JsonNode root;
	private ObjectMapper mapper;

	public SolrJsonResponse() {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
	}

	@Override
	public void setResponse(NamedList<Object> response) {

		super.setResponse(response);

		try {

			String json = getJsonResponse();

			if (json == null) {
				if (response.get("version") != null) {
					root = mapper.readTree(response.toString().replace('=', ':'));
				} else {
					root = mapper.createObjectNode();
				}
				return;
			}
			root = mapper.readTree(json);
		} catch (

		Exception e) {
			throw new InvalidDataAccessResourceUsageException("Unable to parse json from response.", e);
		}
	}

	@Nullable
	public String getJsonResponse() {
		return (String) getResponse().get("json");
	}

	public JsonNode getNode(String name) {
		return root.findValue(name);
	}

}
