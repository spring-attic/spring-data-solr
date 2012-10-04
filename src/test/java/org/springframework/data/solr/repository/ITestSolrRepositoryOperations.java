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
package org.springframework.data.solr.repository;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ITestSolrRepositoryOperations {

	private static final ProductBean POPULAR_AVAILABLE_PRODUCT = createProductBean("1", 5, true);
	private static final ProductBean UNPOPULAR_AVAILABLE_PRODUCT = createProductBean("2", 1, true);
	private static final ProductBean UNAVAILABLE_PRODUCT = createProductBean("3", 3, false);
	private static final ProductBean NAMED_PRODUCT = createProductBean("4", 3, true, "product");

	@Autowired
	private ProductRepository repo;

	@Before
	public void setUp() {
		repo.deleteAll();
		repo.save(Arrays.asList(POPULAR_AVAILABLE_PRODUCT, UNPOPULAR_AVAILABLE_PRODUCT, UNAVAILABLE_PRODUCT, NAMED_PRODUCT));
	}

	@After
	public void tearDown() {
		repo.deleteAll();
	}

	@Test
	public void testFindByNamedQuery() {
		List<ProductBean> found = repo.findByNamedQuery(5);
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(POPULAR_AVAILABLE_PRODUCT.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByIs() {
		List<ProductBean> found = repo.findByName(NAMED_PRODUCT.getName());
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(NAMED_PRODUCT.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByIsNot() {
		List<ProductBean> found = repo.findByNameNot(NAMED_PRODUCT.getName());
		Assert.assertEquals(3, found.size());
	}

	@Test
	public void testFindSingleElementByIs() {
		ProductBean product = repo.findById(POPULAR_AVAILABLE_PRODUCT.getId());
		Assert.assertNotNull(product);
		Assert.assertEquals(POPULAR_AVAILABLE_PRODUCT.getId(), product.getId());
	}

	@Test
	public void testFindByBooleanTrue() {
		List<ProductBean> found = repo.findByAvailableTrue();
		Assert.assertEquals(3, found.size());
	}

	@Test
	public void testFindByBooleanFalse() {
		List<ProductBean> found = repo.findByAvailableFalse();
		Assert.assertEquals(1, found.size());
	}

	@Test
	public void testFindByAvailableUsingQueryAnnotationTrue() {
		List<ProductBean> found = repo.findByAvailableUsingQueryAnnotation(true);
		Assert.assertEquals(3, found.size());
	}

	@Test
	public void testFindByLessThan() {
		List<ProductBean> found = repo.findByPopularityLessThan(2);
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(UNPOPULAR_AVAILABLE_PRODUCT.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByGreaterThan() {
		List<ProductBean> found = repo.findByPopularityGreaterThan(4);
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(POPULAR_AVAILABLE_PRODUCT.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByLike() {
		List<ProductBean> found = repo.findByNameLike(NAMED_PRODUCT.getName().substring(0, 3));
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(NAMED_PRODUCT.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByStartsWith() {
		List<ProductBean> found = repo.findByNameStartsWith(NAMED_PRODUCT.getName().substring(0, 3));
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(NAMED_PRODUCT.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByIn() {
		List<ProductBean> found = repo.findByPopularityIn(Arrays.asList(3, 5));
		Assert.assertEquals(3, found.size());
	}

	@Test
	public void testFindConcatedByAnd() {
		List<ProductBean> found = repo.findByPopularityAndAvailableTrue(POPULAR_AVAILABLE_PRODUCT.getPopularity());
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(POPULAR_AVAILABLE_PRODUCT.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByNear() {
		ProductBean locatedInBuffalow = createProductBean("100", 5, true);
		locatedInBuffalow.setLocation("45.17614,-93.87341");

		ProductBean locatedInNYC = createProductBean("200", 5, true);
		locatedInNYC.setLocation("40.7143,-74.006");

		repo.save(Arrays.asList(locatedInBuffalow, locatedInNYC));

		List<ProductBean> found = repo.findByLocationNear(new GeoLocation(45.15, -93.85), new Distance(5));
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(locatedInBuffalow.getId(), found.get(0).getId());
	}

	private static ProductBean createProductBean(String id, int popularity, boolean available) {
		return createProductBean(id, popularity, available, "");
	}

	private static ProductBean createProductBean(String id, int popularity, boolean available, String name) {
		ProductBean initial = new ProductBean();
		initial.setId(id);
		initial.setAvailable(available);
		initial.setPopularity(popularity);
		if (StringUtils.hasText(name)) {
			initial.setName(name);
		} else {
			initial.setName("name-" + id);
		}
		return initial;
	}
}
