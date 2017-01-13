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
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.QueryParser;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.repository.ProductBean;

/**
 * @author Christoph Strobl
 * @author John Dorman
 * @author Francisco Spaeth
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrQueryCreatorTests {

	private @Mock SolrEntityInformationCreator entityInformationCreatorMock;

	private MappingContext<?, SolrPersistentProperty> mappingContext;
	private RepositoryMetadata metadata = AbstractRepositoryMetadata.getMetadata(SampleRepository.class);
	private QueryParser queryParser;

	@Before
	public void setUp() {
		mappingContext = new SimpleSolrMappingContext();
		queryParser = new DefaultQueryParser();
	}

	@Test
	public void testCreateFindBySingleCriteria() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularity", Integer.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { 100 });
		Assert.assertEquals("popularity:100", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateFindByNotCriteria() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityIsNot", Integer.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { 100 });
		Assert.assertEquals("-popularity:100", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateFindByNotNullCriteria() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityIsNotNull");

		Query query = createQueryForMethodWithArgs(method, new Object[] {});
		Assert.assertEquals("popularity:[* TO *]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateFindByNullCriteria() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityIsNull");

		Query query = createQueryForMethodWithArgs(method, new Object[] {});
		Assert.assertEquals("-popularity:[* TO *]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateFindByAndQuery() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityAndPrice", Integer.class, Float.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { 100, 200f });
		Assert.assertEquals("popularity:100 AND price:200.0", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateFindByOrQuery() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityOrPrice", Integer.class, Float.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { 100, 200f });
		Assert.assertEquals("popularity:100 OR price:200.0", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithTrueClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByAvailableTrue");

		Query query = createQueryForMethodWithArgs(method, new Object[] {});
		Assert.assertEquals("inStock:true", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithFalseClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByAvailableFalse");

		Query query = createQueryForMethodWithArgs(method, new Object[] {});
		Assert.assertEquals("inStock:false", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithLikeClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleLike", String.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { "j73x73r" });
		Assert.assertEquals("title:j73x73r*", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithLikeClauseUsingCollection() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleLike", Collection.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { Arrays.asList("one", "two", "three") });
		Assert.assertEquals("title:(one* two* three*)", queryParser.getQueryString(query));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateQueryWithInvalidCollectionType() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityLike", Collection.class);

		createQueryForMethodWithArgs(method, new Object[] { Arrays.asList(1L, 2L, 3L) });
	}

	@Test
	public void testCreateQueryWithArrayType() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityLike", String[].class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { new String[] { "one", "two", "three" } });
		Assert.assertEquals("popularity:(one* two* three*)", queryParser.getQueryString(query));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateQueryWithInvalidArrayType() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityLike", Long[].class);

		createQueryForMethodWithArgs(method, new Object[] { new Long[] { 1L, 2L, 3L } });
	}

	@Test
	public void testCreateQueryWithLikeNotClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleNotLike", String.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { "j73x73r" });
		Assert.assertEquals("-title:j73x73r*", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithLikeNotClauseUsingCollection() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleNotLike", Collection.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { Arrays.asList("one", "two", "three") });
		Assert.assertEquals("-title:(one* two* three*)", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithStartsWithClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleStartingWith", String.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { "j73x73r" });
		Assert.assertEquals("title:j73x73r*", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithStartsWithClauseUsingCollection() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleStartingWith", Collection.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { Arrays.asList("one", "two", "three") });
		Assert.assertEquals("title:(one* two* three*)", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithEndingWithClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleEndingWith", String.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { "christoph" });
		Assert.assertEquals("title:*christoph", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithEndingWithClauseUsingCollection() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleEndingWith", Collection.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { Arrays.asList("one", "two", "three") });
		Assert.assertEquals("title:(*one *two *three)", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithContainingClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleContaining", String.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { "solr" });
		Assert.assertEquals("title:*solr*", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithContainingClauseUsingCollection() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleContaining", Collection.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { Arrays.asList("one", "two", "three") });
		Assert.assertEquals("title:(*one* *two* *three*)", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithRegexClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByTitleRegex", String.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { "(\\+ \\*)" });
		Assert.assertEquals("title:(\\+ \\*)", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithBetweenClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityBetween", Integer.class, Integer.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { 100, 200 });
		Assert.assertEquals("popularity:[100 TO 200]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithBeforeClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLastModifiedBefore", Date.class);

		Query query = createQueryForMethodWithArgs(method,
				new Object[] { new DateTime(2012, 10, 15, 5, 31, 0, DateTimeZone.UTC) });
		Assert.assertEquals("last_modified:[* TO 2012\\-10\\-15T05\\:31\\:00.000Z}", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithLessThanClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPriceLessThan", Float.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { 100f });
		Assert.assertEquals("price:[* TO 100.0}", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithLessThanEqualsClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPriceLessThanEqual", Float.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { 100f });
		Assert.assertEquals("price:[* TO 100.0]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithAfterClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLastModifiedAfter", Date.class);

		Query query = createQueryForMethodWithArgs(method,
				new Object[] { new DateTime(2012, 10, 15, 5, 31, 0, DateTimeZone.UTC) });
		Assert.assertEquals("last_modified:{2012\\-10\\-15T05\\:31\\:00.000Z TO *]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithGreaterThanClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPriceGreaterThan", Float.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { 10f });
		Assert.assertEquals("price:{10.0 TO *]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithGreaterThanEqualsClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPriceGreaterThanEqual", Float.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { 10f });
		Assert.assertEquals("price:[10.0 TO *]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithInClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityIn", Integer[].class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { new Object[] { 1, 2, 3 } });
		Assert.assertEquals("popularity:(1 2 3)", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithNotInClause() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityNotIn", Integer[].class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { new Object[] { 1, 2, 3 } });
		Assert.assertEquals("-popularity:(1 2 3)", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithNear() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLocationNear", Point.class, Distance.class);

		Query query = createQueryForMethodWithArgs(method,
				new Object[] { new Point(48.303056, 14.290556), new Distance(5) });
		Assert.assertEquals("{!bbox pt=48.303056,14.290556 sfield=store d=5.0}", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithNearWhereUnitIsMiles() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLocationNear", Point.class, Distance.class);

		Query query = createQueryForMethodWithArgs(method,
				new Object[] { new Point(48.303056, 14.290556), new Distance(1, Metrics.MILES) });
		Assert.assertEquals("{!bbox pt=48.303056,14.290556 sfield=store d=1.609344}", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithWithin() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLocationWithin", Point.class, Distance.class);

		Query query = createQueryForMethodWithArgs(method,
				new Object[] { new Point(48.303056, 14.290556), new Distance(5) });
		Assert.assertEquals("{!geofilt pt=48.303056,14.290556 sfield=store d=5.0}", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithWithinWhereUnitIsMiles() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLocationWithin", Point.class, Distance.class);

		Query query = createQueryForMethodWithArgs(method,
				new Object[] { new Point(48.303056, 14.290556), new Distance(1, Metrics.MILES) });
		Assert.assertEquals("{!geofilt pt=48.303056,14.290556 sfield=store d=1.609344}", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithNearUsingBox() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByLocationNear", Box.class);

		Query query = createQueryForMethodWithArgs(method,
				new Object[] { new Box(new Point(48.303056, 14.290556), new Point(48.306377, 14.283128)) });
		Assert.assertEquals("store:[48.303056,14.290556 TO 48.306377,14.283128]", queryParser.getQueryString(query));
	}

	@Test
	public void testCreateQueryWithSortDesc() throws NoSuchMethodException, SecurityException {
		Method method = SampleRepository.class.getMethod("findByPopularityOrderByTitleDesc", Integer.class);

		Query query = createQueryForMethodWithArgs(method, new Object[] { 1 });
		Assert.assertNotNull(query.getSort());
		Assert.assertEquals(Sort.Direction.DESC, query.getSort().getOrderFor("title").getDirection());
	}

	@Test // DATASOLR-139
	public void testCombinationsOfOrAndShouldBeCreatedCorrectly() throws NoSuchMethodException, SecurityException {

		Method method = SampleRepository.class.getMethod("findByNameOrDescriptionAndLastModifiedAfter", String.class,
				String.class, Date.class);

		Query query = createQueryForMethodWithArgs(method,
				new Object[] { "mail", "domain", new DateTime(2012, 10, 15, 5, 31, 0, DateTimeZone.UTC) });
		Assert.assertEquals("name:mail OR description:domain AND last_modified:{2012\\-10\\-15T05\\:31\\:00.000Z TO *]",
				queryParser.getQueryString(query));
	}

	private Query createQueryForMethodWithArgs(Method method, Object[] args) {
		PartTree partTree = new PartTree(method.getName(), method.getReturnType());
		SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, new SpelAwareProxyProjectionFactory(),
				entityInformationCreatorMock);
		SolrQueryCreator creator = new SolrQueryCreator(partTree, new SolrParametersParameterAccessor(queryMethod, args),
				mappingContext);

		return creator.createQuery();
	}

	private interface SampleRepository extends Repository<ProductBean, String> {

		ProductBean findByPopularity(Integer popularity);

		ProductBean findByPopularityIsNot(Integer popularity);

		ProductBean findByPopularityIsNotNull();

		ProductBean findByPopularityIsNull();

		ProductBean findByPopularityAndPrice(Integer popularity, Float price);

		ProductBean findByPopularityOrPrice(Integer popularity, Float price);

		ProductBean findByAvailableTrue();

		ProductBean findByAvailableFalse();

		ProductBean findByTitleLike(String prefix);

		ProductBean findByTitleLike(Collection<String> prefix);

		ProductBean findByPopularityLike(Collection<Long> prefix);

		ProductBean findByPopularityLike(String[] prefix);

		ProductBean findByPopularityLike(Long[] prefix);

		ProductBean findByTitleNotLike(String prefix);

		ProductBean findByTitleNotLike(Collection<String> prefix);

		ProductBean findByTitleStartingWith(String prefix);

		ProductBean findByTitleStartingWith(Collection<String> prefix);

		ProductBean findByTitleEndingWith(String postfix);

		ProductBean findByTitleEndingWith(Collection<String> postfix);

		ProductBean findByTitleContaining(String fragment);

		ProductBean findByTitleContaining(Collection<String> fragment);

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

		ProductBean findByLocationWithin(Point location, Distance distance);

		ProductBean findByLocationNear(Point location, Distance distance);

		ProductBean findByLocationNear(Box bbox);

		ProductBean findByNameOrDescriptionAndLastModifiedAfter(String name, String description, Date date);

	}

}
