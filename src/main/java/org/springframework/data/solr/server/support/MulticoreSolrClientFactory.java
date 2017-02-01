/*
 * Copyright 2012 - 2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.data.solr.core.mapping.SolrDocument;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link MulticoreSolrClientFactory} replaces MulticoreSolrServerFactory from version 1.x.
 * 
 * @author Christoph Strobl
 * @since 2.0
 */
public class MulticoreSolrClientFactory extends SolrClientFactoryBase {

	private boolean createMissingSolrClient = true;
	private Map<String, SolrClient> clientMap = new LinkedHashMap<String, SolrClient>();

	protected MulticoreSolrClientFactory() {
		super();
	}

	public MulticoreSolrClientFactory(SolrClient solrServer) {
		this(solrServer, Collections.<String> emptyList());
	}

	public MulticoreSolrClientFactory(SolrClient solrServer, String... cores) {
		this(solrServer, (cores != null ? Arrays.asList(cores) : Collections.<String> emptyList()));
	}

	public MulticoreSolrClientFactory(SolrClient solrServer, List<String> cores) {
		super(solrServer);
		for (String core : cores) {
			addSolrClientForCore(createClientForCore(solrServer, core), core);
		}
	}

	@Override
	public SolrClient getSolrClient(String core) {
		if (!StringUtils.hasText(core)) {
			return getSolrClient();
		}

		if (createMissingSolrClient && !clientMap.containsKey(core)) {
			clientMap.put(core, createClientForCore(getSolrClient(), core));
		}
		return clientMap.get(core);
	}

	/**
	 * Add SolrClient for core to factory - Will override existing.
	 * 
	 * @param solrClient
	 * @param core
	 */
	public void addSolrClientForCore(SolrClient solrClient, String core) {
		clientMap.put(core, solrClient);
	}

	/**
	 * Remove SolrClient from factory. Calls {@link SolrClient#shutdown()} on remove.
	 * 
	 * @param core
	 */
	public void removeSolrClient(String core) {
		if (clientMap.containsKey(core)) {
			destroy(clientMap.remove(core));
		}
	}

	/**
	 * Get configured {@link SolrClient} for specific class tying to determine core name via {@link SolrDocument} or its
	 * class name.
	 * 
	 * @param clazz
	 * @return
	 */
	public SolrClient getSolrClient(Class<?> clazz) {
		Assert.notNull(clazz, "Clazz must not be null!");

		String coreName = SolrClientUtils.resolveSolrCoreName(clazz);
		return getSolrClient(StringUtils.hasText(coreName) ? coreName : getShortClassName(clazz));
	}

	/**
	 * Get the class short name. Strips the outer class name in case of an inner class.
	 * 
	 * @param clazz
	 * @see ClassUtils#getShortName(Class)
	 * @return
	 */
	protected static String getShortClassName(Class<?> clazz) {
		String shortName = ClassUtils.getShortName(clazz);
		int dotIndex = shortName.lastIndexOf('.');
		return (dotIndex != -1 ? shortName.substring(dotIndex + 1) : shortName);
	}

	@Override
	public List<String> getCores() {
		return new ArrayList<String>(clientMap.keySet());
	}

	@Override
	public void destroy() {
		super.destroy();
		for (SolrClient server : clientMap.values()) {
			destroy(server);
		}
	}

	protected SolrClient createClientForCore(SolrClient reference, String core) {
		if (StringUtils.hasText(core)) {
			return SolrClientUtils.clone(reference, core);
		}
		return reference;
	}

	public boolean isCreateMissingSolrClient() {
		return createMissingSolrClient;
	}

	/**
	 * if true missing solrServers for cores will be created
	 * 
	 * @param createMissingSolrClient default is true
	 */
	public void setCreateMissingSolrClient(boolean createMissingSolrClient) {
		this.createMissingSolrClient = createMissingSolrClient;
	}

}
