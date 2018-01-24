/*
 * Copyright 2012 - 2017 the original author or authors.
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
package org.springframework.data.solr.core.mapping;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collection;

import org.apache.solr.client.solrj.beans.Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
@RunWith(Parameterized.class)
public class SimpleSolrPersitentPropertyFieldNameTests {

	private SimpleSolrPersistentEntity<BeanWithSolrFieldAnnotation> persistentEntity;

	private final String propertyName;
	private final String expectedFieldname;

	public SimpleSolrPersitentPropertyFieldNameTests(String propertyName, String expectedFieldname) {
		this.propertyName = propertyName;
		this.expectedFieldname = expectedFieldname;
	}

	@Before
	public void setUp() {

		TypeInformation<BeanWithSolrFieldAnnotation> typeInformation = ClassTypeInformation
				.from(BeanWithSolrFieldAnnotation.class);

		persistentEntity = new SimpleSolrPersistentEntity<BeanWithSolrFieldAnnotation>(typeInformation);
	}

	@Parameters
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { { "fieldWithSolrjFieldAnnotation", "fieldWithSolrjFieldAnnotation" },
				{ "fieldWithSolrjFieldAnnotationAndValue", "solrj" },
				{ "fieldWithIndexedAnnotation", "fieldWithIndexedAnnotation" },
				{ "fieldWithIndexedAnnotationAndValue", "indexed" }, { "fieldWithBothAnnotations", "solrj" } };
		return Arrays.asList(data);
	}

	@Test
	public void testGetFieldnameReturnsProperNameForAnnotationsIndexedAndField() throws IntrospectionException {
		Assert.assertEquals(this.expectedFieldname,
				getPersistentProperty(BeanWithSolrFieldAnnotation.class, this.propertyName).getFieldName());
	}

	private SimpleSolrPersistentProperty getPersistentProperty(Class<?> clazz, String propertyName)
			throws IntrospectionException {
		PropertyDescriptor descriptor = new PropertyDescriptor(propertyName, clazz);
		java.lang.reflect.Field field = org.springframework.util.ReflectionUtils.findField(clazz, propertyName);

		return new SimpleSolrPersistentProperty(field, descriptor, persistentEntity, new SimpleTypeHolder());
	}

	static class BeanWithSolrFieldAnnotation {

		@Field //
		private String fieldWithSolrjFieldAnnotation;

		@Field("solrj") //
		private String fieldWithSolrjFieldAnnotationAndValue;

		@Indexed //
		private String fieldWithIndexedAnnotation;

		@Indexed("indexed") //
		private String fieldWithIndexedAnnotationAndValue;

		@Field("solrj") //
		@Indexed("indexed") //
		private String fieldWithBothAnnotations;

		public String getFieldWithSolrjFieldAnnotation() {
			return fieldWithSolrjFieldAnnotation;
		}

		public void setFieldWithSolrjFieldAnnotation(String fieldWithSolrjFieldAnnotation) {
			this.fieldWithSolrjFieldAnnotation = fieldWithSolrjFieldAnnotation;
		}

		public String getFieldWithSolrjFieldAnnotationAndValue() {
			return fieldWithSolrjFieldAnnotationAndValue;
		}

		public void setFieldWithSolrjFieldAnnotationAndValue(String fieldWithSolrjFieldAnnotationAndValue) {
			this.fieldWithSolrjFieldAnnotationAndValue = fieldWithSolrjFieldAnnotationAndValue;
		}

		public String getFieldWithIndexedAnnotation() {
			return fieldWithIndexedAnnotation;
		}

		public void setFieldWithIndexedAnnotation(String fieldWithIndexedAnnotation) {
			this.fieldWithIndexedAnnotation = fieldWithIndexedAnnotation;
		}

		public String getFieldWithIndexedAnnotationAndValue() {
			return fieldWithIndexedAnnotationAndValue;
		}

		public void setFieldWithIndexedAnnotationAndValue(String fieldWithIndexedAnnotationAndValue) {
			this.fieldWithIndexedAnnotationAndValue = fieldWithIndexedAnnotationAndValue;
		}

		public String getFieldWithBothAnnotations() {
			return fieldWithBothAnnotations;
		}

		public void setFieldWithBothAnnotations(String fieldWithBothAnnotations) {
			this.fieldWithBothAnnotations = fieldWithBothAnnotations;
		}

	}

}
