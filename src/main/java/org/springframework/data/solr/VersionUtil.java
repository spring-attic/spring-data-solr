/*
 * Copyright 2012 - 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
 * Version util uses {@link org.springframework.util.ClassUtils#isPresent(String, ClassLoader)} to determine presence of
 * certain classes that are unique to some libraries, which allows to en-/disable some of the features in eg.
 * {@link org.springframework.data.solr.core.DefaultQueryParser}.
 *
 * @author Christoph Strobl
 */
public final class VersionUtil {

	private VersionUtil() {
		// hide utility class constructor
	}

	private static final boolean IS_JODATIME_AVAILABLE = ClassUtils.isPresent("org.joda.time.DateTime",
			VersionUtil.class.getClassLoader());

	private static final boolean IS_SOLR_7 = ClassUtils.isPresent("org.apache.solr.client.solrj.V2RequestSupport",
			VersionUtil.class.getClassLoader());

	/**
	 * @return true if {@code org.joda.time.DateTime} is in path
	 */
	public static boolean isJodaTimeAvailable() {
		return IS_JODATIME_AVAILABLE;
	}

	/**
	 * @return {@literal true} if Solr7 is on the classpath.
	 * @since 4.0
	 */
	public static boolean isIsSolr7() {
		return IS_SOLR_7;
	}
}
