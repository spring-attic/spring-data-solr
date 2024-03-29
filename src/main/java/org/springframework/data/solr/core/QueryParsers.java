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
package org.springframework.data.solr.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.core.query.TermsQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 */
public class QueryParsers {

	private final QueryParser defaultQueryParser;

	private final List<QueryParserPair> parserPairs;

	private final Map<Class<?>, QueryParser> cache = new LinkedHashMap<>();
	private final @Nullable MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext;

	/**
	 * @param mappingContext can be {@literal null}.
	 * @since 4.0
	 */
	public QueryParsers(
			@Nullable MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext) {

		this.parserPairs = new ArrayList<>(4);
		this.mappingContext = mappingContext;
		this.defaultQueryParser = new DefaultQueryParser(mappingContext);

		parserPairs.add(new QueryParserPair(TermsQuery.class, new TermsQueryParser(mappingContext)));
		parserPairs.add(new QueryParserPair(FacetQuery.class, defaultQueryParser));
		parserPairs.add(new QueryParserPair(HighlightQuery.class, defaultQueryParser));
		parserPairs.add(new QueryParserPair(Query.class, defaultQueryParser));
	}

	/**
	 * Get the {@link QueryParser} for given query type
	 *
	 * @param clazz
	 * @return {@link DefaultQueryParser} if no matching parser found
	 */
	public QueryParser getForClass(Class<? extends SolrDataQuery> clazz) {

		QueryParser queryParser = cache.get(clazz);
		if (queryParser == null) {
			for (QueryParserPair pair : parserPairs) {
				if (pair.canParser(clazz)) {
					this.cache.put(clazz, pair.getParser());
					queryParser = pair.getParser();
					break;
				}
			}
		}

		return queryParser != null ? queryParser : defaultQueryParser;
	}

	/**
	 * Register additional {@link QueryParser} for {@link SolrQuery}
	 *
	 * @param clazz
	 * @param parser
	 */
	public void registerParser(Class<? extends SolrDataQuery> clazz, QueryParser parser) {
		Assert.notNull(parser, "Cannot register 'null' parser");
		parserPairs.add(0, new QueryParserPair(clazz, parser));
		cache.clear();
	}

	/**
	 * QueryParserPair holds reference form the {@link SolrQuery} to the {@link QueryParser} suitable for it
	 *
	 * @author Christoph Strobl
	 */
	private static class QueryParserPair {

		private final Class<?> clazz;
		private final QueryParser parser;

		/**
		 * @param clazz Class to register parser for
		 * @param parser Parser capable of handling types of given class
		 */
		public QueryParserPair(Class<?> clazz, QueryParser parser) {
			this.parser = parser;
			this.clazz = clazz;
		}

		public QueryParser getParser() {
			return this.parser;
		}

		/**
		 * @param clazz
		 * @return true if {@link ClassUtils#isAssignable(Class, Class)}
		 */
		public boolean canParser(Class<?> clazz) {
			return ClassUtils.isAssignable(this.clazz, clazz);
		}
	}

}
