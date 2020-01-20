/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.solr.repository.query;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Offset.offset;

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
		assertThat(method.hasAnnotatedQuery()).isTrue();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasFilterQuery()).isFalse();
		assertThat(method.getAnnotatedQuery()).isEqualTo("name:?0");
	}

	@Test
	public void testAnnotatedQueryUsageWithoutExplicitAttribute() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQueryWithoutExplicitAttribute", String.class);
		assertThat(method.hasAnnotatedQuery()).isTrue();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFilterQuery()).isFalse();
		assertThat(method.getAnnotatedQuery()).isEqualTo("name:?0");
	}

	@Test
	public void testAnnotatedNamedQueryNameUsage() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedNamedQueryName", String.class);
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasAnnotatedNamedQueryName()).isTrue();
		assertThat(method.hasFilterQuery()).isFalse();
		assertThat(method.getAnnotatedNamedQueryName()).isEqualTo("ProductRepository.namedQuery-1");
	}

	@Test
	public void testWithoutAnnotation() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByName", String.class);
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasFilterQuery()).isFalse();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.isHighlightQuery()).isFalse();
	}

	@Test
	public void testWithSingleFieldProjection() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQueryWithProjectionOnSingleField", String.class);
		assertThat(method.hasAnnotatedQuery()).isTrue();
		assertThat(method.hasProjectionFields()).isTrue();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFilterQuery()).isFalse();
		assertThat(method.getAnnotatedQuery()).isEqualTo("name:?0");
	}

	@Test
	public void testWithMultipleFieldsProjection() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQueryWithProjectionOnMultipleFields", String.class);
		assertThat(method.hasAnnotatedQuery()).isTrue();
		assertThat(method.hasProjectionFields()).isTrue();
		assertThat(method.hasFilterQuery()).isFalse();
		assertThat(method.getProjectionFields().size()).isEqualTo(2);
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.getAnnotatedQuery()).isEqualTo("name:?0");
	}

	@Test
	public void testWithSingleFieldFacet() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularity", String.class);
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFacetFields()).isTrue();
		assertThat(method.hasFacetQueries()).isFalse();
		assertThat(method.hasFilterQuery()).isFalse();

		assertThat(method.getFacetFields().size()).isEqualTo(1);
		assertThat(method.getFacetLimit()).isEqualTo(Integer.valueOf(10));
		assertThat(method.getFacetMinCount()).isEqualTo(Integer.valueOf(1));
	}

	@Test
	public void testWithMultipleFieldFacets() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularityAndPrice", String.class);
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasFacetFields()).isTrue();
		assertThat(method.hasFacetQueries()).isFalse();
		assertThat(method.getFacetFields().size()).isEqualTo(2);
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFilterQuery()).isFalse();
	}

	@Test
	public void testWithMultipleFieldFacetsLimitAndMinCount() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularityAndPriceMinCount3Limit25", String.class);
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasFacetFields()).isTrue();
		assertThat(method.hasFacetQueries()).isFalse();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFilterQuery()).isFalse();

		assertThat(method.getFacetFields().size()).isEqualTo(2);
		assertThat(method.getFacetLimit()).isEqualTo(Integer.valueOf(25));
		assertThat(method.getFacetMinCount()).isEqualTo(Integer.valueOf(3));
	}

	@Test
	public void testWithSingleFieldPivot() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNamePivotOnField1VsField2");
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFacetFields()).isFalse();
		assertThat(method.hasPivotFields()).isTrue();
		assertThat(method.hasFacetQueries()).isFalse();
		assertThat(method.hasFilterQuery()).isFalse();

		assertThat(method.getPivotFields().size()).isEqualTo(1);
		assertThat(method.getFacetLimit()).isEqualTo(Integer.valueOf(10));
		assertThat(method.getFacetMinCount()).isEqualTo(Integer.valueOf(1));
	}

	@Test
	public void testWithMultipleFieldPivot() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNamePivotOnField1VsField2AndField2VsField3");
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasFacetFields()).isFalse();
		assertThat(method.hasPivotFields()).isTrue();
		assertThat(method.hasFacetQueries()).isFalse();
		assertThat(method.getPivotFields().size()).isEqualTo(2);
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFilterQuery()).isFalse();
	}

	@Test // DATSOLR-155
	public void testWithMultipleFieldPivotUsingPivotAnnotation() throws Exception {
		SolrQueryMethod method = getQueryMethodByName(
				"findByNamePivotOnField1VsField2AndField2VsField3UsingPivotAnnotation");
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasFacetFields()).isFalse();
		assertThat(method.hasPivotFields()).isTrue();
		assertThat(method.hasFacetQueries()).isFalse();
		assertThat(method.getPivotFields().size()).isEqualTo(2);
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFilterQuery()).isFalse();
	}

	@Test // DATASOLR-155
	public void testWithMultipleFieldPivotUsingOnlyPivotAnnotation() throws Exception {
		SolrQueryMethod method = getQueryMethodByName(
				"findByNamePivotOnField1VsField2AndField2VsField3UsingOnlyPivotAnnotation");
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasFacetFields()).isFalse();
		assertThat(method.hasPivotFields()).isTrue();
		assertThat(method.hasFacetQueries()).isFalse();
		assertThat(method.getPivotFields().size()).isEqualTo(2);
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFilterQuery()).isFalse();
	}

	@Test
	public void testWithMultipleFieldPivotsLimitAndMinCount() throws Exception {
		SolrQueryMethod method = getQueryMethodByName(
				"findByNamePivotOnField1VsField2AndField2VsField3AndLimitAndMinCount");
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasFacetFields()).isFalse();
		assertThat(method.hasPivotFields()).isTrue();
		assertThat(method.hasFacetQueries()).isFalse();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFilterQuery()).isFalse();

		assertThat(method.getPivotFields().size()).isEqualTo(2);
		assertThat(method.getFacetLimit()).isEqualTo(Integer.valueOf(25));
		assertThat(method.getFacetMinCount()).isEqualTo(Integer.valueOf(3));
	}

	@Test
	public void testWithSingleQueryFacet() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularityQuery", String.class);
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFacetFields()).isFalse();
		assertThat(method.hasFacetQueries()).isTrue();
		assertThat(method.hasFilterQuery()).isFalse();

		assertThat(method.getFacetFields().size()).isEqualTo(0);
		assertThat(method.getFacetQueries().size()).isEqualTo(1);
		assertThat(method.getFacetLimit()).isEqualTo(Integer.valueOf(10));
		assertThat(method.getFacetMinCount()).isEqualTo(Integer.valueOf(1));
		assertThat(method.getFacetQueries().get(0)).isEqualTo("popularity:[* TO 5]");
	}

	@Test
	public void testWithMultipleQueryFacets() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnAvailableQuery", String.class);
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFacetFields()).isFalse();
		assertThat(method.hasFacetQueries()).isTrue();
		assertThat(method.hasFilterQuery()).isFalse();

		assertThat(method.getFacetFields().size()).isEqualTo(0);
		assertThat(method.getFacetQueries().size()).isEqualTo(2);
		assertThat(method.getFacetLimit()).isEqualTo(Integer.valueOf(10));
		assertThat(method.getFacetMinCount()).isEqualTo(Integer.valueOf(1));
		assertThat(method.getFacetQueries().get(0)).isEqualTo("inStock:true");
		assertThat(method.getFacetQueries().get(1)).isEqualTo("inStock:false");
	}

	@Test
	public void testWithMultipleQueryFacetsLimitAndMinCount() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnAvailableQueryMinCount3Limit25", String.class);
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFacetFields()).isFalse();
		assertThat(method.hasFacetQueries()).isTrue();
		assertThat(method.hasFilterQuery()).isFalse();

		assertThat(method.getFacetFields().size()).isEqualTo(0);
		assertThat(method.getFacetQueries().size()).isEqualTo(2);
		assertThat(method.getFacetLimit()).isEqualTo(Integer.valueOf(25));
		assertThat(method.getFacetMinCount()).isEqualTo(Integer.valueOf(3));
		assertThat(method.getFacetQueries().get(0)).isEqualTo("inStock:true");
		assertThat(method.getFacetQueries().get(1)).isEqualTo("inStock:false");
	}

	@Test
	public void testWithFacetPrefix() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findAllFacetOnNameWithPrefix");
		assertThat(method.getFacetFields().size()).isEqualTo(1);
		assertThat(method.getFacetPrefix()).isEqualTo("ip");
	}

	@Test
	public void testWithoutFacetPrefix() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameFacetOnPopularity", String.class);
		assertThat(method.getFacetPrefix()).isNull();
	}

	@Test
	public void testWithSigleFilter() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameStringWith", String.class);
		assertThat(method.hasAnnotatedQuery()).isFalse();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasFacetFields()).isFalse();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFilterQuery()).isTrue();

		assertThat(method.getFilterQueries().size()).isEqualTo(1);
	}

	@Test
	public void testWithMultipleFilters() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findAllFilterAvailableTrueAndPopularityLessThan5", String.class);
		assertThat(method.hasAnnotatedQuery()).isTrue();
		assertThat(method.hasProjectionFields()).isFalse();
		assertThat(method.hasFacetFields()).isFalse();
		assertThat(method.hasAnnotatedNamedQueryName()).isFalse();
		assertThat(method.hasFilterQuery()).isTrue();

		assertThat(method.getFilterQueries().size()).isEqualTo(2);
	}

	@Test
	public void testWithoutQueryDefaultOperator() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameLike", String.class);
		assertThat(method.getDefaultOperator()).isEqualTo(org.springframework.data.solr.core.query.Query.Operator.AND);
	}

	@Test
	public void testWithQueryDefaultOperator() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameStringWith", String.class);
		assertThat(method.getDefaultOperator()).isEqualTo(org.springframework.data.solr.core.query.Query.Operator.NONE);
	}

	@Test
	public void testQueryWithPositiveTimeAllowed() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findAllWithPositiveTimeRestriction", String.class);
		assertThat(method.getTimeAllowed()).isEqualTo(Integer.valueOf(250));
	}

	@Test
	public void testQueryWithNegativeTimeAllowed() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findAllWithNegativeTimeRestriction", String.class);
		assertThat(method.getTimeAllowed()).isNull();
	}

	@Test
	public void testQueryWithDefType() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByNameEndingWith", String.class);
		assertThat(method.getDefType()).isEqualTo("lucene");
	}

	@Test
	public void testQueryWithRequestHandler() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByText", String.class);
		assertThat(method.getRequestHandler()).isEqualTo("/instock");
	}

	@Test
	public void testQueryWithEmptyHighlight() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextLike", String.class);
		assertThat(method.isHighlightQuery()).isTrue();
		assertThat(method.getHighlightFormatter()).isNull();
		assertThat(method.getHighlightQuery()).isNull();
		assertThat(method.getHighlighSnipplets()).isNull();
		assertThat(method.getHighlightFieldNames()).isEmpty();
		assertThat(method.getHighlightFragsize()).isNull();
		assertThat(method.getHighlightPrefix()).isNull();
		assertThat(method.getHighlightPostfix()).isNull();
	}

	@Test
	public void testQueryWithHighlightSingleField() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightSingleField", String.class);
		assertThat(Collections.singletonList("field_1")).isEqualTo(method.getHighlightFieldNames());
	}

	@Test
	public void testQueryWithHighlightMultipleFields() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightMultipleFields", String.class);
		assertThat(Arrays.asList("field_1", "field_2", "field_3")).isEqualTo(method.getHighlightFieldNames());
	}

	@Test
	public void testQueryWithHighlightFormatter() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightFormatter", String.class);
		assertThat(method.getHighlightFormatter()).isEqualTo("simple");
	}

	@Test
	public void testQueryWithHighlightQuery() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightQuery", String.class);
		assertThat(method.getHighlightQuery()).isEqualTo("field_1:value*");
	}

	@Test
	public void testQueryWithHighlightSnipplets() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightSnipplets", String.class);
		assertThat(method.getHighlighSnipplets()).isEqualTo(Integer.valueOf(2));
	}

	@Test
	public void testQueryWithNegativeHighlightSnipplets() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextNegativeHighlightSnipplets", String.class);
		assertThat(method.getHighlighSnipplets()).isNull();
	}

	@Test
	public void testQueryWithHighlightFragsize() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightFragsize", String.class);
		assertThat(method.getHighlightFragsize()).isEqualTo(Integer.valueOf(3));
	}

	@Test
	public void testQueryWithNegativeHighlightFragsize() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextNegativeHighlightFragsize", String.class);
		assertThat(method.getHighlightFragsize()).isNull();
	}

	@Test
	public void testQueryWithHighlightPrefix() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightPrefix", String.class);
		assertThat(method.getHighlightPrefix()).isEqualTo("{prefix}");
	}

	@Test
	public void testQueryWithHighlightPostfix() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByTextHighlightPostfix", String.class);
		assertThat(method.getHighlightPostfix()).isEqualTo("{postfix}");
	}

	@Test // DATASOLR-144
	public void testDeleteAttrbiteOfAnnotatedQueryIsDiscoveredCorrectlty() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("removeByAnnotatedQuery");
		assertThat(method.isDeleteQuery()).isTrue();
	}

	@Test // DATASOLR-144
	public void testDeleteAttrbiteOfAnnotatedQueryIsFalseByDefault() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQuery", String.class);
		assertThat(method.isDeleteQuery()).isFalse();
	}

	@Test // DATASOLR-160
	public void testStatsForField() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByNameWithFieldStats", String.class);
		assertThat(method.getFieldStats()).isEqualTo(Collections.singletonList("field1"));
	}

	@Test // DATASOLR-160
	public void testStatsForFieldAndFacets() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByNameWithFieldAndFacetStats", String.class);
		assertThat(method.getFieldStats()).isEqualTo(Collections.singletonList("field1"));
		assertThat(method.getStatsFacets()).isEqualTo(Collections.singletonList("field2"));
	}

	@Test // DATASOLR-160
	public void testStatsForSelectiveFacets() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByNameWithSelectiveFacetStats", String.class);
		Map<String, String[]> statsSelectiveFacets = method.getStatsSelectiveFacets();
		assertThat(statsSelectiveFacets.size()).isEqualTo(2);
		assertThat(statsSelectiveFacets.get("field1")).isEqualTo(new String[] { "field1_1", "field1_2" });
		assertThat(statsSelectiveFacets.get("field2")).isEqualTo(new String[] { "field2_1", "field2_2" });
	}

	@Test // DATASOLR-160
	public void testStatsForFieldAndFacetsAndSelectiveFacets() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByNameWithFieldStatsAndFacetsStatsAndSelectiveFacetStats",
				String.class);
		assertThat(method.getFieldStats()).isEqualTo(Collections.singletonList("field1"));
		assertThat(method.getStatsFacets()).isEqualTo(Arrays.asList("field2", "field3"));
		Map<String, String[]> statsSelectiveFacets = method.getStatsSelectiveFacets();
		assertThat(statsSelectiveFacets.size()).isEqualTo(1);
		assertThat(statsSelectiveFacets.get("field4")).isEqualTo(new String[] { "field4_1", "field4_2" });
	}

	@Test // DATASOLR-160
	public void testHasStatsDefinition() throws Exception {

		assertThat(getQueryMethodByName("findByNameWithEmptyStats", String.class).hasStatsDefinition()).isFalse();

		assertThat(getQueryMethodByName("findByNameWithFieldStats", String.class).hasStatsDefinition()).isTrue();
		assertThat(getQueryMethodByName("findByNameWithFieldAndFacetStats", String.class).hasStatsDefinition()).isTrue();
		assertThat(getQueryMethodByName("findByNameWithSelectiveFacetStats", String.class).hasStatsDefinition()).isTrue();
		assertThat(getQueryMethodByName("findByNameWithFieldStatsAndFacetsStatsAndSelectiveFacetStats", String.class)
				.hasStatsDefinition()).isTrue();
	}

	/**
	 * DATASOLR-137
	 */
	@Test
	public void shouldApplySpellcheckCorrectly() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByNameWithDefaultSpellcheck", String.class);

		assertThat(method.hasSpellcheck()).isTrue();
		SpellcheckOptions options = method.getSpellcheckOptions();

		assertThat(options.getDictionary()).isNull();
		assertThat(options.getAccuracy()).isNull();
		assertThat(options.getAlternativeTermCount()).isNull();
		assertThat(options.getCollate()).isFalse();
		assertThat(options.getCollateExtendedResults()).isFalse();
		assertThat(options.getMaxCollationCollectDocs()).isNull();
		assertThat(options.getCollateParams().size()).isEqualTo(0);
		assertThat(options.getCount()).isNull();
		assertThat(options.getDictionary()).isNull();
		assertThat(options.getMaxCollationEvaluations()).isNull();
		assertThat(options.getMaxCollations()).isNull();
		assertThat(options.getMaxResultsForSuggest()).isNull();
		assertThat(options.getOnlyMorePopular()).isFalse();
		assertThat(options.getQuery()).isNull();
	}

	/**
	 * DATASOLR-137
	 */
	@Test
	public void shouldApplySpellcheckWithOptionsCorrectly() throws Exception {

		SolrQueryMethod method = getQueryMethodByName("findByNameWithSpellcheckOptions", String.class);

		assertThat(method.hasSpellcheck()).isTrue();
		SpellcheckOptions options = method.getSpellcheckOptions();

		assertThat(options.getAccuracy().doubleValue()).isCloseTo(0.5D, offset(0.0D));
		assertThat(options.getAlternativeTermCount()).isEqualTo(10L);
		assertThat(options.getCollate()).isTrue();
		assertThat(options.getCollateExtendedResults()).isTrue();
		assertThat(options.getMaxCollationCollectDocs()).isEqualTo(10L);
		assertThat(options.getCollateParams().size()).isEqualTo(0);
		assertThat(options.getCount()).isEqualTo(100L);
		assertThat(options.getDictionary()).isEqualTo(new String[] { "myDict" });
		assertThat(options.getMaxCollationEvaluations()).isEqualTo(5L);
		assertThat(options.getMaxCollations()).isEqualTo(3L);
		assertThat(options.getMaxResultsForSuggest()).isEqualTo(7L);
		assertThat(options.getOnlyMorePopular()).isTrue();
		assertThat(options.getQuery()).isNull();
		assertThat(options.getExtendedResults()).isTrue();
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
