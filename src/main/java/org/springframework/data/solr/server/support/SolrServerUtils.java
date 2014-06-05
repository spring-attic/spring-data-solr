/*
 * Copyright 2012 - 2014 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.solr.VersionUtil;
import org.springframework.data.solr.core.mapping.SolrDocument;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Christoph Strobl
 */
public class SolrServerUtils {

	private static final Logger logger = LoggerFactory.getLogger(SolrServerUtils.class);
	private static final String SLASH = "/";

	private SolrServerUtils() {}

	/**
	 * Resolve solr core/collection name for given type.
	 * 
	 * @param type
	 * @return empty string if {@link SolrDocument} not present or {@link SolrDocument#solrCoreName()} is blank.
	 * @since 1.1
	 */
	public static String resolveSolrCoreName(Class<?> type) {
		SolrDocument annotation = AnnotationUtils.findAnnotation(type, SolrDocument.class);
		if (annotation != null && StringUtils.isNotBlank(annotation.solrCoreName())) {
			return annotation.solrCoreName();
		}
		return "";
	}

	public static <T extends SolrServer> T clone(T solrServer) {
		return clone(solrServer, null);
	}

	/**
	 * Create a clone of given {@link SolrServer} and modify baseUrl of clone to point to the given core.
	 * 
	 * @param solrServer Non null reference {@link SolrServer} to copy properties from.
	 * @param core Name of solr core to point to.
	 * @return
	 * @throws BeanInstantiationException if creating instance failed
	 */
	@SuppressWarnings("unchecked")
	public static <T extends SolrServer> T clone(T solrServer, String core) {
		Assert.notNull(solrServer);
		String shortName = getSolrServerTypeName(solrServer);
		if (shortName.equals("SolrServer")) { // cannot create instance of interface,
			return solrServer;
		}

		SolrServer clone = null;
		if (shortName.equals("HttpSolrServer") || shortName.equals("CommonsHttpSolrServer")) {
			clone = cloneHttpSolrServer(solrServer, core);
		} else if (shortName.equals("LBHttpSolrServer")) {
			clone = cloneLBHttpSolrServer(solrServer, core);
		} else if (shortName.equals("CloudSolrServer")) {
			clone = cloneCloudSolrServer(solrServer, core);
		} else if (shortName.equals("EmbeddedSolrServer")) {
			clone = cloneEmbeddedSolrServer(solrServer, core);
		}

		if (clone == null) {
			throw new BeanInstantiationException(solrServer.getClass(), "Cannot create instace of " + shortName + ".");
		}

		copyProperties(solrServer, clone);
		return (T) clone;
	}

	/**
	 * Append core to given baseUrl
	 * 
	 * @param baseUrl
	 * @param core
	 * @return
	 */
	public static String appendCoreToBaseUrl(String baseUrl, String core) {
		Assert.notNull(baseUrl);

		if (!org.springframework.util.StringUtils.hasText(core)) {
			return baseUrl;
		}
		String url = baseUrl;
		if (!StringUtils.endsWith(baseUrl, SLASH)) {
			url = url + SLASH;
		}
		url = url + core;
		return url;
	}

