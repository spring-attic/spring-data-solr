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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.repository.ProductBean;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrQueryCreatorTest {

	@Mock
	private RepositoryMetadata metadataMock;

	@Mock
	private SolrEntityInformationCreator entityInformationCreatorMock;

	private MappingContext<?, SolrPersistentProperty> mappingContext;

	@Before
	public void setUp() {
		mappingContext = new SimpleSolrMappingContext();
	}

	@Test
	public void testCreateFindBySingleCriteria() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularity", Integer.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100 }), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("popularity:100", criteria.getQueryString());
	}

	@Test
	public void testCreateFindByNotCriteria() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityIsNot", Integer.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100 }), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("popularity:-100", criteria.getQueryString());
	}

	@Test
	public void testCreateFindByAndQuery() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityAndPrice", Integer.class, Float.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100, 200f }), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("popularity:100 AND price:200.0", criteria.getQueryString());
	}

	@Test
	public void testCreateFindByOrQuery() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityOrPrice", Integer.class, Float.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100, 200f }), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("popularity:100 OR price:200.0", criteria.getQueryString());
	}

	@Test
	public void testCreateQueryWithTrueClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByAvailableTrue");
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] {}), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("inStock:true", criteria.getQueryString());
	}

	@Test
	public void testCreateQueryWithFalseClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByAvailableFalse");
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] {}), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("inStock:false", criteria.getQueryString());
	}

	@Test
	public void testCreateQueryWithStartsWithClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleStartingWith", String.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "j73x73r" }), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("title:j73x73r*", criteria.getQueryString());
	}

	@Test
	public void testCreateQueryWithEndingWithClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleEndingWith", String.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "christoph" }), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("title:*christoph", criteria.getQueryString());
	}

	@Test
	public void testCreateQueryWithContainingClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleContaining", String.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "solr" }), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("title:*solr*", criteria.getQueryString());
	}

	@Test
	public void testCreateQueryWithRegexClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleRegex", String.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "(\\+ \\*)" }), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("title:(\\+ \\*)", criteria.getQueryString());
	}

	@Test
	public void testCreateQueryWithBetweenClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityBetween", Integer.class, Integer.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100, 200 }), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("popularity:[100 TO 200]", criteria.getQueryString());
	}

	@Test
	public void testCreateQueryWithLessThanClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPriceLessThan", Float.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100f }), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("price:[* TO 100.0]", criteria.getQueryString());
	}

	@Test
	public void testCreateQueryWithGreaterThanClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPriceGreaterThan", Float.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 10f }), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("price:[10.0 TO *]", criteria.getQueryString());
	}

	@Test
	public void testCreateQueryWithInClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityIn", Integer[].class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { new Object[] { 1, 2, 3 } }), mappingContext);

		Query query = creator.createQuery();

		Criteria criteria = query.getCriteria();
		Assert.assertEquals("popularity:(1 2 3)", criteria.getQueryString());
	}

	@Test
	public void testCreateQueryWithSortDesc() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityOrderByTitleDesc", Integer.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 1 }), mappingContext);

		Query query = creator.createQuery();
		Assert.assertNotNull(query.getSort());
		Assert.assertEquals(Sort.Direction.DESC, query.getSort().getOrderFor("title").getDirection());
	}

	private interface SampleRepository {

		ProductBean findByPopularity(Integer popularity);

		ProductBean findByPopularityIsNot(Integer popularity);

		ProductBean findByPopularityAndPrice(Integer popularity, Float price);

		ProductBean findByPopularityOrPrice(Integer popularity, Float price);

		ProductBean findByAvailableTrue();

		ProductBean findByAvailableFalse();

		ProductBean findByTitleStartingWith(String prefix);

		ProductBean findByTitleEndingWith(String postfix);

		ProductBean findByTitleContaining(String fragment);

		ProductBean findByTitleRegex(String expression);

		ProductBean findByPopularityBetween(Integer lower, Integer upper);

		ProductBean findByPriceLessThan(Float price);

		ProductBean findByPriceGreaterThan(Float price);

		ProductBean findByPopularityIn(Integer... values);

		ProductBean findByPopularityOrderByTitleDesc(Integer popularity);

	}

}
