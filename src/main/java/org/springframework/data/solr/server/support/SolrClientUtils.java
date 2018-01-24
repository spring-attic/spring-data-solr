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

import java.beans.PropertyDescriptor;
import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.solr.VersionUtil;
import org.springframework.data.solr.core.mapping.SolrDocument;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link SolrClientUtils} replaces SolrServerUtils from version 1.x
 * 
 * @author Christoph Strobl
 * @since 2.0
 */
public class SolrClientUtils {

	private static final Logger logger = LoggerFactory.getLogger(SolrClientUtils.class);
	private static final String SLASH = "/";

	private SolrClientUtils() {}

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

	public static <T extends SolrClient> T clone(T solrClient) {
		return clone(solrClient, null);
	}

	/**
	 * Create a clone of given {@link SolrClient} and modify baseUrl of clone to point to the given core.
	 * 
	 * @param solrClient Non null reference {@link SolrClient} to copy properties from.
	 * @param core Name of solr core to point to.
	 * @return
	 * @throws BeanInstantiationException if creating instance failed
	 */
	@SuppressWarnings("unchecked")
	public static <T extends SolrClient> T clone(T solrClient, String core) {
		Assert.notNull(solrClient, "SolrClient must not be null!");
		String shortName = getSolrClientTypeName(solrClient);
		if (shortName.equals("SolrClient")) { // cannot create instance of abstract class,
			return solrClient;
		}

		SolrClient clone = null;
		if (shortName.equals("HttpSolrClient")) {
			clone = cloneHttpSolrClient(solrClient, core);
		} else if (shortName.equals("LBHttpSolrClient")) {
			clone = cloneLBHttpSolrClient(solrClient, core);
		} else if (shortName.equals("CloudSolrClient")) {
			clone = cloneCloudSolrClient(solrClient, core);
		} else if (shortName.equals("EmbeddedSolrServer")) {
			clone = cloneEmbeddedSolrServer(solrClient, core);
		}

		if (clone == null) {
			throw new BeanInstantiationException(solrClient.getClass(), "Cannot create instace of " + shortName + ".");
		}

		copyProperties(solrClient, clone);
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
		Assert.notNull(baseUrl, "Solr baseUrl must not be null!");

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

	/**
	 * Close the {@link SolrClient} by calling {@link SolrClient#close()} or {@code shutdown} for the generation 5
	 * libraries.
	 *
	 * @param solrClient must not be {@literal null}.
	 * @throws DataAccessResourceFailureException
	 * @since 2.1
	 */
	public static void close(SolrClient solrClient) {

		Assert.notNull(solrClient, "SolrClient must not be null!");

		try {
			if (solrClient instanceof Closeable) {
				solrClient.close();
			} else {
				Method shutdownMethod = ReflectionUtils.findMethod(solrClient.getClass(), "shutdown");
				if (shutdownMethod != null) {
					shutdownMethod.invoke(solrClient);
				}
			}
		} catch (Exception e) {
			throw new DataAccessResourceFailureException("Cannot close SolrClient", e);
		}
	}

	private static String getSolrClientTypeName(SolrClient solrClient) {
		Class<?> solrClientType = ClassUtils.isCglibProxy(solrClient) ? ClassUtils.getUserClass(solrClient)
				: solrClient.getClass();
		String shortName = ClassUtils.getShortName(solrClientType);
		return shortName;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static SolrClient cloneEmbeddedSolrServer(SolrClient solrClient, String core) {

		CoreContainer coreContainer = ((EmbeddedSolrServer) solrClient).getCoreContainer();
		try {
			Constructor constructor = ClassUtils.getConstructorIfAvailable(solrClient.getClass(), CoreContainer.class,
					String.class);
			return (SolrClient) BeanUtils.instantiateClass(constructor, coreContainer, core);
		} catch (Exception e) {
			throw new BeanInstantiationException(solrClient.getClass(),
					"Cannot create instace of " + solrClient.getClass() + ".", e);
		}
	}

	private static SolrClient cloneHttpSolrClient(SolrClient solrClient, String core) {
		if (solrClient == null) {
			return null;
		}

		Method baseUrlGetterMethod = ClassUtils.getMethodIfAvailable(solrClient.getClass(), "getBaseURL");
		if (baseUrlGetterMethod == null) {
			return null;
		}

		String baseUrl = (String) ReflectionUtils.invokeMethod(baseUrlGetterMethod, solrClient);
		String url = appendCoreToBaseUrl(baseUrl, core);

		try {

			HttpClient clientToUse = readAndCloneHttpClient(solrClient);

			if (clientToUse != null) {
				Constructor<? extends SolrClient> constructor = (Constructor<? extends SolrClient>) ClassUtils
						.getConstructorIfAvailable(solrClient.getClass(), String.class, HttpClient.class);
				if (constructor != null) {
					return (SolrClient) BeanUtils.instantiateClass(constructor, url, clientToUse);
				}
			}

			Constructor<? extends SolrClient> constructor = (Constructor<? extends SolrClient>) ClassUtils
					.getConstructorIfAvailable(solrClient.getClass(), String.class);
			return (SolrClient) BeanUtils.instantiateClass(constructor, url);
		} catch (Exception e) {
			throw new BeanInstantiationException(solrClient.getClass(),
					"Cannot create instace of " + solrClient.getClass() + ". ", e);
		}
	}

	private static LBHttpSolrClient cloneLBHttpSolrClient(SolrClient solrClient, String core) {
		if (solrClient == null) {
			return null;
		}

		LBHttpSolrClient clone = null;
		try {
			if (VersionUtil.isSolr3XAvailable()) {
				clone = cloneSolr3LBHttpServer(solrClient, core);
			} else if (VersionUtil.isSolr4XAvailable() || VersionUtil.isSolr5XAvailable()) {
				clone = cloneSolr4LBHttpServer(solrClient, core);
			}
		} catch (Exception e) {
			throw new BeanInstantiationException(solrClient.getClass(),
					"Cannot create instace of " + solrClient.getClass() + ". ", e);
		}
		Object o = readField(solrClient, "interval");
		if (o != null) {
			clone.setAliveCheckInterval(Integer.valueOf(o.toString()).intValue());
		}
		return clone;
	}

	private static SolrClient cloneCloudSolrClient(SolrClient solrClient, String core) {
		if (VersionUtil.isSolr3XAvailable() || solrClient == null) {
			return null;
		}

		CloudSolrClient cloudServer = (CloudSolrClient) solrClient;
		String zkHost = readField(solrClient, "zkHost");

		Constructor<? extends SolrClient> constructor = (Constructor<? extends SolrClient>) ClassUtils
				.getConstructorIfAvailable(solrClient.getClass(), String.class, LBHttpSolrClient.class);

		CloudSolrClient clone = (CloudSolrClient) BeanUtils.instantiateClass(constructor, zkHost,
				cloneLBHttpSolrClient(cloudServer.getLbClient(), core));

		if (org.springframework.util.StringUtils.hasText(core)) {
			clone.setDefaultCollection(core);
		}
		return clone;
	}

	private static LBHttpSolrClient cloneSolr3LBHttpServer(SolrClient solrClient, String core)
			throws MalformedURLException {
		CopyOnWriteArrayList<?> list = readField(solrClient, "aliveServers");

		String[] servers = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			servers[i] = appendCoreToBaseUrl(list.get(i).toString(), core);
		}
		return new LBHttpSolrClient(servers);
	}

	private static LBHttpSolrClient cloneSolr4LBHttpServer(SolrClient solrClient, String core)
			throws MalformedURLException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		Map<String, ?> map = readField(solrClient, "aliveServers");

		String[] servers = new String[map.size()];
		int i = 0;
		for (String key : map.keySet()) {
			servers[i] = appendCoreToBaseUrl(key, core);
			i++;
		}

		Boolean isInternalCient = readField(solrClient, "clientIsInternal");

		if (isInternalCient != null && !isInternalCient) {
			HttpClient clientToUse = readAndCloneHttpClient(solrClient);
			return new LBHttpSolrClient(clientToUse, servers);
		}
		return new LBHttpSolrClient(servers);
	}

