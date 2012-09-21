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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.solr.SolrServerFactory;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.repository.ProductBean;
import org.springframework.data.solr.repository.Query;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class StringBasedSolrQueryTest {

	@Mock
	private SolrOperations solrOperationsMock;

	@Mock
	private RepositoryMetadata metadataMock;

	@Mock
	private SolrEntityInformationCreator entityInformationCreatorMock;

	@Mock
	SolrServerFactory solrServerFactoryMock;

	@Test
	public void testQueryCreationSingleProperty() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByText", String.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);

		org.springframework.data.solr.core.query.Query query = solrQuery.createQuery(new SolrParametersParameterAccessor(
				queryMethod, new Object[] { "j73x73r" }));

		Assert.assertEquals("textGeneral:j73x73r", query.getCriteria().getQueryString());
	}

	@Test
	public void testQueryCreationWithNegativeProperty() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityAndPrice", Integer.class, Float.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);

		org.springframework.data.solr.core.query.Query query = solrQuery.createQuery(new SolrParametersParameterAccessor(
				queryMethod, new Object[] { Integer.valueOf(-1), Float.valueOf(-2f) }));

		Assert.assertEquals("popularity:\\-1 AND price:\\-2.0", query.getCriteria().getQueryString());
	}

	@Test
	public void testQueryCreationMultiyProperty() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityAndPrice", Integer.class, Float.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);

		org.springframework.data.solr.core.query.Query query = solrQuery.createQuery(new SolrParametersParameterAccessor(
				queryMethod, new Object[] { Integer.valueOf(1), Float.valueOf(2f) }));

		Assert.assertEquals("popularity:1 AND price:2.0", query.getCriteria().getQueryString());
	}

	@Test
	public void testQueryCreationWithNullProperty() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByText", String.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);

		org.springframework.data.solr.core.query.Query query = solrQuery.createQuery(new SolrParametersParameterAccessor(
				queryMethod, new Object[] { null }));

		Assert.assertEquals("textGeneral:null", query.getCriteria().getQueryString());
	}

	private interface SampleRepository {

		@Query("textGeneral:?0")
		ProductBean findByText(String text);

		@Query("popularity:?0 AND price:?1")
		ProductBean findByPopularityAndPrice(Integer popularity, Float price);

	}

}
