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

import java.util.Date;

import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.core.IsNot;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christoph Strobl
 */
public class FieldWithQueryParametersTests {

	private static final String PARAMNAME = "paramname";
	private static final String FIELDNAME = "fieldname";

	private static final QueryParameterImpl OPTION_WITH_STRING = new QueryParameterImpl(PARAMNAME + "-1", "value-1");
	private static final QueryParameterImpl OPTION_WITH_INT = new QueryParameterImpl(PARAMNAME + "-2", 10);
	private static final QueryParameterImpl OPTION_WITH_DATE = new QueryParameterImpl(PARAMNAME + "-3", new Date());

	private FieldWithQueryParameters<QueryParameterImpl> field;

	@Before
	public void setUp() {
		field = new FieldWithQueryParameters<QueryParameterImpl>(FIELDNAME);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createInstanceWithNullForFieldName() {
		new FieldWithQueryParameters<QueryParameterImpl>(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createInstanceWithBlankFieldName() {
		new FieldWithQueryParameters<QueryParameterImpl>(" ");
	}

	@Test
	public void testGetQueryParametersWhenNoneAvailable() {
		Assert.assertThat(field.getQueryParameters(), IsEmptyCollection.empty());
	}

	@Test
	public void testHasQueryParametersWhenNoneAvailable() {
		Assert.assertFalse(field.hasQueryParameters());
	}

	@Test
	public void testGetQueryParameterValueWhenNoneAvailable() {
		Assert.assertNull(field.getQueryParameter(PARAMNAME));
	}

	@Test
	public void testGetIteratorWhenNoneAvailable() {
		Assert.assertThat(field, IsEmptyIterable.emptyIterable());
	}

	@Test
	public void testAddQueryParameter() {
		field.addQueryParameter(OPTION_WITH_STRING);
		Assert.assertTrue(field.hasQueryParameters());
		Assert.assertThat(field.getQueryParameters(), IsNot.not(IsEmptyCollection.empty()));
		Assert.assertThat(field, IsNot.not(IsEmptyIterable.emptyIterable()));
		Assert.assertEquals(OPTION_WITH_STRING, field.getQueryParameter(OPTION_WITH_STRING.getName()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddNullQueryParameter() {
		field.addQueryParameter(null);
	}

	@Test
	public void testAddingMultipleQueryParameters() {
		field.addQueryParameter(OPTION_WITH_STRING);
		field.addQueryParameter(OPTION_WITH_INT);
		field.addQueryParameter(OPTION_WITH_DATE);

		Assert
				.assertThat(field.getQueryParameters(), Matchers.contains(OPTION_WITH_STRING, OPTION_WITH_INT, OPTION_WITH_DATE));
	}

	@Test
	public void testGetParameter() {
		field.addQueryParameter(OPTION_WITH_STRING);
		field.addQueryParameter(OPTION_WITH_INT);

		Assert.assertEquals(OPTION_WITH_STRING, field.getQueryParameter(OPTION_WITH_STRING.getName()));
		Assert.assertEquals(OPTION_WITH_INT, field.getQueryParameter(OPTION_WITH_INT.getName()));
		Assert.assertNull(field.getQueryParameter(OPTION_WITH_DATE.getName()));
	}

	@Test
	public void getParameterValue() {
		field.addQueryParameter(OPTION_WITH_STRING);
		field.addQueryParameter(OPTION_WITH_INT);
		field.addQueryParameter(OPTION_WITH_DATE);

		String stringOptionValue = field.getQueryParameterValue(OPTION_WITH_STRING.getName());
		Integer intOptionValue = field.getQueryParameterValue(OPTION_WITH_INT.getName());
		Date dateOptionValue = field.getQueryParameterValue(OPTION_WITH_DATE.getName());

		Assert.assertEquals(OPTION_WITH_STRING.getValue(), stringOptionValue);
		Assert.assertEquals(OPTION_WITH_INT.getValue(), intOptionValue);
		Assert.assertEquals(OPTION_WITH_DATE.getValue(), dateOptionValue);
	}

}
