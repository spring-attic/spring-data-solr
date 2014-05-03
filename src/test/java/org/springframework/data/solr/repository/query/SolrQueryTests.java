/*
 * Copyright 2012 - 2013 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.apache.solr.common.params.HighlightParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.solr.core.SolrCallback;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.PivotField;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.repository.Facet;
import org.springframework.data.solr.repository.Highlight;
import org.springframework.data.solr.repository.ProductBean;
import org.springframework.data.solr.repository.SolrCrudRepository;
import org.springframework.data.solr.repository.support.MappingSolrEntityInformation;

/**
 * @author Christoph Strobl
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrQueryTests {

	@Mock
	private RepositoryMetadata metadataMock;

	@Mock
	private SolrOperations solrOperationsMock;

	@Mock
	private SolrPersistentEntity<ProductBean> persitentEntityMock;

	private SolrEntityInformationCreator entityInformationCreator;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() {
		entityInformationCreator = new SolrEntityInformationCreatorImpl();
		Mockito.when(persitentEntityMock.getType()).thenReturn(ProductBean.class);
		Mockito.when(solrOperationsMock.execute(Matchers.any(SolrCallback.class))).thenReturn(
				new PageImpl<ProductBean>(Collections.<ProductBean> emptyList()));
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void testQueryWithHighlightAndFaceting() throws NoSuchMethodException, SecurityException {
		createQueryForMethod("findAndApplyHighlightingAndFaceting", Pageable.class).execute(
				new Object[] { new PageRequest(0, 10) });
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testQueryWithHighlight() {
		ArgumentCaptor<HighlightQuery> captor = ArgumentCaptor.forClass(HighlightQuery.class);

		createQueryForMethod("findAndApplyHighlighting", Pageable.class).execute(new Object[] { new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForHighlightPage(captor.capture(),
				(Class<ProductBean>) Matchers.any());

		HighlightOptions capturedOptions = captor.getValue().getHighlightOptions();
		Assert.assertNotNull(capturedOptions);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testQueryWithHighlightParameters() {
		ArgumentCaptor<HighlightQuery> captor = ArgumentCaptor.forClass(HighlightQuery.class);

		createQueryForMethod("findAndApplyHighlightingAllParameters", Pageable.class).execute(
				new Object[] { new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForHighlightPage(captor.capture(),
				(Class<ProductBean>) Matchers.any());

		HighlightOptions capturedOptions = captor.getValue().getHighlightOptions();
		Assert.assertNotNull(capturedOptions);
		Assert.assertEquals("<b>", capturedOptions.getSimplePrefix());
		Assert.assertEquals("</b>", capturedOptions.getSimplePostfix());
		Assert.assertEquals("name", capturedOptions.getFields().get(0).getName());
		Assert.assertEquals("description", capturedOptions.getFields().get(1).getName());
		Assert.assertEquals("simple", capturedOptions.getFormatter());
		Assert.assertEquals(Integer.valueOf(10), capturedOptions.getFragsize());
		Assert.assertEquals(Integer.valueOf(20), capturedOptions.getNrSnipplets());
		Assert
				.assertEquals("name:with", ((SimpleStringCriteria) capturedOptions.getQuery().getCriteria()).getQueryString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testQueryWithParametrizedHighlightQuery() {
		ArgumentCaptor<HighlightQuery> captor = ArgumentCaptor.forClass(HighlightQuery.class);

		createQueryForMethod("findAndApplyHighlightingWithParametrizedHighlightQuery", String.class, Pageable.class)
				.execute(new Object[] { "spring", new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForHighlightPage(captor.capture(),
				(Class<ProductBean>) Matchers.any());

		HighlightOptions capturedOptions = captor.getValue().getHighlightOptions();
		Assert.assertEquals("name:*spring*",
				((SimpleStringCriteria) capturedOptions.getQuery().getCriteria()).getQueryString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testQueryWithNonDefaultHighlightFormatter() {
		ArgumentCaptor<HighlightQuery> captor = ArgumentCaptor.forClass(HighlightQuery.class);

		createQueryForMethod("findAndApplyHighlightingWithNonDefaultFormatter", Pageable.class).execute(
				new Object[] { new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForHighlightPage(captor.capture(),
				(Class<ProductBean>) Matchers.any());

		HighlightOptions capturedOptions = captor.getValue().getHighlightOptions();
		Assert.assertNotNull(capturedOptions);
		Assert.assertNull(capturedOptions.getSimplePrefix());
		Assert.assertNull(capturedOptions.getSimplePrefix());
		Assert.assertNull(capturedOptions.getSimplePostfix());
		Assert.assertEquals("postingshighlighter", capturedOptions.getFormatter());
		Assert.assertEquals("{pre}", capturedOptions.getHighlightParameterValue(HighlightParams.TAG_PRE));
		Assert.assertEquals("{post}", capturedOptions.getHighlightParameterValue(HighlightParams.TAG_POST));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testQueryWithPivotFields() {
		ArgumentCaptor<FacetQuery> captor = ArgumentCaptor.forClass(FacetQuery.class);

		createQueryForMethod("findTitleVsName", Pageable.class).execute(new Object[] { new PageRequest(0, 10) });

		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForFacetPage(captor.capture(),
				(Class<ProductBean>) Matchers.any());

		FacetOptions capturedOptions = captor.getValue().getFacetOptions();
		
		List<PivotField> pivots = capturedOptions.getFacetOnPivots();
		Assert.assertNotNull(capturedOptions);
		Assert.assertEquals(1, pivots.size());
		
		List<Field> pivotFields = pivots.get(0).getFields();
		Assert.assertEquals(2, pivotFields.size());
		Assert.assertEquals("title", pivotFields.get(0).getName());
		Assert.assertEquals("name", pivotFields.get(1).getName());
	}

	private RepositoryQuery createQueryForMethod(String methodName, Class<?>... paramTypes) {
		try {
			return this.createQueryForMethod(Repo1.class.getMethod(methodName, paramTypes));
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	private RepositoryQuery createQueryForMethod(Method method) {
		return new SolrQueryImpl(this.solrOperationsMock, createSolrQueryMethodFrom(method));
	}

	private SolrQueryMethod createSolrQueryMethodFrom(Method method) {
		return new SolrQueryMethod(method, metadataMock, entityInformationCreator);
	}

	private interface Repo1 extends SolrCrudRepository<ProductBean, String> {

		@Facet(fields = { "name" })
		@Highlight
		Page<ProductBean> findAndApplyHighlightingAndFaceting(Pageable page);

		@Highlight
		Page<ProductBean> findAndApplyHighlighting(Pageable page);

		@Highlight(fields = { "name", "description" }, fragsize = 10, snipplets = 20, prefix = "<b>", postfix = "</b>", query = "name:with", formatter = "simple")
		Page<ProductBean> findAndApplyHighlightingAllParameters(Pageable page);

		@Highlight(query = "name:*?0*")
		Page<ProductBean> findAndApplyHighlightingWithParametrizedHighlightQuery(String name, Pageable page);

		@Highlight(formatter = "postingshighlighter", prefix = "{pre}", postfix = "{post}")
		Page<ProductBean> findAndApplyHighlightingWithNonDefaultFormatter(Pageable page);
		
		@Facet(pivotFields={"title,name"})
		FacetPage<ProductBean> findTitleVsName(Pageable page);

	}

	private class SolrEntityInformationCreatorImpl implements SolrEntityInformationCreator {

		@SuppressWarnings("unchecked")
		@Override
		public <T, ID extends Serializable> SolrEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
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
			super(solrOperations, solrQueryMethod);
		}

		@Override
		protected Query createQuery(SolrParameterAccessor parameterAccessor) {
			return new SimpleQuery(new SimpleStringCriteria("fake:query"));
		}

	}

}
