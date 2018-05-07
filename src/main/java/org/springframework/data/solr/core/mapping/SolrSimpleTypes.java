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
package org.springframework.data.solr.core.mapping;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.solr.common.SolrInputDocument;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Set of type that do not need to be converted into a solr readable format
 *
 * @author Christoph Strobl
 */
public class SolrSimpleTypes {

	private SolrSimpleTypes() {
		// hide utility class constructor
	}

	static {
		Set<Class<?>> simpleTypes = new HashSet<>();
		simpleTypes.add(BigInteger.class);
		simpleTypes.add(SolrInputDocument.class);

		SOLR_SIMPLE_TYPES = Collections.unmodifiableSet(simpleTypes);
	}

	private static final Set<Class<?>> SOLR_SIMPLE_TYPES;

	public static final SimpleTypeHolder HOLDER = new SimpleTypeHolder(SOLR_SIMPLE_TYPES, true);

}
