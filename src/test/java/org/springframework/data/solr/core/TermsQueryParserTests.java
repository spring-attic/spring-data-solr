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

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
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
		this.parser = new TermsQueryParser(new SimpleSolrMappingContext());
	}

	@Test
	public void testConstructSolrQueryProcessesTermsLimitCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().limit(100).build();
		assertThat(parser.constructSolrQuery(q, null).getTermsLimit()).isEqualTo(100);
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsLimitLessThanZero() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().limit(-1).build();
		assertThat(parser.constructSolrQuery(q, null).getTermsLimit()).as("Expected SolrQuery default value: 10")
				.isEqualTo(10);
	}

	@Test
	public void testConstructSolrQueryProcessesTermsMaxCountCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().maxCount(100).build();
		assertThat(parser.constructSolrQuery(q, null).getTermsMaxCount()).isEqualTo(100);
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsMaxCountLessThanMinusOne() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().maxCount(-2).build();
		assertThat(parser.constructSolrQuery(q, null).getTermsMaxCount()).as("Expected SolrQuery default value: -1")
				.isEqualTo(-1);
	}

	@Test
	public void testConstructSolrQueryProcessesTermsMinCountCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().minCount(100).build();
		assertThat(parser.constructSolrQuery(q, null).getTermsMinCount()).isEqualTo(100);
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsMinCountLessThanZero() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().minCount(-1).build();
		assertThat(parser.constructSolrQuery(q, null).getTermsMinCount()).as("Expected SolrQuery default value: 1")
				.isEqualTo(1);
	}

	@Test
	public void testConstructSolrQueryProcessesTermsPrefixCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().prefix("springdata").build();
		assertThat(parser.constructSolrQuery(q, null).getTermsPrefix()).isEqualTo("springdata");
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsPrefixWhenNull() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().prefix(null).build();
		assertThat(parser.constructSolrQuery(q, null).getTermsPrefix())
				.as("Expected SolrQuery default value: <empty string>").isEqualTo("");
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsPrefixWhenBlank() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().prefix("   ").build();
		assertThat(parser.constructSolrQuery(q, null).getTermsPrefix())
				.as("Expected SolrQuery default value: <empty string>").isEqualTo("");
	}

	@Test
	public void testConstructSolrQueryProcessesTermsRegexCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().regex("solr").build();
		assertThat(parser.constructSolrQuery(q, null).getTermsRegex()).isEqualTo("solr");
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsRegexWhenNull() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().regex(null).build();
		assertThat(parser.constructSolrQuery(q, null).getTermsRegex()).isNull();
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsRegexWhenBlank() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().regex("   ").build();
		assertThat(parser.constructSolrQuery(q, null).getTermsRegex()).isNull();
	}

	@Test
	public void testConstructSolrQueryProcessesTermsRegexFlagCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().regexFlag(RegexFlag.CASE_INSENSITIVE).build();
		assertThat(parser.constructSolrQuery(q, null).getTermsRegexFlags()).isEqualTo(new String[] { "case_insensitive" });
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsRegexFlagWhenNull() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().regexFlag(null).build();
		assertThat(parser.constructSolrQuery(q, null).getTermsRegexFlags()).isNull();
	}

	@Test
	public void testConstructSolrQueryProcessesTermsSortCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().sort(TermsOptions.Sort.INDEX).build();
		assertThat(parser.constructSolrQuery(q, null).getTermsSortString()).isEqualTo("index");
	}

	@Test
	public void testConstructSolrQueryIgnoresTermsSortWhenNull() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().sort(null).build();
		assertThat(parser.constructSolrQuery(q, null).getTermsSortString()).as("Expected SolrQuery default value: count")
				.isEqualTo("count");
	}

	@Test
	public void testConstructSolrQueryProcessesSingleFieldCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().fields("field_1").build();
		assertThat(parser.constructSolrQuery(q, null).getTermsFields()).isEqualTo(new String[] { "field_1" });
	}

	@Test
	public void testConstructSolrQueryProcessesMultipleFieldsCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder("field_1", "field_2", "field_3").build();
		assertThat(parser.constructSolrQuery(q, null).getTermsFields())
				.isEqualTo(new String[] { "field_1", "field_2", "field_3" });
	}

	@Test
	public void testConstructSolrQueryProcessesRequestHandlerCorrectly() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().handledBy("/termsRequestHandler").build();

		assertThat(parser.constructSolrQuery(q, null).getRequestHandler()).isEqualTo("/termsRequestHandler");
	}

	@Test
	public void testConstructSolrQuerySetRequestHandlerToDefaultWhenNull() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().handledBy(null).build();

		assertThat(parser.constructSolrQuery(q, null).getRequestHandler()).isEqualTo("/terms");
	}

	@Test
	public void testConstructSolrQuerySetRequestHandlerToDefaultWhenBlank() {
		TermsQuery q = SimpleTermsQuery.queryBuilder().handledBy("   ").build();

		assertThat(parser.constructSolrQuery(q, null).getRequestHandler()).isEqualTo("/terms");
	}

}
