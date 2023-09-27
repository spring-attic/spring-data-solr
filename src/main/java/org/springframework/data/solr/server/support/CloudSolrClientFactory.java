/*
 * Copyright 2014 - 2015 the original author or authors.
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
package org.springframework.data.solr.server.support;

import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.springframework.data.solr.core.SolrTemplate;

/**
 * A factory class which can be used as a parameter to {@link SolrTemplate} constructor in order to use
 * {@link CloudSolrClient} and connect to a SolrCloud collection installation.
 * 
 * @author Christos Manios
 * 
 */
public class CloudSolrClientFactory extends SolrClientFactoryBase {

	public CloudSolrClientFactory() {

	}

	public CloudSolrClientFactory(SolrClient client) {
		super(client);
	}

	/**
	 * Returns the same as {@link #getSolrClient()}, as we are in SolrCloud mode.
	 */
	@Override
	public SolrClient getSolrClient(String core) {
		return this.getSolrClient();
	}

	/**
	 * Returns <code>null</code>, as we are in SolrCloud mode.
	 */
	@Override
	public List<String> getCores() {
		return null;
	}

}
