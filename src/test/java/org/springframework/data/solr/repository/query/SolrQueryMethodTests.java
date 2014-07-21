/*
 * Copyright 2012 - 2014 the original author or authors.
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
import java.util.Arrays;
import java.util.List;

import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.repository.Facet;
import org.springframework.data.solr.repository.Highlight;
import org.springframework.data.solr.repository.Pivot;
import org.springframework.data.solr.repository.ProductBean;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.support.SolrEntityInformationCreatorImpl;

/**
 * @author Christoph Strobl
 * @author Andrey Paramonov
 * @author Francisco Spaeth
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
		Assert.assertFalse(method.isHighlightQuery());
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
	public void testWithSingleFieldPivot() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNamePivotOnField1VsField2");
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFacetFields());
		Assert.assertTrue(method.hasPivotFields());
		Assert.assertFalse(method.hasFacetQueries());
		Assert.assertFalse(method.hasFilterQuery());

		Assert.assertEquals(1, method.getPivotFields().size());
		Assert.assertEquals(Integer.valueOf(10), method.getFacetLimit());
		Assert.assertEquals(Integer.valueOf(1), method.getFacetMinCount());
	}

	@Test
	public void testWithMultipleFieldPivot() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNamePivotOnField1VsField2AndField2VsField3");
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertFalse(method.hasFacetFields());
		Assert.assertTrue(method.hasPivotFields());
		Assert.assertFalse(method.hasFacetQueries());
		Assert.assertEquals(2, method.getPivotFields().size());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFilterQuery());
	}

	/**
	 * @see DATSOLR-155
	 */
	@Test
	public void testWithMultipleFieldPivotUsingPivotAnnotation() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNamePivotOnField1VsField2AndField2VsField3UsingPivotAnnotation");
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertFalse(method.hasFacetFields());
		Assert.assertTrue(method.hasPivotFields());
		Assert.assertFalse(method.hasFacetQueries());
		Assert.assertEquals(2, method.getPivotFields().size());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFilterQuery());
	}

	/**
	 * @see DATASOLR-155
	 */
	@Test
	public void testWithMultipleFieldPivotUsingOnlyPivotAnnotation() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNamePivotOnField1VsField2AndField2VsField3UsingOnlyPivotAnnotation");
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertFalse(method.hasFacetFields());
		Assert.assertTrue(method.hasPivotFields());
		Assert.assertFalse(method.hasFacetQueries());
		Assert.assertEquals(2, method.getPivotFields().size());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFilterQuery());
	}

	@Test
	public void testWithMultipleFieldPivotsLimitAndMinCount() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNamePivotOnField1VsField2AndField2VsField3AndLimitAndMinCount");
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasProjectionFields());
		Assert.assertFalse(method.hasFacetFields());
		Assert.assertTrue(method.hasPivotFields());
		Assert.assertFalse(method.hasFacetQueries());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertFalse(method.hasFilterQuery());

		Assert.assertEquals(2, method.getPivotFields().size());
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
	public void testWithFacetPrefix() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findAllFacetOnNameWithPrefix");
		Assert.assertEquals(1, method.getFacetFields().size());
		Assert.assertEquals("ip", method.getFacetPrefix());
	}

	@Test
	public void testWithoutFacetPrefix() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularity", String.class);
		Assert.assertNull(method.getFacetPrefix());
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

	@Test
	public void testQueryWithEmptyHighlight() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextLike", String.class);
		Assert.assertTrue(method.isHighlightQuery());
		Assert.assertNull(method.getHighlightFormatter());
		Assert.assertNull(method.getHighlightQuery());
		Assert.assertNull(method.getHighlighSnipplets());
		Assert.assertThat(method.getHighlightFieldNames(), IsEmptyCollection.empty());
		Assert.assertNull(method.getHighlightFragsize());
		Assert.assertNull(method.getHighlightPrefix());
		Assert.assertNull(method.getHighlightPostfix());
	}

	@Test
	public void testQueryWithHighlightSingleField() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightSingleField", String.class);
		Assert.assertThat(Arrays.asList("field_1"), IsEqual.equalTo(method.getHighlightFieldNames()));
	}

	@Test
	public void testQueryWithHighlightMultipleFields() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightMultipleFields", String.class);
		Assert.assertThat(Arrays.asList("field_1", "field_2", "field_3"), IsEqual.equalTo(method.getHighlightFieldNames()));
	}

	@Test
	public void testQueryWithHighlightFormatter() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightFormatter", String.class);
		Assert.assertEquals("simple", method.getHighlightFormatter());
	}

	@Test
	public void testQueryWithHighlightQuery() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightQuery", String.class);
		Assert.assertEquals("field_1:value*", method.getHighlightQuery());
	}

	@Test
	public void testQueryWithHighlightSnipplets() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightSnipplets", String.class);
		Assert.assertEquals(Integer.valueOf(2), method.getHighlighSnipplets());
	}

	@Test
	public void testQueryWithNegativeHighlightSnipplets() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextNegativeHighlightSnipplets", String.class);
		Assert.assertNull(method.getHighlighSnipplets());
	}

	@Test
	public void testQueryWithHighlightFragsize() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightFragsize", String.class);
		Assert.assertEquals(Integer.valueOf(3), method.getHighlightFragsize());
	}

	@Test
	public void testQueryWithNegativeHighlightFragsize() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextNegativeHighlightFragsize", String.class);
		Assert.assertNull(method.getHighlightFragsize());
	}

	@Test
	public void testQueryWithHighlightPrefix() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightPrefix", String.class);
		Assert.assertEquals("{prefix}", method.getHighlightPrefix());
	}

	@Test
	public void testQueryWithHighlightPostfix() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightPostfix", String.class);
		Assert.assertEquals("{postfix}", method.getHighlightPostfix());
	}

	/**
	 * @see DATASOLR-144
	 */
	@Test
	public void testDeleteAttrbiteOfAnnotatedQueryIsDiscoveredCorrectlty() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("removeByAnnotatedQuery");
		Assert.assertTrue(method.isDeleteQuery());
	}

	/**
	 * @see DATASOLR-144
	 */
	@Test
	public void testDeleteAttrbiteOfAnnotatedQueryIsFalseByDefault() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQuery", String.class);
		Assert.assertFalse(method.isDeleteQuery());
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

		@Facet(fields = "name", prefix = "ip")
		List<ProductBean> findAllFacetOnNameWithPrefix();

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

		@Query(value = "*:*", delete = true)
		List<ProductBean> removeByAnnotatedQuery();

		@Highlight
		List<ProductBean> findByTextLike(String text);

		@Highlight(fields = "field_1")
		List<ProductBean> findByTextHighlightSingleField(String text);

		@Highlight(fields = { "field_1", "field_2", "field_3" })
		List<ProductBean> findByTextHighlightMultipleFields(String text);

		@Highlight(formatter = "simple")
		List<ProductBean> findByTextHighlightFormatter(String text);

		@Highlight(query = "field_1:value*")
		List<ProductBean> findByTextHighlightQuery(String text);

		@Highlight(snipplets = 2)
		List<ProductBean> findByTextHighlightSnipplets(String text);

		@Highlight(snipplets = -2)
		List<ProductBean> findByTextNegativeHighlightSnipplets(String text);

		@Highlight(fragsize = 3)
		List<ProductBean> findByTextHighlightFragsize(String text);

		@Highlight(fragsize = -3)
		List<ProductBean> findByTextNegativeHighlightFragsize(String text);

		@Highlight(prefix = "{prefix}")
		List<ProductBean> findByTextHighlightPrefix(String text);

		@Highlight(postfix = "{postfix}")
		List<ProductBean> findByTextHighlightPostfix(String text);

		@Facet(pivots = { @Pivot({ "field1", "field2" }) })
		List<ProductBean> findByNamePivotOnField1VsField2();

		@Facet(pivots = { @Pivot({ "field1", "field2" }), @Pivot({ "field2", "field3" }) })
		List<ProductBean> findByNamePivotOnField1VsField2AndField2VsField3();

		@Facet(pivots = { @Pivot({ "field1", "field2" }), @Pivot({ "field2", "field3" }) }, minCount = 3, limit = 25)
		List<ProductBean> findByNamePivotOnField1VsField2AndField2VsField3AndLimitAndMinCount();

		@Facet(pivots = { @Pivot({ "field4", "field5" }), @Pivot({ "field5", "field6" }) })
		List<ProductBean> findByNamePivotOnField1VsField2AndField2VsField3UsingPivotAnnotation();

		@Facet(pivots = { @Pivot({ "field1", "field2" }), @Pivot({ "field2", "field3" }) })
		List<ProductBean> findByNamePivotOnField1VsField2AndField2VsField3UsingOnlyPivotAnnotation();

	}

}
