/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.FacetParams.FacetRangeInclude;
import org.apache.solr.common.params.FacetParams.FacetRangeOther;
import org.junit.Test;
import org.springframework.data.solr.core.query.FacetOptions.FacetSort;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithDateRangeParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithFacetParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithNumericRangeParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithRangeParameters;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public class FacetOptionsTests {

	@Test
	public void testFacetOptionsEmptyConstructor() {
		FacetOptions options = new FacetOptions();
		assertThat(options.hasFacets()).isFalse();
		assertThat(options.hasFields()).isFalse();
		assertThat(options.hasFacetQueries()).isFalse();
	}

	@Test
	public void testFacetOptionsConstructorSingleField() {
		FacetOptions options = new FacetOptions(new SimpleField("field_1"));
		assertThat(options.hasFacets()).isTrue();
		assertThat(options.hasFields()).isTrue();
		assertThat(options.getFacetOnFields().size()).isEqualTo(1);
		assertThat(options.hasFacetQueries()).isFalse();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFacetOptionsConstructorSingleNullValueField() {
		new FacetOptions((SimpleField) null);
	}

	@Test
	public void testFacetOptionsConstructorSingleFieldname() {
		FacetOptions options = new FacetOptions("field_1");
		assertThat(options.hasFacets()).isTrue();
		assertThat(options.hasFields()).isTrue();
		assertThat(options.getFacetOnFields().size()).isEqualTo(1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFacetOptionsConstructorSingleNullValueFieldname() {
		new FacetOptions((String) null);
	}

	@Test
	public void testAddFacetOnField() {
		FacetOptions options = new FacetOptions();
		options.addFacetOnField(new SimpleField("field_1"));
		options.addFacetOnField(new SimpleField("field_2"));

		assertThat(options.getFacetOnFields().size()).isEqualTo(2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFacetOnFieldNullValue() {
		new FacetOptions().addFacetOnField((Field) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFacetOnFieldWithoutFieldname() {
		new FacetOptions().addFacetOnField(new SimpleField(""));
	}

	@Test
	public void testAddFacetOnPivotWithFieldNames() {
		FacetOptions options = new FacetOptions();
		options.addFacetOnPivot("field_1", "field2");
		assertThat(options.hasFacets()).isTrue();
		assertThat(options.hasPivotFields()).isTrue();
		assertThat(options.getFacetOnPivots().size()).isEqualTo(1);
		assertThat(options.hasFields()).isTrue();
		assertThat(options.getFacetOnPivots().get(0).getName()).isEqualTo("field_1,field2");
	}

	@Test
	public void testAddFacetOnPivotWithField() {
		FacetOptions options = new FacetOptions();
		options.addFacetOnPivot(new SimpleField("field_1"), new SimpleField("field2"));
		assertThat(options.hasFacets()).isTrue();
		assertThat(options.hasPivotFields()).isTrue();
		assertThat(options.getFacetOnPivots().size()).isEqualTo(1);
		assertThat(options.hasFields()).isTrue();
		assertThat(options.getFacetOnPivots().get(0).getName()).isEqualTo("field_1,field2");
	}

	@Test
	public void testAddMultipleFacetOnPivotWithField() {
		FacetOptions options = new FacetOptions();
		options.addFacetOnPivot(new SimpleField("field_1"), new SimpleField("field2"));
		options.addFacetOnPivot(new SimpleField("field_3"), new SimpleField("field4"));
		assertThat(options.hasFacets()).isTrue();
		assertThat(options.hasPivotFields()).isTrue();
		assertThat(options.getFacetOnPivots().size()).isEqualTo(2);
		assertThat(options.hasFields()).isTrue();
		assertThat(options.getFacetOnPivots().get(0).getName()).isEqualTo("field_1,field2");
		assertThat(options.getFacetOnPivots().get(1).getName()).isEqualTo("field_3,field4");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFacetOnPivotWithoutFieldName() {
		FacetOptions options = new FacetOptions();
		options.addFacetOnPivot("field_1", "");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFacetOnPivotWithNullFieldName() {
		FacetOptions options = new FacetOptions();
		options.addFacetOnPivot("field_1", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFacetOnPivotWithEmptyField() {
		FacetOptions options = new FacetOptions();
		options.addFacetOnPivot(new SimpleField("field_1"), new SimpleField(""));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFacetOnPivotWithNullField() {
		FacetOptions options = new FacetOptions();
		options.addFacetOnPivot(new SimpleField("field_1"), null);
	}

	@Test
	public void testAddFacetOnQueryConstructorSingleQuery() {
		FacetOptions options = new FacetOptions(new SimpleQuery(new SimpleStringCriteria("field_1:[* TO 5]")));
		assertThat(options.hasFacets()).isTrue();
		assertThat(options.hasFacetQueries()).isTrue();
		assertThat(options.getFacetQueries().size()).isEqualTo(1);
		assertThat(options.hasFields()).isFalse();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFacetOnQueryConstructorSingleNullQuery() {
		new FacetOptions((SolrDataQuery) null);
	}

	@Test
	public void testAddFacetOnQuery() {
		FacetOptions options = new FacetOptions();
		options.addFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_1:[* TO 5]")));
		options.addFacetQuery(new SimpleQuery(new SimpleStringCriteria("field_1:[6 TO *]")));

		assertThat(options.hasFacets()).isTrue();
		assertThat(options.hasFacetQueries()).isTrue();
		assertThat(options.getFacetQueries().size()).isEqualTo(2);
		assertThat(options.hasFields()).isFalse();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFacetOnNullQuery() {
		FacetOptions options = new FacetOptions();
		options.addFacetQuery(null);
	}

	@Test
	public void testSetFacetSort() {
		FacetOptions options = new FacetOptions();
		assertThat(options.getFacetSort()).isNotNull();
		assertThat(options.getFacetSort()).isEqualTo(FacetOptions.DEFAULT_FACET_SORT);

		options.setFacetSort(FacetSort.INDEX);
		assertThat(options.getFacetSort()).isEqualTo(FacetSort.INDEX);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetFacetSortWithNullValue() {
		new FacetOptions().setFacetSort(null);
	}

	@Test
	public void testSetFacetLimit() {
		FacetOptions options = new FacetOptions();
		assertThat(options.getFacetLimit()).isEqualTo(FacetOptions.DEFAULT_FACET_LIMIT);

		options.setFacetLimit(20);
		assertThat(options.getFacetLimit()).isEqualTo(20);

		options.setFacetLimit(-1);
		assertThat(options.getFacetLimit()).isEqualTo(-1);
	}

	@Test
	public void testSetFacetMinCount() {
		FacetOptions options = new FacetOptions();
		assertThat(options.getFacetMinCount()).isEqualTo(FacetOptions.DEFAULT_FACET_MIN_COUNT);

		options.setFacetMinCount(20);
		assertThat(options.getFacetMinCount()).isEqualTo(20);

		options.setFacetMinCount(-1);
		assertThat(options.getFacetMinCount()).isEqualTo(0);
	}

	@Test
	public void testGetFieldsWithFacetParameters() {
		FacetOptions options = new FacetOptions();
		options.addFacetOnField(new SimpleField("field_1"));
		options.addFacetOnField(new FieldWithFacetParameters("field_2").setPrefix("prefix"));

		assertThat(options.getFacetOnFields().size()).isEqualTo(2);
		assertThat(options.getFieldsWithParameters().size()).isEqualTo(1);
		assertThat(options.getFieldsWithParameters().iterator().next().getName()).isEqualTo("field_2");
	}

	@Test
	public void testGetFieldsWithFacetParametersNoFieldsAvailable() {
		FacetOptions options = new FacetOptions();
		options.addFacetOnField(new SimpleField("field_1"));

		assertThat(options.getFacetOnFields().size()).isEqualTo(1);
		assertThat(options.getFieldsWithParameters().isEmpty()).isTrue();
	}

	@Test
	public void testHasFacetPrefix() {
		FacetOptions options = new FacetOptions();
		options.setFacetPrefix("prefix");
		assertThat(options.hasFacetPrefix()).isTrue();
	}

	@Test
	public void testHasBlankFacetPrefix() {
		FacetOptions options = new FacetOptions();
		options.setFacetPrefix("  ");
		assertThat(options.hasFacetPrefix()).isFalse();
	}

	@Test
	public void testHasNullFacetPrefix() {
		FacetOptions options = new FacetOptions();
		options.setFacetPrefix(null);
		assertThat(options.hasFacetPrefix()).isFalse();
	}

	@Test // DATSOLR-86
	public void testFacetQueryWithFacetRangeFields() {

		final FieldWithDateRangeParameters lastModifiedField = new FieldWithDateRangeParameters( //
				"last_modified", //
				new GregorianCalendar(2013, Calendar.NOVEMBER, 30).getTime(), //
				new GregorianCalendar(2014, Calendar.JANUARY, 1).getTime(), //
				"+1DAY" //
		);

		final FieldWithNumericRangeParameters popularityField = new FieldWithNumericRangeParameters( //
				"popularity", //
				100, //
				800, //
				200 //
		);

		FacetOptions facetRangeOptions = new FacetOptions() //
				.addFacetByRange(lastModifiedField) //
				.addFacetByRange(popularityField);

		Collection<FieldWithRangeParameters<?, ?, ?>> fieldsWithRangeParameters = facetRangeOptions
				.getFieldsWithRangeParameters();

		assertThat(fieldsWithRangeParameters).containsExactly(lastModifiedField, popularityField);
	}

	@Test // DATSOLR-86
	public void testDateRangeFacetAccessors() {
		Date start = new Date(100);
		Date end = new Date(10000000);
		String gap = "+1DAY";
		boolean hardEnd = true;
		FacetRangeInclude include = FacetRangeInclude.LOWER;
		FacetRangeOther other = FacetRangeOther.BEFORE;

		FieldWithDateRangeParameters dateRangeField = new FieldWithDateRangeParameters( //
				"name", //
				start, //
				end, //
				gap//
		)//
				.setHardEnd(hardEnd) //
				.setInclude(include) //
				.setOther(other);

		assertThat(dateRangeField.getName()).isEqualTo("name");
		assertThat(dateRangeField.getStart()).isEqualTo(start);
		assertThat(dateRangeField.getEnd()).isEqualTo(end);
		assertThat(dateRangeField.getGap()).isEqualTo(gap);
		assertThat(dateRangeField.getHardEnd()).isEqualTo(hardEnd);
		assertThat(dateRangeField.getInclude()).isEqualTo(include);
		assertThat(dateRangeField.getOther()).isEqualTo(other);

		assertThat(dateRangeField.getQueryParameter(FacetParams.FACET_RANGE_START).getValue()).isEqualTo(start);
		assertThat(dateRangeField.getQueryParameter(FacetParams.FACET_RANGE_END).getValue()).isEqualTo(end);
		assertThat(dateRangeField.getQueryParameter(FacetParams.FACET_RANGE_GAP).getValue()).isEqualTo(gap);
		assertThat(dateRangeField.getQueryParameter(FacetParams.FACET_RANGE_HARD_END).getValue()).isEqualTo(hardEnd);
		assertThat(dateRangeField.getQueryParameter(FacetParams.FACET_RANGE_INCLUDE).getValue()).isEqualTo(include);
		assertThat(dateRangeField.getQueryParameter(FacetParams.FACET_RANGE_OTHER).getValue()).isEqualTo(other);
	}

	@Test // DATSOLR-86
	public void testNumericRangeFacetAccessors() {
		int start = 100;
		int end = 10000000;
		int gap = 200;
		boolean hardEnd = true;
		FacetRangeInclude include = FacetRangeInclude.LOWER;
		FacetRangeOther other = FacetRangeOther.BEFORE;

		FieldWithNumericRangeParameters numRangeField = new FieldWithNumericRangeParameters( //
				"name", //
				start, //
				end, //
				gap //
		)//
				.setHardEnd(hardEnd) //
				.setInclude(include) //
				.setOther(other);

		assertThat(numRangeField.getName()).isEqualTo("name");
		assertThat(numRangeField.getStart()).isEqualTo(start);
		assertThat(numRangeField.getEnd()).isEqualTo(end);
		assertThat(numRangeField.getGap()).isEqualTo(gap);
		assertThat(numRangeField.getHardEnd()).isEqualTo(hardEnd);
		assertThat(numRangeField.getInclude()).isEqualTo(include);
		assertThat(numRangeField.getOther()).isEqualTo(other);

		assertThat(numRangeField.getQueryParameter(FacetParams.FACET_RANGE_START).getValue()).isEqualTo(start);
		assertThat(numRangeField.getQueryParameter(FacetParams.FACET_RANGE_END).getValue()).isEqualTo(end);
		assertThat(numRangeField.getQueryParameter(FacetParams.FACET_RANGE_GAP).getValue()).isEqualTo(gap);
		assertThat(numRangeField.getQueryParameter(FacetParams.FACET_RANGE_HARD_END).getValue()).isEqualTo(hardEnd);
		assertThat(numRangeField.getQueryParameter(FacetParams.FACET_RANGE_INCLUDE).getValue()).isEqualTo(include);
		assertThat(numRangeField.getQueryParameter(FacetParams.FACET_RANGE_OTHER).getValue()).isEqualTo(other);
	}

	@Test // DATSOLR-86
	public void testDateRangeFacetAccessorsAfterNullSet() {
		FieldWithDateRangeParameters dateRangeField = new FieldWithDateRangeParameters( //
				"name", //
				new Date(100), //
				new Date(10000000), //
				"+1DAY"//
		)//
				.setHardEnd(true) //
				.setInclude(FacetRangeInclude.LOWER) //
				.setOther(FacetRangeOther.BEFORE);

		dateRangeField.setHardEnd(null);
		dateRangeField.setInclude(null);
		dateRangeField.setOther(null);

		assertThat(dateRangeField.getQueryParameter(FacetParams.FACET_RANGE_HARD_END)).isNull();
		assertThat(dateRangeField.getQueryParameter(FacetParams.FACET_RANGE_INCLUDE)).isNull();
		assertThat(dateRangeField.getQueryParameter(FacetParams.FACET_RANGE_OTHER)).isNull();
	}

	@Test // DATSOLR-86
	public void testNumericRangeFacetAccessorsAfterNullSet() {
		FieldWithNumericRangeParameters numRangeField = new FieldWithNumericRangeParameters( //
				"name", //
				100, //
				10000000, //
				200 //
		)//
				.setHardEnd(true) //
				.setInclude(FacetRangeInclude.LOWER) //
				.setOther(FacetRangeOther.BEFORE);

		numRangeField.setHardEnd(null);
		numRangeField.setInclude(null);
		numRangeField.setOther(null);

		assertThat(numRangeField.getQueryParameter(FacetParams.FACET_RANGE_HARD_END)).isNull();
		assertThat(numRangeField.getQueryParameter(FacetParams.FACET_RANGE_INCLUDE)).isNull();
		assertThat(numRangeField.getQueryParameter(FacetParams.FACET_RANGE_OTHER)).isNull();
	}

}
