/*
 * Copyright 2012-2017 the original author or authors.
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

import static org.hamcrest.collection.IsCollectionWithSize.*;
import static org.hamcrest.collection.IsIterableContainingInOrder.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsCollectionContaining;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SolrPageRequest;
import org.springframework.data.solr.core.query.result.FacetAndHighlightPage;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.FacetQueryEntry;
import org.springframework.data.solr.core.query.result.FieldStatsResult;
import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.core.query.result.SpellcheckedPage;
import org.springframework.data.solr.core.query.result.StatsPage;
import org.springframework.data.solr.repository.ProductBean.ContentType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @author John Dorman
 * @author Francisco Spaeth
 * @author David Webb
 * @author Petar Tahchiev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ITestSolrRepositoryOperations {

	private static final ProductBean POPULAR_AVAILABLE_PRODUCT = createProductBean("1", 5, true);
	private static final ProductBean UNPOPULAR_AVAILABLE_PRODUCT = createProductBean("2", 1, true);
	private static final ProductBean UNAVAILABLE_PRODUCT = createProductBean("3", 3, false);
	private static final ProductBean NAMED_PRODUCT = createProductBean("4", 3, true, "product");

	@Autowired private ProductRepository repo;

	@Before
	public void setUp() {
		repo.deleteAll();
		repo.saveAll(
				Arrays.asList(POPULAR_AVAILABLE_PRODUCT, UNPOPULAR_AVAILABLE_PRODUCT, UNAVAILABLE_PRODUCT, NAMED_PRODUCT));
	}

	@After
	public void tearDown() {
		repo.deleteAll();
	}

	@Test
	public void testFindOne() {
		ProductBean found = repo.findById(POPULAR_AVAILABLE_PRODUCT.getId()).get();
		Assert.assertEquals(POPULAR_AVAILABLE_PRODUCT.getId(), found.getId());
	}

	@Test
	public void testFindOneThatDoesNotExist() {
		Assert.assertFalse(repo.findById(POPULAR_AVAILABLE_PRODUCT.getId().concat("XX-XX-XX")).isPresent());
	}

	@Test
	public void testExists() {
		Assert.assertTrue(repo.existsById(POPULAR_AVAILABLE_PRODUCT.getId()));
	}

	@Test
	public void testExistsOneThatDoesNotExist() {
		Assert.assertFalse(repo.existsById(POPULAR_AVAILABLE_PRODUCT.getId().concat("XX-XX-XX")));
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
	public void testFindByIsNull() {
		ProductBean beanWithoutName = createProductBean("5", 3, true, "product");
		beanWithoutName.setName(null);
		repo.save(beanWithoutName);

		List<ProductBean> found = repo.findByNameIsNull();
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(beanWithoutName.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByIsNotNull() {
		ProductBean beanWithoutName = createProductBean("5", 3, true, "product");
		beanWithoutName.setName(null);
		repo.save(beanWithoutName);

		List<ProductBean> found = repo.findByNameIsNotNull();
		Assert.assertEquals(4, found.size());
		for (ProductBean foundBean : found) {
			Assert.assertFalse(beanWithoutName.getId().equals(foundBean.getId()));
		}
	}

	@Test
	public void testFindSingleElementByIs() {
		ProductBean product = repo.findProductBeanById(POPULAR_AVAILABLE_PRODUCT.getId());
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

		repo.saveAll(Arrays.asList(modifiedMid2012, modifiedMid2011));
		List<ProductBean> found = repo
				.findByLastModifiedBefore(new DateTime(2011, 12, 31, 23, 59, 59, DateTimeZone.UTC).toDate());
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(modifiedMid2011.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByLessThan() {
		List<ProductBean> found = repo.findByPopularityLessThan(3);
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(UNPOPULAR_AVAILABLE_PRODUCT.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByLessThanEqual() {
		List<ProductBean> found = repo.findByPopularityLessThanEqual(3);
		Assert.assertEquals(3, found.size());
	}

	@Test
	public void testFindByAfter() {
		repo.deleteAll();
		ProductBean modifiedMid2012 = createProductBean("2012", 5, true);
		modifiedMid2012.setLastModified(new DateTime(2012, 6, 1, 0, 0, 0, DateTimeZone.UTC).toDate());

		ProductBean modifiedMid2011 = createProductBean("2011", 5, true);
		modifiedMid2011.setLastModified(new DateTime(2011, 6, 1, 0, 0, 0, DateTimeZone.UTC).toDate());

		repo.saveAll(Arrays.asList(modifiedMid2012, modifiedMid2011));
		List<ProductBean> found = repo
				.findByLastModifiedAfter(new DateTime(2012, 1, 1, 0, 0, 0, DateTimeZone.UTC).toDate());
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(modifiedMid2012.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByGreaterThan() {
		List<ProductBean> found = repo.findByPopularityGreaterThan(3);
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(POPULAR_AVAILABLE_PRODUCT.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByGreaterThanEqual() {
		List<ProductBean> found = repo.findByPopularityGreaterThanEqual(3);
		Assert.assertEquals(3, found.size());
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
	public void testFindByWithin() {
		ProductBean locatedInBuffalow = createProductBean("100", 5, true);
		locatedInBuffalow.setLocation("45.17614,-93.87341");

		ProductBean locatedInNYC = createProductBean("200", 5, true);
		locatedInNYC.setLocation("40.7143,-74.006");

		repo.saveAll(Arrays.asList(locatedInBuffalow, locatedInNYC));

		List<ProductBean> found = repo.findByLocationWithin(new Point(45.15, -93.85), new Distance(5));
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(locatedInBuffalow.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByNear() {
		ProductBean locatedInBuffalow = createProductBean("100", 5, true);
		locatedInBuffalow.setLocation("45.17614,-93.87341");

		ProductBean locatedInNYC = createProductBean("200", 5, true);
		locatedInNYC.setLocation("40.7143,-74.006");

		repo.saveAll(Arrays.asList(locatedInBuffalow, locatedInNYC));

		List<ProductBean> found = repo.findByLocationNear(new Point(45.15, -93.85), new Distance(5));
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(locatedInBuffalow.getId(), found.get(0).getId());
	}

	@Test
	public void testFindByNearWithBox() {
		ProductBean locatedInBuffalow = createProductBean("100", 5, true);
		locatedInBuffalow.setLocation("45.17614,-93.87341");

		ProductBean locatedInNYC = createProductBean("200", 5, true);
		locatedInNYC.setLocation("40.7143,-74.006");

		repo.saveAll(Arrays.asList(locatedInBuffalow, locatedInNYC));

		List<ProductBean> found = repo.findByLocationNear(new Box(new Point(45, -94), new Point(46, -93)));
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(locatedInBuffalow.getId(), found.get(0).getId());
	}

	@Test
	public void testFindWithSortAsc() {
		repo.deleteAll();

		List<ProductBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			values.add(createProductBean(Integer.toString(i), i, true));
		}
		repo.saveAll(values);

		List<ProductBean> found = repo.findByAvailableTrueOrderByPopularityAsc();

		ProductBean prev = found.get(0);
		for (int i = 1; i < found.size(); i++) {
			ProductBean cur = found.get(i);
			Assert.assertTrue(Long.valueOf(cur.getPopularity()) > Long.valueOf(prev.getPopularity()));
			prev = cur;
		}
	}

	@Test
	public void testFindWithSortDesc() {
		repo.deleteAll();

		List<ProductBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			values.add(createProductBean(Integer.toString(i), i, true));
		}
		repo.saveAll(values);

		List<ProductBean> found = repo.findByAvailableTrueOrderByPopularityDesc();

		ProductBean prev = found.get(0);
		for (int i = 1; i < found.size(); i++) {
			ProductBean cur = found.get(i);
			Assert.assertTrue(Long.valueOf(cur.getPopularity()) < Long.valueOf(prev.getPopularity()));
			prev = cur;
		}
	}

	@Test
	public void testFindWithSortDescForAnnotatedQuery() {
		repo.deleteAll();

		List<ProductBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			values.add(createProductBean(Integer.toString(i), i, true));
		}
		repo.saveAll(values);

		List<ProductBean> found = repo.findByAvailableWithAnnotatedQueryUsingSort(true,
				new Sort(Direction.DESC, "popularity"));

		ProductBean prev = found.get(0);
		for (int i = 1; i < found.size(); i++) {
			ProductBean cur = found.get(i);
			Assert.assertTrue(Long.valueOf(cur.getPopularity()) < Long.valueOf(prev.getPopularity()));
			prev = cur;
		}
	}

	@Test
	public void testFindWithSortDescInPageableForAnnotatedQuery() {
		repo.deleteAll();

		List<ProductBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			values.add(createProductBean(Integer.toString(i), i, true));
		}
		repo.saveAll(values);

		Page<ProductBean> found = repo.findByAvailableWithAnnotatedQueryUsingSortInPageable(true,
				new PageRequest(0, 50, new Sort(Direction.DESC, "popularity")));

		ProductBean prev = found.getContent().get(0);
		for (int i = 1; i < found.getContent().size(); i++) {
			ProductBean cur = found.getContent().get(i);
			Assert.assertTrue(Long.valueOf(cur.getPopularity()) < Long.valueOf(prev.getPopularity()));
			prev = cur;
		}
	}

	@Test
	public void testFindWithSortDescForNamedQuery() {
		repo.deleteAll();

		List<ProductBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			values.add(createProductBean(Integer.toString(i), i, true));
		}
		repo.saveAll(values);

		List<ProductBean> found = repo.findByAvailableWithSort(true, new Sort(Direction.DESC, "popularity"));

		ProductBean prev = found.get(0);
		for (int i = 1; i < found.size(); i++) {
			ProductBean cur = found.get(i);
			Assert.assertTrue(Long.valueOf(cur.getPopularity()) < Long.valueOf(prev.getPopularity()));
			prev = cur;
		}
	}

	@Test
	public void testFindWithSortDescInPageableForNamedQuery() {
		repo.deleteAll();

		List<ProductBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			values.add(createProductBean(Integer.toString(i), i, true));
		}
		repo.saveAll(values);

		Page<ProductBean> found = repo.findByAvailableWithSort(true,
				new PageRequest(0, 30, new Sort(Direction.DESC, "popularity")));

		ProductBean prev = found.getContent().get(0);
		for (int i = 1; i < found.getContent().size(); i++) {
			ProductBean cur = found.getContent().get(i);
			Assert.assertTrue(Long.valueOf(cur.getPopularity()) < Long.valueOf(prev.getPopularity()));
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
		Assert.assertTrue(page1.hasNext());
		Assert.assertEquals(3, page1.getTotalElements());

		pageable = new PageRequest(1, 2);
		Page<ProductBean> page2 = repo.findByNameStartingWith("name", pageable);
		Assert.assertEquals(1, page2.getNumberOfElements());
		Assert.assertFalse(page2.hasNext());
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
	public void testFacetOnSingleQuery() {
		FacetPage<ProductBean> facetPage = repo.findAllFacetQueryPopularity(new PageRequest(0, 10));
		Assert.assertEquals(0, facetPage.getFacetFields().size());
		Page<FacetQueryEntry> facets = facetPage.getFacetQueryResult();
		Assert.assertEquals(1, facets.getContent().size());
		Assert.assertEquals("popularity:[* TO 3]", facets.getContent().get(0).getValue());
		Assert.assertEquals(3, facets.getContent().get(0).getValueCount());
	}

	@Test
	public void testFacetWithParametrizedQuery() {
		FacetPage<ProductBean> facetPage = repo.findAllFacetQueryPopularity(3, new PageRequest(0, 10));
		Assert.assertEquals(0, facetPage.getFacetFields().size());
		Page<FacetQueryEntry> facets = facetPage.getFacetQueryResult();
		Assert.assertEquals(1, facets.getContent().size());
		Assert.assertEquals("popularity:[* TO 3]", facets.getContent().get(0).getValue());
		Assert.assertEquals(3, facets.getContent().get(0).getValueCount());
	}

	@Test
	public void testFacetOnMulipleQueries() {
		FacetPage<ProductBean> facetPage = repo.findAllFacetQueryAvailableTrueAndAvailableFalse(new PageRequest(0, 10));
		Assert.assertEquals(0, facetPage.getFacetFields().size());
		Page<FacetQueryEntry> facets = facetPage.getFacetQueryResult();
		Assert.assertEquals(2, facets.getContent().size());
		Assert.assertEquals("inStock:true", facets.getContent().get(0).getValue());
		Assert.assertEquals(3, facets.getContent().get(0).getValueCount());
		Assert.assertEquals("inStock:false", facets.getContent().get(1).getValue());
		Assert.assertEquals(1, facets.getContent().get(1).getValueCount());
	}

	@Test
	public void testFacetWithStaticPrefix() {
		FacetPage<ProductBean> facetPage = repo.findAllFacetOnNameWithStaticPrefix(new PageRequest(0, 10));
		Assert.assertEquals(1, facetPage.getFacetFields().size());
		Page<FacetFieldEntry> page = facetPage.getFacetResultPage("name");
		Assert.assertEquals(1, page.getContent().size());

		Assert.assertEquals("name", page.getContent().get(0).getField().getName());
		Assert.assertEquals("product", page.getContent().get(0).getValue());
		Assert.assertEquals(1, page.getContent().get(0).getValueCount());
	}

	@Test
	public void testFacetWithDynamicPrefix() {
		FacetPage<ProductBean> facetPage = repo.findAllFacetOnNameWithDynamicPrefix("pro", new PageRequest(0, 10));
		Assert.assertEquals(1, facetPage.getFacetFields().size());
		Page<FacetFieldEntry> page = facetPage.getFacetResultPage("name");
		Assert.assertEquals(1, page.getContent().size());

		Assert.assertEquals("name", page.getContent().get(0).getField().getName());
		Assert.assertEquals("product", page.getContent().get(0).getValue());
		Assert.assertEquals(1, page.getContent().get(0).getValueCount());
	}

	@Test // DATASOLR-244
	public void testQueryWithFacetAndHighlight() {

		FacetAndHighlightPage<ProductBean> page = repo.findByNameFacetOnNameHighlightAll("na", new PageRequest(0, 10));
		Assert.assertEquals(3, page.getNumberOfElements());

		Assert.assertTrue(page.getFacetFields().size() > 0);

		for (ProductBean product : page) {
			List<Highlight> highlights = page.getHighlights(product);
			assertThat(highlights, IsNot.not(IsEmptyCollection.empty()));
			for (Highlight highlight : highlights) {
				Assert.assertEquals("name", highlight.getField().getName());
				assertThat(highlight.getSnipplets(), IsNot.not(IsEmptyCollection.empty()));
				for (String s : highlight.getSnipplets()) {
					Assert.assertTrue("expected to find <em>name</em> but was \"" + s + "\"", s.contains("<em>name</em>"));
				}
			}
		}
	}

	@Test // DATASOLR-244
	public void testFacetAndHighlightWithPrefixPostfix() {

		FacetAndHighlightPage<ProductBean> page = repo.findByNameFacetOnInStockHighlightAllWithPreAndPostfix("na",
				new PageRequest(0, 10));
		Assert.assertEquals(3, page.getNumberOfElements());
		Assert.assertTrue(page.getFacetFields().size() > 0);

		for (ProductBean product : page) {
			List<Highlight> highlights = page.getHighlights(product);
			assertThat(highlights, IsNot.not(IsEmptyCollection.empty()));
			for (Highlight highlight : highlights) {
				Assert.assertEquals("name", highlight.getField().getName());
				assertThat(highlight.getSnipplets(), IsNot.not(IsEmptyCollection.empty()));
				for (String s : highlight.getSnipplets()) {
					Assert.assertTrue("expected to find <b>name</b> but was \"" + s + "\"", s.contains("<b>name</b>"));
				}
			}
		}
	}

	@Test // DATASOLR-244
	public void testFacetAndHighlightWithFields() {

		ProductBean beanWithText = createProductBean("withName", 5, true);
		beanWithText.setDescription("some text with name in it");
		repo.save(beanWithText);

		FacetAndHighlightPage<ProductBean> page = repo.findByNameFacetOnNameHighlightAllLimitToFields("na",
				new PageRequest(0, 10));
		Assert.assertEquals(4, page.getNumberOfElements());
		Assert.assertTrue(page.getFacetFields().size() > 0);

		for (ProductBean product : page) {
			List<Highlight> highlights = page.getHighlights(product);
			if (!product.getId().equals(beanWithText.getId())) {
				assertThat(highlights, IsEmptyCollection.empty());
			} else {
				assertThat(highlights, IsNot.not(IsEmptyCollection.empty()));
				for (Highlight highlight : highlights) {
					Assert.assertEquals("description", highlight.getField().getName());
					assertThat(highlight.getSnipplets(), IsNot.not(IsEmptyCollection.empty()));
					for (String s : highlight.getSnipplets()) {
						Assert.assertTrue("expected to find <em>name</em> but was \"" + s + "\"", s.contains("<em>name</em>"));
					}
				}
			}
		}
	}

	@Test // DATASOLR-244
	public void testFacetAndHighlightWithFieldsAndFacetResult() {

		ProductBean beanWithText = createProductBean("withName", 5, true);
		beanWithText.setDescription("some text with name in it");
		repo.save(beanWithText);

		FacetAndHighlightPage<ProductBean> page = repo.findByNameFacetOnNameHighlightAllLimitToFields("",
				new PageRequest(0, 10));
		Assert.assertEquals(5, page.getNumberOfElements());
		Assert.assertTrue(page.getFacetFields().size() > 0);

		for (ProductBean product : page) {
			List<Highlight> highlights = page.getHighlights(product);
			if (!product.getId().equals(beanWithText.getId())) {
				assertThat(highlights, IsEmptyCollection.empty());
			} else {
				assertThat(highlights, IsNot.not(IsEmptyCollection.empty()));
				for (Highlight highlight : highlights) {
					Assert.assertEquals("description", highlight.getField().getName());
					assertThat(highlight.getSnipplets(), IsNot.not(IsEmptyCollection.empty()));
					for (String s : highlight.getSnipplets()) {
						Assert.assertTrue("expected to find <em>name</em> but was \"" + s + "\"", s.contains("<em>name</em>"));
					}
				}
			}
		}

		Assert.assertEquals("name", page.getFacetResultPage("name").getContent().get(0).getKey().getName());
		Assert.assertEquals("product", page.getFacetResultPage("name").getContent().get(0).getValue());
		Assert.assertEquals(1, page.getFacetResultPage("name").getContent().get(0).getValueCount());
	}

	@Test // DATASOLR-244
	public void testFacetAndHighlightWithQueryOverride() {

		ProductBean beanWithText = createProductBean("withName", 5, true);
		beanWithText.setDescription("some text with name in it");
		repo.save(beanWithText);

		FacetAndHighlightPage<ProductBean> page = repo.findByNameFacetOnStoreHighlightWihtQueryOverride("na", "some",
				new PageRequest(0, 10));
		Assert.assertEquals(4, page.getNumberOfElements());
		Assert.assertTrue(page.getFacetFields().size() > 0);

		for (ProductBean product : page) {
			List<Highlight> highlights = page.getHighlights(product);
			for (Highlight highlight : highlights) {
				Assert.assertEquals("description", highlight.getField().getName());
				for (String s : highlight.getSnipplets()) {
					Assert.assertTrue("expected to find <em>some</em> but was \"" + s + "\"", s.contains("<em>some</em>"));
				}
			}
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
	public void testParametrizedFilter() {
		List<ProductBean> found = repo.findByPopularityLessThan(4, true);
		Assert.assertEquals(2, found.size());
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

	@Test
	@Ignore("https://issues.apache.org/jira/browse/SOLR-12069")
	public void testDefaultAndOperator() {
		List<ProductBean> found = repo.findByAvailableIn(Collections.singletonList(Boolean.TRUE));
		Assert.assertEquals(3, found.size());

		found = repo.findByAvailableIn(Arrays.asList(Boolean.TRUE, Boolean.FALSE));
		Assert.assertTrue(found.isEmpty());
	}

	@Test
	@Ignore("https://issues.apache.org/jira/browse/SOLR-12069")
	public void testDefaultOrOperator() {
		List<ProductBean> found = repo.findByAvailableInWithOrOperator(Collections.singletonList(Boolean.TRUE));
		Assert.assertEquals(3, found.size());

		found = repo.findByAvailableInWithOrOperator(Arrays.asList(Boolean.TRUE, Boolean.FALSE));
		Assert.assertEquals(4, found.size());
	}

	@Test
	public void testTimeAllowed() {
		List<ProductBean> found = repo.findAllWithExecutiontimeRestriction();
		Assert.assertEquals(4, found.size());
	}

	@Test
	public void testWithBoost() {
		repo.deleteAll();
		ProductBean beanWithName = createProductBean("1", 5, true, "stackoverflow");
		beanWithName.setTitle(Collections.singletonList("indexoutofbounds"));

		ProductBean beanWithTitle = createProductBean("2", 5, true, "indexoutofbounds");
		beanWithTitle.setTitle(Collections.singletonList("stackoverflow"));

		repo.saveAll(Arrays.asList(beanWithName, beanWithTitle));

		List<ProductBean> found = repo.findByNameStartsWithOrTitleStartsWith("indexoutofbounds", "indexoutofbounds");
		Assert.assertEquals(2, found.size());
		Assert.assertEquals(beanWithTitle.getId(), found.get(0).getId());
		Assert.assertEquals(beanWithName.getId(), found.get(1).getId());
	}

	@Test
	public void testWithDefTypeLucene() {
		ProductBean anotherProductBean = createProductBean("5", 3, true, "an other product");
		repo.save(anotherProductBean);

		List<ProductBean> found = repo.findByNameIn(Arrays.asList(NAMED_PRODUCT.getName(), anotherProductBean.getName()));
		Assert.assertEquals(2, found.size());

		assertThat(found, Matchers.containsInAnyOrder(anotherProductBean, NAMED_PRODUCT));
	}

	@Test
	public void testQueryWithRequestHandler() {
		ProductBean availableBeanWithDescription = createProductBean("withDescriptionAvailable", 5, true);
		availableBeanWithDescription.setDescription("some text with name in it");
		repo.save(availableBeanWithDescription);

		ProductBean unavailableBeanWithDescription = createProductBean("withDescriptionUnAvailable", 5, false);
		unavailableBeanWithDescription.setDescription("some text with name in it");
		repo.save(unavailableBeanWithDescription);

		List<ProductBean> found = repo.findByDescription("some");

		Assert.assertEquals(1, found.size());
		Assert.assertEquals(availableBeanWithDescription.getId(), found.get(0).getId());
	}

	@Test
	public void testQueryWithHighlight() {
		HighlightPage<ProductBean> page = repo.findByNameHighlightAll("na", new PageRequest(0, 10));
		Assert.assertEquals(3, page.getNumberOfElements());

		for (ProductBean product : page) {
			List<Highlight> highlights = page.getHighlights(product);
			assertThat(highlights, IsNot.not(IsEmptyCollection.empty()));
			for (Highlight highlight : highlights) {
				Assert.assertEquals("name", highlight.getField().getName());
				assertThat(highlight.getSnipplets(), IsNot.not(IsEmptyCollection.empty()));
				for (String s : highlight.getSnipplets()) {
					Assert.assertTrue("expected to find <em>name</em> but was \"" + s + "\"", s.contains("<em>name</em>"));
				}
			}
		}
	}

	@Test
	public void testHighlightWithPrefixPostfix() {
		HighlightPage<ProductBean> page = repo.findByNameHighlightAllWithPreAndPostfix("na", new PageRequest(0, 10));
		Assert.assertEquals(3, page.getNumberOfElements());

		for (ProductBean product : page) {
			List<Highlight> highlights = page.getHighlights(product);
			assertThat(highlights, IsNot.not(IsEmptyCollection.empty()));
			for (Highlight highlight : highlights) {
				Assert.assertEquals("name", highlight.getField().getName());
				assertThat(highlight.getSnipplets(), IsNot.not(IsEmptyCollection.empty()));
				for (String s : highlight.getSnipplets()) {
					Assert.assertTrue("expected to find <b>name</b> but was \"" + s + "\"", s.contains("<b>name</b>"));
				}
			}
		}
	}

	@Test
	public void testHighlightWithFields() {
		ProductBean beanWithText = createProductBean("withName", 5, true);
		beanWithText.setDescription("some text with name in it");
		repo.save(beanWithText);

		HighlightPage<ProductBean> page = repo.findByNameHighlightAllLimitToFields("na", new PageRequest(0, 10));
		Assert.assertEquals(4, page.getNumberOfElements());

		for (ProductBean product : page) {
			List<Highlight> highlights = page.getHighlights(product);
			if (!product.getId().equals(beanWithText.getId())) {
				assertThat(highlights, IsEmptyCollection.empty());
			} else {
				assertThat(highlights, IsNot.not(IsEmptyCollection.empty()));
				for (Highlight highlight : highlights) {
					Assert.assertEquals("description", highlight.getField().getName());
					assertThat(highlight.getSnipplets(), IsNot.not(IsEmptyCollection.empty()));
					for (String s : highlight.getSnipplets()) {
						Assert.assertTrue("expected to find <em>name</em> but was \"" + s + "\"", s.contains("<em>name</em>"));
					}
				}
			}
		}
	}

	@Test
	public void testHighlightWithQueryOverride() {
		ProductBean beanWithText = createProductBean("withName", 5, true);
		beanWithText.setDescription("some text with name in it");
		repo.save(beanWithText);

		HighlightPage<ProductBean> page = repo.findByNameHighlightWihtQueryOverride("na", "some", new PageRequest(0, 10));
		Assert.assertEquals(4, page.getNumberOfElements());

		for (ProductBean product : page) {
			List<Highlight> highlights = page.getHighlights(product);
			for (Highlight highlight : highlights) {
				Assert.assertEquals("description", highlight.getField().getName());
				for (String s : highlight.getSnipplets()) {
					Assert.assertTrue("expected to find <em>some</em> but was \"" + s + "\"", s.contains("<em>some</em>"));
				}
			}
		}
	}

	@Test // DATASOLR-143
	public void testCountByWorksCorrectly() {

		assertThat(repo.countProductBeanByName(NAMED_PRODUCT.getName()), Is.is(1L));
		assertThat(repo.countByName(NAMED_PRODUCT.getName()), Is.is(1L));
	}

	@Test // DATASOLR-144
	public void testDereivedDeleteQueryRemovesDocumentsCorrectly() {

		long referenceCount = repo.count();
		repo.deleteByName(NAMED_PRODUCT.getName());
		assertThat(repo.existsById(NAMED_PRODUCT.getId()), Is.is(false));
		assertThat(repo.count(), Is.is(referenceCount - 1));
	}

	@Test // DATASOLR-144
	public void testDerivedDeleteByQueryRemovesDocumentAndReturnsNumberDeletedCorrectly() {

		long referenceCount = repo.count();
		long nrDeleted = repo.deleteProductBeanByName(NAMED_PRODUCT.getName());
		assertThat(repo.existsById(NAMED_PRODUCT.getId()), Is.is(false));
		assertThat(repo.count(), Is.is(referenceCount - nrDeleted));
	}

	@Test // DATASOLR-144
	public void testDerivedDeleteByQueryRemovesDocumentAndReturnsListOfDeletedDocumentsCorrectly() {

		List<ProductBean> result = repo.removeByName(NAMED_PRODUCT.getName());
		assertThat(repo.existsById(NAMED_PRODUCT.getId()), Is.is(false));
		assertThat(result, hasSize(1));
		assertThat(result.get(0).getId(), IsEqual.equalTo(NAMED_PRODUCT.getId()));
	}

	@Test // DATASOLR-144
	public void testAnnotatedDeleteByQueryRemovesDocumensCorrectly() {

		long referenceCount = repo.count();
		repo.removeUsingAnnotatedQuery(NAMED_PRODUCT.getName());
		assertThat(repo.existsById(NAMED_PRODUCT.getId()), Is.is(false));
		assertThat(repo.count(), Is.is(referenceCount - 1));
	}

	@Test // DATASOLR-170
	public void findTopNResultAppliesLimitationCorrectly() {

		List<ProductBean> result = repo.findTop2ByNameStartingWith("na");
		assertThat(result, hasSize(2));
	}

	@Test // DATASOLR-170
	public void findTopNResultAppliesLimitationForPageableCorrectly() {

		List<ProductBean> beans = createProductBeans(10, "top");
		repo.saveAll(beans);

		Page<ProductBean> result = repo.findTop3ByNameStartsWith("to", new PageRequest(0, 2));
		assertThat(result.getNumberOfElements(), IsEqual.equalTo(2));
		assertThat(result.getContent(), IsCollectionContaining.hasItems(beans.get(0), beans.get(1)));
	}

	@Test // DATASOLR-170
	public void findTopNResultAppliesLimitationForPageableCorrectlyForPage1() {

		List<ProductBean> beans = createProductBeans(10, "top");
		repo.saveAll(beans);

		Page<ProductBean> result = repo.findTop3ByNameStartsWith("to", new PageRequest(1, 2));
		assertThat(result.getNumberOfElements(), IsEqual.equalTo(1));
		assertThat(result.getContent(), IsCollectionContaining.hasItems(beans.get(2)));
	}

	@Test // DATASOLR-170
	public void findTopNResultReturnsEmptyListIfOusideOfRange() {

		repo.saveAll(createProductBeans(10, "top"));

		Page<ProductBean> result = repo.findTop3ByNameStartsWith("to", new PageRequest(1, 5));
		assertThat(result.getNumberOfElements(), IsEqual.equalTo(0));
		assertThat(result.hasNext(), IsEqual.equalTo(false));
	}

	@Test // DATASOLR-186
	public void sliceShouldReturnCorrectly() {

		repo.saveAll(createProductBeans(10, "slice"));

		Slice<ProductBean> slice = repo.findProductBeanByName("slice", new PageRequest(0, 2));
		assertThat(slice.getNumberOfElements(), Is.is(2));
	}

	@Test // DATASOLR-186
	public void sliceShouldReturnAllElementsWhenPageableIsBigEnoughCorrectly() {

		repo.saveAll(createProductBeans(10, "slice"));

		Slice<ProductBean> slice = repo.findProductBeanByName("slice", new PageRequest(0, 20));
		assertThat(slice.getNumberOfElements(), Is.is(10));
	}

	@Test // DATASOLR-186
	public void sliceShouldBeEmptyWhenPageableOutOfRange() {

		repo.saveAll(createProductBeans(10, "slice"));

		Slice<ProductBean> slice = repo.findProductBeanByName("slice", new PageRequest(1, 20));
		assertThat(slice.hasContent(), Is.is(false));
	}

	@Test // DATASOLR-160
	public void testStatsAnnotatedMethod() {

		ProductBean created = createProductBean("1", 1, true);
		created.setPrice(1F);
		created.setAvailable(true);
		created.setLastModified(new Date());
		created.setWeight(10F);

		repo.save(created);

		StatsPage<ProductBean> statsPage = repo.findAllWithStats(new SolrPageRequest(0, 0));

		FieldStatsResult id = statsPage.getFieldStatsResult("id");
		FieldStatsResult price = statsPage.getFieldStatsResult("price");
		FieldStatsResult weight = statsPage.getFieldStatsResult("weight");

		Assert.assertNotNull(id);
		Assert.assertNotNull(price);
		Assert.assertNotNull(price.getFacetStatsResult("id"));
		Assert.assertNotNull(price.getFacetStatsResult("last_modified"));
		Assert.assertNull(price.getFacetStatsResult("inStock"));
		Assert.assertNotNull(id.getFacetStatsResult("id"));
		Assert.assertNotNull(id.getFacetStatsResult("last_modified"));
		Assert.assertNull(id.getFacetStatsResult("inStock"));

		Assert.assertNotNull(weight);
		Assert.assertNotNull(weight.getFacetStatsResult("inStock"));
		Assert.assertNull(weight.getFacetStatsResult("last_modified"));
		Assert.assertNull(weight.getFacetStatsResult("id"));
	}

	@Test // DATASOLR-137
	public void testFindByNameWithSpellcheckSeggestion() {

		ProductBean greenProduct = createProductBean("5", 3, true, "green");
		repo.save(greenProduct);

		SpellcheckedPage<ProductBean> found = repo.findByName("gren", new PageRequest(0, 20));
		assertThat(found.hasContent(), Is.is(false));
		assertThat(found.getSuggestions().size(), Is.is(Matchers.greaterThan(0)));
		assertThat(found.getSuggestions(), Matchers.contains("green"));
	}

	@Test // DATASOLR-375
	public void derivedFinderUsingEnum() {

		ProductBean html = createProductBean("5", 3, true, "html");
		html.setContentType(ContentType.HTML);

		ProductBean json = createProductBean("6", 3, true, "json");
		json.setContentType(ContentType.JSON);

		repo.saveAll(Arrays.asList(html, json));

		List<ProductBean> result = repo.findByContentType(ContentType.HTML);
		assertThat(result, hasSize(1));
		assertThat(result, contains(html));
	}

	@Test // DATASOLR-451
	public void testFindByWithinAnd() {

		ProductBean locatedInBuffalow = createProductBean("100", 5, true, "awesome");
		locatedInBuffalow.setLocation("45.17614,-93.87341");

		ProductBean locatedInNYC = createProductBean("200", 5, true, "super-awesome");
		locatedInNYC.setLocation("40.7143,-74.006");

		repo.saveAll(Arrays.asList(locatedInBuffalow, locatedInNYC));

		List<ProductBean> found = repo.findByLocationWithinAndNameLike(new Point(45.15, -93.85), new Distance(5), "super");
		Assert.assertEquals(0, found.size());
	}

	@Test // DATASOLR-451
	public void testFindByWithinAnd2() {

		ProductBean locatedInBuffalow = createProductBean("100", 5, true, "awesome");
		locatedInBuffalow.setLocation("45.17614,-93.87341");

		ProductBean locatedInNYC = createProductBean("200", 5, true, "super-awesome");
		locatedInNYC.setLocation("40.7143,-74.006");

		repo.saveAll(Arrays.asList(locatedInBuffalow, locatedInNYC));

		List<ProductBean> found = repo.findByNameLikeAndLocationWithin("awesome", new Point(45.15, -93.85), new Distance(5));
		Assert.assertEquals(1, found.size());
	}

	private static List<ProductBean> createProductBeans(int nrToCreate, String prefix) {

		List<ProductBean> beans = new ArrayList<>(nrToCreate);
		for (int i = 0; i < nrToCreate; i++) {
			String id = StringUtils.hasText(prefix) ? (prefix + "-" + i) : Integer.toString(i);
			beans.add(createProductBean(id, 0, true, id));
		}
		return beans;
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
