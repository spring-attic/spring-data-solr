/*
 * Copyright 2012 - 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.repository.support;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.After;
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
		assertThat(repository).isNotNull();
	}

	@Test
	public void testCRUDOperations() throws InterruptedException {
		ProductBean initial = createProductBean("1");

		ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);
		assertThat(repository.count()).isEqualTo(0);

		repository.save(initial);
		assertThat(repository.count()).isEqualTo(1);

		ProductBean loaded = repository.findById(initial.getId()).get();
		assertThat(loaded.getName()).isEqualTo(initial.getName());

		loaded.setName("name changed");
		repository.save(loaded);
		assertThat(repository.count()).isEqualTo(1);

		loaded = repository.findById(initial.getId()).get();
		assertThat(loaded.getName()).isEqualTo("name changed");

		repository.delete(loaded);

		Thread.sleep(200);
		assertThat(repository.count()).isEqualTo(0);
	}

	@Test
	public void testAnnotatedQuery() {
		ProductBean initial = createProductBean("1");

		ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);
		repository.save(initial);

		Page<ProductBean> result = repository.findByAnnotatedQuery("na", PageRequest.of(0, 5));
		assertThat(result.getContent().size()).isEqualTo(1);
	}

	@Test
	public void testScoredAnnotatedQuery() {
		ProductBean initial = createProductBean("1");

		ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);
		repository.save(initial);

		ScoredPage<ProductBean> result = repository.findByAnnotatedQuery1("na", PageRequest.of(0, 5));
		assertThat(result.getContent().size()).isEqualTo(1);
		assertThat(result.getMaxScore()).isEqualTo(Float.valueOf(1));
	}

	@Test
	public void testPartTreeQuery() {
		ProductBean availableProduct = createProductBean("1");
		ProductBean unavailableProduct = createProductBean("2");
		unavailableProduct.setAvailable(false);

		ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);

		repository.saveAll(Arrays.asList(availableProduct, unavailableProduct));
		assertThat(repository.count()).isEqualTo(2);

		Page<ProductBean> result = repository.findByAvailableTrue(PageRequest.of(0, 10));
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).getId()).isEqualTo(availableProduct.getId());
	}

	@Test
	public void testCollectionResultQuery() {
		ProductBean availableProduct = createProductBean("1");
		ProductBean unavailableProduct = createProductBean("2");
		unavailableProduct.setAvailable(false);

		ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);

		repository.saveAll(Arrays.asList(availableProduct, unavailableProduct));
		assertThat(repository.count()).isEqualTo(2);

		List<ProductBean> result = repository.findByAvailableTrue();
		assertThat(result.size()).isEqualTo(1);
		assertThat(result.get(0).getId()).isEqualTo(availableProduct.getId());
	}

	@Test
	public void testSingleResultQuery() {
		ProductBean initial = createProductBean("1");

		ProductBeanRepository repository = factory.getRepository(ProductBeanRepository.class);
		repository.save(initial);

		ProductBean result = repository.findSingleElement(initial.getId());

		assertThat(result.getId()).isEqualTo(initial.getId());
		assertThat(result.getName()).isEqualTo(initial.getName());
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
