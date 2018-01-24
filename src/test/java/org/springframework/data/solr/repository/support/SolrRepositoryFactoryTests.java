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
package org.springframework.data.solr.repository.support;

import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.repository.ProductBean;
import org.springframework.data.solr.repository.SolrCrudRepository;
import org.springframework.data.solr.repository.query.SolrEntityInformation;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrRepositoryFactoryTests {

	@Mock private SolrOperations solrOperationsMock;

	@Mock private SolrConverter solrConverterMock;

	@Mock @SuppressWarnings("rawtypes") private MappingContext mappingContextMock;

	@Mock private SolrPersistentEntity<ProductBean> solrEntityMock;

	@Mock private SolrPersistentProperty solrPersistentPropertyMock;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {
		Mockito.when(solrEntityMock.getIdProperty()).thenReturn(solrPersistentPropertyMock);
		Mockito.when(solrPersistentPropertyMock.getFieldName()).thenReturn("id");
		Mockito.when(solrOperationsMock.getConverter()).thenReturn(solrConverterMock);
		Mockito.when(solrConverterMock.getMappingContext()).thenReturn(mappingContextMock);
	}

	@Test
	public void testGetEntityInformation() {
		initMappingContext();

		SolrRepositoryFactory repoFactory = new SolrRepositoryFactory(solrOperationsMock);
		SolrEntityInformation<ProductBean, String> entityInformation = repoFactory.getEntityInformation(ProductBean.class);
		Assert.assertTrue(entityInformation instanceof MappingSolrEntityInformation);
	}

	@Test
	public void testGetRepository() {
		initMappingContext();

		SolrRepositoryFactory repoFactory = new SolrRepositoryFactory(solrOperationsMock);
		ProductRepository repository = repoFactory.getRepository(ProductRepository.class);
		Assert.assertNotNull(repository);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetRepositoryOfUnmanageableType() {

		SolrTemplate template = new SolrTemplate(new HttpSolrClient("http://solrserver:8983/solr"), null);
		template.afterPropertiesSet();
		new SolrRepositoryFactory(template).getRepository(UnmanagedEntityRepository.class);
	}

	@SuppressWarnings("unchecked")
	private void initMappingContext() {
		Mockito.when(mappingContextMock.getPersistentEntity(ProductBean.class)).thenReturn(solrEntityMock);
		Mockito.when(solrEntityMock.getType()).thenReturn(ProductBean.class);
	}

	interface ProductRepository extends Repository<ProductBean, String> {

	}

	interface UnmanagedEntityRepository extends SolrCrudRepository<Object, String> {

	}

}
