/*
 * Copyright 2014 the original author or authors.
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
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @since 1.4
 */
public class SolrRealtimeGetRequest extends SolrRequest {

	private static final long serialVersionUID = 1500782684874146272L;
	private Collection<String> ids;

	public SolrRealtimeGetRequest(Serializable... ids) {
		this(Arrays.asList(ids));
	}

	public SolrRealtimeGetRequest(Collection<? extends Serializable> ids) {
		super(METHOD.GET, "/get");

		Assert.notEmpty(ids, "At least one 'id' id required for real time get request.");
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
	public QueryResponse process(SolrServer server) throws SolrServerException, IOException {

		try {
			long startTime = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
			QueryResponse res = new QueryResponse(server.request(this), server);
			long endTime = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
			res.setElapsedTime(endTime - startTime);
			return res;
		} catch (SolrServerException e) {
			throw e;
		} catch (SolrException s) {
			throw s;
		} catch (Exception e) {
			throw new SolrServerException("Error executing query", e);
		}
	}
}
