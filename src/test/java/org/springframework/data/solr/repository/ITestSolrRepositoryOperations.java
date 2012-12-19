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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
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
	public void testFindOne() {
		ProductBean found = repo.findOne(POPULAR_AVAILABLE_PRODUCT.getId());
		Assert.assertEquals(POPULAR_AVAILABLE_PRODUCT.getId(), found.getId());
	}

	@Test
	public void testFindOneThatDoesNotExist() {
		Assert.assertNull(repo.findOne(POPULAR_AVAILABLE_PRODUCT.getId().concat("XX-XX-XX")));
	}

	@Test
	public void testExists() {
		Assert.assertTrue(repo.exists(POPULAR_AVAILABLE_PRODUCT.getId()));
	}

	@Test
	public void testExistsOneThatDoesNotExist() {
		Assert.assertFalse(repo.exists(POPULAR_AVAILABLE_PRODUCT.getId().concat("XX-XX-XX")));
	}

	@Test
	public void testCount() {
		Assert.assertEquals(4, repo.count());
	}

	@Test
	public void testFindOneByCriteria() {
		ProductBean found = repo.findByNameAndAvailableTrue(NAMED_PRODUCT.getName());
		Assert.assertEquals(NAMED_PRODUCT.getId(), found.getId());
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
	public void testFindByBefore() {
		repo.deleteAll();
		ProductBean modifiedMid2012 = createProductBean("2012", 5, true);
		modifiedMid2012.setLastModified(new DateTime(2012, 6, 1, 0, 0, 0, DateTimeZone.UTC).toDate());

		ProductBean modifiedMid2011 = createProductBean("2011", 5, true);
		modifiedMid2011.setLastModified(new DateTime(2011, 6, 1, 0, 0, 0, DateTimeZone.UTC).toDate());

		repo.save(Arrays.asList(modifiedMid2012, modifiedMid2011));
		List<ProductBean> found = repo.findByLastModifiedBefore(new DateTime(2011, 12, 31, 23, 59, 59, DateTimeZone.UTC)
				.toDate());
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(modifiedMid2011.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByLessThan() {
		List<ProductBean> found = repo.findByPopularityLessThan(2);
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(UNPOPULAR_AVAILABLE_PRODUCT.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByAfter() {
		repo.deleteAll();
		ProductBean modifiedMid2012 = createProductBean("2012", 5, true);
		modifiedMid2012.setLastModified(new DateTime(2012, 6, 1, 0, 0, 0, DateTimeZone.UTC).toDate());

		ProductBean modifiedMid2011 = createProductBean("2011", 5, true);
		modifiedMid2011.setLastModified(new DateTime(2011, 6, 1, 0, 0, 0, DateTimeZone.UTC).toDate());

		repo.save(Arrays.asList(modifiedMid2012, modifiedMid2011));
		List<ProductBean> found = repo
				.findByLastModifiedAfter(new DateTime(2012, 1, 1, 0, 0, 0, DateTimeZone.UTC).toDate());
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(modifiedMid2012.getId(), found.get(0).getId());
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
	public void testFindByNotIn() {
		List<ProductBean> found = repo.findByPopularityNotIn(Arrays.asList(3, 5));
		Assert.assertEquals(1, found.size());
	}

	@Test
	public void testFindConcatedByAnd() {
		List<ProductBean> found = repo.findByPopularityAndAvailableTrue(POPULAR_AVAILABLE_PRODUCT.getPopularity());
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(POPULAR_AVAILABLE_PRODUCT.getId(), found.get(0).getId());
	}

	@Test
	public void testFindConcatedByOr() {
		List<ProductBean> found = repo.findByPopularityOrAvailableFalse(UNPOPULAR_AVAILABLE_PRODUCT.getPopularity());
		Assert.assertEquals(2, found.size());
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

	@Test
	public void testFindWithSort() {
		repo.deleteAll();

		List<ProductBean> values = new ArrayList<ProductBean>();
		for (int i = 0; i < 10; i++) {
			values.add(createProductBean(Integer.toString(i), 5, true));
		}
		repo.save(values);

		List<ProductBean> found = repo.findByAvailableTrueOrderByNameDesc();

		ProductBean prev = found.get(0);
		for (int i = 1; i < found.size(); i++) {
			ProductBean cur = found.get(i);
			Assert.assertTrue(Long.valueOf(cur.getId()) < Long.valueOf(prev.getId()));
			prev = cur;
		}
	}

	@Test
	public void testFindByRegex() {
		List<ProductBean> found = repo.findByNameRegex("na*");
		Assert.assertEquals(3, found.size());
		for (ProductBean bean : found) {
			Assert.assertTrue(bean.getName().startsWith("na"));
		}
	}

	@Test
	public void testPagination() {
		Pageable pageable = new PageRequest(0, 2);
		Page<ProductBean> page1 = repo.findByNameStartingWith("name", pageable);
		Assert.assertEquals(pageable.getPageSize(), page1.getNumberOfElements());
		Assert.assertTrue(page1.hasNextPage());
		Assert.assertEquals(3, page1.getTotalElements());

		pageable = new PageRequest(1, 2);
		Page<ProductBean> page2 = repo.findByNameStartingWith("name", pageable);
		Assert.assertEquals(1, page2.getNumberOfElements());
		Assert.assertFalse(page2.hasNextPage());
		Assert.assertEquals(3, page2.getTotalElements());
	}

	@Test
	public void testPaginationNoElementsFound() {
		Pageable pageable = new PageRequest(0, 2);
		Page<ProductBean> page = repo.findByNameStartingWith("hpotsirhc", pageable);
		Assert.assertEquals(0, page.getNumberOfElements());
		Assert.assertTrue(page.getContent().isEmpty());
	}

	@Test
	public void testProjectionOnFieldsForStringBasedQuery() {
		List<ProductBean> found = repo.findByNameStartsWithProjectionOnNameAndId("name");
		for (ProductBean bean : found) {
			Assert.assertNotNull(bean.getName());
			Assert.assertNotNull(bean.getId());

			Assert.assertNull(bean.getPopularity());
		}
	}

	@Test
	public void testProjectionOnFieldsForDerivedQuery() {
		List<ProductBean> found = repo.findByNameStartingWith("name");
		for (ProductBean bean : found) {
			Assert.assertNotNull(bean.getName());
			Assert.assertNotNull(bean.getId());

			Assert.assertNull(bean.getPopularity());
		}
	}

	@Test
	public void testFacetOnSingleField() {
		FacetPage<ProductBean> facetPage = repo.findAllFacetOnPopularity(new PageRequest(0, 10));
		Assert.assertEquals(1, facetPage.getFacetFields().size());
		Page<FacetFieldEntry> page = facetPage.getFacetResultPage(facetPage.getFacetFields().iterator().next());
		Assert.assertEquals(3, page.getContent().size());
		for (FacetFieldEntry entry : page) {
			Assert.assertEquals("popularity", entry.getField().getName());
		}
	}

	@Test
	public void testFacetOnMultipleFields() {
		FacetPage<ProductBean> facetPage = repo.findAllFacetOnPopularityAndAvailable(new PageRequest(0, 10));
		Assert.assertEquals(2, facetPage.getFacetFields().size());

		Page<FacetFieldEntry> popularityPage = facetPage.getFacetResultPage(new SimpleField("popularity"));
		Assert.assertEquals(3, popularityPage.getContent().size());
		for (FacetFieldEntry entry : popularityPage) {
			Assert.assertEquals("popularity", entry.getField().getName());
		}

		Page<FacetFieldEntry> availablePage = facetPage.getFacetResultPage(new SimpleField("inStock"));
		Assert.assertEquals(2, availablePage.getContent().size());
		for (FacetFieldEntry entry : availablePage) {
			Assert.assertEquals("inStock", entry.getField().getName());
		}
	}

	@Test
	public void testSingleFilter() {
		List<ProductBean> found = repo.findAllFilterAvailableTrue();
		Assert.assertEquals(3, found.size());
		for (ProductBean bean : found) {
			Assert.assertTrue(bean.isAvailable());
		}
	}

	@Test
	public void testMultipleFilters() {
		List<ProductBean> found = repo.findAllFilterAvailableTrueAndPopularityLessThanEqual3();
		Assert.assertEquals(2, found.size());
		for (ProductBean bean : found) {
			Assert.assertTrue(bean.isAvailable());
			Assert.assertTrue(bean.getPopularity() <= 3);
		}
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