	private static HttpClient readAndCloneHttpClient(SolrClient solrClient)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		HttpClient sourceClient = readField(solrClient, "httpClient");
		return cloneHttpClient(sourceClient);
	}

	private static HttpClient cloneHttpClient(HttpClient sourceClient)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		if (sourceClient == null) {
			return null;
		}

		Class<?> clientType = ClassUtils.getUserClass(sourceClient);

		Constructor<?> constructor = ClassUtils.getConstructorIfAvailable(clientType, ClientConnectionManager.class,
				HttpParams.class);
		if (constructor != null) {

			HttpClient targetClient = (HttpClient) constructor.newInstance(sourceClient.getConnectionManager(),
					sourceClient.getParams());
			BeanUtils.copyProperties(sourceClient, targetClient);
			return targetClient;
		}

		constructor = ClassUtils.getConstructorIfAvailable(clientType, HttpParams.class);
		if (constructor != null) {

			HttpClient targetClient = (HttpClient) constructor.newInstance(sourceClient.getParams());
			BeanUtils.copyProperties(sourceClient, targetClient);
			return targetClient;
		} else {
			return new DefaultHttpClient(sourceClient.getParams());
		}

	}

	@SuppressWarnings("unchecked")
	private static <T> T readField(SolrClient solrServer, String fieldName) {
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
	private static void copyProperties(SolrClient source, SolrClient target) {
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
