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
package org.springframework.data.solr.core.query;

import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Assert;
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
		Assert.assertThat(options.getFields(), IsEmptyCollection.emptyCollectionOf(Field.class));
		Assert.assertThat(options.getFieldsWithHighlightParameters(),
				IsEmptyCollection.emptyCollectionOf(FieldWithHighlightParameters.class));
		Assert.assertThat(options.getHighlightParameters(), IsEmptyCollection.emptyCollectionOf(HighlightParameter.class));
		Assert.assertNull(options.getQuery());
		Assert.assertFalse(options.hasFields());
		Assert.assertFalse(options.hasQuery());
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
		Assert.assertTrue(options.hasFields());
		Assert.assertThat(options.getFields(), Matchers.contains(FIELD_1, FIELD_2));
		Assert.assertThat(options.getFieldsWithHighlightParameters(), IsEmptyCollection.empty());
	}

	@Test
	public void testWithMultipleFieldsAndFieldWithHighlightParameters() {
		options.addField(FIELD_1);
		options.addField(FIELD_2);
		options.addField(FIELD_WITH_HIGHLIGHT_OPTIONS);
		Assert.assertThat(options.getFields(), Matchers.contains(FIELD_1, FIELD_2, FIELD_WITH_HIGHLIGHT_OPTIONS));
		Assert.assertThat(options.getFieldsWithHighlightParameters(), Matchers.contains(FIELD_WITH_HIGHLIGHT_OPTIONS));
	}

	@Test
	public void testHasQuery() {
		options.setQuery(new SimpleQuery(new SimpleStringCriteria("*:*")));
		Assert.assertTrue(options.hasQuery());
	}

	@Test
	public void testAddParamters() {
		options.addHighlightParameter(PARAMETER_NAME, PARAMETER_VALUE);
		Assert.assertEquals(1, options.getHighlightParameters().size());

		HighlightParameter parameter = options.getHighlightParameters().iterator().next();
		Assert.assertEquals(PARAMETER_NAME, parameter.getName());
		Assert.assertEquals(PARAMETER_VALUE, parameter.getValue());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddParamterWithNullName() {
		options.addHighlightParameter(null, PARAMETER_VALUE);
	}

	@Test
	public void testGetHighlighParameterValue() {
		options.addHighlightParameter(PARAMETER_NAME, PARAMETER_VALUE);
		Assert.assertEquals(1, options.getHighlightParameters().size());
		String parameterValue = options.getHighlightParameterValue(PARAMETER_NAME);
		Assert.assertEquals(PARAMETER_VALUE, parameterValue);
	}

	@Test
	public void testGetHighlighParameterValueForParameterThatDoesNotExist() {
		options.addHighlightParameter(PARAMETER_NAME, PARAMETER_VALUE);
		Assert.assertEquals(1, options.getHighlightParameters().size());
		Assert.assertNull(options.getHighlightParameterValue("ParameterThatDoesNotExist"));
	}

	@Test
	public void testGetHighlighParameterValueForNullParameterName() {
		options.addHighlightParameter(PARAMETER_NAME, PARAMETER_VALUE);
		Assert.assertEquals(1, options.getHighlightParameters().size());
		Assert.assertNull(options.getHighlightParameterValue(null));
	}

}
