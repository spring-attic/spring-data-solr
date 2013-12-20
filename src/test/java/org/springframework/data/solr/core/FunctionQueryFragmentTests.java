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

import java.util.Arrays;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrQuery;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.data.solr.core.geo.Point;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.functions.AbstractFunction;
import org.springframework.data.solr.core.query.functions.CurrencyFunction;
import org.springframework.data.solr.core.query.functions.DefaultValueFunction;
import org.springframework.data.solr.core.query.functions.DistanceFunction;
import org.springframework.data.solr.core.query.functions.DivideFunction;
import org.springframework.data.solr.core.query.functions.ExistsFunction;
import org.springframework.data.solr.core.query.functions.Function;
import org.springframework.data.solr.core.query.functions.GeoDistanceFunction;
import org.springframework.data.solr.core.query.functions.GeoHashFunction;
import org.springframework.data.solr.core.query.functions.IfFunction;
import org.springframework.data.solr.core.query.functions.MaxFunction;
import org.springframework.data.solr.core.query.functions.NotFunction;
import org.springframework.data.solr.core.query.functions.ProductFunction;
import org.springframework.data.solr.core.query.functions.QueryFunction;
import org.springframework.data.solr.core.query.functions.TermFrequencyFunction;

/**
 * @author Christoph Strobl
 */
@RunWith(Parameterized.class)
public class FunctionQueryFragmentTests {

	private QueryParserBase<SimpleQuery> queryParser = new QueryParserBase<SimpleQuery>() {

		@Override
		public SolrQuery doConstructSolrQuery(SimpleQuery query) {
			return null;
		}

	};

	@Parameter(value = 0)
	public Function function;