	private static String getSolrServerTypeName(SolrServer solrServer) {
		Class<?> solrServerType = ClassUtils.isCglibProxy(solrServer) ? ClassUtils.getUserClass(solrServer) : solrServer
				.getClass();
		String shortName = ClassUtils.getShortName(solrServerType);
		return shortName;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static SolrServer cloneEmbeddedSolrServer(SolrServer solrServer, String core) {
		String solrHome = ((EmbeddedSolrServer) solrServer).getCoreContainer().getSolrHome();
		try {
			Constructor constructor = ClassUtils.getConstructorIfAvailable(solrServer.getClass(), CoreContainer.class,
					String.class);
			return (SolrServer) BeanUtils.instantiateClass(constructor, new CoreContainer(solrHome), core);
		} catch (Exception e) {
			throw new BeanInstantiationException(solrServer.getClass(), "Cannot create instace of " + solrServer.getClass()
					+ ".", e);
		}
	}

	private static SolrServer cloneHttpSolrServer(SolrServer solrServer, String core) {
		if (solrServer == null) {
			return null;
		}
		Method baseUrlGetterMethod = ClassUtils.getMethodIfAvailable(solrServer.getClass(), "getBaseURL");
		if (baseUrlGetterMethod == null) {
			return null;
		}
		String baseUrl = (String) ReflectionUtils.invokeMethod(baseUrlGetterMethod, solrServer);
		String url = appendCoreToBaseUrl(baseUrl, core);
		Constructor<? extends SolrServer> constructor = (Constructor<? extends SolrServer>) ClassUtils
				.getConstructorIfAvailable(solrServer.getClass(), String.class);
		return (SolrServer) BeanUtils.instantiateClass(constructor, url);
	}

	private static LBHttpSolrServer cloneLBHttpSolrServer(SolrServer solrServer, String core) {
		if (solrServer == null) {
			return null;
		}

		LBHttpSolrServer clone = null;
		try {
			if (VersionUtil.isSolr3XAvailable()) {
				clone = cloneSolr3LBHttpServer(solrServer, core);
			} else if (VersionUtil.isSolr4XAvailable()) {
				clone = cloneSolr4LBHttpServer(solrServer, core);
			}
		} catch (MalformedURLException e) {
			throw new BeanInstantiationException(solrServer.getClass(), "Cannot create instace of " + solrServer.getClass()
					+ ". ", e);
		}
		Object o = readField(solrServer, "interval");
		if (o != null) {
			clone.setAliveCheckInterval(Integer.valueOf(o.toString()).intValue());
		}
		return clone;
	}

	private static SolrServer cloneCloudSolrServer(SolrServer solrServer, String core) {
		if (VersionUtil.isSolr3XAvailable() || solrServer == null) {
			return null;
		}

		CloudSolrServer cloudServer = (CloudSolrServer) solrServer;
		String zkHost = readField(solrServer, "zkHost");

		Constructor<? extends SolrServer> constructor = (Constructor<? extends SolrServer>) ClassUtils
				.getConstructorIfAvailable(solrServer.getClass(), String.class, LBHttpSolrServer.class);

		CloudSolrServer clone = (CloudSolrServer) BeanUtils.instantiateClass(constructor, zkHost,
				cloneLBHttpSolrServer(cloudServer.getLbServer(), core));

		if (org.springframework.util.StringUtils.hasText(core)) {
			clone.setDefaultCollection(core);
		}
		return clone;
	}

	private static LBHttpSolrServer cloneSolr3LBHttpServer(SolrServer solrServer, String core)
			throws MalformedURLException {
		CopyOnWriteArrayList<?> list = readField(solrServer, "aliveServers");

		String[] servers = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			servers[i] = appendCoreToBaseUrl(list.get(i).toString(), core);
		}
		return new LBHttpSolrServer(servers);
	}

	private static LBHttpSolrServer cloneSolr4LBHttpServer(SolrServer solrServer, String core)
			throws MalformedURLException {
		Map<String, ?> map = readField(solrServer, "aliveServers");

		String[] servers = new String[map.size()];
		int i = 0;
		for (String key : map.keySet()) {
			servers[i] = appendCoreToBaseUrl(key, core);
			i++;
		}
		return new LBHttpSolrServer(servers);
	}

	@SuppressWarnings("unchecked")
	private static <T> T readField(SolrServer solrServer, String fieldName) {
		Field field = ReflectionUtils.findField(solrServer.getClass(), fieldName);
		if (field == null) {
			return null;
		}
		ReflectionUtils.makeAccessible(field);
		return (T) ReflectionUtils.getField(field, solrServer);
	}

	/**
	 * Solr property names do not match the getters/setters used for them. Check on any write method, try to find the
	 * according property and set the value for it. Will ignore all other, and nested properties
	 * 
	 * @param source
	 * @param target
	 */
	private static void copyProperties(SolrServer source, SolrServer target) {
		BeanWrapperImpl wrapperImpl = new BeanWrapperImpl(source);
		for (PropertyDescriptor pd : wrapperImpl.getPropertyDescriptors()) {
			Method writer = pd.getWriteMethod();
			if (writer != null) {
				try {
					Field property = ReflectionUtils.findField(source.getClass(), pd.getName());
					if (property != null) {
						ReflectionUtils.makeAccessible(property);
						Object o = ReflectionUtils.getField(property, source);
						if (o != null) {
							writer.invoke(target, o);
						}
					}
				} catch (Exception e) {
					logger.warn("Could not copy property value for: " + pd.getName(), e);
				}
			}
		}
	}

}
