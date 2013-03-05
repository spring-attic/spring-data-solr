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
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import org.springframework.data.solr.core.QueryParser;
import org.springframework.data.solr.core.geo.BoundingBox;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.Distance.Unit;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.repository.ProductBean;

/**
 * @author Christoph Strobl
 * @author John Dorman
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrQueryCreatorTests {

	@Mock
	private RepositoryMetadata metadataMock;

	@Mock
	private SolrEntityInformationCreator entityInformationCreatorMock;

	private MappingContext<?, SolrPersistentProperty> mappingContext;

	private QueryParser queryParser;

	@Before
	public void setUp() {
		mappingContext = new SimpleSolrMappingContext();
		queryParser = new QueryParser();
	}

	@Test
	public void testCreateFindBySingleCriteria() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularity", Integer.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100 }), mappingContext);

		Query query = creator.createQuery();
		Assert.assertEquals("popularity:100", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateFindByNotCriteria() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityIsNot", Integer.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100 }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("-popularity:100", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateFindByNotNullCriteria() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityIsNotNull");
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] {}), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("popularity:[* TO *]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateFindByNullCriteria() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityIsNull");
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] {}), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("-popularity:[* TO *]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateFindByAndQuery() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityAndPrice", Integer.class, Float.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100, 200f }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("popularity:100 AND price:200.0", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateFindByOrQuery() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityOrPrice", Integer.class, Float.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100, 200f }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("popularity:100 OR price:200.0", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithTrueClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByAvailableTrue");
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] {}), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("inStock:true", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithFalseClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByAvailableFalse");
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] {}), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("inStock:false", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithLikeClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleLike", String.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "j73x73r" }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("title:j73x73r*", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithLikeNotClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleNotLike", String.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "j73x73r" }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("-title:j73x73r*", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithStartsWithClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleStartingWith", String.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "j73x73r" }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("title:j73x73r*", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithEndingWithClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleEndingWith", String.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "christoph" }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("title:*christoph", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithContainingClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleContaining", String.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "solr" }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("title:*solr*", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithRegexClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleRegex", String.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "(\\+ \\*)" }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("title:(\\+ \\*)", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithBetweenClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityBetween", Integer.class, Integer.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100, 200 }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("popularity:[100 TO 200]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithBeforeClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLastModifiedBefore", Date.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { new DateTime(2012, 10, 15, 5, 31, 0, DateTimeZone.UTC) }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("last_modified:[* TO 2012\\-10\\-15T05\\:31\\:00.000Z}", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithLessThanClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPriceLessThan", Float.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100f }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("price:[* TO 100.0}", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithLessThanEqualsClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPriceLessThanEqual", Float.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 100f }), mappingContext);

		Query query = creator.createQuery();

		Assert.assertEquals("price:[* TO 100.0]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithAfterClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLastModifiedAfter", Date.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { new DateTime(2012, 10, 15, 5, 31, 0, DateTimeZone.UTC) }), mappingContext);

		Query query = creator.createQuery();
		Assert.assertEquals("last_modified:{2012\\-10\\-15T05\\:31\\:00.000Z TO *]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithGreaterThanClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPriceGreaterThan", Float.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 10f }), mappingContext);

		Query query = creator.createQuery();
		Assert.assertEquals("price:{10.0 TO *]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithGreaterThanEqualsClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPriceGreaterThanEqual", Float.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { 10f }), mappingContext);

		Query query = creator.createQuery();
		Assert.assertEquals("price:[10.0 TO *]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithInClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityIn", Integer[].class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { new Object[] { 1, 2, 3 } }), mappingContext);

		Query query = creator.createQuery();
		Assert.assertEquals("popularity:(1 2 3)", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithNotInClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityNotIn", Integer[].class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { new Object[] { 1, 2, 3 } }), mappingContext);

		Query query = creator.createQuery();
		Assert.assertEquals("-popularity:(1 2 3)", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithNear() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLocationNear", GeoLocation.class, Distance.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { new GeoLocation(48.303056, 14.290556), new Distance(5) }), mappingContext);

		Query query = creator.createQuery();
		Assert.assertEquals("{!bbox pt=48.303056,14.290556 sfield=store d=5.0}", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithNearWhereUnitIsMiles() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLocationNear", GeoLocation.class, Distance.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { new GeoLocation(48.303056, 14.290556), new Distance(1, Unit.MILES) }), mappingContext);

		Query query = creator.createQuery();
		Assert.assertEquals("{!bbox pt=48.303056,14.290556 sfield=store d=1.609344}", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithWithin() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLocationWithin", GeoLocation.class, Distance.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { new GeoLocation(48.303056, 14.290556), new Distance(5) }), mappingContext);

		Query query = creator.createQuery();
		Assert.assertEquals("{!geofilt pt=48.303056,14.290556 sfield=store d=5.0}", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithWithinWhereUnitIsMiles() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLocationWithin", GeoLocation.class, Distance.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod,
				new Object[] { new GeoLocation(48.303056, 14.290556), new Distance(1, Unit.MILES) }), mappingContext);

		Query query = creator.createQuery();
		Assert.assertEquals("{!geofilt pt=48.303056,14.290556 sfield=store d=1.609344}", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithNearUsingBoundingBox() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLocationNear", BoundingBox.class);
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());

		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadataMock, entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree,
				new SolrParametersParameterAccessor(queryMethod, new Object[] { new BoundingBox(new GeoLocation(48.303056,
						14.290556), new GeoLocation(48.306377, 14.283128)) }), mappingContext);

		Query query = creator.createQuery();
		Assert.assertEquals("store:[48.303056,14.290556 TO 48.306377,14.283128]", queryParser.getQueryString(query));
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

		ProductBean findByPopularityIsNotNull();

		ProductBean findByPopularityIsNull();

		ProductBean findByPopularityAndPrice(Integer popularity, Float price);

		ProductBean findByPopularityOrPrice(Integer popularity, Float price);

		ProductBean findByAvailableTrue();

		ProductBean findByAvailableFalse();

		ProductBean findByTitleLike(String prefix);

		ProductBean findByTitleNotLike(String prefix);

		ProductBean findByTitleStartingWith(String prefix);

		ProductBean findByTitleEndingWith(String postfix);

		ProductBean findByTitleContaining(String fragment);

		ProductBean findByTitleRegex(String expression);

		ProductBean findByPopularityBetween(Integer lower, Integer upper);

		ProductBean findByLastModifiedBefore(Date date);

		ProductBean findByPriceLessThan(Float price);

		ProductBean findByPriceLessThanEqual(Float price);

		ProductBean findByLastModifiedAfter(Date date);

		ProductBean findByPriceGreaterThan(Float price);

		ProductBean findByPriceGreaterThanEqual(Float price);

		ProductBean findByPopularityIn(Integer... values);

		ProductBean findByPopularityNotIn(Integer... values);

		ProductBean findByPopularityOrderByTitleDesc(Integer popularity);

		ProductBean findByLocationWithin(GeoLocation location, Distance distance);

		ProductBean findByLocationNear(GeoLocation location, Distance distance);

		ProductBean findByLocationNear(BoundingBox bbox);

	}

}
