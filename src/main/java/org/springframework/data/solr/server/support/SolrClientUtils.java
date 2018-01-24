/*
 * Copyright 2012 - 2018 the original author or authors.
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

import java.io.Closeable;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.solr.core.mapping.SolrDocument;
import org.springframework.util.Assert;
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
}
