/*
 * Copyright 2012 - 2013 the original author or authors.
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

import org.apache.solr.client.solrj.SolrServer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.solr.core.mapping.SolrDocument;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 */
public class MulticoreSolrServerFactory extends SolrServerFactoryBase {

	private boolean createMissingSolrServer = true;
	private Map<String, SolrServer> serverMap = new LinkedHashMap<String, SolrServer>();

	protected MulticoreSolrServerFactory() {
		super();
	}

	public MulticoreSolrServerFactory(SolrServer solrServer) {
		this(solrServer, Collections.<String> emptyList());
	}

	public MulticoreSolrServerFactory(SolrServer solrServer, String... cores) {
		this(solrServer, (cores != null ? Arrays.asList(cores) : Collections.<String> emptyList()));
	}

	public MulticoreSolrServerFactory(SolrServer solrServer, List<String> cores) {
		super(solrServer);
		for (String core : cores) {
			addSolrServerForCore(createServerForCore(solrServer, core), core);
		}
	}

	@Override
	public SolrServer getSolrServer(String core) {
		if (!StringUtils.hasText(core)) {
			return getSolrServer();
		}

		if (createMissingSolrServer && !serverMap.containsKey(core)) {
			serverMap.put(core, createServerForCore(getSolrServer(), core));
		}
		return serverMap.get(core);
	}

	/**
	 * Add SolrServer for core to factory - Will override existing.
	 * 
	 * @param solrServer
	 * @param core
	 */
	public void addSolrServerForCore(SolrServer solrServer, String core) {
		serverMap.put(core, solrServer);
	}

	/**
	 * Remove SolrServer from factory. Calls {@link SolrServer#shutdown()} on remove.
	 * 
	 * 
	 * @param core
	 */
	public void removeSolrSever(String core) {
		if (serverMap.containsKey(core)) {
			destroy(serverMap.remove(core));
		}
	}

	/**
	 * Get configured {@link SolrServer} for specific class tying to determine core name via {@link SolrDocument} or its
	 * class name.
	 * 
	 * @param clazz
	 * @return
	 */
	public SolrServer getSolrServer(Class<?> clazz) {
		Assert.notNull(clazz);

		String coreName = getShortClassName(clazz);
		SolrDocument annotation = AnnotationUtils.findAnnotation(clazz, SolrDocument.class);
		if (annotation != null && StringUtils.hasText(annotation.solrCoreName())) {
			coreName = annotation.solrCoreName();
		}
		return getSolrServer(coreName);
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
		return new ArrayList<String>(serverMap.keySet());
	}

	@Override
	public void destroy() {
		super.destroy();
		for (SolrServer server : serverMap.values()) {
			destroy(server);
		}
	}

	protected SolrServer createServerForCore(SolrServer reference, String core) {
		if (StringUtils.hasText(core)) {
			return SolrServerUtils.clone(reference, core);
		}
		return reference;
	}

	public boolean isCreateMissingSolrServer() {
		return createMissingSolrServer;
	}

	/**
	 * if true missing solrServers for cores will be created
	 * 
	 * @param createMissingSolrServer default is true
	 */
	public void setCreateMissingSolrServer(boolean createMissingSolrServer) {
		this.createMissingSolrServer = createMissingSolrServer;
	}

}
