/*
 * Copyright 2014 - 2016 the original author or authors.
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
package org.springframework.data.solr;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @since 1.4
 * @deprecated since 2.1. Please use {@link SolrClient#getById(Collection)} instead.
 */
@Deprecated
public class SolrRealtimeGetRequest extends SolrRequest<QueryResponse> {

	private static final long serialVersionUID = 1500782684874146272L;
	private Collection<String> ids;

	public SolrRealtimeGetRequest(Serializable... ids) {
		this(Arrays.asList(ids));
	}

	public SolrRealtimeGetRequest(Collection<? extends Serializable> ids) {
		super(METHOD.GET, "/get");

		Assert.notEmpty(ids, "At least one 'id' is required for real time get request.");
		Assert.noNullElements(ids.toArray(), "Real time get request can't be made for 'null' id.");

		toStringIds(ids);
	}

	private void toStringIds(Collection<? extends Serializable> ids) {

		this.ids = new ArrayList<String>(ids.size());
		for (Serializable id : ids) {
			this.ids.add(id.toString());
		}
	}

	@Override
	public SolrParams getParams() {
		return new ModifiableSolrParams().add("ids", this.ids.toArray(new String[this.ids.size()]));
	}

	@Override
	public Collection<ContentStream> getContentStreams() throws IOException {
		return null;
	}

	@Override
	protected QueryResponse createResponse(SolrClient client) {
		return new QueryResponse();
	}
}
