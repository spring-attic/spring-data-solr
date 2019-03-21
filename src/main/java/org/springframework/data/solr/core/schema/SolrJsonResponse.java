/*
 * Copyright 2014 the original author or authors.
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

import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.common.util.NamedList;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class SolrJsonResponse extends SolrResponseBase {

	private static final long serialVersionUID = 5727953031460362404L;
	private JsonNode root;

	@Override
	public void setResponse(NamedList<Object> response) {

		super.setResponse(response);
		try {
			root = new ObjectMapper().readTree((String) getResponse().get("json"));
		} catch (Exception e) {
			throw new InvalidDataAccessResourceUsageException("Unable to parse json from response.", e);
		}
	}

	public String getJsonResponse() {
		return (String) getResponse().get("json");
	}

	public JsonNode getNode(String name) {
		return root.findValue(name);
	}

}
