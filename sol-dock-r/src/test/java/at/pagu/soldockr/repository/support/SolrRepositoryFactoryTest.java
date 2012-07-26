/*
 * Copyright (C) 2012 sol-dock-r authors.
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
package at.pagu.soldockr.repository.support;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;

import at.pagu.soldockr.core.SolrOperations;
import at.pagu.soldockr.core.convert.SolrConverter;
import at.pagu.soldockr.core.mapping.SolrPersistentEntity;
import at.pagu.soldockr.repository.ProductBean;
import at.pagu.soldockr.repository.query.SolrEntityInformation;

@RunWith(MockitoJUnitRunner.class)
public class SolrRepositoryFactoryTest {

  @Mock
  private SolrOperations solrOperationsMock;

  @Mock
  private SolrConverter solrConverterMock;

  @Mock
  @SuppressWarnings("rawtypes")
  private MappingContext mappingContextMock;

  @Mock
  private SolrPersistentEntity<ProductBean> solrEntityMock;

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() {
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

  @SuppressWarnings("unchecked")
  private void initMappingContext() {
    Mockito.when(mappingContextMock.getPersistentEntity(ProductBean.class)).thenReturn(solrEntityMock);
    Mockito.when(solrEntityMock.getType()).thenReturn(ProductBean.class);
  }

  interface ProductRepository extends Repository<ProductBean, String> {

  }

}
