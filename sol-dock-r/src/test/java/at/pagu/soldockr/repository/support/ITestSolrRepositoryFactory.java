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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import at.pagu.soldockr.AbstractITestWithEmbeddedSolrServer;
import at.pagu.soldockr.core.HttpSolrServerFactory;
import at.pagu.soldockr.core.SolrTemplate;
import at.pagu.soldockr.repository.ProductBean;
import at.pagu.soldockr.repository.Query;
import at.pagu.soldockr.repository.SolrCrudRepository;

public class ITestSolrRepositoryFactory extends AbstractITestWithEmbeddedSolrServer {

  private SolrRepositoryFactory factory;

  @Before
  public void setUp() {
    SolrTemplate template = new SolrTemplate(new HttpSolrServerFactory(solrServer));
    factory = new SolrRepositoryFactory(template);
  }

  @Test
  public void testGetRepository() {
    ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);
    Assert.assertNotNull(repository);
  }

  @Test
  public void testCRUDOperations() {
    ProductBean initial = createProductBean("1");

    ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);
    Assert.assertEquals(0, repository.count());

    repository.save(initial);
    Assert.assertEquals(1, repository.count());

    ProductBean loaded = repository.findOne(initial.getId());
    Assert.assertEquals(initial.getName(), loaded.getName());

    loaded.setName("name changed");
    repository.save(loaded);
    Assert.assertEquals(1, repository.count());

    loaded = repository.findOne(initial.getId());
    Assert.assertEquals("name changed", loaded.getName());

    repository.delete(loaded);
    Assert.assertEquals(0, repository.count());

  }

  @Test
  public void testAnnotatedQuery() {
    ProductBean initial = createProductBean("1");

    ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);
    repository.save(initial);

    Page<ProductBean> result = repository.findByAnnotatedQuery("na", new PageRequest(0, 5));
    Assert.assertEquals(1, result.getContent().size());
  }

  private ProductBean createProductBean(String id) {
    ProductBean initial = new ProductBean();
    initial.setId(id);
    initial.setAvailable(true);
    initial.setName("name-" + id);
    return initial;
  }

  public interface ProductBeanRepository extends SolrCrudRepository<ProductBean> {

    Page<ProductBean> findByAvailableTrue(Pageable page);

    @Query("name:?0*")
    Page<ProductBean> findByAnnotatedQuery(String prefix, Pageable page);

  }

}
