/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.solr.core.QueryParserBase.BasePredicateProcessor;
import org.springframework.data.solr.core.QueryParserBase.DefaultProcessor;
import org.springframework.data.solr.core.QueryParserBase.NamedObjectsQuery;
import org.springframework.data.solr.core.QueryParserBase.PredicateProcessor;
import org.springframework.data.solr.core.QueryParserBase.WildcardProcessor;
import org.springframework.data.solr.core.query.Criteria.OperationKey;
import org.springframework.data.solr.core.query.Criteria.Predicate;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.Function;
import org.springframework.data.solr.core.query.GroupOptions;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleCalculatedField;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 */
public class QueryParserBaseTests {

	@Rule public ExpectedException thrown = ExpectedException.none();

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
		Assert.assertTrue(processor.canProcess(new Predicate(OperationKey.STARTS_WITH, SOME_VALUE)));
		Assert.assertTrue(processor.canProcess(new Predicate(OperationKey.ENDS_WITH, SOME_VALUE)));
		Assert.assertTrue(processor.canProcess(new Predicate(OperationKey.CONTAINS, SOME_VALUE)));
		assertProcessorCannotProcessInvalidOrNullOperationKey(processor);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testDefaultProcessorCanProcess() {
		DefaultProcessor processor = this.parser.new DefaultProcessor();
		Assert.assertTrue(processor.canProcess(new Predicate((String) null, SOME_VALUE)));
		Assert.assertTrue(processor.canProcess(new Predicate(INVALID_OPERATION_KEY, null)));
	}

