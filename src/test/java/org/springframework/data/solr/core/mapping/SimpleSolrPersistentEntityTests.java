/*
 * Copyright 2012 - 2014 the original author or authors.
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

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.solr.core.mapping.SimpleSolrPersistentPropertyTest.BeanWithScore;
import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleSolrPersistentEntityTests {

	private static final String CORE_NAME = "core1";

	@SuppressWarnings("rawtypes") @Mock TypeInformation typeInfo;

	@Mock private SolrPersistentProperty property;

	@SuppressWarnings("unchecked")
	@Test
	public void testPersistentEntityWithSolrDocumentAnnotation() {

		Mockito.when(typeInfo.getType()).thenReturn(SearchableBeanWithSolrDocumentAnnotation.class);

		SimpleSolrPersistentEntity<SearchableBeanWithSolrDocumentAnnotation> pe = new SimpleSolrPersistentEntity<SearchableBeanWithSolrDocumentAnnotation>(
				typeInfo);
		Assert.assertEquals(CORE_NAME, pe.getSolrCoreName());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPersistentEntityShouldReadSolrCoreNameFromParentClass() {

		Mockito.when(typeInfo.getType()).thenReturn(InheritingClass.class);

		SimpleSolrPersistentEntity<InheritingClass> pe = new SimpleSolrPersistentEntity<InheritingClass>(typeInfo);
		Assert.assertEquals(CORE_NAME, pe.getSolrCoreName());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPersistentEntityWithoutSolrDocumentAnnotation() {

		Mockito.when(typeInfo.getType()).thenReturn(SearchableBeanWithoutSolrDocumentAnnotation.class);

		SimpleSolrPersistentEntity<SearchableBeanWithoutSolrDocumentAnnotation> pe = new SimpleSolrPersistentEntity<SearchableBeanWithoutSolrDocumentAnnotation>(
				typeInfo);
		Assert.assertEquals("searchablebeanwithoutsolrdocumentannotation", pe.getSolrCoreName());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPersistentEntityWithEmptySolrDocumentAnnotation() {

		Mockito.when(typeInfo.getType()).thenReturn(SearchableBeanWithEmptySolrDocumentAnnotation.class);

		SimpleSolrPersistentEntity<SearchableBeanWithEmptySolrDocumentAnnotation> pe = new SimpleSolrPersistentEntity<SearchableBeanWithEmptySolrDocumentAnnotation>(
				typeInfo);
		Assert.assertEquals("searchablebeanwithemptysolrdocumentannotation", pe.getSolrCoreName());
	}

	/**
	 * @see DATASOLR-88
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testPersistentEntityShouldReadDocumentBoostFromSolrDocumentAnnotation() {

		Mockito.when(typeInfo.getType()).thenReturn(DocumentWithBoost.class);

		SimpleSolrPersistentEntity<DocumentWithBoost> pe = new SimpleSolrPersistentEntity<DocumentWithBoost>(typeInfo);
		Assert.assertThat(pe.isBoosted(), Is.is(true));
		Assert.assertThat(pe.getBoost(), Is.is(100f));
	}

	/**
	 * @see DATASOLR-88
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testPersistentEntityShouldNotBeBoostenWhenSolrDocumentAnnotationHasDefaultBoostValue() {

		Mockito.when(typeInfo.getType()).thenReturn(SearchableBeanWithEmptySolrDocumentAnnotation.class);

		SimpleSolrPersistentEntity<SearchableBeanWithEmptySolrDocumentAnnotation> pe = new SimpleSolrPersistentEntity<SearchableBeanWithEmptySolrDocumentAnnotation>(
				typeInfo);
		Assert.assertThat(pe.isBoosted(), Is.is(false));
		Assert.assertThat(pe.getBoost(), IsNull.nullValue());
	}

	/**
	 * @see DATASOLR-210
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testPersistentEntityWithScoreProperty() {

		Mockito.when(typeInfo.getType()).thenReturn(BeanWithScore.class);
		Mockito.when(property.isScoreProperty()).thenReturn(true);
		Mockito.when(property.getFieldName()).thenReturn("myScoreProperty");

		SimpleSolrPersistentEntity<BeanWithScore> pe = new SimpleSolrPersistentEntity<BeanWithScore>(typeInfo);
		pe.addPersistentProperty(property);

		Assert.assertTrue(pe.hasScoreProperty());
		Assert.assertEquals(property, pe.getScoreProperty());
		Assert.assertTrue(pe.isScoreProperty(property));
		Assert.assertEquals("myScoreProperty", pe.getScoreProperty().getFieldName());

	}

	@SolrDocument(solrCoreName = CORE_NAME)
	static class SearchableBeanWithSolrDocumentAnnotation {}

	@SolrDocument
	static class SearchableBeanWithEmptySolrDocumentAnnotation {}

	static class SearchableBeanWithoutSolrDocumentAnnotation {}

	@SolrDocument(solrCoreName = CORE_NAME)
	static class ParentClassWithSolrDocumentAnnotation {}

	static class InheritingClass extends ParentClassWithSolrDocumentAnnotation {}

	@SolrDocument(boost = 100)
	static class DocumentWithBoost {}

	static class DocumentWithScore {}

}
