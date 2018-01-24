/*
 * Copyright 2012 - 2018 the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.result.ScoredPage;
import org.springframework.data.solr.repository.ProductBean;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.SolrCrudRepository;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public class ITestSolrRepositoryFactory extends AbstractITestWithEmbeddedSolrServer {

	private SolrRepositoryFactory factory;

	@Before
	public void setUp() {
		SolrTemplate template = new SolrTemplate(server);
		template.afterPropertiesSet();
		factory = new SolrRepositoryFactory(template);
	}

	@After
	public void tearDown() throws SolrServerException, IOException {
		cleanDataInSolr();
	}

	@Test
	public void testGetRepository() {
		ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);
		Assert.assertNotNull(repository);
	}

	@Test
	public void testCRUDOperations() throws InterruptedException {
		ProductBean initial = createProductBean("1");

		ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);
		Assert.assertEquals(0, repository.count());

		repository.save(initial);
		Assert.assertEquals(1, repository.count());

		ProductBean loaded = repository.findById(initial.getId()).get();
		Assert.assertEquals(initial.getName(), loaded.getName());

		loaded.setName("name changed");
		repository.save(loaded);
		Assert.assertEquals(1, repository.count());

		loaded = repository.findById(initial.getId()).get();
		Assert.assertEquals("name changed", loaded.getName());

		repository.delete(loaded);

		Thread.sleep(200);
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

	@Test
	public void testScoredAnnotatedQuery() {
		ProductBean initial = createProductBean("1");

		ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);
		repository.save(initial);

		ScoredPage<ProductBean> result = repository.findByAnnotatedQuery1("na", new PageRequest(0, 5));
		Assert.assertEquals(1, result.getContent().size());
		Assert.assertEquals(Float.valueOf(1), result.getMaxScore());
	}

	@Test
	public void testPartTreeQuery() {
		ProductBean availableProduct = createProductBean("1");
		ProductBean unavailableProduct = createProductBean("2");
		unavailableProduct.setAvailable(false);

		ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);

		repository.saveAll(Arrays.asList(availableProduct, unavailableProduct));
		Assert.assertEquals(2, repository.count());

		Page<ProductBean> result = repository.findByAvailableTrue(new PageRequest(0, 10));
		Assert.assertEquals(1, result.getTotalElements());
		Assert.assertEquals(availableProduct.getId(), result.getContent().get(0).getId());
	}

	@Test
	public void testCollectionResultQuery() {
		ProductBean availableProduct = createProductBean("1");
		ProductBean unavailableProduct = createProductBean("2");
		unavailableProduct.setAvailable(false);

		ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);

		repository.saveAll(Arrays.asList(availableProduct, unavailableProduct));
		Assert.assertEquals(2, repository.count());

		List<ProductBean> result = repository.findByAvailableTrue();
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(availableProduct.getId(), result.get(0).getId());
	}

	@Test
	public void testSingleResultQuery() {
		ProductBean initial = createProductBean("1");

		ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);
		repository.save(initial);

		ProductBean result = repository.findSingleElement(initial.getId());

		Assert.assertEquals(initial.getId(), result.getId());
		Assert.assertEquals(initial.getName(), result.getName());
	}

	private ProductBean createProductBean(String id) {
		ProductBean initial = new ProductBean();
		initial.setId(id);
		initial.setAvailable(true);
		initial.setName("name-" + id);
		return initial;
	}

	public interface ProductBeanRepository extends SolrCrudRepository<ProductBean, String> {

		Page<ProductBean> findByAvailableTrue(Pageable page);

		@Query("name:?0*")
		Page<ProductBean> findByAnnotatedQuery(String prefix, Pageable page);

		@Query("inStock:true")
		List<ProductBean> findByAvailableTrue();

		@Query("id:?0")
		ProductBean findSingleElement(String id);

		@Query(value = "name:?0*", fields = { "*", "score" })
		ScoredPage<ProductBean> findByAnnotatedQuery1(String prefix, Pageable page);
	}

}
