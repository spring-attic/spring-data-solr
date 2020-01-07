/*
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.repository.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.data.solr.core.mapping.SolrDocument;
import org.springframework.data.solr.repository.SolrCrudRepository;
import org.springframework.data.solr.repository.SolrRepository;

/**
 * @author Christoph Strobl
 */
public class SolrRepositoryConfigExtensionUnitTests {

	private SolrRepositoryConfigExtension extension = new SolrRepositoryConfigExtension();

	@Test // DATASOLR-184
	public void shouldReturnSolrDocumentAsIdentifyingAnnotation() {

		assertThat(extension.getIdentifyingAnnotations()).hasSize(1);
		assertThat(extension.getIdentifyingAnnotations()).contains(SolrDocument.class);
	}

	@Test // DATASOLR-184
	public void shoudReturnStoreSpecificRepositoryInterfacesAsIdentifyingTypes() {

		assertThat(extension.getIdentifyingTypes()).hasSize(2);
		assertThat(extension.getIdentifyingTypes()).contains(SolrRepository.class, SolrCrudRepository.class);
	}
}
