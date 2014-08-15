/*
 * Copyright 2012 - 2014 the original author or authors.
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
package org.springframework.data.solr.core.query;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class SimpleGroupQueryTest {

	@Test
	public void testAddGroupByFunction() {
		Function f1 = Mockito.mock(Function.class);
		Function f2 = Mockito.mock(Function.class);

		SimpleGroupQuery simpleGroupQuery = new SimpleGroupQuery();
		
		simpleGroupQuery.addGroupByFunction(f1);
		simpleGroupQuery.addGroupByFunction(f2);
		
		Assert.assertArrayEquals(new Function[] {f1, f2}, simpleGroupQuery.getGroupByFunctions().toArray());
	}

	@Test
	public void testAddGroupByQuery() {
		Query q1 = Mockito.mock(Query.class);
		Query q2 = Mockito.mock(Query.class);
		
		SimpleGroupQuery simpleGroupQuery = new SimpleGroupQuery();
		
		simpleGroupQuery.addGroupByQuery(q1);
		simpleGroupQuery.addGroupByQuery(q2);
		
		Assert.assertArrayEquals(new Query[] {q1, q2}, simpleGroupQuery.getGroupByQueries().toArray());
	}

	@Test
	public void testSetGroupTotalCount() {
		SimpleGroupQuery simpleGroupQuery = new SimpleGroupQuery();
		
		Assert.assertFalse(simpleGroupQuery.isGroupTotalCount());
		
		simpleGroupQuery.setGroupTotalCount(true);
		
		Assert.assertTrue(simpleGroupQuery.isGroupTotalCount());
	}

	@Test
	public void testGetGroupPageRequest() {
		SimpleGroupQuery simpleGroupQuery = new SimpleGroupQuery();
		Assert.assertNull(simpleGroupQuery.getGroupPageRequest());
		
		simpleGroupQuery.setGroupOffset(1);
		assertPageable(simpleGroupQuery.getGroupPageRequest(), 1, 1, null);
	}

	@Test
	public void testGetGroupPageRequestWithSort() {
		SimpleGroupQuery simpleGroupQuery = new SimpleGroupQuery();
		simpleGroupQuery.setGroupOffset(1);
		Sort sort = new Sort("field_1");
		simpleGroupQuery.addGroupSort(sort);
		assertPageable(simpleGroupQuery.getGroupPageRequest(), 1, 1, sort);
	}

	@Test
	public void testGetGroupPageRequestWithLimit() {
		SimpleGroupQuery simpleGroupQuery = new SimpleGroupQuery();
		simpleGroupQuery.setGroupLimit(10);
		assertPageable(simpleGroupQuery.getGroupPageRequest(), 0, 10, null);
	}

	private void assertPageable(Pageable groupPageRequest, int offset, int limit, Sort sort) {
		Assert.assertEquals(offset, groupPageRequest.getOffset());
		Assert.assertEquals(limit, groupPageRequest.getPageSize());
		Assert.assertEquals(sort, groupPageRequest.getSort());
	}

}