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
package org.springframework.data.solr.core.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @since 1.1
 */
public interface Function {

	/**
	 * solr readable representation of function
	 *
	 * @return
	 */
	String getOperation();

	/**
	 * @return
	 */
	Iterable<?> getArguments();

	/**
	 * @return true if {@link #getArguments()} is not empty
	 */
	boolean hasArguments();

	/**
	 * Convert the Function to a Solr readable {@link String} in the given {@link Context}.
	 *
	 * @param context must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 4.2
	 */
	default String toSolrFunction(Context context) {

		StringBuilder sb = new StringBuilder();

		sb.append("{!func}");
		sb.append(getOperation());
		sb.append('(');

		if (hasArguments()) {

			List<String> solrReadableArguments = new ArrayList<>();
			for (Object arg : getArguments()) {

				Assert.notNull(arg, "Unable to parse 'null' within function arguments.");
				solrReadableArguments.add(context.convert(arg));
			}
			sb.append(StringUtils.join(solrReadableArguments, ','));
		}
		sb.append(')');

		return sb.toString();
	}

	/**
	 * Get the {@link Map} of already Solr readable (converted) arguments for this {@link Function} in the given
	 * {@link Context}.
	 *
	 * @param context must not be {@literal null}.
	 * @return never {@literal null}.
	 * @since 4.1
	 */
	default Map<String, String> getArgumentMap(Context context) {
		return Collections.emptyMap();
	}

	/**
	 * The {@link Context} the {@link Function} is used in.
	 * 
	 * @since 4.1
	 */
	interface Context {

		/**
		 * Convert the given value into a Solr readable {@link String} argument.
		 *
		 * @param value must not be {@literal null}.
		 * @return the Solr readable representation of the given value.
		 */
		String convert(Object value);

		Target getTarget();

		/**
		 * The actual context target.
		 */
		enum Target {
			SORT, PROJECTION, QUERY
		}
	}

}
