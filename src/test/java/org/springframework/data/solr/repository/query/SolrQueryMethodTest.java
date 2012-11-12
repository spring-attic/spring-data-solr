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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.repository.ProductBean;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.support.SolrEntityInformationCreatorImpl;

/**
 * @author Christoph Strobl
 */
public class SolrQueryMethodTest {

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
		Assert.assertEquals("name:?0", method.getAnnotatedQuery());
	}

	@Test
	public void testAnnotatedQueryUsageWithoutExplicitAttribute() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQueryWithoutExplicitAttribute", String.class);
		Assert.assertTrue(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertEquals("name:?0", method.getAnnotatedQuery());
	}

	@Test
	public void testAnnotatedNamedQueryNameUsage() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedNamedQueryName", String.class);
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertTrue(method.hasAnnotatedNamedQueryName());
		Assert.assertEquals("ProductRepository.namedQuery-1", method.getAnnotatedNamedQueryName());
	}

	@Test
	public void testWithoutAnnotation() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByName", String.class);
		Assert.assertFalse(method.hasAnnotatedQuery());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
	}

	@Test
	public void testWithSingleFieldProjection() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQueryWithProjectionOnSingleField", String.class);
		Assert.assertTrue(method.hasAnnotatedQuery());
		Assert.assertTrue(method.hasProjectionFields());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertEquals("name:?0", method.getAnnotatedQuery());
	}

	@Test
	public void testWithMultipleFieldsProjection() throws Exception {
		SolrQueryMethod method = getQueryMethodByName("findByAnnotatedQueryWithProjectionOnMultipleFields", String.class);
		Assert.assertTrue(method.hasAnnotatedQuery());
		Assert.assertTrue(method.hasProjectionFields());
		Assert.assertEquals(2, method.getProjectionFields().size());
		Assert.assertFalse(method.hasAnnotatedNamedQueryName());
		Assert.assertEquals("name:?0", method.getAnnotatedQuery());
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

	}

}
