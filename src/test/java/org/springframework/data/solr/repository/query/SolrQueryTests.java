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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.solr.common.params.HighlightParams;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.solr.core.SolrCallback;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.convert.MappingSolrConverter;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.StatsOptions;
import org.springframework.data.solr.repository.Facet;
import org.springframework.data.solr.repository.Highlight;
import org.springframework.data.solr.repository.ProductBean;
import org.springframework.data.solr.repository.SelectiveStats;
import org.springframework.data.solr.repository.SolrCrudRepository;
import org.springframework.data.solr.repository.Stats;
import org.springframework.data.solr.repository.support.MappingSolrEntityInformation;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SolrQueryTests {

	private @Mock SolrOperations solrOperationsMock;
	private @Mock SolrPersistentEntity<ProductBean> persitentEntityMock;

	private SolrEntityInformationCreator entityInformationCreator;
	private RepositoryMetadata metadataMock = AbstractRepositoryMetadata.getMetadata(Repo1.class);
	private SimpleSolrMappingContext mappingContext;
	private SolrConverter solrConverter;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() {

		mappingContext = new SimpleSolrMappingContext();
		solrConverter = new MappingSolrConverter(mappingContext);
		entityInformationCreator = new SolrEntityInformationCreatorImpl();
		Mockito.when(persitentEntityMock.getType()).thenReturn(ProductBean.class);
		Mockito.when(solrOperationsMock.execute(Mockito.any(SolrCallback.class)))
				.thenReturn(new PageImpl<>(Collections.<ProductBean> emptyList()));
		Mockito.when(solrOperationsMock.getConverter()).thenReturn(solrConverter);
	}

	@Test
	public void testQueryWithHighlightAndFaceting() throws NoSuchMethodException, SecurityException {
		createQueryForMethod("findAndApplyHighlightingAndFaceting", Pageable.class)
				.execute(new Object[] { new PageRequest(0, 10) });
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testQueryWithHighlight() {
		ArgumentCaptor<HighlightQuery> captor = ArgumentCaptor.forClass(HighlightQuery.class);

		createQueryForMethod("findAndApplyHighlighting", Pageable.class).execute(new Object[] { new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForHighlightPage(Mockito.eq("collection-1"),
				captor.capture(), (Class<ProductBean>) Mockito.any());

		HighlightOptions capturedOptions = captor.getValue().getHighlightOptions();
		Assert.assertNotNull(capturedOptions);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testQueryWithHighlightParameters() {
		ArgumentCaptor<HighlightQuery> captor = ArgumentCaptor.forClass(HighlightQuery.class);

		createQueryForMethod("findAndApplyHighlightingAllParameters", Pageable.class)
				.execute(new Object[] { new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForHighlightPage(Mockito.eq("collection-1"),
				captor.capture(), (Class<ProductBean>) Mockito.any());

		HighlightOptions capturedOptions = captor.getValue().getHighlightOptions();
		Assert.assertNotNull(capturedOptions);
		Assert.assertEquals("<b>", capturedOptions.getSimplePrefix());
		Assert.assertEquals("</b>", capturedOptions.getSimplePostfix());
		Assert.assertEquals("name", capturedOptions.getFields().get(0).getName());
		Assert.assertEquals("description", capturedOptions.getFields().get(1).getName());
		Assert.assertEquals("simple", capturedOptions.getFormatter());
		Assert.assertEquals(Integer.valueOf(10), capturedOptions.getFragsize());
		Assert.assertEquals(Integer.valueOf(20), capturedOptions.getNrSnipplets());
		Assert.assertEquals("name:with",
				((SimpleStringCriteria) capturedOptions.getQuery().getCriteria()).getQueryString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testQueryWithParametrizedHighlightQuery() {
		ArgumentCaptor<HighlightQuery> captor = ArgumentCaptor.forClass(HighlightQuery.class);

		createQueryForMethod("findAndApplyHighlightingWithParametrizedHighlightQuery", String.class, Pageable.class)
				.execute(new Object[] { "spring", new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForHighlightPage(Mockito.eq("collection-1"),
				captor.capture(), (Class<ProductBean>) Mockito.any());

		HighlightOptions capturedOptions = captor.getValue().getHighlightOptions();
		Assert.assertEquals("name:*spring*",
				((SimpleStringCriteria) capturedOptions.getQuery().getCriteria()).getQueryString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testQueryWithNonDefaultHighlightFormatter() {
		ArgumentCaptor<HighlightQuery> captor = ArgumentCaptor.forClass(HighlightQuery.class);

		createQueryForMethod("findAndApplyHighlightingWithNonDefaultFormatter", Pageable.class)
				.execute(new Object[] { new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForHighlightPage(Mockito.eq("collection-1"),
				captor.capture(), (Class<ProductBean>) Mockito.any());

		HighlightOptions capturedOptions = captor.getValue().getHighlightOptions();
		Assert.assertNotNull(capturedOptions);
		Assert.assertNull(capturedOptions.getSimplePrefix());
		Assert.assertNull(capturedOptions.getSimplePrefix());
		Assert.assertNull(capturedOptions.getSimplePostfix());
		Assert.assertEquals("postingshighlighter", capturedOptions.getFormatter());
		Assert.assertEquals("{pre}", capturedOptions.getHighlightParameterValue(HighlightParams.TAG_PRE));
		Assert.assertEquals("{post}", capturedOptions.getHighlightParameterValue(HighlightParams.TAG_POST));
	}

	@Test // DATASOLR-170
	public void shouldApplyLimitCorrectlyWhenPageSizeToBig() throws NoSuchMethodException, SecurityException {

		Method method = Repo1.class.getMethod("findTop5ByName", String.class, Pageable.class);
		SolrQueryMethod sqm = createSolrQueryMethodFrom(method);

		PartTreeSolrQuery ptsq = new PartTreeSolrQuery("collection-1", sqm, this.solrOperationsMock);

		ptsq.execute(new Object[] { "foo", new PageRequest(0, 10) });

		ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForPage(Mockito.eq("collection-1"), captor.capture(),
				(Class<?>) Mockito.any());

		Assert.assertThat(captor.getValue().getPageRequest().getPageNumber(), IsEqual.equalTo(0));
		Assert.assertThat(captor.getValue().getPageRequest().getPageSize(), IsEqual.equalTo(5));
	}

	@Test // DATASOLR-170
	public void shouldApplyLimitCorrectlyToPageWhenPageInsideLimit() throws NoSuchMethodException, SecurityException {

		Method method = Repo1.class.getMethod("findTop5ByName", String.class, Pageable.class);
		SolrQueryMethod sqm = createSolrQueryMethodFrom(method);

		PartTreeSolrQuery ptsq = new PartTreeSolrQuery("collection-1", sqm, this.solrOperationsMock);

		ptsq.execute(new Object[] { "foo", new PageRequest(1, 2) });

		ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForPage(Mockito.eq("collection-1"), captor.capture(),
				(Class<?>) Mockito.any());

		Assert.assertThat(captor.getValue().getPageRequest().getPageNumber(), IsEqual.equalTo(1));
		Assert.assertThat(captor.getValue().getPageRequest().getPageSize(), IsEqual.equalTo(2));
	}

	@Test // DATASOLR-170
	public void shouldNotCallServerIfPageOutsideLimit() throws NoSuchMethodException, SecurityException {

		Method method = Repo1.class.getMethod("findTop5ByName", String.class, Pageable.class);
		SolrQueryMethod sqm = createSolrQueryMethodFrom(method);

		PartTreeSolrQuery ptsq = new PartTreeSolrQuery(sqm, this.solrOperationsMock);

		ptsq.execute(new Object[] { "foo", new PageRequest(2, 5) });

		Mockito.verify(solrOperationsMock, Mockito.never()).queryForPage(Mockito.eq("collection-1"),
				Mockito.any(Query.class), (Class<?>) Mockito.any());
	}

	@Test // DATASOLR-186
	public void sliceShouldTriggerPagedExecution() {

		createQueryForMethod("findByName", String.class, Pageable.class)
				.execute(new Object[] { "sliceme", new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForPage(Mockito.eq("collection-1"),
				Mockito.any(Query.class), Mockito.<Class<ProductBean>> any());
	}

	@SuppressWarnings("unchecked")
	@Test // DATASOLR-160
	public void testQueryWithStats() {
		ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

		createQueryForMethod("findAndApplyStats", Pageable.class).execute(new Object[] { new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForPage(Mockito.eq("collection-1"), captor.capture(),
				(Class<ProductBean>) Mockito.any());

		StatsOptions capturedOptions = captor.getValue().getStatsOptions();

		Assert.assertEquals(2, capturedOptions.getFields().size());
		Assert.assertTrue(
				capturedOptions.getFields().containsAll(Arrays.asList(new SimpleField("field1"), new SimpleField("field4"))));

		Assert.assertEquals(2, capturedOptions.getFacets().size());
		Assert.assertTrue(
				capturedOptions.getFacets().containsAll(Arrays.asList(new SimpleField("field2"), new SimpleField("field3"))));

		Collection<Field> selectiveFacetsField = capturedOptions.getSelectiveFacets().get(new SimpleField("field4"));
		List<SimpleField> selectiveFacetsFields = Arrays.asList(new SimpleField("field4_1"), new SimpleField("field4_2"));
		Assert.assertEquals(1, capturedOptions.getSelectiveFacets().size());
		Assert.assertTrue(selectiveFacetsField.containsAll(selectiveFacetsFields));
	}

	@SuppressWarnings("unchecked")
	@Test // DATASOLR-160
	public void testQueryWithStatsNonSelective() {
		ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

		createQueryForMethod("findAndApplyStatsNonSelective", Pageable.class)
				.execute(new Object[] { new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForPage(Mockito.eq("collection-1"), captor.capture(),
				(Class<ProductBean>) Mockito.any());

		StatsOptions capturedOptions = captor.getValue().getStatsOptions();

		Assert.assertEquals(1, capturedOptions.getFields().size());
		Assert.assertTrue(capturedOptions.getFields().containsAll(Collections.singletonList(new SimpleField("field1"))));

		Assert.assertEquals(2, capturedOptions.getFacets().size());
		Assert.assertTrue(
				capturedOptions.getFacets().containsAll(Arrays.asList(new SimpleField("field2"), new SimpleField("field3"))));

		Assert.assertThat(capturedOptions.getSelectiveFacets().entrySet(), IsEmptyIterable.emptyIterable());
	}

	@SuppressWarnings("unchecked")
	@Test // DATASOLR-160
	public void testQueryWithStatsNoFacets() {
		ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

		createQueryForMethod("findAndApplyStatsNoFacets", Pageable.class).execute(new Object[] { new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForPage(Mockito.eq("collection-1"), captor.capture(),
				(Class<ProductBean>) Mockito.any());

		StatsOptions capturedOptions = captor.getValue().getStatsOptions();

		Assert.assertEquals(1, capturedOptions.getFields().size());
		Assert.assertTrue(capturedOptions.getFields().containsAll(Collections.singletonList(new SimpleField("field1"))));

		Assert.assertThat(capturedOptions.getFacets(), IsEmptyIterable.emptyIterable());
		Assert.assertThat(capturedOptions.getSelectiveFacets().entrySet(), IsEmptyIterable.emptyIterable());
	}

	@Test // DATASOLR-402
	public void singleEntityExecutionShouldUseCollectionNameWhenReturningOptional() {

		createQueryForMethod("findAndReturnNotOptional").execute(new Object[] {});

		Mockito.verify(solrOperationsMock).queryForObject(Mockito.eq("collection-1"), Mockito.any(), Mockito.any());
	}

	private RepositoryQuery createQueryForMethod(String methodName, Class<?>... paramTypes) {
		try {
			return this.createQueryForMethod(Repo1.class.getMethod(methodName, paramTypes));
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	private RepositoryQuery createQueryForMethod(Method method) {
		return new SolrQueryImpl(this.solrOperationsMock, createSolrQueryMethodFrom(method));
	}

	private SolrQueryMethod createSolrQueryMethodFrom(Method method) {
		return new SolrQueryMethod(method, metadataMock, new SpelAwareProxyProjectionFactory(), entityInformationCreator);
	}

	private interface Repo1 extends SolrCrudRepository<ProductBean, String> {

		@Facet(fields = { "name" })
		@Highlight
		Page<ProductBean> findAndApplyHighlightingAndFaceting(Pageable page);

		@Highlight
		Page<ProductBean> findAndApplyHighlighting(Pageable page);

		@Highlight(fields = { "name", "description" }, fragsize = 10, snipplets = 20, prefix = "<b>", postfix = "</b>",
				query = "name:with", formatter = "simple")
		Page<ProductBean> findAndApplyHighlightingAllParameters(Pageable page);

		@Highlight(query = "name:*?0*")
		Page<ProductBean> findAndApplyHighlightingWithParametrizedHighlightQuery(String name, Pageable page);

		@Highlight(formatter = "postingshighlighter", prefix = "{pre}", postfix = "{post}")
		Page<ProductBean> findAndApplyHighlightingWithNonDefaultFormatter(Pageable page);

		Page<ProductBean> findTop5ByName(String name, Pageable page);

		Slice<ProductBean> findByName(String name, Pageable page);

		@Stats(value = "field1", facets = { "field2", "field3" }, //
				selective = @SelectiveStats(field = "field4", facets = { "field4_1", "field4_2" }))
		Page<ProductBean> findAndApplyStats(Pageable page);

		@Stats(value = "field1", facets = { "field2", "field3" })
		Page<ProductBean> findAndApplyStatsNonSelective(Pageable page);

		@Stats(value = "field1")
		Page<ProductBean> findAndApplyStatsNoFacets(Pageable page);

		ProductBean findAndReturnNotOptional();
	}

	private class SolrEntityInformationCreatorImpl implements SolrEntityInformationCreator {

		@SuppressWarnings("unchecked")
		@Override
		public <T, ID> SolrEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
			return (SolrEntityInformation<T, ID>) new SolrEntityInformationImpl(persitentEntityMock);
		}
	}

	private class SolrEntityInformationImpl extends MappingSolrEntityInformation<ProductBean, String> {

		public SolrEntityInformationImpl(SolrPersistentEntity<ProductBean> entity) {
			super(entity);
		}

		@Override
		public Class<String> getIdType() {
			return String.class;
		}

		@Override
		public Class<ProductBean> getJavaType() {
			return ProductBean.class;
		}

	}

	private class SolrQueryImpl extends AbstractSolrQuery {

		public SolrQueryImpl(SolrOperations solrOperations, SolrQueryMethod solrQueryMethod) {
			super("collection-1", solrOperations, solrQueryMethod);
		}

		@Override
		protected Query createQuery(SolrParameterAccessor parameterAccessor) {
			return new SimpleQuery(new SimpleStringCriteria("fake:query"));
		}

	}

}
