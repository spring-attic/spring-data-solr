/*
 * Copyright 2012 - 2015 the original author or authors.
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

import java.lang.reflect.Field;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Manios Christos
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("CloudSolrClientFactoryTest-context.xml")
public class CloudSolrClientFactoryTests {

	@Autowired
	ApplicationContext context;

	private static final String zkHost = "127.0.0.1:2181,127.0.0.1:2182";
	private static final Integer expectedHttpConnectTimeout = 1500;
	private static final Integer expectedHttpSoTimeout = 1800;
	private static final Integer expectedZkConnectTimeout = 1300;
	private static final Integer expectedZkClientTimeout = 1400;
	private static final String collectionName = "jet2pilot";

	/**
	 * Testing issue DATASOLR-211
	 */
	@Test
	public void testCreateCloudSorlClientUsingFactory() {

		// test solrtemplate
		SolrTemplate solrTemplate = context.getBean("solrTemplate", SolrTemplate.class);
		Assert.assertNotNull(solrTemplate);

		CloudSolrClient clientFromfactoryBean = context.getBean("cloudSolrClientFactory", CloudSolrClient.class);
		Assert.assertNotNull(clientFromfactoryBean);

		// check that solr client is not null
		CloudSolrClient solrClient = (CloudSolrClient) solrTemplate.getSolrClient();
		Assert.assertNotNull(solrClient);

		Assert.assertSame(solrClient, clientFromfactoryBean);

		Assert.assertEquals(collectionName, solrClient.getDefaultCollection());

		// get httpParams() which is deprecated in order to test timeouts
		// I could not find another way to get them..
		HttpParams httpParams = solrClient.getLbClient().getHttpClient().getParams();

		Assert.assertEquals(expectedHttpConnectTimeout, (Integer) HttpConnectionParams.getConnectionTimeout(httpParams));
		Assert.assertEquals(expectedHttpSoTimeout, (Integer) HttpConnectionParams.getSoTimeout(httpParams));

		// now try to get private fields using reflection

		try {
			// try to get zkHost
			Field actualZkHostField = solrClient.getClass().getDeclaredField("zkHost");

			// try to get zkConnectTimeout
			Field actualZkConnectTimeoutField = solrClient.getClass().getDeclaredField("zkConnectTimeout");

			// try to get zkClientTimeout
			Field actualZkClientTimeoutField = solrClient.getClass().getDeclaredField("zkClientTimeout");

			Assert.assertEquals(zkHost, (String) actualZkHostField.get(solrClient));
			Assert.assertEquals(expectedZkConnectTimeout.intValue(), actualZkConnectTimeoutField.getInt(solrClient));
			Assert.assertEquals(expectedZkClientTimeout.intValue(), actualZkClientTimeoutField.getInt(solrClient));

		} catch (NoSuchFieldException e) {
		} catch (SecurityException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}

	}
}
