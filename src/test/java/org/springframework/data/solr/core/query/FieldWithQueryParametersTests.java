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

import java.util.Date;

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
		field = new FieldWithQueryParameters<>(FIELDNAME);
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
		assertThat(field.getQueryParameters()).isEmpty();
	}

	@Test
	public void testHasQueryParametersWhenNoneAvailable() {
		assertThat(field.hasQueryParameters()).isFalse();
	}

	@Test
	public void testGetQueryParameterValueWhenNoneAvailable() {
		assertThat(field.getQueryParameter(PARAMNAME)).isNull();
	}

	@Test
	public void testGetIteratorWhenNoneAvailable() {
		assertThat(field).isEmpty();
	}

	@Test
	public void testAddQueryParameter() {
		field.addQueryParameter(OPTION_WITH_STRING);
		assertThat(field.hasQueryParameters()).isTrue();
		assertThat(field.getQueryParameters()).isNotEmpty();
		assertThat(field).isNotEmpty();
		assertThat(field.getQueryParameter(OPTION_WITH_STRING.getName())).isEqualTo(OPTION_WITH_STRING);
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

		assertThat(field.getQueryParameters()).containsExactly(OPTION_WITH_STRING, OPTION_WITH_INT, OPTION_WITH_DATE);
	}

	@Test
	public void testGetParameter() {
		field.addQueryParameter(OPTION_WITH_STRING);
		field.addQueryParameter(OPTION_WITH_INT);

		assertThat(field.getQueryParameter(OPTION_WITH_STRING.getName())).isEqualTo(OPTION_WITH_STRING);
		assertThat(field.getQueryParameter(OPTION_WITH_INT.getName())).isEqualTo(OPTION_WITH_INT);
		assertThat(field.getQueryParameter(OPTION_WITH_DATE.getName())).isNull();
	}

	@Test
	public void getParameterValue() {
		field.addQueryParameter(OPTION_WITH_STRING);
		field.addQueryParameter(OPTION_WITH_INT);
		field.addQueryParameter(OPTION_WITH_DATE);

		String stringOptionValue = field.getQueryParameterValue(OPTION_WITH_STRING.getName());
		Integer intOptionValue = field.getQueryParameterValue(OPTION_WITH_INT.getName());
		Date dateOptionValue = field.getQueryParameterValue(OPTION_WITH_DATE.getName());

		assertThat(stringOptionValue).isEqualTo(OPTION_WITH_STRING.getValue());
		assertThat(intOptionValue).isEqualTo(OPTION_WITH_INT.getValue());
		assertThat(dateOptionValue).isEqualTo(OPTION_WITH_DATE.getValue());
	}

}
