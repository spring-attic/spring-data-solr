/*
 * Copyright 2012 - 2016 the original author or authors.
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
package org.springframework.data.solr;

import org.springframework.util.ClassUtils;

/**
 * Version util uses {@link org.springframework.util.ClassUtils#isPresent(String)} to determine presence of certain
 * classes that are unique to some libraries, which allows to en-/disable some of the features in eg.
 * {@link org.springframework.data.solr.core.DefaultQueryParser}.
 * 
 * @author Christoph Strobl
 */
public final class VersionUtil {

	private VersionUtil() {
		// hide utility class constructor
	}

	private static final boolean IS_SOLR_3_X_AVAILABLE = ClassUtils
			.isPresent("org.apache.solr.client.solrj.impl.CommonsHttpSolrServer", VersionUtil.class.getClassLoader());

	private static final boolean IS_JODATIME_AVAILABLE = ClassUtils.isPresent("org.joda.time.DateTime",
			VersionUtil.class.getClassLoader());

	private static final boolean IS_SOLR_4_2_AVAILABLE = ClassUtils.isPresent("org.apache.solr.parser.ParseException",
			VersionUtil.class.getClassLoader());

	private static final boolean IS_SOLR_4_X_AVAILABLE = ClassUtils
			.isPresent("org.apache.solr.client.solrj.impl.CloudSolrServer", VersionUtil.class.getClassLoader());

	private static final boolean IS_SOLR_5_X_AVAILABLE = ClassUtils.isPresent("org.apache.solr.client.solrj.SolrClient",
			VersionUtil.class.getClassLoader());

	/**
	 * @return true if {@code org.joda.time.DateTime} is in path
	 */
	public static boolean isJodaTimeAvailable() {
		return IS_JODATIME_AVAILABLE;
	}

	/**
	 * @return true if {@link org.apache.solr.client.solrj.impl.CommonsHttpSolrServer} (removed in solr 4.0.0) is in path
	 */
	public static boolean isSolr3XAvailable() {
		return IS_SOLR_3_X_AVAILABLE;
	}

	/**
	 * @return true if {@link org.apache.solr.client.solrj.impl.CloudSolrServer} (introduced in solr 4.0.0) is in path
	 */
	public static boolean isSolr4XAvailable() {
		return IS_SOLR_4_X_AVAILABLE;
	}

	/**
	 * @return true if {@code org.apache.solr.parser.ParseException} is in path
	 */
	public static boolean isSolr420Available() {
		return IS_SOLR_4_2_AVAILABLE;
	}

	/**
	 * @return true if {@link org.apache.solr.client.solrj.SolrClient} (introduced in solr 5.0.0) is in path
	 */
	public static boolean isSolr5XAvailable() {
		return IS_SOLR_5_X_AVAILABLE;
	}

}
