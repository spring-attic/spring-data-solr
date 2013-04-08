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
package org.springframework.data.solr.repository.query;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.repository.Facet;
import org.springframework.data.solr.repository.ProductBean;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.support.SolrEntityInformationCreatorImpl;

/**
 * @author Christoph Strobl
 * @author Andrey Paramonov
 */
public class SolrQueryMethodTests {

	SolrEntityInformationCreator creator;

	@Before
	public void setUp() {
		creator = new SolrEntityInformationCreatorImpl(new SimpleSolrMappingContext());
	}

	@Test
	public void testAnnotatedQueryUsage() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQuery", String.class);
		Assert.assertTrue(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertFalse(method.hasFilterQuery());
		Assert.assertEquals("name:?0", method.getAnnotatedQuery());
	}

	@Test
	public void testAnnotatedQueryUsageWithoutExplicitAttribute() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQueryWithoutExplicitAttribute", String.class);
		Assert.assertTrue(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFilterQuery());
		Assert.assertEquals("name:?0", method.getAnnotatedQuery());
	}

	@Test
	public void testAnnotatedNamedQueryNameUsage() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedNamedQueryName", String.class);
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertTrue(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFilterQuery());
		Assert.assertEquals("ProductRepository.namedQuery-1", method.getAnnotatedNamedQueryName());
	}

	@Test
	public void testWithoutAnnotation() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByName", String.class);
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasFilterQuery());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
	}

	@Test
	public void testWithSingleFieldProjection() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQueryWithProjectionOnSingleField", String.class);
		Assert.assertTrue(method.hasAnnotatedQuery());
		Assert.assertTrue(method.hasProjectionFields());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFilterQuery());
		Assert.assertEquals("name:?0", method.getAnnotatedQuery());
	}

	@Test
	public void testWithMultipleFieldsProjection() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQueryWithProjectionOnMultipleFields", String.class);
		Assert.assertTrue(method.hasAnnotatedQuery());
		Assert.assertTrue(method.hasProjectionFields());
		Assert.assertFalse(method.hasFilterQuery());
		Assert.assertEquals(2, method.getProjectionFields().size());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertEquals("name:?0", method.getAnnotatedQuery());
	}

	@Test
	public void testWithSingleFieldFacet() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularity", String.class);
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertTrue(method.hasFacetFields());
		Assert.assertFalse(method.hasFacetQueries());
		Assert.assertFalse(method.hasFilterQuery());

		Assert.assertEquals(1, method.getFacetFields().size());
		Assert.assertEquals(Integer.valueOf(10), method.getFacetLimit());
		Assert.assertEquals(Integer.valueOf(1), method.getFacetMinCount());
	}

	@Test
	public void testWithMultipleFieldFacets() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularityAndPrice", String.class);
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertTrue(method.hasFacetFields());
		Assert.assertFalse(method.hasFacetQueries());
		Assert.assertEquals(2, method.getFacetFields().size());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFilterQuery());
	}

	@Test
	public void testWithMultipleFieldFacetsLimitAndMinCount() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularityAndPriceMinCount3Limit25", String.class);
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertTrue(method.hasFacetFields());
		Assert.assertFalse(method.hasFacetQueries());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFilterQuery());

		Assert.assertEquals(2, method.getFacetFields().size());
		Assert.assertEquals(Integer.valueOf(25), method.getFacetLimit());
		Assert.assertEquals(Integer.valueOf(3), method.getFacetMinCount());
	}

	@Test
	public void testWithSingleQueryFacet() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularityQuery", String.class);
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFacetFields());
		Assert.assertTrue(method.hasFacetQueries());
		Assert.assertFalse(method.hasFilterQuery());

		Assert.assertEquals(0, method.getFacetFields().size());
		Assert.assertEquals(1, method.getFacetQueries().size());
		Assert.assertEquals(Integer.valueOf(10), method.getFacetLimit());
		Assert.assertEquals(Integer.valueOf(1), method.getFacetMinCount());
		Assert.assertEquals("popularity:[* TO 5]", method.getFacetQueries().get(0));
	}

	@Test
	public void testWithMultipleQueryFacets() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnAvailableQuery", String.class);
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFacetFields());
		Assert.assertTrue(method.hasFacetQueries());
		Assert.assertFalse(method.hasFilterQuery());

		Assert.assertEquals(0, method.getFacetFields().size());
		Assert.assertEquals(2, method.getFacetQueries().size());
		Assert.assertEquals(Integer.valueOf(10), method.getFacetLimit());
		Assert.assertEquals(Integer.valueOf(1), method.getFacetMinCount());
		Assert.assertEquals("inStock:true", method.getFacetQueries().get(0));
		Assert.assertEquals("inStock:false", method.getFacetQueries().get(1));
	}

	@Test
	public void testWithMultipleQueryFacetsLimitAndMinCount() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnAvailableQueryMinCount3Limit25", String.class);
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFacetFields());
		Assert.assertTrue(method.hasFacetQueries());
		Assert.assertFalse(method.hasFilterQuery());

		Assert.assertEquals(0, method.getFacetFields().size());
		Assert.assertEquals(2, method.getFacetQueries().size());
		Assert.assertEquals(Integer.valueOf(25), method.getFacetLimit());
		Assert.assertEquals(Integer.valueOf(3), method.getFacetMinCount());
		Assert.assertEquals("inStock:true", method.getFacetQueries().get(0));
		Assert.assertEquals("inStock:false", method.getFacetQueries().get(1));
	}

	@Test
	public void testWithSigleFilter() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameStringWith", String.class);
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertFalse(method.hasFacetFields());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertTrue(method.hasFilterQuery());

		Assert.assertEquals(1, method.getFilterQueries().size());
	}

	@Test
	public void testWithMultipleFilters() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findAllFilterAvailableTrueAndPopularityLessThan5", String.class);
		Assert.assertTrue(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertFalse(method.hasFacetFields());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertTrue(method.hasFilterQuery());

		Assert.assertEquals(2, method.getFilterQueries().size());
	}

	@Test
	public void testWithoutQueryDefaultOperator() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameLike", String.class);
		Assert.assertEquals(org.springframework.data.solr.core.query.Query.Operator.AND, method.getDefaultOperator());
	}

	@Test
	public void testWithQueryDefaultOperator() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameStringWith", String.class);
		Assert.assertEquals(org.springframework.data.solr.core.query.Query.Operator.NONE, method.getDefaultOperator());
	}

	@Test
	public void testQueryWithPositiveTimeAllowed() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findAllWithPositiveTimeRestriction", String.class);
		Assert.assertEquals(Integer.valueOf(250), method.getTimeAllowed());
	}

	@Test
	public void testQueryWithNegativeTimeAllowed() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findAllWithNegativeTimeRestriction", String.class);
		Assert.assertNull(method.getTimeAllowed());
	}

	@Test
	public void testQueryWithDefType() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameEndingWith", String.class);
		Assert.assertEquals("lucene", method.getDefType());
	}

	@Test
	public void testQueryWithRequestHandler() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByText", String.class);
		Assert.assertEquals("/instock", method.getRequestHandler());
	}

	private SolrQueryMethod getQueryMethodByName(String name, Class<?>... parameters) throws Exception {
		Method method = Repo1.class.getMethod(name, parameters);
		return new SolrQueryMethod(method, new DefaultRepositoryMetadata(Repo1.class), creator);
	}

	interface Repo1 extends Repository<ProductBean, String> {

		@Query(value = "name:?0")
		List<ProductBean> findByAnnotatedQuery(String name);

		@Query("name:?0")
		List<ProductBean> findByAnnotatedQueryWithoutExplicitAttribute(String name);

		@Query(name = "ProductRepository.namedQuery-1")
		List<ProductBean> findByAnnotatedNamedQueryName(String name);

		List<ProductBean> findByName(String name);

		@Query(value = "name:?0", fields = "popularity")
		List<ProductBean> findByAnnotatedQueryWithProjectionOnSingleField(String name);

		@Query(value = "name:?0", fields = { "popularity", "price" })
		List<ProductBean> findByAnnotatedQueryWithProjectionOnMultipleFields(String name);

		@Facet(fields = { "popularity" })
		List<ProductBean> findByNameFacetOnPopularity(String name);

		@Facet(fields = { "popularity", "price" })
		List<ProductBean> findByNameFacetOnPopularityAndPrice(String name);

		@Facet(fields = { "popularity", "price" }, minCount = 3, limit = 25)
		List<ProductBean> findByNameFacetOnPopularityAndPriceMinCount3Limit25(String name);

		@Facet(queries = { "popularity:[* TO 5]" })
		List<ProductBean> findByNameFacetOnPopularityQuery(String name);

		@Facet(queries = { "inStock:true", "inStock:false" })
		List<ProductBean> findByNameFacetOnAvailableQuery(String name);

		@Facet(queries = { "inStock:true", "inStock:false" }, minCount = 3, limit = 25)
		List<ProductBean> findByNameFacetOnAvailableQueryMinCount3Limit25(String name);

		@Query(filters = { "inStock:true" })
		List<ProductBean> findByNameStringWith(String name);

		@Query(value = "*:*", filters = { "inStock:true", "popularity:[* TO 5]" })
		List<ProductBean> findAllFilterAvailableTrueAndPopularityLessThan5(String name);

		@Query(defaultOperator = org.springframework.data.solr.core.query.Query.Operator.AND)
		List<ProductBean> findByNameLike(String prefix);

		@Query(value = "*:*", timeAllowed = 250)
		List<ProductBean> findAllWithPositiveTimeRestriction(String name);

		@Query(value = "*:*", timeAllowed = -10)
		List<ProductBean> findAllWithNegativeTimeRestriction(String name);

		@Query(defType = "lucene")
		List<ProductBean> findByNameEndingWith(String name);

		@Query(requestHandler = "/instock")
		List<ProductBean> findByText(String text);
	}

}
