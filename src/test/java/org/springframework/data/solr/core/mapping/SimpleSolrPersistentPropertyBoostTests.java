/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.mapping;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 */
@RunWith(Parameterized.class)
public class SimpleSolrPersistentPropertyBoostTests {

	@SuppressWarnings("rawtypes")//
	private TypeInformation typeInfoMock;

	private SimpleSolrPersistentEntity<BeanWithSolrFieldAnnotation> persistentEntity;

	public @Parameter(0) String propertyName;
	public @Parameter(1) Float expectedBoost;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() {
		typeInfoMock = Mockito.mock(TypeInformation.class);

		Mockito.when(typeInfoMock.getType()).thenReturn(BeanWithSolrFieldAnnotation.class);
		persistentEntity = new SimpleSolrPersistentEntity<BeanWithSolrFieldAnnotation>(typeInfoMock);
	}

	@Parameters(name = "{index}: {0} should be boosted by {1}")
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { { "fieldWithEmptyBoostAnnotation", null },
				{ "fieldWithBoostViaIndexedAnnotation", 100f }, { "fieldWithInvalidBoostViaIndexedAnnotation", null } };
		return Arrays.asList(data);
	}

	@Test // DATASOLR-88
	public void testGetBoostShouldReflectAnnotatedValues() throws IntrospectionException {
		Assert.assertThat(getPersistentProperty(BeanWithSolrFieldAnnotation.class, this.propertyName).getBoost(),
				Is.is(expectedBoost));
	}

	private SimpleSolrPersistentProperty getPersistentProperty(Class<?> clazz, String propertyName)
			throws IntrospectionException {
		PropertyDescriptor descriptor = new PropertyDescriptor(propertyName, clazz);
		java.lang.reflect.Field field = org.springframework.util.ReflectionUtils.findField(clazz, propertyName);

		return new SimpleSolrPersistentProperty(field, descriptor, persistentEntity, new SimpleTypeHolder());
	}

	static class BeanWithSolrFieldAnnotation {

		@Indexed//
		private String fieldWithEmptyBoostAnnotation;

		@Indexed(boost = 100)//
		private String fieldWithBoostViaIndexedAnnotation;

		@Indexed(boost = Float.NaN)//
		private String fieldWithInvalidBoostViaIndexedAnnotation;

		public String getFieldWithEmptyBoostAnnotation() {
			return fieldWithEmptyBoostAnnotation;
		}

		public void setFieldWithEmptyBoostAnnotation(String fieldWithEmptyBoostAnnotation) {
			this.fieldWithEmptyBoostAnnotation = fieldWithEmptyBoostAnnotation;
		}

		public String getFieldWithBoostViaIndexedAnnotation() {
			return fieldWithBoostViaIndexedAnnotation;
		}

		public void setFieldWithBoostViaIndexedAnnotation(String fieldWithBoostViaIndexedAnnotation) {
			this.fieldWithBoostViaIndexedAnnotation = fieldWithBoostViaIndexedAnnotation;
		}

		public String getFieldWithInvalidBoostViaIndexedAnnotation() {
			return fieldWithInvalidBoostViaIndexedAnnotation;
		}

		public void setFieldWithInvalidBoostViaIndexedAnnotation(String fieldWithInvalidBoostViaIndexedAnnotation) {
			this.fieldWithInvalidBoostViaIndexedAnnotation = fieldWithInvalidBoostViaIndexedAnnotation;
		}

	}

}
