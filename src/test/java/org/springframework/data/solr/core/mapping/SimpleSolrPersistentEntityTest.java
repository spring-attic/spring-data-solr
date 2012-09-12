/*
 * Copyright 2012 the original author or authors.
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

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.util.TypeInformation;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleSolrPersistentEntityTest {

	private static final String CORE_NAME = "core1";

	@SuppressWarnings("rawtypes")
	@Mock
	TypeInformation typeInfo;

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

	@SolrDocument(solrCoreName = CORE_NAME)
	static class SearchableBeanWithSolrDocumentAnnotation {
	}

	@SolrDocument
	static class SearchableBeanWithEmptySolrDocumentAnnotation {
	}

	static class SearchableBeanWithoutSolrDocumentAnnotation {
	}

}