	@Parameter(value = 1)
	public String expectedQueryFragment;

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] {
				{ CurrencyFunction.currency("field_1"), "currency(field_1)" },
				{ CurrencyFunction.currency("field_1", "EUR"), "currency(field_1,EUR)" },
				{ CurrencyFunction.currency(new SimpleField("field_1")), "currency(field_1)" },
				{ CurrencyFunction.currency(new SimpleField("field_1"), "EUR"), "currency(field_1,EUR)" },
				{ DefaultValueFunction.defaultValue("field_1", "value"), "def(field_1,value)" },
				{ DefaultValueFunction.defaultValue("field_1", 1), "def(field_1,1)" },
				{ DefaultValueFunction.defaultValue("field_1", new Foo()), "def(field_1,foo())" },
				{ DefaultValueFunction.defaultValue(new SimpleField("field_1"), "value"), "def(field_1,value)" },
				{ DefaultValueFunction.defaultValue(new SimpleField("field_1"), 1), "def(field_1,1)" },
				{ DefaultValueFunction.defaultValue(new SimpleField("field_1"), new Foo()), "def(field_1,foo())" },
				{ DefaultValueFunction.defaultValue(new Foo(), "value"), "def(foo(),value)" },
				{ DefaultValueFunction.defaultValue(new Foo(), 1), "def(foo(),1)" },
				{ DefaultValueFunction.defaultValue(new Foo(), new Bar()), "def(foo(),bar())" },
				{ DistanceFunction.euclideanDistance().between(new Point(1, 2), new Point(3, 4)), "dist(2,1,2,3,4)" },
				{ DistanceFunction.euclideanDistance().between(new Point(1, 2, 3), new Point(4, 5, 6)), "dist(2,1,2,3,4,5,6)" },
				{ DistanceFunction.infiniteNormDistance().between(new Point(1, 2), new Point(3, 4)), "dist(Infinite,1,2,3,4)" },
				{ DistanceFunction.infiniteNormDistance().between(new Point(1, 2, 3), new Point(4, 5, 6)),
						"dist(Infinite,1,2,3,4,5,6)" },
				{ DistanceFunction.manhattanDistance().between(new Point(1, 2), new Point(3, 4)), "dist(1,1,2,3,4)" },
				{ DistanceFunction.manhattanDistance().between(new Point(1, 2, 3), new Point(4, 5, 6)), "dist(1,1,2,3,4,5,6)" },
				{ DistanceFunction.sparsenessDistance().between(new Point(1, 2), new Point(3, 4)), "dist(0,1,2,3,4)" },
				{ DistanceFunction.sparsenessDistance().between(new Point(1, 2, 3), new Point(4, 5, 6)), "dist(0,1,2,3,4,5,6)" },
				{ DivideFunction.divide(new Foo()).by(new Bar()), "div(foo(),bar())" },
				{ DivideFunction.divide(new Foo()).by(Long.valueOf(3)), "div(foo(),3)" },
				{ DivideFunction.divide(new Foo()).by("field_1"), "div(foo(),field_1)" },
				{ DivideFunction.divide(new Foo()).by(new SimpleField("field_1")), "div(foo(),field_1)" },
				{ DivideFunction.divide(Long.valueOf(3)).by(new Bar()), "div(3,bar())" },
				{ DivideFunction.divide(Long.valueOf(3)).by(Long.valueOf(3)), "div(3,3)" },
				{ DivideFunction.divide(Long.valueOf(3)).by("field_1"), "div(3,field_1)" },
				{ DivideFunction.divide(Long.valueOf(3)).by(new SimpleField("field_1")), "div(3,field_1)" },
				{ DivideFunction.divide("field_1").by(new Bar()), "div(field_1,bar())" },
				{ DivideFunction.divide("field_1").by(3), "div(field_1,3)" },
				{ DivideFunction.divide("field_1").by("field_2"), "div(field_1,field_2)" },
				{ DivideFunction.divide("field_1").by(new SimpleField("field_2")), "div(field_1,field_2)" },
				{ DivideFunction.divide(new SimpleField("field_1")).by(new Bar()), "div(field_1,bar())" },
				{ DivideFunction.divide(new SimpleField("field_1")).by(3), "div(field_1,3)" },
				{ DivideFunction.divide(new SimpleField("field_1")).by("field_2"), "div(field_1,field_2)" },
				{ DivideFunction.divide(new SimpleField("field_1")).by(new SimpleField("field_2")), "div(field_1,field_2)" },
				{ ExistsFunction.exists("field_3"), "exists(field_3)" },
				{ ExistsFunction.exists(new Foo()), "exists(foo())" },
				{ ExistsFunction.exists(new SimpleField("field_1")), "exists(field_1)" },
				{ GeoDistanceFunction.distanceFrom("field_1").to(new GeoLocation(12, 13)), "geodist(field_1,12.0,13.0)" },
				{ GeoDistanceFunction.distanceFrom(new SimpleField("field_1")).to(new GeoLocation(12, 13)),
						"geodist(field_1,12.0,13.0)" },
				{ GeoDistanceFunction.distanceFrom("field_1").to(12D, 13D), "geodist(field_1,12.0,13.0)" },
				{ GeoHashFunction.geohash(new GeoLocation(1, 2)), "geohash(1.0,2.0)" },
				{ GeoHashFunction.geohash(1, 2), "geohash(1.0,2.0)" },
				{ IfFunction.when(new Foo()).then("field_1").otherwise(3), "if(foo(),field_1,3)" },
				{ IfFunction.when(new Foo()).then(new SimpleField("field_1")).otherwise(3), "if(foo(),field_1,3)" },
				{ IfFunction.when("field_1").then(new Foo()).otherwise(new Bar()), "if(field_1,foo(),bar())" },
				{ IfFunction.when(new SimpleField("field_1")).then(new Foo()).otherwise(new Bar()), "if(field_1,foo(),bar())" },
				{ MaxFunction.max(new Foo(), new Bar()), "max(foo(),bar())" },
				{ MaxFunction.max(new Foo(), Long.valueOf(3)), "max(foo(),3)" },
				{ MaxFunction.max(new Foo(), "field_1"), "max(foo(),field_1)" },
				{ MaxFunction.max(Long.valueOf(3), new Bar()), "max(3,bar())" },
				{ MaxFunction.max(Long.valueOf(3), Long.valueOf(4)), "max(3,4)" },
				{ MaxFunction.max(Long.valueOf(3), "field_1"), "max(3,field_1)" },
				{ MaxFunction.max("field_1", new Bar()), "max(field_1,bar())" },
				{ MaxFunction.max("field_1", Long.valueOf(3)), "max(field_1,3)" },
				{ MaxFunction.max("field_1", "field_2"), "max(field_1,field_2)" },
				{ NotFunction.not("field_1"), "not(field_1)" },
				{ NotFunction.not(new Foo()), "not(foo())" },
				{ NotFunction.not(new SimpleField("field_1")), "not(field_1)" },
				{ ProductFunction.product("field_1").times("field_2").build(), "product(field_1,field_2)" },
				{ ProductFunction.product(new SimpleField("field_1")).times("field_2").build(), "product(field_1,field_2)" },
				{ ProductFunction.product(Long.valueOf(3)).times("field_2").build(), "product(3,field_2)" },
				{ ProductFunction.product("field_1").times("field_2").build(), "product(field_1,field_2)" },
				{ ProductFunction.product(new Foo()).times(new SimpleField("field_1")).build(), "product(foo(),field_1)" },
				{ ProductFunction.product(new Foo()).times(new SimpleField("field_1")).times(new Bar()).build(),
						"product(foo(),field_1,bar())" },
				{
						ProductFunction.product(new Foo()).times(new SimpleField("field_1")).times(new Bar())
								.times(Long.valueOf(3)).build(), "product(foo(),field_1,bar(),3)" },
				{
						ProductFunction.product(new Foo()).times(new SimpleField("field_1")).times(new Bar())
								.times(Long.valueOf(3)).times(new SimpleField("field_2")).build(),
						"product(foo(),field_1,bar(),3,field_2)" },
				{ QueryFunction.query(new Criteria("field_1").is("value")), "query(field_1:value)" },
				{ QueryFunction.query(new SimpleQuery(new Criteria("field_1").is("value"))), "query(field_1:value)" },
				{ TermFrequencyFunction.termFequency("term").inField(new SimpleField("field_1")), "termfreq(field_1,term)" },
				{ TermFrequencyFunction.termFequency("term").inField("field_1"), "termfreq(field_1,term)" } };
		return Arrays.asList(data);
	}

	@Test
	public void queryParserConstructsExpectedFragment() {
		Assert.assertThat(queryParser.createFunctionFragment(this.function), IsEqual.equalTo(this.expectedQueryFragment));
	}

	private static class Foo extends AbstractFunction {

		@Override
		public String getOperation() {
			return "foo";
		}

		@Override
		public String toString() {
			return "Foo";
		}

	}

	private static class Bar extends AbstractFunction {

		@Override
		public String getOperation() {
			return "bar";
		}

		@Override
		public String toString() {
			return "Bar";
		}

	}
}
