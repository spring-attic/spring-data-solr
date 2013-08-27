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

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.QueryParserBase.BaseCriteriaEntryProcessor;
import org.springframework.data.solr.core.QueryParserBase.CriteriaEntryProcessor;
import org.springframework.data.solr.core.QueryParserBase.DefaultProcessor;
import org.springframework.data.solr.core.QueryParserBase.WildcardProcessor;
import org.springframework.data.solr.core.query.Criteria.CriteriaEntry;
import org.springframework.data.solr.core.query.Criteria.OperationKey;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.SolrDataQuery;

/**
 * @author Christoph Strobl
 */
public class QueryParserBaseTests {

	private static final String SOME_VALUE = "some value";
	private static final String INVALID_OPERATION_KEY = "invalid";

	private QueryParserBase<SolrDataQuery> parser;

	@Before
	public void setUp() {
		parser = new QueryParserBase<SolrDataQuery>() {
			@Override
			public SolrQuery doConstructSolrQuery(SolrDataQuery query) {
				return null;
			}
		};
	}

	@Test
	public void testExpressionProcessorCanProcess() {
		assertProcessorCanProcess(this.parser.new ExpressionProcessor(), OperationKey.EXPRESSION);
	}

	@Test
	public void testBetweenProcessorCanProcess() {
		assertProcessorCanProcess(this.parser.new BetweenProcessor(), OperationKey.BETWEEN);
	}

	@Test
	public void testNearProcessorCanProcess() {
		assertProcessorCanProcess(this.parser.new NearProcessor(), OperationKey.NEAR);
	}

	@Test
	public void testWithinProcessorCanProcess() {
		assertProcessorCanProcess(this.parser.new WithinProcessor(), OperationKey.WITHIN);
	}

	@Test
	public void testFuzzyProcessorCanProcess() {
		assertProcessorCanProcess(this.parser.new FuzzyProcessor(), OperationKey.FUZZY);
	}

	@Test
	public void testSloppyProcessorCanProcess() {
		assertProcessorCanProcess(this.parser.new SloppyProcessor(), OperationKey.SLOPPY);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testWildcardProcessorCanProcess() {
		WildcardProcessor processor = this.parser.new WildcardProcessor();
		Assert.assertTrue(processor.canProcess(new CriteriaEntry(OperationKey.STARTS_WITH, SOME_VALUE)));
		Assert.assertTrue(processor.canProcess(new CriteriaEntry(OperationKey.ENDS_WITH, SOME_VALUE)));
		Assert.assertTrue(processor.canProcess(new CriteriaEntry(OperationKey.CONTAINS, SOME_VALUE)));
		assertProcessorCannotProcessInvalidOrNullOperationKey(processor);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testDefaultProcessorCanProcess() {
		DefaultProcessor processor = this.parser.new DefaultProcessor();
		Assert.assertTrue(processor.canProcess(new CriteriaEntry((String) null, SOME_VALUE)));
		Assert.assertTrue(processor.canProcess(new CriteriaEntry(INVALID_OPERATION_KEY, null)));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testBaseCritieraEntryProcessor() {
		BaseCriteriaEntryProcessor processor = this.parser.new BaseCriteriaEntryProcessor() {

			@Override
			public boolean canProcess(CriteriaEntry criteriaEntry) {
				return true;
			}

			@Override
			protected Object doProcess(CriteriaEntry criteriaEntry, Field field) {
				return "X";
			}
		};

		Assert.assertNull(processor.process(null, null));
		Assert.assertNull(processor.process(new CriteriaEntry("some key", null), null));
		Assert.assertEquals("X", processor.process(new CriteriaEntry("some key", SOME_VALUE), null));

	}

	private void assertProcessorCanProcess(CriteriaEntryProcessor processor, OperationKey key) {
		Assert.assertTrue(processor.canProcess(new CriteriaEntry(key, SOME_VALUE)));
		assertProcessorCannotProcessInvalidOrNullOperationKey(processor);
	}

	private void assertProcessorCannotProcessInvalidOrNullOperationKey(CriteriaEntryProcessor processor) {
		Assert.assertFalse(processor.canProcess(new CriteriaEntry(INVALID_OPERATION_KEY, null)));
		Assert.assertFalse(processor.canProcess(new CriteriaEntry((String) null, null)));
	}

}
