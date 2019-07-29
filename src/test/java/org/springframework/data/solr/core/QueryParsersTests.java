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

import static org.assertj.core.api.Assertions.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.core.query.TermsQuery;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 */
public class QueryParsersTests {

	private QueryParsers parsers;

	@Before
	public void setUp() {
		this.parsers = new QueryParsers(new SimpleSolrMappingContext());
	}

	@Test
	public void testGetForClassReturnsDefaultParserForUnknowQueryType() {
		assertThat(parsers.getForClass(SomeSolrQuery.class)).isInstanceOf(DefaultQueryParser.class);
	}

	@Test
	public void testGetForClassReturnsTermsQueryParserForTermsQueryType() {
		assertThat(parsers.getForClass(TermsQuery.class)).isInstanceOf(TermsQueryParser.class);
	}

	@Test
	public void testGetForClassReturnsCustomQueryParserIfAdded() {
		parsers.registerParser(SomeSolrQuery.class, new CustomQueryParser());
		assertThat(parsers.getForClass(SomeSolrQuery.class)).isInstanceOf(CustomQueryParser.class);
	}

	private interface SomeSolrQuery extends SolrDataQuery {

	}

	private class CustomQueryParser extends QueryParserBase<SolrDataQuery> {

		public CustomQueryParser() {
			super(new SimpleSolrMappingContext());
		}

		@Override
		public SolrQuery doConstructSolrQuery(SolrDataQuery query, @Nullable Class<?> domainType) {
			return null;
		}

	}

}
