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

import at.pagu.soldockr.core.mapping.SolrPersistentEntity;
import at.pagu.soldockr.repository.ProductBean;
import at.pagu.soldockr.repository.query.SolrEntityInformation;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingSolrEntityInformationTest {

  private static final String PRODUCT_BEAN_SIMPLE_NAME = "productbean";

  @Mock
  private SolrPersistentEntity<ProductBean> persistentEntity;

  @Before
  public void setUp() {
    Mockito.when(persistentEntity.getType()).thenReturn(ProductBean.class);
    Mockito.when(persistentEntity.getSolrCoreName()).thenReturn(PRODUCT_BEAN_SIMPLE_NAME);
  }

  @Test
  public void testSolrCoreRetrievalWhenNotExplicitlySet() {
    SolrEntityInformation<ProductBean, String> entityInformation = new MappingSolrEntityInformation<ProductBean, String>(persistentEntity);
    Assert.assertEquals(PRODUCT_BEAN_SIMPLE_NAME, entityInformation.getSolrCoreName());
  }

  @Test
  public void testSolrCoreRetrievalWhenSet() {
    final String coreName = "core1";
    SolrEntityInformation<ProductBean, String> entityInformation = new MappingSolrEntityInformation<ProductBean, String>(persistentEntity, coreName);
    Assert.assertEquals(coreName, entityInformation.getSolrCoreName());
  }

  @Test
  public void testIdType() {
    SolrEntityInformation<ProductBean, String> entityInformation = new MappingSolrEntityInformation<ProductBean, String>(persistentEntity);
    Assert.assertEquals(String.class, entityInformation.getIdType());
  }

}
