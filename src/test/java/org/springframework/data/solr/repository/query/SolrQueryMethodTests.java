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
package org.springframework.data.solr.repository.query;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsEmptyCollection.*;
import static org.hamcrest.number.IsCloseTo.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.query.SpellcheckOptions;
import org.springframework.data.solr.repository.Facet;
import org.springframework.data.solr.repository.Highlight;
import org.springframework.data.solr.repository.Pivot;
import org.springframework.data.solr.repository.ProductBean;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.SelectiveStats;
import org.springframework.data.solr.repository.Spellcheck;
import org.springframework.data.solr.repository.Stats;
import org.springframework.data.solr.repository.support.SolrEntityInformationCreatorImpl;

/**
 * @author Christoph Strobl
 * @author Andrey Paramonov
 * @author Francisco Spaeth
 * @author Oliver Gierke
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
		assertTrue(method.hasAnnotatedQuery());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasProjectionFields());
		assertFalse(method.hasFilterQuery());
		assertEquals("name:?0", method.getAnnotatedQuery());
	}

	@Test
	public void testAnnotatedQueryUsageWithoutExplicitAttribute() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQueryWithoutExplicitAttribute", String.class);
		assertTrue(method.hasAnnotatedQuery());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFilterQuery());
		assertEquals("name:?0", method.getAnnotatedQuery());
	}

	@Test
	public void testAnnotatedNamedQueryNameUsage() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedNamedQueryName", String.class);
		assertFalse(method.hasAnnotatedQuery());
		assertTrue(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFilterQuery());
		assertEquals("ProductRepository.namedQuery-1", method.getAnnotatedNamedQueryName());
	}

	@Test
	public void testWithoutAnnotation() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByName", String.class);
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasFilterQuery());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.isHighlightQuery());
	}

	@Test
	public void testWithSingleFieldProjection() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQueryWithProjectionOnSingleField", String.class);
		assertTrue(method.hasAnnotatedQuery());
		assertTrue(method.hasProjectionFields());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFilterQuery());
		assertEquals("name:?0", method.getAnnotatedQuery());
	}

	@Test
	public void testWithMultipleFieldsProjection() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQueryWithProjectionOnMultipleFields", String.class);
		assertTrue(method.hasAnnotatedQuery());
		assertTrue(method.hasProjectionFields());
		assertFalse(method.hasFilterQuery());
		assertEquals(2, method.getProjectionFields().size());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertEquals("name:?0", method.getAnnotatedQuery());
	}

	@Test
	public void testWithSingleFieldFacet() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularity", String.class);
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertTrue(method.hasFacetFields());
		assertFalse(method.hasFacetQueries());
		assertFalse(method.hasFilterQuery());

		assertEquals(1, method.getFacetFields().size());
		assertEquals(Integer.valueOf(10), method.getFacetLimit());
		assertEquals(Integer.valueOf(1), method.getFacetMinCount());
	}

	@Test
	public void testWithMultipleFieldFacets() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularityAndPrice", String.class);
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertTrue(method.hasFacetFields());
		assertFalse(method.hasFacetQueries());
		assertEquals(2, method.getFacetFields().size());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFilterQuery());
	}

	@Test
	public void testWithMultipleFieldFacetsLimitAndMinCount() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularityAndPriceMinCount3Limit25", String.class);
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertTrue(method.hasFacetFields());
		assertFalse(method.hasFacetQueries());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFilterQuery());

		assertEquals(2, method.getFacetFields().size());
		assertEquals(Integer.valueOf(25), method.getFacetLimit());
		assertEquals(Integer.valueOf(3), method.getFacetMinCount());
	}

	@Test
	public void testWithSingleFieldPivot() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNamePivotOnField1VsField2");
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFacetFields());
		assertTrue(method.hasPivotFields());
		assertFalse(method.hasFacetQueries());
		assertFalse(method.hasFilterQuery());

		assertEquals(1, method.getPivotFields().size());
		assertEquals(Integer.valueOf(10), method.getFacetLimit());
		assertEquals(Integer.valueOf(1), method.getFacetMinCount());
	}

	@Test
	public void testWithMultipleFieldPivot() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNamePivotOnField1VsField2AndField2VsField3");
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertFalse(method.hasFacetFields());
		assertTrue(method.hasPivotFields());
		assertFalse(method.hasFacetQueries());
		assertEquals(2, method.getPivotFields().size());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFilterQuery());
	}

	@Test // DATSOLR-155
	public void testWithMultipleFieldPivotUsingPivotAnnotation() throws Exception {
		SolrQueryMethod method = getQueryMethodByName(
				"findByNamePivotOnField1VsField2AndField2VsField3UsingPivotAnnotation");
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertFalse(method.hasFacetFields());
		assertTrue(method.hasPivotFields());
		assertFalse(method.hasFacetQueries());
		assertEquals(2, method.getPivotFields().size());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFilterQuery());
	}

	@Test // DATASOLR-155
	public void testWithMultipleFieldPivotUsingOnlyPivotAnnotation() throws Exception {
		SolrQueryMethod method = getQueryMethodByName(
				"findByNamePivotOnField1VsField2AndField2VsField3UsingOnlyPivotAnnotation");
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertFalse(method.hasFacetFields());
		assertTrue(method.hasPivotFields());
		assertFalse(method.hasFacetQueries());
		assertEquals(2, method.getPivotFields().size());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFilterQuery());
	}

	@Test
	public void testWithMultipleFieldPivotsLimitAndMinCount() throws Exception {
		SolrQueryMethod method = getQueryMethodByName(
				"findByNamePivotOnField1VsField2AndField2VsField3AndLimitAndMinCount");
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertFalse(method.hasFacetFields());
		assertTrue(method.hasPivotFields());
		assertFalse(method.hasFacetQueries());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFilterQuery());

		assertEquals(2, method.getPivotFields().size());
		assertEquals(Integer.valueOf(25), method.getFacetLimit());
		assertEquals(Integer.valueOf(3), method.getFacetMinCount());
	}

	@Test
	public void testWithSingleQueryFacet() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularityQuery", String.class);
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFacetFields());
		assertTrue(method.hasFacetQueries());
		assertFalse(method.hasFilterQuery());

		assertEquals(0, method.getFacetFields().size());
		assertEquals(1, method.getFacetQueries().size());
		assertEquals(Integer.valueOf(10), method.getFacetLimit());
		assertEquals(Integer.valueOf(1), method.getFacetMinCount());
		assertEquals("popularity:[* TO 5]", method.getFacetQueries().get(0));
	}

	@Test
	public void testWithMultipleQueryFacets() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnAvailableQuery", String.class);
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFacetFields());
		assertTrue(method.hasFacetQueries());
		assertFalse(method.hasFilterQuery());

		assertEquals(0, method.getFacetFields().size());
		assertEquals(2, method.getFacetQueries().size());
		assertEquals(Integer.valueOf(10), method.getFacetLimit());
		assertEquals(Integer.valueOf(1), method.getFacetMinCount());
		assertEquals("inStock:true", method.getFacetQueries().get(0));
		assertEquals("inStock:false", method.getFacetQueries().get(1));
	}

	@Test
	public void testWithMultipleQueryFacetsLimitAndMinCount() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnAvailableQueryMinCount3Limit25", String.class);
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertFalse(method.hasFacetFields());
		assertTrue(method.hasFacetQueries());
		assertFalse(method.hasFilterQuery());

		assertEquals(0, method.getFacetFields().size());
		assertEquals(2, method.getFacetQueries().size());
		assertEquals(Integer.valueOf(25), method.getFacetLimit());
		assertEquals(Integer.valueOf(3), method.getFacetMinCount());
		assertEquals("inStock:true", method.getFacetQueries().get(0));
		assertEquals("inStock:false", method.getFacetQueries().get(1));
	}

	@Test
	public void testWithFacetPrefix() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findAllFacetOnNameWithPrefix");
		assertEquals(1, method.getFacetFields().size());
		assertEquals("ip", method.getFacetPrefix());
	}

	@Test
	public void testWithoutFacetPrefix() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularity", String.class);
		assertNull(method.getFacetPrefix());
	}

	@Test
	public void testWithSigleFilter() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameStringWith", String.class);
		assertFalse(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertFalse(method.hasFacetFields());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertTrue(method.hasFilterQuery());

		assertEquals(1, method.getFilterQueries().size());
	}

	@Test
	public void testWithMultipleFilters() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findAllFilterAvailableTrueAndPopularityLessThan5", String.class);
		assertTrue(method.hasAnnotatedQuery());
		assertFalse(method.hasProjectionFields());
		assertFalse(method.hasFacetFields());
		assertFalse(method.hasAnnotatedNamedQueryName());
		assertTrue(method.hasFilterQuery());

		assertEquals(2, method.getFilterQueries().size());
	}

	@Test
	public void testWithoutQueryDefaultOperator() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameLike", String.class);
		assertEquals(org.springframework.data.solr.core.query.Query.Operator.AND, method.getDefaultOperator());
	}

	@Test
	public void testWithQueryDefaultOperator() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameStringWith", String.class);
		assertEquals(org.springframework.data.solr.core.query.Query.Operator.NONE, method.getDefaultOperator());
	}

	@Test
	public void testQueryWithPositiveTimeAllowed() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findAllWithPositiveTimeRestriction", String.class);
		assertEquals(Integer.valueOf(250), method.getTimeAllowed());
	}

	@Test
	public void testQueryWithNegativeTimeAllowed() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findAllWithNegativeTimeRestriction", String.class);
		assertNull(method.getTimeAllowed());
	}

	@Test
	public void testQueryWithDefType() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameEndingWith", String.class);
		assertEquals("lucene", method.getDefType());
	}

	@Test
	public void testQueryWithRequestHandler() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByText", String.class);
		assertEquals("/instock", method.getRequestHandler());
	}

	@Test
	public void testQueryWithEmptyHighlight() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextLike", String.class);
		assertTrue(method.isHighlightQuery());
		assertNull(method.getHighlightFormatter());
		assertNull(method.getHighlightQuery());
		assertNull(method.getHighlighSnipplets());
		assertThat(method.getHighlightFieldNames(), empty());
		assertNull(method.getHighlightFragsize());
		assertNull(method.getHighlightPrefix());
		assertNull(method.getHighlightPostfix());
	}

	@Test
	public void testQueryWithHighlightSingleField() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightSingleField", String.class);
		assertThat(Collections.singletonList("field_1"), equalTo(method.getHighlightFieldNames()));
	}

	@Test
	public void testQueryWithHighlightMultipleFields() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightMultipleFields", String.class);
		assertThat(Arrays.asList("field_1", "field_2", "field_3"), equalTo(method.getHighlightFieldNames()));
	}

	@Test
	public void testQueryWithHighlightFormatter() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightFormatter", String.class);
		assertEquals("simple", method.getHighlightFormatter());
	}

	@Test
	public void testQueryWithHighlightQuery() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightQuery", String.class);
		assertEquals("field_1:value*", method.getHighlightQuery());
	}

	@Test
	public void testQueryWithHighlightSnipplets() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightSnipplets", String.class);
		assertEquals(Integer.valueOf(2), method.getHighlighSnipplets());
	}

	@Test
	public void testQueryWithNegativeHighlightSnipplets() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextNegativeHighlightSnipplets", String.class);
		assertNull(method.getHighlighSnipplets());
	}

	@Test
	public void testQueryWithHighlightFragsize() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightFragsize", String.class);
		assertEquals(Integer.valueOf(3), method.getHighlightFragsize());
	}

	@Test
	public void testQueryWithNegativeHighlightFragsize() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextNegativeHighlightFragsize", String.class);
		assertNull(method.getHighlightFragsize());
	}

	@Test
	public void testQueryWithHighlightPrefix() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightPrefix", String.class);
		assertEquals("{prefix}", method.getHighlightPrefix());
	}

	@Test
	public void testQueryWithHighlightPostfix() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightPostfix", String.class);
		assertEquals("{postfix}", method.getHighlightPostfix());
	}

	@Test // DATASOLR-144
	public void testDeleteAttrbiteOfAnnotatedQueryIsDiscoveredCorrectlty() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("removeByAnnotatedQuery");
		assertTrue(method.isDeleteQuery());
	}

	@Test // DATASOLR-144
	public void testDeleteAttrbiteOfAnnotatedQueryIsFalseByDefault() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQuery", String.class);
		assertFalse(method.isDeleteQuery());
	}

	@Test // DATASOLR-160
	public void testStatsForField() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByNameWithFieldStats", String.class);
		assertEquals(Collections.singletonList("field1"), method.getFieldStats());
	}

	@Test // DATASOLR-160
	public void testStatsForFieldAndFacets() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByNameWithFieldAndFacetStats", String.class);
		assertEquals(Collections.singletonList("field1"), method.getFieldStats());
		assertEquals(Collections.singletonList("field2"), method.getStatsFacets());
	}

	@Test // DATASOLR-160
	public void testStatsForSelectiveFacets() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByNameWithSelectiveFacetStats", String.class);
		Map<String, String[]> statsSelectiveFacets = method.getStatsSelectiveFacets();
		assertEquals(2, statsSelectiveFacets.size());
		assertArrayEquals(new String[] { "field1_1", "field1_2" }, statsSelectiveFacets.get("field1"));
		assertArrayEquals(new String[] { "field2_1", "field2_2" }, statsSelectiveFacets.get("field2"));
	}

	@Test // DATASOLR-160
	public void testStatsForFieldAndFacetsAndSelectiveFacets() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByNameWithFieldStatsAndFacetsStatsAndSelectiveFacetStats",
				String.class);
		assertEquals(Collections.singletonList("field1"), method.getFieldStats());
		assertEquals(Arrays.asList("field2", "field3"), method.getStatsFacets());
		Map<String, String[]> statsSelectiveFacets = method.getStatsSelectiveFacets();
		assertEquals(1, statsSelectiveFacets.size());
		assertArrayEquals(new String[] { "field4_1", "field4_2" }, statsSelectiveFacets.get("field4"));
	}

	@Test // DATASOLR-160
	public void testHasStatsDefinition() throws Exception {

		assertFalse(getQueryMethodByName("findByNameWithEmptyStats", String.class).hasStatsDefinition());

		assertTrue(getQueryMethodByName("findByNameWithFieldStats", String.class).hasStatsDefinition());
		assertTrue(getQueryMethodByName("findByNameWithFieldAndFacetStats", String.class).hasStatsDefinition());
		assertTrue(getQueryMethodByName("findByNameWithSelectiveFacetStats", String.class).hasStatsDefinition());
		assertTrue(getQueryMethodByName("findByNameWithFieldStatsAndFacetsStatsAndSelectiveFacetStats", String.class)
				.hasStatsDefinition());
	}

	/**
	 * DATASOLR-137
	 */
	@Test
	public void shouldApplySpellcheckCorrectly() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByNameWithDefaultSpellcheck", String.class);

		assertTrue(method.hasSpellcheck());
		SpellcheckOptions options = method.getSpellcheckOptions();

		assertThat(options.getDictionary(), is(nullValue()));
		assertThat(options.getAccuracy(), is(nullValue()));
		assertThat(options.getAlternativeTermCount(), is(nullValue()));
		assertThat(options.getCollate(), is(false));
		assertThat(options.getCollateExtendedResults(), is(false));
		assertThat(options.getMaxCollationCollectDocs(), is(nullValue()));
		assertThat(options.getCollateParams().size(), is(0));
		assertThat(options.getCount(), is(nullValue()));
		assertThat(options.getDictionary(), is(nullValue()));
		assertThat(options.getMaxCollationEvaluations(), is(nullValue()));
		assertThat(options.getMaxCollations(), is(nullValue()));
		assertThat(options.getMaxResultsForSuggest(), is(nullValue()));
		assertThat(options.getOnlyMorePopular(), is(false));
		assertThat(options.getQuery(), is(nullValue()));
	}

	/**
	 * DATASOLR-137
	 */
	@Test
	public void shouldApplySpellcheckWithOptionsCorrectly() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByNameWithSpellcheckOptions", String.class);

		assertTrue(method.hasSpellcheck());
		SpellcheckOptions options = method.getSpellcheckOptions();

		assertThat(options.getAccuracy().doubleValue(), is(closeTo(0.5D, 0.0D)));
		assertThat(options.getAlternativeTermCount(), is(10L));
		assertThat(options.getCollate(), is(true));
		assertThat(options.getCollateExtendedResults(), is(true));
		assertThat(options.getMaxCollationCollectDocs(), is(10L));
		assertThat(options.getCollateParams().size(), is(0));
		assertThat(options.getCount(), is(100L));
		assertThat(options.getDictionary(), is(equalTo(new String[] { "myDict" })));
		assertThat(options.getMaxCollationEvaluations(), is(5L));
		assertThat(options.getMaxCollations(), is(3L));
		assertThat(options.getMaxResultsForSuggest(), is(7L));
		assertThat(options.getOnlyMorePopular(), is(true));
		assertThat(options.getQuery(), is(nullValue()));
		assertThat(options.getExtendedResults(), is(true));
	}

	private SolrQueryMethod getQueryMethodByName(String name, Class<?>... parameters) throws Exception {
		Method method = Repo1.class.getMethod(name, parameters);
		return new SolrQueryMethod(method, new DefaultRepositoryMetadata(Repo1.class),
				new SpelAwareProxyProjectionFactory(), creator);
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

		@Stats("field1")
		List<ProductBean> findByNameWithFieldStats(String name);

		@Stats(value = "field1", facets = "field2")
		List<ProductBean> findByNameWithFieldAndFacetStats(String name);

		@Stats( //
				selective = { @SelectiveStats(field = "field1", facets = { "field1_1", "field1_2" }), //
						@SelectiveStats(field = "field2", facets = { "field2_1", "field2_2" }) //
				}//
		)
		List<ProductBean> findByNameWithSelectiveFacetStats(String name);

		@Stats(//
				value = "field1", //
				facets = { "field2", "field3" }, //
				selective = @SelectiveStats(field = "field4", facets = { "field4_1", "field4_2" }) //
		)
		List<ProductBean> findByNameWithFieldStatsAndFacetsStatsAndSelectiveFacetStats(String name);

		@Stats
		List<ProductBean> findByNameWithEmptyStats(String name);

		@Spellcheck
		List<ProductBean> findByNameWithDefaultSpellcheck(String name);

		@Spellcheck(accuracy = 0.5F, alternativeTermCount = 10, buildDictionary = true, collate = true,
				collateExtendedResults = true, count = 100, dictionaries = "myDict", maxCollationEvaluations = 5,
				maxCollationCollectDocs = 10, maxCollations = 3, maxCollationsTries = 9, maxResultsForSuggest = 7,
				onlyMorePopular = true, extendedResults = true)
		List<ProductBean> findByNameWithSpellcheckOptions(String name);
	}

}
