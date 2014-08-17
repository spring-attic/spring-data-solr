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
package org.springframework.data.solr.core.query.result;

import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.Function;
import org.springframework.data.solr.core.query.Query;

public class SolrGroupResultPageTest {

	@Test
	public void testGetGroupResultField() {
		@SuppressWarnings("unchecked")
		GroupResult<Object> gr = new SimpleGroupResult<Object>(1, null, "name", Mockito.mock(Page.class));

		Field field = Mockito.mock(Field.class);
		Mockito.when(field.getName()).thenReturn("name");

		Map<Object, String> objectsName = Collections.<Object, String> singletonMap(field, "name");

		SolrGroupResultPage<Object> result = new SolrGroupResultPage<Object>(Collections.singletonList(gr), objectsName);

		Assert.assertEquals(gr, result.getGroupResult(field));
		Assert.assertEquals(gr, result.getGroupResult("name"));
	}

	@Test
	public void testGetGroupResultFunction() {
		@SuppressWarnings("unchecked")
		GroupResult<Object> gr = new SimpleGroupResult<Object>(1, null, "name", Mockito.mock(Page.class));

		Function func = Mockito.mock(Function.class);

		Map<Object, String> objectsName = Collections.<Object, String> singletonMap(func, "name");

		SolrGroupResultPage<Object> result = new SolrGroupResultPage<Object>(Collections.singletonList(gr), objectsName);

		Assert.assertEquals(gr, result.getGroupResult(func));
	}

	@Test
	public void testGetGroupResultQuery() {
		@SuppressWarnings("unchecked")
		GroupResult<Object> gr = new SimpleGroupResult<Object>(1, null, "name", Mockito.mock(Page.class));

		Query query = Mockito.mock(Query.class);

		Map<Object, String> objectsName = Collections.<Object, String> singletonMap(query, "name");

		SolrGroupResultPage<Object> result = new SolrGroupResultPage<Object>(Collections.singletonList(gr), objectsName);

		Assert.assertEquals(gr, result.getGroupResult(query));
	}

	@Test
	public void testGetGroupResultString() {
		@SuppressWarnings("unchecked")
		GroupResult<Object> gr = new SimpleGroupResult<Object>(1, null, "name", Mockito.mock(Page.class));

		SolrGroupResultPage<Object> result = new SolrGroupResultPage<Object>(Collections.singletonList(gr),
				Collections.<Object, String> emptyMap());

		Assert.assertEquals(gr, result.getGroupResult("name"));
	}

	@Test
	public void testInexistentGroupResult() {
		SolrGroupResultPage<Object> result = new SolrGroupResultPage<Object>(Collections.<GroupResult<Object>>emptyList(),
				Collections.<Object, String> emptyMap());

		Assert.assertNull(result.getGroupResult("name"));
		Assert.assertNull(result.getGroupResult(Mockito.mock(Query.class)));
		Assert.assertNull(result.getGroupResult(Mockito.mock(Field.class)));
		Assert.assertNull(result.getGroupResult(Mockito.mock(Function.class)));
	}

}
