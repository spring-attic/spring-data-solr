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

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.QueryParser;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.repository.ProductBean;
import org.springframework.data.solr.repository.Query;

/**
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class StringBasedSolrQueryTests {

	private @Mock SolrOperations solrOperationsMock;
	private @Mock SolrEntityInformationCreator entityInformationCreatorMock;

	private RepositoryMetadata metadata = AbstractRepositoryMetadata.getMetadata(SampleRepository.class);
	private ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
	private QueryParser queryParser;

	@Before
	public void setUp() {
		this.queryParser = new DefaultQueryParser(new SimpleSolrMappingContext());
	}

	@Test
	public void testQueryCreationSingleProperty() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByText", String.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, factory, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);

		org.springframework.data.solr.core.query.Query query = solrQuery
				.createQuery(new SolrParametersParameterAccessor(queryMethod, new Object[] { "j73x73r" }));

		assertThat(queryParser.getQueryString(query, ProductBean.class)).isEqualTo("textGeneral:j73x73r");
	}

	@Test
	public void testQueryCreationWithNegativeProperty() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityAndPrice", Integer.class, Float.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, factory, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);

		org.springframework.data.solr.core.query.Query query = solrQuery.createQuery(
				new SolrParametersParameterAccessor(queryMethod, new Object[] { Integer.valueOf(-1), Float.valueOf(-2f) }));

		assertThat(queryParser.getQueryString(query, ProductBean.class)).isEqualTo("popularity:\\-1 AND price:\\-2.0");
	}

	@Test
	public void testQueryCreationMultiyProperty() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityAndPrice", Integer.class, Float.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, factory, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);

		org.springframework.data.solr.core.query.Query query = solrQuery.createQuery(
				new SolrParametersParameterAccessor(queryMethod, new Object[] { Integer.valueOf(1), Float.valueOf(2f) }));

		assertThat(queryParser.getQueryString(query, ProductBean.class)).isEqualTo("popularity:1 AND price:2.0");
	}

	@Test
	public void testQueryCreationWithNullProperty() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByText", String.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, factory, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);

		org.springframework.data.solr.core.query.Query query = solrQuery
				.createQuery(new SolrParametersParameterAccessor(queryMethod, new Object[] { null }));

		assertThat(queryParser.getQueryString(query, ProductBean.class)).isEqualTo("textGeneral:null");
	}

	@Test
	public void testWithPointProperty() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLocationNear", Point.class, Distance.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, factory, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);

		org.springframework.data.solr.core.query.Query query = solrQuery.createQuery(new SolrParametersParameterAccessor(
				queryMethod, new Object[] { new Point(48.303056, 14.290556), new Distance(5) }));

		assertThat(queryParser.getQueryString(query, ProductBean.class))
				.isEqualTo("{!geofilt pt=48.303056,14.290556 sfield=store d=5.0}");
	}

	@Test
	public void testWithPointPropertyWhereDistanceIsInMiles() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLocationNear", Point.class, Distance.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, factory, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);

		org.springframework.data.solr.core.query.Query query = solrQuery.createQuery(new SolrParametersParameterAccessor(
				queryMethod, new Object[] { new Point(48.303056, 14.290556), new Distance(1, Metrics.MILES) }));

		assertThat(queryParser.getQueryString(query, ProductBean.class))
				.isEqualTo("{!geofilt pt=48.303056,14.290556 sfield=store d=1.609344}");
	}

	@Test
	public void testWithProjectionOnSingleField() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByNameProjectionOnPopularity", String.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, factory, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);

		org.springframework.data.solr.core.query.Query query = solrQuery
				.createQuery(new SolrParametersParameterAccessor(queryMethod, new Object[] { "christoph" }));

		assertThat(queryParser.getQueryString(query, ProductBean.class)).isEqualTo("name:christoph*");
		assertThat(query.getProjectionOnFields().size()).isEqualTo(1);
		assertThat(query.getProjectionOnFields().get(0).getName()).isEqualTo("popularity");
	}

	@Test
	public void testWithProjectionOnMultipleFields() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByNameProjectionOnPopularityAndPrice", String.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, factory, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);

		org.springframework.data.solr.core.query.Query query = solrQuery
				.createQuery(new SolrParametersParameterAccessor(queryMethod, new Object[] { "strobl" }));

		assertThat(queryParser.getQueryString(query, ProductBean.class)).isEqualTo("name:strobl*");
		assertThat(query.getProjectionOnFields().size()).isEqualTo(2);
		assertThat(query.getProjectionOnFields().get(0).getName()).isEqualTo("popularity");
		assertThat(query.getProjectionOnFields().get(1).getName()).isEqualTo("price");
	}

	@Test
	public void testWithSort() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByNameWithSort", String.class, Sort.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, factory, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);
		Sort sort = Sort.by(Direction.DESC, "popularity", "price");

		org.springframework.data.solr.core.query.Query query = solrQuery
				.createQuery(new SolrParametersParameterAccessor(queryMethod, new Object[] { "spring", sort }));

		assertThat(queryParser.getQueryString(query, ProductBean.class)).isEqualTo("name:spring");
		assertThat(query.getSort()).isEqualTo(sort);
	}

	@Test
	public void testWithSortInPageable() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByNameWithSortInPageable", String.class, Pageable.class);
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, factory, entityInformationCreatorMock);

		StringBasedSolrQuery solrQuery = new StringBasedSolrQuery(queryMethod, solrOperationsMock);
		Sort sort = Sort.by(Direction.DESC, "popularity", "price");

		org.springframework.data.solr.core.query.Query query = solrQuery.createQuery(
				new SolrParametersParameterAccessor(queryMethod, new Object[] { "spring", PageRequest.of(0, 10, sort) }));

		assertThat(queryParser.getQueryString(query, ProductBean.class)).isEqualTo("name:spring");
		assertThat(query.getSort()).isEqualTo(sort);
	}

	private interface SampleRepository extends Repository<ProductBean, String> {

		@Query("textGeneral:?0")
		ProductBean findByText(String text);

		@Query("popularity:?0 AND price:?1")
		ProductBean findByPopularityAndPrice(Integer popularity, Float price);

		@Query("{!geofilt pt=?0 sfield=store d=?1}")
		ProductBean findByLocationNear(Point location, Distance distace);

		@Query(value = "name:?0*", fields = "popularity")
		ProductBean findByNameProjectionOnPopularity(String name);

		@Query(value = "name:?0*", fields = { "popularity", "price" })
		ProductBean findByNameProjectionOnPopularityAndPrice(String name);

		@Query(value = "name:?0")
		ProductBean findByNameWithSort(String name, Sort sort);

		@Query(value = "name:?0")
		Page<ProductBean> findByNameWithSortInPageable(String name, Pageable pageable);
	}
}
