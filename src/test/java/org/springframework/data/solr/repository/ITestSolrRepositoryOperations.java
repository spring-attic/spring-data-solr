///*
// * Copyright 2012-2020 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.springframework.data.solr.repository;
//
//import static org.assertj.core.api.Assertions.*;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//
//import org.joda.time.DateTime;
//import org.joda.time.DateTimeZone;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Slice;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.domain.Sort.Direction;
//import org.springframework.data.geo.Box;
//import org.springframework.data.geo.Distance;
//import org.springframework.data.geo.Point;
//import org.springframework.data.solr.core.query.SimpleField;
//import org.springframework.data.solr.core.query.SolrPageRequest;
//import org.springframework.data.solr.core.query.result.FacetAndHighlightPage;
//import org.springframework.data.solr.core.query.result.FacetFieldEntry;
//import org.springframework.data.solr.core.query.result.FacetPage;
//import org.springframework.data.solr.core.query.result.FacetQueryEntry;
//import org.springframework.data.solr.core.query.result.FieldStatsResult;
//import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;
//import org.springframework.data.solr.core.query.result.HighlightPage;
//import org.springframework.data.solr.core.query.result.SpellcheckedPage;
//import org.springframework.data.solr.core.query.result.StatsPage;
//import org.springframework.data.solr.repository.ProductBean.ContentType;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.util.StringUtils;
//
///**
// * @author Christoph Strobl
// * @author John Dorman
// * @author Francisco Spaeth
// * @author David Webb
// * @author Petar Tahchiev
// */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration
//public class ITestSolrRepositoryOperations {
//
//	private static final ProductBean POPULAR_AVAILABLE_PRODUCT = createProductBean("1", 5, true);
//	private static final ProductBean UNPOPULAR_AVAILABLE_PRODUCT = createProductBean("2", 1, true);
//	private static final ProductBean UNAVAILABLE_PRODUCT = createProductBean("3", 3, false);
//	private static final ProductBean NAMED_PRODUCT = createProductBean("4", 3, true, "product");
//
//	@Autowired private ProductRepository repo;
//
//	@Before
//	public void setUp() {
//		repo.deleteAll();
//		repo.saveAll(
//				Arrays.asList(POPULAR_AVAILABLE_PRODUCT, UNPOPULAR_AVAILABLE_PRODUCT, UNAVAILABLE_PRODUCT, NAMED_PRODUCT));
//	}
//
//	@After
//	public void tearDown() {
//		repo.deleteAll();
//	}
//
//	@Test
//	public void testFindOne() {
//		ProductBean found = repo.findById(POPULAR_AVAILABLE_PRODUCT.getId()).get();
//		assertThat(found.getId()).isEqualTo(POPULAR_AVAILABLE_PRODUCT.getId());
//	}
//
//	@Test
//	public void testFindOneThatDoesNotExist() {
//		assertThat(repo.findById(POPULAR_AVAILABLE_PRODUCT.getId().concat("XX-XX-XX")).isPresent()).isFalse();
//	}
//
//	@Test
//	public void testExists() {
//		assertThat(repo.existsById(POPULAR_AVAILABLE_PRODUCT.getId())).isTrue();
//	}
//
//	@Test
//	public void testExistsOneThatDoesNotExist() {
//		assertThat(repo.existsById(POPULAR_AVAILABLE_PRODUCT.getId().concat("XX-XX-XX"))).isFalse();
//	}
//
//	@Test
//	public void testCount() {
//		assertThat(repo.count()).isEqualTo(4);
//	}
//
//	@Test
//	public void testFindOneByCriteria() {
//		ProductBean found = repo.findByNameAndAvailableTrue(NAMED_PRODUCT.getName());
//		assertThat(found.getId()).isEqualTo(NAMED_PRODUCT.getId());
//	}
//
//	@Test
//	public void testFindByNamedQuery() {
//		List<ProductBean> found = repo.findByNamedQuery(5);
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(POPULAR_AVAILABLE_PRODUCT.getId());
//	}
//
//	@Test
//	public void testFindByIs() {
//		List<ProductBean> found = repo.findByName(NAMED_PRODUCT.getName());
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(NAMED_PRODUCT.getId());
//	}
//
//	@Test
//	public void testFindByIsNot() {
//		List<ProductBean> found = repo.findByNameNot(NAMED_PRODUCT.getName());
//		assertThat(found.size()).isEqualTo(3);
//	}
//
//	@Test
//	public void testFindByIsNull() {
//		ProductBean beanWithoutName = createProductBean("5", 3, true, "product");
//		beanWithoutName.setName(null);
//		repo.save(beanWithoutName);
//
//		List<ProductBean> found = repo.findByNameIsNull();
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(beanWithoutName.getId());
//	}
//
//	@Test
//	public void testFindByIsNotNull() {
//		ProductBean beanWithoutName = createProductBean("5", 3, true, "product");
//		beanWithoutName.setName(null);
//		repo.save(beanWithoutName);
//
//		List<ProductBean> found = repo.findByNameIsNotNull();
//		assertThat(found.size()).isEqualTo(4);
//		for (ProductBean foundBean : found) {
//			assertThat(beanWithoutName.getId().equals(foundBean.getId())).isFalse();
//		}
//	}
//
//	@Test
//	public void testFindSingleElementByIs() {
//		ProductBean product = repo.findProductBeanById(POPULAR_AVAILABLE_PRODUCT.getId());
//		assertThat(product).isNotNull();
//		assertThat(product.getId()).isEqualTo(POPULAR_AVAILABLE_PRODUCT.getId());
//	}
//
//	@Test
//	public void testFindByBooleanTrue() {
//		List<ProductBean> found = repo.findByAvailableTrue();
//		assertThat(found.size()).isEqualTo(3);
//	}
//
//	@Test
//	public void testFindByBooleanFalse() {
//		List<ProductBean> found = repo.findByAvailableFalse();
//		assertThat(found.size()).isEqualTo(1);
//	}
//
//	@Test
//	public void testFindByAvailableUsingQueryAnnotationTrue() {
//		List<ProductBean> found = repo.findByAvailableUsingQueryAnnotation(true);
//		assertThat(found.size()).isEqualTo(3);
//	}
//
//	@Test
//	public void testFindByBefore() {
//		repo.deleteAll();
//		ProductBean modifiedMid2012 = createProductBean("2012", 5, true);
//		modifiedMid2012.setLastModified(new DateTime(2012, 6, 1, 0, 0, 0, DateTimeZone.UTC).toDate());
//
//		ProductBean modifiedMid2011 = createProductBean("2011", 5, true);
//		modifiedMid2011.setLastModified(new DateTime(2011, 6, 1, 0, 0, 0, DateTimeZone.UTC).toDate());
//
//		repo.saveAll(Arrays.asList(modifiedMid2012, modifiedMid2011));
//		List<ProductBean> found = repo
//				.findByLastModifiedBefore(new DateTime(2011, 12, 31, 23, 59, 59, DateTimeZone.UTC).toDate());
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(modifiedMid2011.getId());
//	}
//
//	@Test
//	public void testFindByLessThan() {
//		List<ProductBean> found = repo.findByPopularityLessThan(3);
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(UNPOPULAR_AVAILABLE_PRODUCT.getId());
//	}
//
//	@Test
//	public void testFindByLessThanEqual() {
//		List<ProductBean> found = repo.findByPopularityLessThanEqual(3);
//		assertThat(found.size()).isEqualTo(3);
//	}
//
//	@Test
//	public void testFindByAfter() {
//		repo.deleteAll();
//		ProductBean modifiedMid2012 = createProductBean("2012", 5, true);
//		modifiedMid2012.setLastModified(new DateTime(2012, 6, 1, 0, 0, 0, DateTimeZone.UTC).toDate());
//
//		ProductBean modifiedMid2011 = createProductBean("2011", 5, true);
//		modifiedMid2011.setLastModified(new DateTime(2011, 6, 1, 0, 0, 0, DateTimeZone.UTC).toDate());
//
//		repo.saveAll(Arrays.asList(modifiedMid2012, modifiedMid2011));
//		List<ProductBean> found = repo
//				.findByLastModifiedAfter(new DateTime(2012, 1, 1, 0, 0, 0, DateTimeZone.UTC).toDate());
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(modifiedMid2012.getId());
//	}
//
//	@Test
//	public void testFindByGreaterThan() {
//		List<ProductBean> found = repo.findByPopularityGreaterThan(3);
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(POPULAR_AVAILABLE_PRODUCT.getId());
//	}
//
//	@Test
//	public void testFindByGreaterThanEqual() {
//		List<ProductBean> found = repo.findByPopularityGreaterThanEqual(3);
//		assertThat(found.size()).isEqualTo(3);
//	}
//
//	@Test
//	public void testFindByLike() {
//		List<ProductBean> found = repo.findByNameLike(NAMED_PRODUCT.getName().substring(0, 3));
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(NAMED_PRODUCT.getId());
//	}
//
//	@Test
//	public void testFindByStartsWith() {
//		List<ProductBean> found = repo.findByNameStartsWith(NAMED_PRODUCT.getName().substring(0, 3));
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(NAMED_PRODUCT.getId());
//	}
//
//	@Test
//	public void testFindByIn() {
//		List<ProductBean> found = repo.findByPopularityIn(Arrays.asList(3, 5));
//		assertThat(found.size()).isEqualTo(3);
//	}
//
//	@Test
//	public void testFindByNotIn() {
//		List<ProductBean> found = repo.findByPopularityNotIn(Arrays.asList(3, 5));
//		assertThat(found.size()).isEqualTo(1);
//	}
//
//	@Test
//	public void testFindConcatedByAnd() {
//		List<ProductBean> found = repo.findByPopularityAndAvailableTrue(POPULAR_AVAILABLE_PRODUCT.getPopularity());
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(POPULAR_AVAILABLE_PRODUCT.getId());
//	}
//
//	@Test
//	public void testFindConcatedByOr() {
//		List<ProductBean> found = repo.findByPopularityOrAvailableFalse(UNPOPULAR_AVAILABLE_PRODUCT.getPopularity());
//		assertThat(found.size()).isEqualTo(2);
//	}
//
//	@Test
//	public void testFindByWithin() {
//		ProductBean locatedInBuffalow = createProductBean("100", 5, true);
//		locatedInBuffalow.setLocation("45.17614,-93.87341");
//
//		ProductBean locatedInNYC = createProductBean("200", 5, true);
//		locatedInNYC.setLocation("40.7143,-74.006");
//
//		repo.saveAll(Arrays.asList(locatedInBuffalow, locatedInNYC));
//
//		List<ProductBean> found = repo.findByLocationWithin(new Point(45.15, -93.85), new Distance(5));
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(locatedInBuffalow.getId());
//	}
//
//	@Test
//	public void testFindByNear() {
//		ProductBean locatedInBuffalow = createProductBean("100", 5, true);
//		locatedInBuffalow.setLocation("45.17614,-93.87341");
//
//		ProductBean locatedInNYC = createProductBean("200", 5, true);
//		locatedInNYC.setLocation("40.7143,-74.006");
//
//		repo.saveAll(Arrays.asList(locatedInBuffalow, locatedInNYC));
//
//		List<ProductBean> found = repo.findByLocationNear(new Point(45.15, -93.85), new Distance(5));
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(locatedInBuffalow.getId());
//	}
//
//	@Test
//	public void testFindByNearWithBox() {
//		ProductBean locatedInBuffalow = createProductBean("100", 5, true);
//		locatedInBuffalow.setLocation("45.17614,-93.87341");
//
//		ProductBean locatedInNYC = createProductBean("200", 5, true);
//		locatedInNYC.setLocation("40.7143,-74.006");
//
//		repo.saveAll(Arrays.asList(locatedInBuffalow, locatedInNYC));
//
//		List<ProductBean> found = repo.findByLocationNear(new Box(new Point(45, -94), new Point(46, -93)));
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(locatedInBuffalow.getId());
//	}
//
//	@Test
//	public void testFindWithSortAsc() {
//		repo.deleteAll();
//
//		List<ProductBean> values = new ArrayList<>();
//		for (int i = 0; i < 10; i++) {
//			values.add(createProductBean(Integer.toString(i), i, true));
//		}
//		repo.saveAll(values);
//
//		List<ProductBean> found = repo.findByAvailableTrueOrderByPopularityAsc();
//
//		ProductBean prev = found.get(0);
//		for (int i = 1; i < found.size(); i++) {
//			ProductBean cur = found.get(i);
//			assertThat(Long.valueOf(cur.getPopularity()) > Long.valueOf(prev.getPopularity())).isTrue();
//			prev = cur;
//		}
//	}
//
//	@Test
//	public void testFindWithSortDesc() {
//		repo.deleteAll();
//
//		List<ProductBean> values = new ArrayList<>();
//		for (int i = 0; i < 10; i++) {
//			values.add(createProductBean(Integer.toString(i), i, true));
//		}
//		repo.saveAll(values);
//
//		List<ProductBean> found = repo.findByAvailableTrueOrderByPopularityDesc();
//
//		ProductBean prev = found.get(0);
//		for (int i = 1; i < found.size(); i++) {
//			ProductBean cur = found.get(i);
//			assertThat(Long.valueOf(cur.getPopularity()) < Long.valueOf(prev.getPopularity())).isTrue();
//			prev = cur;
//		}
//	}
//
//	@Test
//	public void testFindWithSortDescForAnnotatedQuery() {
//		repo.deleteAll();
//
//		List<ProductBean> values = new ArrayList<>();
//		for (int i = 0; i < 10; i++) {
//			values.add(createProductBean(Integer.toString(i), i, true));
//		}
//		repo.saveAll(values);
//
//		List<ProductBean> found = repo.findByAvailableWithAnnotatedQueryUsingSort(true,
//				Sort.by(Direction.DESC, "popularity"));
//
//		ProductBean prev = found.get(0);
//		for (int i = 1; i < found.size(); i++) {
//			ProductBean cur = found.get(i);
//			assertThat(Long.valueOf(cur.getPopularity()) < Long.valueOf(prev.getPopularity())).isTrue();
//			prev = cur;
//		}
//	}
//
//	@Test
//	public void testFindWithSortDescInPageableForAnnotatedQuery() {
//		repo.deleteAll();
//
//		List<ProductBean> values = new ArrayList<>();
//		for (int i = 0; i < 10; i++) {
//			values.add(createProductBean(Integer.toString(i), i, true));
//		}
//		repo.saveAll(values);
//
//		Page<ProductBean> found = repo.findByAvailableWithAnnotatedQueryUsingSortInPageable(true,
//				PageRequest.of(0, 50, Sort.by(Direction.DESC, "popularity")));
//
//		ProductBean prev = found.getContent().get(0);
//		for (int i = 1; i < found.getContent().size(); i++) {
//			ProductBean cur = found.getContent().get(i);
//			assertThat(Long.valueOf(cur.getPopularity()) < Long.valueOf(prev.getPopularity())).isTrue();
//			prev = cur;
//		}
//	}
//
//	@Test
//	public void testFindWithSortDescForNamedQuery() {
//		repo.deleteAll();
//
//		List<ProductBean> values = new ArrayList<>();
//		for (int i = 0; i < 10; i++) {
//			values.add(createProductBean(Integer.toString(i), i, true));
//		}
//		repo.saveAll(values);
//
//		List<ProductBean> found = repo.findByAvailableWithSort(true, Sort.by(Direction.DESC, "popularity"));
//
//		ProductBean prev = found.get(0);
//		for (int i = 1; i < found.size(); i++) {
//			ProductBean cur = found.get(i);
//			assertThat(Long.valueOf(cur.getPopularity()) < Long.valueOf(prev.getPopularity())).isTrue();
//			prev = cur;
//		}
//	}
//
//	@Test
//	public void testFindWithSortDescInPageableForNamedQuery() {
//		repo.deleteAll();
//
//		List<ProductBean> values = new ArrayList<>();
//		for (int i = 0; i < 10; i++) {
//			values.add(createProductBean(Integer.toString(i), i, true));
//		}
//		repo.saveAll(values);
//
//		Page<ProductBean> found = repo.findByAvailableWithSort(true,
//				PageRequest.of(0, 30, Sort.by(Direction.DESC, "popularity")));
//
//		ProductBean prev = found.getContent().get(0);
//		for (int i = 1; i < found.getContent().size(); i++) {
//			ProductBean cur = found.getContent().get(i);
//			assertThat(Long.valueOf(cur.getPopularity()) < Long.valueOf(prev.getPopularity())).isTrue();
//			prev = cur;
//		}
//	}
//
//	@Test
//	public void testFindByRegex() {
//		List<ProductBean> found = repo.findByNameRegex("na*");
//		assertThat(found.size()).isEqualTo(3);
//		for (ProductBean bean : found) {
//			assertThat(bean.getName().startsWith("na")).isTrue();
//		}
//	}
//
//	@Test
//	public void testPagination() {
//		Pageable pageable = PageRequest.of(0, 2);
//		Page<ProductBean> page1 = repo.findByNameStartingWith("name", pageable);
//		assertThat(page1.getNumberOfElements()).isEqualTo(pageable.getPageSize());
//		assertThat(page1.hasNext()).isTrue();
//		assertThat(page1.getTotalElements()).isEqualTo(3);
//
//		pageable = PageRequest.of(1, 2);
//		Page<ProductBean> page2 = repo.findByNameStartingWith("name", pageable);
//		assertThat(page2.getNumberOfElements()).isEqualTo(1);
//		assertThat(page2.hasNext()).isFalse();
//		assertThat(page2.getTotalElements()).isEqualTo(3);
//	}
//
//	@Test
//	public void testPaginationNoElementsFound() {
//		Pageable pageable = PageRequest.of(0, 2);
//		Page<ProductBean> page = repo.findByNameStartingWith("hpotsirhc", pageable);
//		assertThat(page.getNumberOfElements()).isEqualTo(0);
//		assertThat(page.getContent().isEmpty()).isTrue();
//	}
//
//	@Test
//	public void testProjectionOnFieldsForStringBasedQuery() {
//		List<ProductBean> found = repo.findByNameStartsWithProjectionOnNameAndId("name");
//		for (ProductBean bean : found) {
//			assertThat(bean.getName()).isNotNull();
//			assertThat(bean.getId()).isNotNull();
//
//			assertThat(bean.getPopularity()).isNull();
//		}
//	}
//
//	@Test
//	public void testProjectionOnFieldsForDerivedQuery() {
//		List<ProductBean> found = repo.findByNameStartingWith("name");
//		for (ProductBean bean : found) {
//			assertThat(bean.getName()).isNotNull();
//			assertThat(bean.getId()).isNotNull();
//
//			assertThat(bean.getPopularity()).isNull();
//		}
//	}
//
//	@Test
//	public void testFacetOnSingleField() {
//		FacetPage<ProductBean> facetPage = repo.findAllFacetOnPopularity(PageRequest.of(0, 10));
//		assertThat(facetPage.getFacetFields().size()).isEqualTo(1);
//		Page<FacetFieldEntry> page = facetPage.getFacetResultPage(facetPage.getFacetFields().iterator().next());
//		assertThat(page.getContent().size()).isEqualTo(3);
//		for (FacetFieldEntry entry : page) {
//			assertThat(entry.getField().getName()).isEqualTo("popularity");
//		}
//	}
//
//	@Test
//	public void testFacetOnMultipleFields() {
//		FacetPage<ProductBean> facetPage = repo.findAllFacetOnPopularityAndAvailable(PageRequest.of(0, 10));
//		assertThat(facetPage.getFacetFields().size()).isEqualTo(2);
//
//		Page<FacetFieldEntry> popularityPage = facetPage.getFacetResultPage(new SimpleField("popularity"));
//		assertThat(popularityPage.getContent().size()).isEqualTo(3);
//		for (FacetFieldEntry entry : popularityPage) {
//			assertThat(entry.getField().getName()).isEqualTo("popularity");
//		}
//
//		Page<FacetFieldEntry> availablePage = facetPage.getFacetResultPage(new SimpleField("inStock"));
//		assertThat(availablePage.getContent().size()).isEqualTo(2);
//		for (FacetFieldEntry entry : availablePage) {
//			assertThat(entry.getField().getName()).isEqualTo("inStock");
//		}
//	}
//
//	@Test
//	public void testFacetOnSingleQuery() {
//		FacetPage<ProductBean> facetPage = repo.findAllFacetQueryPopularity(PageRequest.of(0, 10));
//		assertThat(facetPage.getFacetFields().size()).isEqualTo(0);
//		Page<FacetQueryEntry> facets = facetPage.getFacetQueryResult();
//		assertThat(facets.getContent().size()).isEqualTo(1);
//		assertThat(facets.getContent().get(0).getValue()).isEqualTo("popularity:[* TO 3]");
//		assertThat(facets.getContent().get(0).getValueCount()).isEqualTo(3);
//	}
//
//	@Test
//	public void testFacetWithParametrizedQuery() {
//		FacetPage<ProductBean> facetPage = repo.findAllFacetQueryPopularity(3, PageRequest.of(0, 10));
//		assertThat(facetPage.getFacetFields().size()).isEqualTo(0);
//		Page<FacetQueryEntry> facets = facetPage.getFacetQueryResult();
//		assertThat(facets.getContent().size()).isEqualTo(1);
//		assertThat(facets.getContent().get(0).getValue()).isEqualTo("popularity:[* TO 3]");
//		assertThat(facets.getContent().get(0).getValueCount()).isEqualTo(3);
//	}
//
//	@Test
//	public void testFacetOnMulipleQueries() {
//		FacetPage<ProductBean> facetPage = repo.findAllFacetQueryAvailableTrueAndAvailableFalse(PageRequest.of(0, 10));
//		assertThat(facetPage.getFacetFields().size()).isEqualTo(0);
//		Page<FacetQueryEntry> facets = facetPage.getFacetQueryResult();
//		assertThat(facets.getContent().size()).isEqualTo(2);
//		assertThat(facets.getContent().get(0).getValue()).isEqualTo("inStock:true");
//		assertThat(facets.getContent().get(0).getValueCount()).isEqualTo(3);
//		assertThat(facets.getContent().get(1).getValue()).isEqualTo("inStock:false");
//		assertThat(facets.getContent().get(1).getValueCount()).isEqualTo(1);
//	}
//
//	@Test
//	public void testFacetWithStaticPrefix() {
//		FacetPage<ProductBean> facetPage = repo.findAllFacetOnNameWithStaticPrefix(PageRequest.of(0, 10));
//		assertThat(facetPage.getFacetFields().size()).isEqualTo(1);
//		Page<FacetFieldEntry> page = facetPage.getFacetResultPage("name");
//		assertThat(page.getContent().size()).isEqualTo(1);
//
//		assertThat(page.getContent().get(0).getField().getName()).isEqualTo("name");
//		assertThat(page.getContent().get(0).getValue()).isEqualTo("product");
//		assertThat(page.getContent().get(0).getValueCount()).isEqualTo(1);
//	}
//
//	@Test
//	public void testFacetWithDynamicPrefix() {
//		FacetPage<ProductBean> facetPage = repo.findAllFacetOnNameWithDynamicPrefix("pro", PageRequest.of(0, 10));
//		assertThat(facetPage.getFacetFields().size()).isEqualTo(1);
//		Page<FacetFieldEntry> page = facetPage.getFacetResultPage("name");
//		assertThat(page.getContent().size()).isEqualTo(1);
//
//		assertThat(page.getContent().get(0).getField().getName()).isEqualTo("name");
//		assertThat(page.getContent().get(0).getValue()).isEqualTo("product");
//		assertThat(page.getContent().get(0).getValueCount()).isEqualTo(1);
//	}
//
//	@Test // DATASOLR-244
//	public void testQueryWithFacetAndHighlight() {
//
//		FacetAndHighlightPage<ProductBean> page = repo.findByNameFacetOnNameHighlightAll("na", PageRequest.of(0, 10));
//		assertThat(page.getNumberOfElements()).isEqualTo(3);
//
//		assertThat(page.getFacetFields().size() > 0).isTrue();
//
//		for (ProductBean product : page) {
//			List<Highlight> highlights = page.getHighlights(product);
//			assertThat(highlights).isNotEmpty();
//			for (Highlight highlight : highlights) {
//				assertThat(highlight.getField().getName()).isEqualTo("name");
//				assertThat(highlight.getSnipplets()).isNotEmpty();
//				for (String s : highlight.getSnipplets()) {
//					assertThat(s.contains("<em>name</em>")).as("expected to find <em>name</em> but was \"" + s + "\"").isTrue();
//				}
//			}
//		}
//	}
//
//	@Test // DATASOLR-244
//	public void testFacetAndHighlightWithPrefixPostfix() {
//
//		FacetAndHighlightPage<ProductBean> page = repo.findByNameFacetOnInStockHighlightAllWithPreAndPostfix("na",
//				PageRequest.of(0, 10));
//		assertThat(page.getNumberOfElements()).isEqualTo(3);
//		assertThat(page.getFacetFields().size() > 0).isTrue();
//
//		for (ProductBean product : page) {
//			List<Highlight> highlights = page.getHighlights(product);
//			assertThat(highlights).isNotEmpty();
//			for (Highlight highlight : highlights) {
//				assertThat(highlight.getField().getName()).isEqualTo("name");
//				assertThat(highlight.getSnipplets()).isNotEmpty();
//				for (String s : highlight.getSnipplets()) {
//					assertThat(s.contains("<b>name</b>")).as("expected to find <b>name</b> but was \"" + s + "\"").isTrue();
//				}
//			}
//		}
//	}
//
//	@Test // DATASOLR-244
//	public void testFacetAndHighlightWithFields() {
//
//		ProductBean beanWithText = createProductBean("withName", 5, true);
//		beanWithText.setDescription("some text with name in it");
//		repo.save(beanWithText);
//
//		FacetAndHighlightPage<ProductBean> page = repo.findByNameFacetOnNameHighlightAllLimitToFields("na",
//				PageRequest.of(0, 10));
//		assertThat(page.getNumberOfElements()).isEqualTo(4);
//		assertThat(page.getFacetFields().size() > 0).isTrue();
//
//		for (ProductBean product : page) {
//			List<Highlight> highlights = page.getHighlights(product);
//			if (!product.getId().equals(beanWithText.getId())) {
//				assertThat(highlights).isEmpty();
//			} else {
//				assertThat(highlights).isNotEmpty();
//				for (Highlight highlight : highlights) {
//					assertThat(highlight.getField().getName()).isEqualTo("description");
//					assertThat(highlight.getSnipplets()).isNotEmpty();
//					for (String s : highlight.getSnipplets()) {
//						assertThat(s.contains("<em>name</em>")).as("expected to find <em>name</em> but was \"" + s + "\"").isTrue();
//					}
//				}
//			}
//		}
//	}
//
//	@Test // DATASOLR-244
//	public void testFacetAndHighlightWithFieldsAndFacetResult() {
//
//		ProductBean beanWithText = createProductBean("withName", 5, true);
//		beanWithText.setDescription("some text with name in it");
//		repo.save(beanWithText);
//
//		FacetAndHighlightPage<ProductBean> page = repo.findByNameFacetOnNameHighlightAllLimitToFields("*",
//				PageRequest.of(0, 10));
//		assertThat(page.getNumberOfElements()).isEqualTo(5);
//		assertThat(page.getFacetFields().size() > 0).isTrue();
//
//		for (ProductBean product : page) {
//			List<Highlight> highlights = page.getHighlights(product);
//			if (!product.getId().equals(beanWithText.getId())) {
//				assertThat(highlights).isEmpty();
//			} else {
//				assertThat(highlights).isNotEmpty();
//				for (Highlight highlight : highlights) {
//					assertThat(highlight.getField().getName()).isEqualTo("description");
//					assertThat(highlight.getSnipplets()).isNotEmpty();
//					for (String s : highlight.getSnipplets()) {
//						assertThat(s.contains("<em>name</em>")).as("expected to find <em>name</em> but was \"" + s + "\"").isTrue();
//					}
//				}
//			}
//		}
//
//		assertThat(page.getFacetResultPage("name").getContent().get(0).getKey().getName()).isEqualTo("name");
//		assertThat(page.getFacetResultPage("name").getContent().get(0).getValue()).isEqualTo("product");
//		assertThat(page.getFacetResultPage("name").getContent().get(0).getValueCount()).isEqualTo(1);
//	}
//
//	@Test // DATASOLR-244
//	public void testFacetAndHighlightWithQueryOverride() {
//
//		ProductBean beanWithText = createProductBean("withName", 5, true);
//		beanWithText.setDescription("some text with name in it");
//		repo.save(beanWithText);
//
//		FacetAndHighlightPage<ProductBean> page = repo.findByNameFacetOnStoreHighlightWihtQueryOverride("na", "some",
//				PageRequest.of(0, 10));
//		assertThat(page.getNumberOfElements()).isEqualTo(4);
//		assertThat(page.getFacetFields().size() > 0).isTrue();
//
//		for (ProductBean product : page) {
//			List<Highlight> highlights = page.getHighlights(product);
//			for (Highlight highlight : highlights) {
//				assertThat(highlight.getField().getName()).isEqualTo("description");
//				for (String s : highlight.getSnipplets()) {
//					assertThat(s.contains("<em>some</em>")).as("expected to find <em>some</em> but was \"" + s + "\"").isTrue();
//				}
//			}
//		}
//	}
//
//	@Test
//	public void testSingleFilter() {
//		List<ProductBean> found = repo.findAllFilterAvailableTrue();
//		assertThat(found.size()).isEqualTo(3);
//		for (ProductBean bean : found) {
//			assertThat(bean.isAvailable()).isTrue();
//		}
//	}
//
//	@Test
//	public void testParametrizedFilter() {
//		List<ProductBean> found = repo.findByPopularityLessThan(4, true);
//		assertThat(found.size()).isEqualTo(2);
//	}
//
//	@Test
//	public void testMultipleFilters() {
//		List<ProductBean> found = repo.findAllFilterAvailableTrueAndPopularityLessThanEqual3();
//		assertThat(found.size()).isEqualTo(2);
//		for (ProductBean bean : found) {
//			assertThat(bean.isAvailable()).isTrue();
//			assertThat(bean.getPopularity() <= 3).isTrue();
//		}
//	}
//
//	@Test
//	@Ignore("https://issues.apache.org/jira/browse/SOLR-12069")
//	public void testDefaultAndOperator() {
//		List<ProductBean> found = repo.findByAvailableIn(Collections.singletonList(Boolean.TRUE));
//		assertThat(found.size()).isEqualTo(3);
//
//		found = repo.findByAvailableIn(Arrays.asList(Boolean.TRUE, Boolean.FALSE));
//		assertThat(found.isEmpty()).isTrue();
//	}
//
//	@Test
//	@Ignore("https://issues.apache.org/jira/browse/SOLR-12069")
//	public void testDefaultOrOperator() {
//		List<ProductBean> found = repo.findByAvailableInWithOrOperator(Collections.singletonList(Boolean.TRUE));
//		assertThat(found.size()).isEqualTo(3);
//
//		found = repo.findByAvailableInWithOrOperator(Arrays.asList(Boolean.TRUE, Boolean.FALSE));
//		assertThat(found.size()).isEqualTo(4);
//	}
//
//	@Test
//	public void testTimeAllowed() {
//		List<ProductBean> found = repo.findAllWithExecutiontimeRestriction();
//		assertThat(found.size()).isEqualTo(4);
//	}
//
//	@Test
//	public void testWithBoost() {
//		repo.deleteAll();
//		ProductBean beanWithName = createProductBean("1", 5, true, "stackoverflow");
//		beanWithName.setTitle(Collections.singletonList("indexoutofbounds"));
//
//		ProductBean beanWithTitle = createProductBean("2", 5, true, "indexoutofbounds");
//		beanWithTitle.setTitle(Collections.singletonList("stackoverflow"));
//
//		repo.saveAll(Arrays.asList(beanWithName, beanWithTitle));
//
//		List<ProductBean> found = repo.findByNameStartsWithOrTitleStartsWith("indexoutofbounds", "indexoutofbounds");
//		assertThat(found.size()).isEqualTo(2);
//		assertThat(found.get(0).getId()).isEqualTo(beanWithTitle.getId());
//		assertThat(found.get(1).getId()).isEqualTo(beanWithName.getId());
//	}
//
//	@Test
//	public void testWithDefTypeLucene() {
//		ProductBean anotherProductBean = createProductBean("5", 3, true, "an other product");
//		repo.save(anotherProductBean);
//
//		List<ProductBean> found = repo.findByNameIn(Arrays.asList(NAMED_PRODUCT.getName(), anotherProductBean.getName()));
//		assertThat(found.size()).isEqualTo(2);
//
//		assertThat(found).contains(anotherProductBean, NAMED_PRODUCT);
//	}
//
//	@Test
//	public void testQueryWithRequestHandler() {
//		ProductBean availableBeanWithDescription = createProductBean("withDescriptionAvailable", 5, true);
//		availableBeanWithDescription.setDescription("some text with name in it");
//		repo.save(availableBeanWithDescription);
//
//		ProductBean unavailableBeanWithDescription = createProductBean("withDescriptionUnAvailable", 5, false);
//		unavailableBeanWithDescription.setDescription("some text with name in it");
//		repo.save(unavailableBeanWithDescription);
//
//		List<ProductBean> found = repo.findByDescription("some");
//
//		assertThat(found.size()).isEqualTo(1);
//		assertThat(found.get(0).getId()).isEqualTo(availableBeanWithDescription.getId());
//	}
//
//	@Test
//	public void testQueryWithHighlight() {
//		HighlightPage<ProductBean> page = repo.findByNameHighlightAll("na", PageRequest.of(0, 10));
//		assertThat(page.getNumberOfElements()).isEqualTo(3);
//
//		for (ProductBean product : page) {
//			List<Highlight> highlights = page.getHighlights(product);
//			assertThat(highlights).isNotEmpty();
//			for (Highlight highlight : highlights) {
//				assertThat(highlight.getField().getName()).isEqualTo("name");
//				assertThat(highlight.getSnipplets()).isNotEmpty();
//				for (String s : highlight.getSnipplets()) {
//					assertThat(s.contains("<em>name</em>")).as("expected to find <em>name</em> but was \"" + s + "\"").isTrue();
//				}
//			}
//		}
//	}
//
//	@Test
//	public void testHighlightWithPrefixPostfix() {
//		HighlightPage<ProductBean> page = repo.findByNameHighlightAllWithPreAndPostfix("na", PageRequest.of(0, 10));
//		assertThat(page.getNumberOfElements()).isEqualTo(3);
//
//		for (ProductBean product : page) {
//			List<Highlight> highlights = page.getHighlights(product);
//			assertThat(highlights).isNotEmpty();
//			for (Highlight highlight : highlights) {
//				assertThat(highlight.getField().getName()).isEqualTo("name");
//				assertThat(highlight.getSnipplets()).isNotEmpty();
//				for (String s : highlight.getSnipplets()) {
//					assertThat(s.contains("<b>name</b>")).as("expected to find <b>name</b> but was \"" + s + "\"").isTrue();
//				}
//			}
//		}
//	}
//
//	@Test
//	public void testHighlightWithFields() {
//		ProductBean beanWithText = createProductBean("withName", 5, true);
//		beanWithText.setDescription("some text with name in it");
//		repo.save(beanWithText);
//
//		HighlightPage<ProductBean> page = repo.findByNameHighlightAllLimitToFields("na", PageRequest.of(0, 10));
//		assertThat(page.getNumberOfElements()).isEqualTo(4);
//
//		for (ProductBean product : page) {
//			List<Highlight> highlights = page.getHighlights(product);
//			if (!product.getId().equals(beanWithText.getId())) {
//				assertThat(highlights).isEmpty();
//			} else {
//				assertThat(highlights).isNotEmpty();
//				for (Highlight highlight : highlights) {
//					assertThat(highlight.getField().getName()).isEqualTo("description");
//					assertThat(highlight.getSnipplets()).isNotEmpty();
//					for (String s : highlight.getSnipplets()) {
//						assertThat(s.contains("<em>name</em>")).as("expected to find <em>name</em> but was \"" + s + "\"").isTrue();
//					}
//				}
//			}
//		}
//	}
//
//	@Test
//	public void testHighlightWithQueryOverride() {
//		ProductBean beanWithText = createProductBean("withName", 5, true);
//		beanWithText.setDescription("some text with name in it");
//		repo.save(beanWithText);
//
//		HighlightPage<ProductBean> page = repo.findByNameHighlightWihtQueryOverride("na", "some", PageRequest.of(0, 10));
//		assertThat(page.getNumberOfElements()).isEqualTo(4);
//
//		for (ProductBean product : page) {
//			List<Highlight> highlights = page.getHighlights(product);
//			for (Highlight highlight : highlights) {
//				assertThat(highlight.getField().getName()).isEqualTo("description");
//				for (String s : highlight.getSnipplets()) {
//					assertThat(s.contains("<em>some</em>")).as("expected to find <em>some</em> but was \"" + s + "\"").isTrue();
//				}
//			}
//		}
//	}
//
//	@Test // DATASOLR-143
//	public void testCountByWorksCorrectly() {
//
//		assertThat(repo.countProductBeanByName(NAMED_PRODUCT.getName())).isEqualTo(1L);
//		assertThat(repo.countByName(NAMED_PRODUCT.getName())).isEqualTo(1L);
//	}
//
//	@Test // DATASOLR-144
//	public void testDereivedDeleteQueryRemovesDocumentsCorrectly() {
//
//		long referenceCount = repo.count();
//		repo.deleteByName(NAMED_PRODUCT.getName());
//		assertThat(repo.existsById(NAMED_PRODUCT.getId())).isEqualTo(false);
//		assertThat(repo.count()).isEqualTo(referenceCount - 1);
//	}
//
//	@Test // DATASOLR-144
//	public void testDerivedDeleteByQueryRemovesDocumentAndReturnsNumberDeletedCorrectly() {
//
//		long referenceCount = repo.count();
//		long nrDeleted = repo.deleteProductBeanByName(NAMED_PRODUCT.getName());
//		assertThat(repo.existsById(NAMED_PRODUCT.getId())).isEqualTo(false);
//		assertThat(repo.count()).isEqualTo(referenceCount - nrDeleted);
//	}
//
//	@Test // DATASOLR-144
//	public void testDerivedDeleteByQueryRemovesDocumentAndReturnsListOfDeletedDocumentsCorrectly() {
//
//		List<ProductBean> result = repo.removeByName(NAMED_PRODUCT.getName());
//		assertThat(repo.existsById(NAMED_PRODUCT.getId())).isEqualTo(false);
//		assertThat(result).hasSize(1);
//		assertThat(result.get(0).getId()).isEqualTo(NAMED_PRODUCT.getId());
//	}
//
//	@Test // DATASOLR-144
//	public void testAnnotatedDeleteByQueryRemovesDocumensCorrectly() {
//
//		long referenceCount = repo.count();
//		repo.removeUsingAnnotatedQuery(NAMED_PRODUCT.getName());
//		assertThat(repo.existsById(NAMED_PRODUCT.getId())).isEqualTo(false);
//		assertThat(repo.count()).isEqualTo(referenceCount - 1);
//	}
//
//	@Test // DATASOLR-170
//	public void findTopNResultAppliesLimitationCorrectly() {
//
//		List<ProductBean> result = repo.findTop2ByNameStartingWith("na");
//		assertThat(result).hasSize(2);
//	}
//
//	@Test // DATASOLR-170
//	public void findTopNResultAppliesLimitationForPageableCorrectly() {
//
//		List<ProductBean> beans = createProductBeans(10, "top");
//		repo.saveAll(beans);
//
//		Page<ProductBean> result = repo.findTop3ByNameStartsWith("to", PageRequest.of(0, 2));
//		assertThat(result.getNumberOfElements()).isEqualTo(2);
//		assertThat(result.getContent()).contains(beans.get(0), beans.get(1));
//	}
//
//	@Test // DATASOLR-170
//	public void findTopNResultAppliesLimitationForPageableCorrectlyForPage1() {
//
//		List<ProductBean> beans = createProductBeans(10, "top");
//		repo.saveAll(beans);
//
//		Page<ProductBean> result = repo.findTop3ByNameStartsWith("to", PageRequest.of(1, 2));
//		assertThat(result.getNumberOfElements()).isEqualTo(1);
//		assertThat(result.getContent()).contains(beans.get(2));
//	}
//
//	@Test // DATASOLR-170
//	public void findTopNResultReturnsEmptyListIfOusideOfRange() {
//
//		repo.saveAll(createProductBeans(10, "top"));
//
//		Page<ProductBean> result = repo.findTop3ByNameStartsWith("to", PageRequest.of(1, 5));
//		assertThat(result.getNumberOfElements()).isEqualTo(0);
//		assertThat(result.hasNext()).isEqualTo(false);
//	}
//
//	@Test // DATASOLR-186
//	public void sliceShouldReturnCorrectly() {
//
//		repo.saveAll(createProductBeans(10, "slice"));
//
//		Slice<ProductBean> slice = repo.findProductBeanByName("slice", PageRequest.of(0, 2));
//		assertThat(slice.getNumberOfElements()).isEqualTo(2);
//	}
//
//	@Test // DATASOLR-186
//	public void sliceShouldReturnAllElementsWhenPageableIsBigEnoughCorrectly() {
//
//		repo.saveAll(createProductBeans(10, "slice"));
//
//		Slice<ProductBean> slice = repo.findProductBeanByName("slice", PageRequest.of(0, 20));
//		assertThat(slice.getNumberOfElements()).isEqualTo(10);
//	}
//
//	@Test // DATASOLR-186
//	public void sliceShouldBeEmptyWhenPageableOutOfRange() {
//
//		repo.saveAll(createProductBeans(10, "slice"));
//
//		Slice<ProductBean> slice = repo.findProductBeanByName("slice", PageRequest.of(1, 20));
//		assertThat(slice.hasContent()).isEqualTo(false);
//	}
//
//	@Test // DATASOLR-160
//	public void testStatsAnnotatedMethod() {
//
//		ProductBean created = createProductBean("1", 1, true);
//		created.setPrice(1F);
//		created.setAvailable(true);
//		created.setLastModified(new Date());
//		created.setWeight(10F);
//
//		repo.save(created);
//
//		StatsPage<ProductBean> statsPage = repo.findAllWithStats(new SolrPageRequest(0, 0));
//
//		FieldStatsResult id = statsPage.getFieldStatsResult("id");
//		FieldStatsResult price = statsPage.getFieldStatsResult("price");
//		FieldStatsResult weight = statsPage.getFieldStatsResult("weight");
//
//		assertThat(id).isNotNull();
//		assertThat(price).isNotNull();
//		assertThat(price.getFacetStatsResult("id")).isNotNull();
//		assertThat(price.getFacetStatsResult("last_modified")).isNotNull();
//		assertThat(price.getFacetStatsResult("inStock")).isNull();
//		assertThat(id.getFacetStatsResult("id")).isNotNull();
//		assertThat(id.getFacetStatsResult("last_modified")).isNotNull();
//		assertThat(id.getFacetStatsResult("inStock")).isNull();
//
//		assertThat(weight).isNotNull();
//		assertThat(weight.getFacetStatsResult("inStock")).isNotNull();
//		assertThat(weight.getFacetStatsResult("last_modified")).isNull();
//		assertThat(weight.getFacetStatsResult("id")).isNull();
//	}
//
//	@Test // DATASOLR-137
//	public void testFindByNameWithSpellcheckSeggestion() {
//
//		ProductBean greenProduct = createProductBean("5", 3, true, "green");
//		repo.save(greenProduct);
//
//		SpellcheckedPage<ProductBean> found = repo.findByName("gren", PageRequest.of(0, 20));
//		assertThat(found.hasContent()).isEqualTo(false);
//		assertThat(found.getSuggestions().size()).isGreaterThan(0);
//		assertThat(found.getSuggestions()).containsExactly("green");
//	}
//
//	@Test // DATASOLR-375
//	public void derivedFinderUsingEnum() {
//
//		ProductBean html = createProductBean("5", 3, true, "html");
//		html.setContentType(ContentType.HTML);
//
//		ProductBean json = createProductBean("6", 3, true, "json");
//		json.setContentType(ContentType.JSON);
//
//		repo.saveAll(Arrays.asList(html, json));
//
//		List<ProductBean> result = repo.findByContentType(ContentType.HTML);
//		assertThat(result).hasSize(1);
//		assertThat(result).containsExactly(html);
//	}
//
//	@Test // DATASOLR-451
//	public void testFindByWithinAnd() {
//
//		ProductBean locatedInBuffalow = createProductBean("100", 5, true, "awesome");
//		locatedInBuffalow.setLocation("45.17614,-93.87341");
//
//		ProductBean locatedInNYC = createProductBean("200", 5, true, "super-awesome");
//		locatedInNYC.setLocation("40.7143,-74.006");
//
//		repo.saveAll(Arrays.asList(locatedInBuffalow, locatedInNYC));
//
//		List<ProductBean> found = repo.findByLocationWithinAndNameLike(new Point(45.15, -93.85), new Distance(5), "super");
//		assertThat(found.size()).isEqualTo(0);
//	}
//
//	@Test // DATASOLR-451
//	public void testFindByWithinAnd2() {
//
//		ProductBean locatedInBuffalow = createProductBean("100", 5, true, "awesome");
//		locatedInBuffalow.setLocation("45.17614,-93.87341");
//
//		ProductBean locatedInNYC = createProductBean("200", 5, true, "super-awesome");
//		locatedInNYC.setLocation("40.7143,-74.006");
//
//		repo.saveAll(Arrays.asList(locatedInBuffalow, locatedInNYC));
//
//		List<ProductBean> found = repo.findByNameLikeAndLocationWithin("awesome", new Point(45.15, -93.85),
//				new Distance(5));
//		assertThat(found.size()).isEqualTo(1);
//	}
//
//	@Test // DATASOLR-466
//	public void testOrderByConsidersMappedFieldName() {
//
//		List<ProductBean> result = repo.findByOrderByAvailableDesc();
//
//		assertThat(result.get(0).isAvailable()).isTrue();
//		assertThat(result.get(result.size() - 1).isAvailable()).isFalse();
//	}
//
//	private static List<ProductBean> createProductBeans(int nrToCreate, String prefix) {
//
//		List<ProductBean> beans = new ArrayList<>(nrToCreate);
//		for (int i = 0; i < nrToCreate; i++) {
//			String id = StringUtils.hasText(prefix) ? prefix + "-" + i : Integer.toString(i);
//			beans.add(createProductBean(id, 0, true, id));
//		}
//		return beans;
//	}
//
//	private static ProductBean createProductBean(String id, int popularity, boolean available) {
//		return createProductBean(id, popularity, available, "");
//	}
//
//	private static ProductBean createProductBean(String id, int popularity, boolean available, String name) {
//		ProductBean initial = new ProductBean();
//		initial.setId(id);
//		initial.setAvailable(available);
//		initial.setPopularity(popularity);
//		if (StringUtils.hasText(name)) {
//			initial.setName(name);
//		} else {
//			initial.setName("name-" + id);
//		}
//		return initial;
//	}
//}
