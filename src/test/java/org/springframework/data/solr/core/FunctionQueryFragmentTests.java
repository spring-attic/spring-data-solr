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
import org.springframework.data.solr.core.geo.Point;
import org.springframework.data.solr.core.query.AbstractFunction;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.CurrencyFunction;
import org.springframework.data.solr.core.query.DefaultValueFunction;
import org.springframework.data.solr.core.query.DistanceFunction;
import org.springframework.data.solr.core.query.DivideFunction;
import org.springframework.data.solr.core.query.ExistsFunction;
import org.springframework.data.solr.core.query.Function;
import org.springframework.data.solr.core.query.GeoDistanceFunction;
import org.springframework.data.solr.core.query.GeoHashFunction;
import org.springframework.data.solr.core.query.IfFunction;
import org.springframework.data.solr.core.query.MaxFunction;
import org.springframework.data.solr.core.query.NotFunction;
import org.springframework.data.solr.core.query.ProductFunction;
import org.springframework.data.solr.core.query.QueryFunction;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.TermFrequencyFunction;

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

	@Parameter(value = 0) public Function function;

	@Parameter(value = 1) public String expectedQueryFragment;

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { { CurrencyFunction.currency("field_1"), "{!func}currency(field_1)" },
				{ CurrencyFunction.currency("field_1", "EUR"), "{!func}currency(field_1,EUR)" },
				{ CurrencyFunction.currency(new SimpleField("field_1")), "{!func}currency(field_1)" },
				{ CurrencyFunction.currency(new SimpleField("field_1"), "EUR"), "{!func}currency(field_1,EUR)" },
				{ DefaultValueFunction.defaultValue("field_1", "value"), "{!func}def(field_1,value)" },
				{ DefaultValueFunction.defaultValue("field_1", 1), "{!func}def(field_1,1)" },
				{ DefaultValueFunction.defaultValue("field_1", new Foo()), "{!func}def(field_1,foo())" },
				{ DefaultValueFunction.defaultValue(new SimpleField("field_1"), "value"), "{!func}def(field_1,value)" },
				{ DefaultValueFunction.defaultValue(new SimpleField("field_1"), 1), "{!func}def(field_1,1)" },
				{ DefaultValueFunction.defaultValue(new SimpleField("field_1"), new Foo()), "{!func}def(field_1,foo())" },
				{ DefaultValueFunction.defaultValue(new Foo(), "value"), "{!func}def(foo(),value)" },
				{ DefaultValueFunction.defaultValue(new Foo(), 1), "{!func}def(foo(),1)" },
				{ DefaultValueFunction.defaultValue(new Foo(), new Bar()), "{!func}def(foo(),bar())" },
				{ DistanceFunction.euclideanDistance().between(new Point(1, 2), new Point(3, 4)),
						"{!func}dist(2,1.0,2.0,3.0,4.0)" },
				{ DistanceFunction.euclideanDistance().between(new Point(1, 2, 3), new Point(4, 5, 6)),
						"{!func}dist(2,1.0,2.0,3.0,4.0,5.0,6.0)" },
				{ DistanceFunction.infiniteNormDistance().between(new Point(1, 2), new Point(3, 4)),
						"{!func}dist(Infinite,1.0,2.0,3.0,4.0)" },
				{ DistanceFunction.infiniteNormDistance().between(new Point(1, 2, 3), new Point(4, 5, 6)),
						"{!func}dist(Infinite,1.0,2.0,3.0,4.0,5.0,6.0)" },
				{ DistanceFunction.manhattanDistance().between(new Point(1, 2), new Point(3, 4)),
						"{!func}dist(1,1.0,2.0,3.0,4.0)" },
				{ DistanceFunction.manhattanDistance().between(new Point(1, 2, 3), new Point(4, 5, 6)),
						"{!func}dist(1,1.0,2.0,3.0,4.0,5.0,6.0)" },
				{ DistanceFunction.sparsenessDistance().between(new Point(1, 2), new Point(3, 4)),
						"{!func}dist(0,1.0,2.0,3.0,4.0)" },
				{ DistanceFunction.sparsenessDistance().between(new Point(1, 2, 3), new Point(4, 5, 6)),
						"{!func}dist(0,1.0,2.0,3.0,4.0,5.0,6.0)" },
				{ DivideFunction.divide(new Foo()).by(new Bar()), "{!func}div(foo(),bar())" },
				{ DivideFunction.divide(new Foo()).by(3L), "{!func}div(foo(),3)" },
				{ DivideFunction.divide(new Foo()).by("field_1"), "{!func}div(foo(),field_1)" },
				{ DivideFunction.divide(new Foo()).by(new SimpleField("field_1")), "{!func}div(foo(),field_1)" },
				{ DivideFunction.divide(3L).by(new Bar()), "{!func}div(3,bar())" },
				{ DivideFunction.divide(3L).by(3L), "{!func}div(3,3)" },
				{ DivideFunction.divide(3L).by("field_1"), "{!func}div(3,field_1)" },
				{ DivideFunction.divide(3L).by(new SimpleField("field_1")), "{!func}div(3,field_1)" },
				{ DivideFunction.divide("field_1").by(new Bar()), "{!func}div(field_1,bar())" },
				{ DivideFunction.divide("field_1").by(3), "{!func}div(field_1,3)" },
				{ DivideFunction.divide("field_1").by("field_2"), "{!func}div(field_1,field_2)" },
				{ DivideFunction.divide("field_1").by(new SimpleField("field_2")), "{!func}div(field_1,field_2)" },
				{ DivideFunction.divide(new SimpleField("field_1")).by(new Bar()), "{!func}div(field_1,bar())" },
				{ DivideFunction.divide(new SimpleField("field_1")).by(3), "{!func}div(field_1,3)" },
				{ DivideFunction.divide(new SimpleField("field_1")).by("field_2"), "{!func}div(field_1,field_2)" },
				{ DivideFunction.divide(new SimpleField("field_1")).by(new SimpleField("field_2")),
						"{!func}div(field_1,field_2)" },
				{ ExistsFunction.exists("field_3"), "{!func}exists(field_3)" },
				{ ExistsFunction.exists(new Foo()), "{!func}exists(foo())" },
				{ ExistsFunction.exists(new SimpleField("field_1")), "{!func}exists(field_1)" },
				{ GeoDistanceFunction.distanceFrom("field_1").to(new org.springframework.data.geo.Point(12, 13)),
						"{!func}geodist(field_1,12.0,13.0)" },
				{ GeoDistanceFunction.distanceFrom(new SimpleField("field_1"))
						.to(new org.springframework.data.geo.Point(12, 13)), "{!func}geodist(field_1,12.0,13.0)" },
				{ GeoDistanceFunction.distanceFrom("field_1").to(12D, 13D), "{!func}geodist(field_1,12.0,13.0)" },
				{ GeoHashFunction.geohash(new org.springframework.data.geo.Point(1, 2)), "{!func}geohash(1.0,2.0)" },
				{ GeoHashFunction.geohash(1, 2), "{!func}geohash(1.0,2.0)" },
				{ IfFunction.when(new Foo()).then("field_1").otherwise(3), "{!func}if(foo(),field_1,3)" },
				{ IfFunction.when(new Foo()).then(new SimpleField("field_1")).otherwise(3), "{!func}if(foo(),field_1,3)" },
				{ IfFunction.when("field_1").then(new Foo()).otherwise(new Bar()), "{!func}if(field_1,foo(),bar())" },
				{ IfFunction.when(new SimpleField("field_1")).then(new Foo()).otherwise(new Bar()),
						"{!func}if(field_1,foo(),bar())" },
				{ MaxFunction.max(new Foo(), new Bar()), "{!func}max(foo(),bar())" },
				{ MaxFunction.max(new Foo(), 3L),
						"{!func}max(foo(),3)" },
				{ MaxFunction.max(new Foo(), "field_1"), "{!func}max(foo(),field_1)" },
				{ MaxFunction.max(3L, new Bar()), "{!func}max(3,bar())" }, { MaxFunction.max(3L, 4L), "{!func}max(3,4)" },
				{ MaxFunction.max(3L, "field_1"), "{!func}max(3,field_1)" },
				{ MaxFunction.max("field_1", new Bar()), "{!func}max(field_1,bar())" },
				{ MaxFunction.max("field_1", 3L), "{!func}max(field_1,3)" },
				{ MaxFunction.max("field_1", "field_2"), "{!func}max(field_1,field_2)" },
				{ NotFunction.not("field_1"), "{!func}not(field_1)" }, { NotFunction.not(new Foo()), "{!func}not(foo())" },
				{ NotFunction.not(new SimpleField("field_1")), "{!func}not(field_1)" },
				{ ProductFunction.product("field_1").times("field_2").build(), "{!func}product(field_1,field_2)" },
				{ ProductFunction.product(new SimpleField("field_1")).times("field_2").build(),
						"{!func}product(field_1,field_2)" },
				{ ProductFunction.product(3L).times("field_2").build(), "{!func}product(3,field_2)" },
				{ ProductFunction.product("field_1").times("field_2").build(), "{!func}product(field_1,field_2)" },
				{ ProductFunction.product(new Foo()).times(new SimpleField("field_1")).build(),
						"{!func}product(foo(),field_1)" },
				{ ProductFunction.product(new Foo()).times(new SimpleField("field_1")).times(new Bar()).build(),
						"{!func}product(foo(),field_1,bar())" },
				{ ProductFunction.product(new Foo()).times(new SimpleField("field_1")).times(new Bar()).times(3L).build(),
						"{!func}product(foo(),field_1,bar(),3)" },
				{ ProductFunction.product(new Foo()).times(new SimpleField("field_1")).times(new Bar()).times(3L)
						.times(new SimpleField("field_2")).build(), "{!func}product(foo(),field_1,bar(),3,field_2)" },
				{ QueryFunction.query(new Criteria("field_1").is("value")), "{!func}query(field_1:value)" },
				{ QueryFunction.query(new SimpleQuery(new Criteria("field_1").is("value"))), "{!func}query(field_1:value)" },
				{ TermFrequencyFunction.termFequency("term").inField(new SimpleField("field_1")),
						"{!func}termfreq(field_1,term)" },
				{ TermFrequencyFunction.termFequency("term").inField("field_1"), "{!func}termfreq(field_1,term)" } };
		return Arrays.asList(data);
	}

	@Test // DATAREDIS-307
	public void queryParserConstructsExpectedFragment() {
		Assert.assertThat(queryParser.createFunctionFragment(this.function, 0),
				IsEqual.equalTo(this.expectedQueryFragment));
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
