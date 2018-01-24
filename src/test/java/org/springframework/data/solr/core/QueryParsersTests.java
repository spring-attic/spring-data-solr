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
package org.springframework.data.solr.core;

import org.apache.solr.client.solrj.SolrQuery;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.core.query.TermsQuery;

/**
 * @author Christoph Strobl
 */
public class QueryParsersTests {

	private QueryParsers parsers;

	@Before
	public void setUp() {
		this.parsers = new QueryParsers();
	}

	@Test
	public void testGetForClassReturnsDefaultParserForUnknowQueryType() {
		Assert.assertThat(parsers.getForClass(SomeSolrQuery.class), IsInstanceOf.instanceOf(DefaultQueryParser.class));
	}

	@Test
	public void testGetForClassReturnsTermsQueryParserForTermsQueryType() {
		Assert.assertThat(parsers.getForClass(TermsQuery.class), IsInstanceOf.instanceOf(TermsQueryParser.class));
	}

	@Test
	public void testGetForClassReturnsCustomQueryParserIfAdded() {
		parsers.registerParser(SomeSolrQuery.class, new CustomQueryParser());
		Assert.assertThat(parsers.getForClass(SomeSolrQuery.class), IsInstanceOf.instanceOf(CustomQueryParser.class));
	}

	private interface SomeSolrQuery extends SolrDataQuery {

	}

	private class CustomQueryParser extends QueryParserBase<SolrDataQuery> {

		@Override
		public SolrQuery doConstructSolrQuery(SolrDataQuery query) {
			return null;
		}

	}

}
