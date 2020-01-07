/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.solr.core.query;

import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.Map;

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

		assertThat(configured.getFacets().toArray()).isEqualTo(statsOptions.getFacets().toArray());
		assertThat(configured.getFields().toArray()).isEqualTo(statsOptions.getFields().toArray());
		assertThat(configured.getSelectiveFacets()).isEqualTo(statsOptions.getSelectiveFacets());
		assertThat(statsOptions.isCalcDistinct()).isTrue();
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
		assertThat(selectiveFacets.size()).isEqualTo(2);
		assertThat(selectiveFacets.get(new SimpleField("fieldName1")).size()).isEqualTo(1);
		assertThat(selectiveFacets.get(new SimpleField("fieldName2")).size()).isEqualTo(3);
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
		assertThat(selectiveFacets.size()).isEqualTo(2);
		assertThat(selectiveFacets.get(new SimpleField("fieldName1")).size()).isEqualTo(1);
		assertThat(selectiveFacets.get(new SimpleField("fieldName2")).size()).isEqualTo(3);
		assertThat(configured.isCalcDistinct()).isTrue();
		assertThat(configured.isSelectiveCalcDistincts(new SimpleField("fieldName1"))).isTrue();
		assertThat(configured.isSelectiveCalcDistincts(new SimpleField("fieldName2"))).isNull();
	}

}
