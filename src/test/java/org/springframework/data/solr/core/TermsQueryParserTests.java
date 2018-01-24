/*
 * Copyright 2012 - 2013 the original author or authors.
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.query.SimpleTermsQuery;
import org.springframework.data.solr.core.query.TermsOptions;
import org.springframework.data.solr.core.query.TermsOptions.RegexFlag;
import org.springframework.data.solr.core.query.TermsQuery;

/**
 * @author Christoph Strobl
 */
public class TermsQueryParserTests {

	private TermsQueryParser parser;

	@Before
	public void setUp() {
		this.parser = new TermsQueryParser();
	}

	@Test
	public void testConstructSolrQueryProcessesTermsLimitCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().limit(100).build();
		Assert.assertEquals(100, parser.constructSolrQuery(q).getTermsLimit());
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsLimitLessThanZero() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().limit(-1).build();
		Assert.assertEquals("Expected SolrQuery default value: 10", 10, parser.constructSolrQuery(q).getTermsLimit());
	}

	@Test
	public void testConstructSolrQueryProcessesTermsMaxCountCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().maxCount(100).build();
		Assert.assertEquals(100, parser.constructSolrQuery(q).getTermsMaxCount());
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsMaxCountLessThanMinusOne() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().maxCount(-2).build();
		Assert.assertEquals("Expected SolrQuery default value: -1", -1, parser.constructSolrQuery(q).getTermsMaxCount());
	}

	@Test
	public void testConstructSolrQueryProcessesTermsMinCountCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().minCount(100).build();
		Assert.assertEquals(100, parser.constructSolrQuery(q).getTermsMinCount());
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsMinCountLessThanZero() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().minCount(-1).build();
		Assert.assertEquals("Expected SolrQuery default value: 1", 1, parser.constructSolrQuery(q).getTermsMinCount());
	}

	@Test
	public void testConstructSolrQueryProcessesTermsPrefixCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().prefix("springdata").build();
		Assert.assertEquals("springdata", parser.constructSolrQuery(q).getTermsPrefix());
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsPrefixWhenNull() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().prefix(null).build();
		Assert.assertEquals("Expected SolrQuery default value: <empty string>", "", parser.constructSolrQuery(q)
				.getTermsPrefix());
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsPrefixWhenBlank() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().prefix("   ").build();
		Assert.assertEquals("Expected SolrQuery default value: <empty string>", "", parser.constructSolrQuery(q)
				.getTermsPrefix());
	}

	@Test
	public void testConstructSolrQueryProcessesTermsRegexCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().regex("solr").build();
		Assert.assertEquals("solr", parser.constructSolrQuery(q).getTermsRegex());
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsRegexWhenNull() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().regex(null).build();
		Assert.assertNull(parser.constructSolrQuery(q).getTermsRegex());
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsRegexWhenBlank() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().regex("   ").build();
		Assert.assertNull(parser.constructSolrQuery(q).getTermsRegex());
	}

	@Test
	public void testConstructSolrQueryProcessesTermsRegexFlagCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().regexFlag(RegexFlag.CASE_INSENSITIVE).build();
		Assert.assertArrayEquals(new String[] { "case_insensitive" }, parser.constructSolrQuery(q).getTermsRegexFlags());
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsRegexFlagWhenNull() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().regexFlag(null).build();
		Assert.assertNull(parser.constructSolrQuery(q).getTermsRegexFlags());
	}

	@Test
	public void testConstructSolrQueryProcessesTermsSortCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().sort(TermsOptions.Sort.INDEX).build();
		Assert.assertEquals("index", parser.constructSolrQuery(q).getTermsSortString());
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsSortWhenNull() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().sort(null).build();
		Assert.assertEquals("Expected SolrQuery default value: count", "count", parser.constructSolrQuery(q)
				.getTermsSortString());
	}

	@Test
	public void testConstructSolrQueryProcessesSingleFieldCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().fields("field_1").build();
		Assert.assertArrayEquals(new String[] { "field_1" }, parser.constructSolrQuery(q).getTermsFields());
	}

	@Test
	public void testConstructSolrQueryProcessesMultipleFieldsCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder("field_1", "field_2", "field_3").build();
		Assert.assertArrayEquals(new String[] { "field_1", "field_2", "field_3" }, parser.constructSolrQuery(q)
				.getTermsFields());
	}

	@Test
	public void testConstructSolrQueryProcessesRequestHandlerCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().handledBy("/termsRequestHandler").build();

		Assert.assertEquals("/termsRequestHandler", parser.constructSolrQuery(q).getRequestHandler());
	}

	@Test
	public void testConstructSolrQuerySetRequestHandlerToDefaultWhenNull() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().handledBy(null).build();

		Assert.assertEquals("/terms", parser.constructSolrQuery(q).getRequestHandler());
	}

	@Test
	public void testConstructSolrQuerySetRequestHandlerToDefaultWhenBlank() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().handledBy("   ").build();

		Assert.assertEquals("/terms", parser.constructSolrQuery(q).getRequestHandler());
	}

}
