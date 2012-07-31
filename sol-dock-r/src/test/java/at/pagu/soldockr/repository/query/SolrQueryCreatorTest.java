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
package at.pagu.soldockr.repository.query;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.parser.PartTree;

import at.pagu.soldockr.core.mapping.SimpleSolrMappingContext;
import at.pagu.soldockr.core.mapping.SolrPersistentProperty;
import at.pagu.soldockr.core.query.Criteria;
import at.pagu.soldockr.core.query.Query;
import at.pagu.soldockr.repository.ProductBean;

@RunWith(MockitoJUnitRunner.class)
public class SolrQueryCreatorTest {

  @Mock
  private RepositoryMetadata metadataMock;

  @Mock
  private SolrEntityInformationCreator entityInformationCreatorMock;

  private MappingContext<?, SolrPersistentProperty> mappingContext;

  @Before
  public void setUp() {
    mappingContext = new SimpleSolrMappingContext();
  }

  @Test
  public void testCreateFindBySingleCriteria() throws NoSuchMethodException, SecurityException {
    Method method = SampleRepository.class.getMethod("findByPopularity", Integer.class);
    PartTree partTree = new PartTree(method.getName(), method.getReturnType());

    SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
    SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod, new Object[] {100}), mappingContext);

    Query query = creator.createQuery();

    Criteria criteria = query.getCriteria();
    Assert.assertEquals("popularity:100", criteria.getQueryString());
  }

  @Test
  public void testCreateFindByAndQuery() throws NoSuchMethodException, SecurityException {
    Method method = SampleRepository.class.getMethod("findByPopularityAndPrice", Integer.class, Float.class);
    PartTree partTree = new PartTree(method.getName(), method.getReturnType());

    SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
    SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod, new Object[] {100, 200f}), mappingContext);

    Query query = creator.createQuery();

    Criteria criteria = query.getCriteria();
    Assert.assertEquals("popularity:100 AND price:200.0", criteria.getQueryString());
  }

  @Test
  public void testCreateFindByOrQuery() throws NoSuchMethodException, SecurityException {
    Method method = SampleRepository.class.getMethod("findByPopularityOrPrice", Integer.class, Float.class);
    PartTree partTree = new PartTree(method.getName(), method.getReturnType());

    SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
    SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod, new Object[] {100, 200f}), mappingContext);

    Query query = creator.createQuery();

    Criteria criteria = query.getCriteria();
    Assert.assertEquals("popularity:100 OR price:200.0", criteria.getQueryString());
  }

  private interface SampleRepository {

    ProductBean findByPopularity(Integer popularity);

    ProductBean findByPopularityAndPrice(Integer popularity, Float price);

    ProductBean findByPopularityOrPrice(Integer popularity, Float price);

  }
}
