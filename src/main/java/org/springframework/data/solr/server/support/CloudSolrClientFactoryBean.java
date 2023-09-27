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

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * 
 * @author Christos Manios
 *
 */
public class CloudSolrClientFactoryBean extends CloudSolrClientFactory implements FactoryBean<SolrClient>,
		InitializingBean, DisposableBean {

	private String zkHost;
	private String collection;
	private Integer zkClientTimeout;
	private Integer zkConnectTimeout;
	private Integer httpConnectTimeout;
	private Integer httpSoTimeout;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(zkHost);
		Assert.hasText(collection);

		initSolrClient();
	}

	private void initSolrClient() {

		// create a new CloudSolrClient with a specified comma delimited string
		// format which contains IP1:port,IP2:port of Zookeeper ensemble
		CloudSolrClient solrClient = new CloudSolrClient(zkHost);

		// set collection name
		solrClient.setDefaultCollection(collection);

		// set Zookeeper ensemble connection timeout
		if (zkConnectTimeout != null) {
			solrClient.setZkConnectTimeout(zkConnectTimeout);
		}

		// set Zookeeper ensemble client timeout
		if (zkClientTimeout != null) {
			solrClient.setZkClientTimeout(zkClientTimeout);
		}

		// set http connection timeout
		if (httpConnectTimeout != null) {
			solrClient.getLbClient().setConnectionTimeout(httpConnectTimeout);
		}

		// set http read timeout
		if (httpSoTimeout != null) {
			solrClient.getLbClient().setSoTimeout(httpSoTimeout);
		}

		this.setSolrClient(solrClient);
	}

	@Override
	public SolrClient getObject() throws Exception {
		return getSolrClient();
	}

	@Override
	public Class<?> getObjectType() {
		if (getSolrClient() == null) {
			return CloudSolrClient.class;
		}
		return getSolrClient().getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	/**
	 * Returns a pair of IP and its respective port of the Zookeeper server which belongs to the Zookeeper ensemble. This
	 * string can contain multiple IP:port definitions separated by comma.
	 * <p>
	 * Example: <code>192.168.1.1:2181,192.168.1.2:2181,192.168.1.3:2181</code>
	 * </p>
	 * 
	 */
	public String getZkHost() {
		return zkHost;
	}

	/**
	 * Sets the IPs and their respective ports of the Zookeeper servers which belong to the Zookeeper ensemble. This
	 * string can contain multiple IP:port definitions separated by comma.
	 * <p>
	 * Example: <code>192.168.1.1:2181,192.168.1.2:2181,192.168.1.3:2181</code>
	 * </p>
	 * 
	 */
	public void setZkHost(String zkHost) {
		this.zkHost = zkHost;
	}

	/**
	 * Returns the SolrCloud collection name on which this {@link CloudSolrClient} will be connected to.
	 */
	public String getCollection() {
		return collection;
	}

	/**
	 * Set the SolrCloud collection name on which this {@link CloudSolrClient} will be connected to.
	 */
	public void setCollection(String collectionName) {
		this.collection = collectionName;
	}

	/**
	 * Returns the HTTP connection timeout of underlying {@link LBHttpSolrClient} used for queries, in milliseconds.
	 * <p>
	 * Default is 0 (infinite timeout)
	 * </p>
	 */
	public Integer getHttpConnectTimeout() {
		return httpConnectTimeout;
	}

	/**
	 * Sets the HTTP connection timeout of underlying {@link LBHttpSolrClient} in milliseconds.
	 * 
	 * @param httpConnectTimeout HTTP connect timeout in milliseconds . Default is 0 (infinite timeout)
	 *
	 */
	public void setHttpConnectTimeout(Integer httpConnectTimeout) {
		this.httpConnectTimeout = httpConnectTimeout;
	}

	/**
	 * Returns the HTTP soTimeout (read timeout) of underlying {@link LBHttpSolrClient} used for queries, in milliseconds.
	 * <p>
	 * Default is 0 (infinite timeout)
	 * </p>
	 */
	public Integer getHttpSoTimeout() {
		return httpSoTimeout;
	}

	/**
	 * Sets the HTTP soTimeout (read timeout) of underlying {@link LBHttpSolrClient} in milliseconds.
	 * 
	 * @param httpReadTimeout HTTP read timeout in milliseconds. Default is 0 (infinite timeout)
	 */
	public void setHttpSoTimeout(Integer httpSoTimeout) {
		this.httpSoTimeout = httpSoTimeout;
	}

	/**
	 * Returns the client timeout to the zookeeper ensemble in milliseconds
	 */
	public Integer getZkClientTimeout() {
		return zkClientTimeout;
	}

	/**
	 * Sets the client timeout to the zookeeper ensemble in milliseconds. Default value: 10000ms
	 * 
	 * @param zkClientTimeout client timeout to zookeeper ensemble in milliseconds.
	 */
	public void setZkClientTimeout(Integer zkClientTimeout) {
		this.zkClientTimeout = zkClientTimeout;
	}

	/**
	 * Returns the connection timeout to the zookeeper ensemble in milliseconds. Default value: 10000ms
	 * 
	 * @param zkConnectTimeout connection timeout to zookeeper ensemble in milliseconds.
	 */
	public Integer getZkConnectTimeout() {
		return zkConnectTimeout;
	}

	/**
	 * Sets the connection timeout to the zookeeper ensemble in milliseconds. Default value: 10000ms
	 * 
	 * @param zkConnectTimeout connection timeout to zookeeper ensemble in milliseconds.
	 */
	public void setZkConnectTimeout(Integer zkConnectTimeout) {
		this.zkConnectTimeout = zkConnectTimeout;
	}

}