	@Test
	public void testFunctionProcessorCanProcessFunctions() {
		assertProcessorCanProcess(this.parser.new FunctionProcessor(), OperationKey.FUNCTION);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testBaseCritieraEntryProcessor() {
		BasePredicateProcessor processor = this.parser.new BasePredicateProcessor() {

			@Override
			public boolean canProcess(Predicate predicate) {
				return true;
			}

			@Override
			protected Object doProcess(Predicate predicate, Field field) {
				return "X";
			}
		};

		Assert.assertNull(processor.process(null, null));
		Assert.assertNull(processor.process(new Predicate("some key", null), null));
		Assert.assertEquals("X", processor.process(new Predicate("some key", SOME_VALUE), null));
	}

	@Test
	public void testFunctionFragmemtAppendsMultipleArgumentsCorrectly() {
		Foo function = new Foo(Arrays.asList("one", "two"));

		Assert.assertThat(parser.createFunctionFragment(function, 0), Is.is("{!func}foo(one,two)"));
	}

	@Test
	public void testFunctionFragmemtAppendsSingleArgumentCorrectly() {
		Foo function = new Foo(Arrays.asList("one"));

		Assert.assertThat(parser.createFunctionFragment(function, 0), Is.is("{!func}foo(one)"));
	}

	@Test
	public void testFunctionFragmemtIgnoresNullArguments() {
		Foo function = new Foo(null);

		Assert.assertThat(parser.createFunctionFragment(function, 0), Is.is("{!func}foo()"));
	}

	@Test
	public void testFunctionFragmemtIgnoresEmptyArguments() {
		Foo function = new Foo(Collections.emptyList());

		Assert.assertThat(parser.createFunctionFragment(function, 0), Is.is("{!func}foo()"));
	}

	@Test
	public void testCreateFunctionFragmemtThrowsExceptionOnNullInArguments() {
		List<Object> args = new ArrayList<Object>(1);
		args.add(null);

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Unable to parse 'null' within function arguments.");
		parser.createFunctionFragment(new Foo(args), 0);
	}

	@Test
	public void testCreateFunctionFragementsWihtNetsedFunction() {
		Foo function = new Foo(Arrays.asList(new Bar(Arrays.asList("nested"))));
		Assert.assertThat(parser.createFunctionFragment(function, 0), Is.is("{!func}foo(bar(nested))"));
	}

	@Test
	public void testCreateFunctionFragmentConvertsPointProperty() {
		Foo function = new Foo(Arrays.asList(new Point(37.767624D, -122.48526D)));

		Assert.assertThat(parser.createFunctionFragment(function, 0), Is.is("{!func}foo(37.767624,-122.48526)"));
	}

	@Test
	public void testCreateFunctionFragmentConvertsDistanceProperty() {
		Foo function = new Foo(Arrays.asList(new Distance(5, Metrics.KILOMETERS)));

		Assert.assertThat(parser.createFunctionFragment(function, 0), Is.is("{!func}foo(5.0)"));
	}

	@Test
	public void testCreateFunctionFragmentUsesToStringForUnknowObject() {
		Foo function = new Foo(Arrays.asList(new FooBar()));

		Assert.assertThat(parser.createFunctionFragment(function, 0), Is.is("{!func}foo(FooBar [])"));
	}

	@Test
	public void testCreateFunctionFieldFragmentIgnoresBlankAlias() {
		SimpleCalculatedField ff = new SimpleCalculatedField(" ", new Foo(null));
		Assert.assertThat(parser.createCalculatedFieldFragment(ff), Is.is("{!func}foo()"));
	}

	@Test
	public void testCreateFunctionFieldFragmentIgnoresNullAlias() {
		SimpleCalculatedField ff = new SimpleCalculatedField(null, new Foo(null));
		Assert.assertThat(parser.createCalculatedFieldFragment(ff), Is.is("{!func}foo()"));
	}

	@Test
	public void testCreateFunctionFieldFragmentPrependsAliasCorrectly() {
		SimpleCalculatedField ff = new SimpleCalculatedField("alias", new Foo(null));
		Assert.assertThat(parser.createCalculatedFieldFragment(ff), Is.is("alias:{!func}foo()"));
	}

	@Test // DATASOLR-121
	public void testNamedObjectsGroupQuery() {
		List<Function> functionList = Arrays.asList(Mockito.mock(Function.class), Mockito.mock(Function.class));
		List<Query> queriesList = Arrays.asList(Mockito.mock(Query.class), Mockito.mock(Query.class));

		Query groupQueryMock = Mockito.mock(Query.class);
		GroupOptions groupOptions = Mockito.mock(GroupOptions.class);
		Mockito.when(groupQueryMock.getGroupOptions()).thenReturn(groupOptions);
		Mockito.when(groupOptions.getGroupByFunctions()).thenReturn(functionList);
		Mockito.when(groupOptions.getGroupByQueries()).thenReturn(queriesList);

		NamedObjectsQuery decorator = new NamedObjectsQuery(groupQueryMock);
		decorator.setName(functionList.get(0), "nameFunc0");
		decorator.setName(functionList.get(1), "nameFunc1");
		decorator.setName(queriesList.get(0), "nameQuery0");
		decorator.setName(queriesList.get(1), "nameQuery1");
		Map<String, Object> objectNames = decorator.getNamesAssociation();

		Assert.assertEquals(functionList.get(0), objectNames.get("nameFunc0"));
		Assert.assertEquals(functionList.get(1), objectNames.get("nameFunc1"));
		Assert.assertEquals(queriesList.get(0), objectNames.get("nameQuery0"));
		Assert.assertEquals(queriesList.get(1), objectNames.get("nameQuery1"));
	}

	private void assertProcessorCanProcess(PredicateProcessor processor, OperationKey key) {
		Assert.assertTrue(processor.canProcess(new Predicate(key, SOME_VALUE)));
		assertProcessorCannotProcessInvalidOrNullOperationKey(processor);
	}

	private void assertProcessorCannotProcessInvalidOrNullOperationKey(PredicateProcessor processor) {
		Assert.assertFalse(processor.canProcess(new Predicate(INVALID_OPERATION_KEY, null)));
		Assert.assertFalse(processor.canProcess(new Predicate((String) null, null)));
	}

	private static class Foo implements Function {

		private List<?> arguments;

		public Foo(List<?> args) {
			this.arguments = args;
		}

		@Override
		public String getOperation() {
			return "foo";
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public List getArguments() {
			return this.arguments;
		}

		@Override
		public boolean hasArguments() {
			return !CollectionUtils.isEmpty(this.arguments);
		}

	}

	private static class Bar implements Function {

		private List<?> arguments;

		public Bar(List<?> args) {
			this.arguments = args;
		}

		@Override
		public String getOperation() {
			return "bar";
		}

		@Override
		public Iterable<?> getArguments() {
			return this.arguments;
		}

		@Override
		public boolean hasArguments() {
			return !CollectionUtils.isEmpty(this.arguments);
		}

	}

	private static class FooBar {

		@Override
		public String toString() {
			return "FooBar []";
		}

	}

}
