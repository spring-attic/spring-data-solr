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
package org.springframework.data.solr.core.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.query.HighlightOptions.FieldWithHighlightParameters;
import org.springframework.data.solr.core.query.HighlightOptions.HighlightParameter;

/**
 * @author Christoph Strobl
 */
public class HighlightOptionsTests {

	private static final String PARAMETER_VALUE = "rocks";

	private static final String PARAMETER_NAME = "spring";

	private HighlightOptions options;

	private static final Field FIELD_1 = new SimpleField("field_1");
	private static final Field FIELD_2 = new SimpleField("field_2");
	private static final FieldWithHighlightParameters FIELD_WITH_HIGHLIGHT_OPTIONS = new FieldWithHighlightParameters(
			"field_2");

	@Before
	public void setUp() {
		this.options = new HighlightOptions();
	}

	@Test
	public void testEmptyOption() {
		assertThat(options.getFields()).isEmpty();
		assertThat(options.getFieldsWithHighlightParameters()).isEmpty();
		assertThat(options.getHighlightParameters()).isEmpty();
		assertThat(options.getQuery()).isNull();
		assertThat(options.hasFields()).isFalse();
		assertThat(options.hasQuery()).isFalse();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddNullField() {
		options.addField((Field) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddNullFieldname() {
		options.addField((String) null);
	}

	@Test
	public void testWithMultipleFields() {
		options.addField(FIELD_1);
		options.addField(FIELD_2);
		assertThat(options.hasFields()).isTrue();
		assertThat(options.getFields()).containsExactly(FIELD_1, FIELD_2);
		assertThat(options.getFieldsWithHighlightParameters()).isEmpty();
	}

	@Test
	public void testWithMultipleFieldsAndFieldWithHighlightParameters() {
		options.addField(FIELD_1);
		options.addField(FIELD_2);
		options.addField(FIELD_WITH_HIGHLIGHT_OPTIONS);
		assertThat(options.getFields()).containsExactly(FIELD_1, FIELD_2, FIELD_WITH_HIGHLIGHT_OPTIONS);
		assertThat(options.getFieldsWithHighlightParameters()).containsExactly(FIELD_WITH_HIGHLIGHT_OPTIONS);
	}

	@Test
	public void testHasQuery() {
		options.setQuery(new SimpleQuery(new SimpleStringCriteria("*:*")));
		assertThat(options.hasQuery()).isTrue();
	}

	@Test
	public void testAddParamters() {
		options.addHighlightParameter(PARAMETER_NAME, PARAMETER_VALUE);
		assertThat(options.getHighlightParameters().size()).isEqualTo(1);

		HighlightParameter parameter = options.getHighlightParameters().iterator().next();
		assertThat(parameter.getName()).isEqualTo(PARAMETER_NAME);
		assertThat(parameter.getValue()).isEqualTo(PARAMETER_VALUE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddParamterWithNullName() {
		options.addHighlightParameter(null, PARAMETER_VALUE);
	}

	@Test
	public void testGetHighlighParameterValue() {
		options.addHighlightParameter(PARAMETER_NAME, PARAMETER_VALUE);
		assertThat(options.getHighlightParameters().size()).isEqualTo(1);
		String parameterValue = options.getHighlightParameterValue(PARAMETER_NAME);
		assertThat(parameterValue).isEqualTo(PARAMETER_VALUE);
	}

	@Test
	public void testGetHighlighParameterValueForParameterThatDoesNotExist() {
		options.addHighlightParameter(PARAMETER_NAME, PARAMETER_VALUE);
		assertThat(options.getHighlightParameters().size()).isEqualTo(1);
		assertThat(options.<Object> getHighlightParameterValue("ParameterThatDoesNotExist")).isNull();
	}

	@Test
	public void testGetHighlighParameterValueForNullParameterName() {
		options.addHighlightParameter(PARAMETER_NAME, PARAMETER_VALUE);
		assertThat(options.getHighlightParameters().size()).isEqualTo(1);
		assertThat(options.<Object> getHighlightParameterValue(null)).isNull();
	}

}
