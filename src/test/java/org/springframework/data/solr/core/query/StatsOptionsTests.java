/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.solr.core.query;

import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Francisco Spaeth
 * @author Christoph Strobl
 */
public class StatsOptionsTests {

	@Test(expected = IllegalArgumentException.class) // DATSOLR-160
	public void shouldThrowExceptionWhenAddingNullField() {
		new StatsOptions().addField((Field) null);
	}

	@Test(expected = IllegalArgumentException.class) // DATSOLR-160
	public void shouldThrowExceptionWhenAddingNullFieldName() {
		new StatsOptions().addField((String) null);
	}

	@Test(expected = IllegalArgumentException.class) // DATSOLR-160
	public void shouldThrowExceptionWhenAddingNullFacetField() {
		new StatsOptions().addFacet((Field) null);
	}

	@Test(expected = IllegalArgumentException.class) // DATSOLR-160
	public void shouldThrowExceptionWhenAddingNullFacetFieldName() {
		new StatsOptions().addFacet((String) null);
	}

	@Test // DATSOLR-160
	public void testFluency() {

		StatsOptions statsOptions = new StatsOptions();

		StatsOptions configured = statsOptions//
				.addField("fieldName").addSelectiveFacet("fieldFacetSelective")//
				.addField("secondField")//
				.setCalcDistinct(true)//
				.addFacet("fieldFacet");

		Assert.assertArrayEquals(statsOptions.getFacets().toArray(), configured.getFacets().toArray());
		Assert.assertArrayEquals(statsOptions.getFields().toArray(), configured.getFields().toArray());
		Assert.assertEquals(statsOptions.getSelectiveFacets(), configured.getSelectiveFacets());
		Assert.assertTrue(statsOptions.isCalcDistinct());
	}

	@Test // DATSOLR-160
	public void testMultipleSelectiveFacet() {

		StatsOptions configured = new StatsOptions().addField("fieldName1") //
				.addSelectiveFacet("fieldFacetSelective1")//
				.addField("fieldName2") //
				.addSelectiveFacet("fieldFacetSelective1")//
				.addSelectiveFacet("fieldFacetSelective2")//
				.addSelectiveFacet("fieldFacetSelective3");

		Map<Field, Collection<Field>> selectiveFacets = configured.getSelectiveFacets();
		Assert.assertEquals(2, selectiveFacets.size());
		Assert.assertEquals(1, selectiveFacets.get(new SimpleField("fieldName1")).size());
		Assert.assertEquals(3, selectiveFacets.get(new SimpleField("fieldName2")).size());
	}

	@Test // DATSOLR-160
	public void testSelectiveFacetAndSelectiveCountDistinct() {

		StatsOptions configured = new StatsOptions().setCalcDistinct(true)//
				.addField("fieldName1") //
				.addSelectiveFacet("fieldFacetSelective1")//
				.setSelectiveCalcDistinct(true) //
				.addField("fieldName2") //
				.addSelectiveFacet("fieldFacetSelective1")//
				.addSelectiveFacet("fieldFacetSelective2")//
				.addSelectiveFacet("fieldFacetSelective3");

		Map<Field, Collection<Field>> selectiveFacets = configured.getSelectiveFacets();
		Assert.assertEquals(2, selectiveFacets.size());
		Assert.assertEquals(1, selectiveFacets.get(new SimpleField("fieldName1")).size());
		Assert.assertEquals(3, selectiveFacets.get(new SimpleField("fieldName2")).size());
		Assert.assertTrue(configured.isCalcDistinct());
		Assert.assertTrue(configured.isSelectiveCalcDistincts(new SimpleField("fieldName1")));
		Assert.assertNull(configured.isSelectiveCalcDistincts(new SimpleField("fieldName2")));
	}

}
