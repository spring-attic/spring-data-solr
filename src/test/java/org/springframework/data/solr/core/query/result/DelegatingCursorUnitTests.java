/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.query.result;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsEqual.*;
import static org.hamcrest.core.IsNot.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CursorMarkParams;
import org.junit.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.solr.core.query.result.DelegatingCursor.PartialResult;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 */
public class DelegatingCursorUnitTests {

	@Test(expected = InvalidDataAccessApiUsageException.class) // DATASOLR-162
	public void shouldThrowExceptionWhenOpeningMultipleTimes() {
		new DelegatingCursorFake<Object>(null).open().open();
	}

	@Test // DATASOLR-162
	public void shouldNotHaveNextWhenNoElementsAvailable() {
		assertThat(new DelegatingCursorFake<Object>(null).open().hasNext(), is(false));
	}

	@Test(expected = NoSuchElementException.class) // DATASOLR-162
	public void nextShouldThrowExceptionWhenNoMoreElementsAvailable() {
		new DelegatingCursorFake<Object>(null).open().next();
	}

	@Test // DATASOLR-162
	public void shouldReturnElementsInValidOrder() {

		PartialResult<String> result = new PartialResult<String>("*", Arrays.asList("spring", "data", "solr"));
		DelegatingCursor<String> cursor = new DelegatingCursorFake<String>(Collections.singleton(result)).open();

		assertThat(cursor.next(), equalTo("spring"));
		assertThat(cursor.next(), equalTo("data"));
		assertThat(cursor.next(), equalTo("solr"));
	}

	@Test // DATASOLR-162
	public void shouldStopWhenNoMoreElementsAvailableAndAlreadyFinished() {

		PartialResult<String> result = new PartialResult<String>("*", Arrays.asList("spring", "data", "solr"));
		DelegatingCursor<String> cursor = new DelegatingCursorFake<String>(Collections.singleton(result)).open();

		cursor.next();
		cursor.next();
		cursor.next();
		assertThat(cursor.hasNext(), is(false));
	}

	@Test // DATASOLR-162
	public void shouldFetchNextSetOfElementsWhenNotFinishedAndCurrentResultsEndReached() {

		PartialResult<String> result1 = new PartialResult<String>("foo", Arrays.asList("spring", "data"));
		PartialResult<String> result2 = new PartialResult<String>("foo", Arrays.asList("solr"));

		@SuppressWarnings("unchecked")
		DelegatingCursor<String> cursor = new DelegatingCursorFake<String>(Arrays.asList(result1, result2)).open();

		assertThat(cursor.next(), equalTo("spring"));
		assertThat(cursor.next(), equalTo("data"));
		assertThat(cursor.next(), equalTo("solr"));
	}

	@Test // DATASOLR-162
	public void shouldDetermineEndOfResultsCorrectly() {

		PartialResult<String> result1 = new PartialResult<String>("foo", Arrays.asList("spring", "data"));
		PartialResult<String> result2 = new PartialResult<String>("foo", Arrays.asList("solr"));

		@SuppressWarnings("unchecked")
		DelegatingCursor<String> cursor = new DelegatingCursorFake<String>(Arrays.asList(result1, result2)).open();

		cursor.next();
		cursor.next();
		cursor.next();
		assertThat(cursor.hasNext(), is(false));
	}

	@Test // DATASOLR-162
	public void shouldFinishLoopingWhenCursorMarkEqualsPreviousOne() {

		PartialResult<String> result1 = new PartialResult<String>("foo", Arrays.asList("spring"));
		PartialResult<String> result2 = new PartialResult<String>("bar", Arrays.asList("data"));
		PartialResult<String> result3 = new PartialResult<String>("bar", Arrays.asList("solr"));

		@SuppressWarnings("unchecked")
		DelegatingCursor<String> cursor = new DelegatingCursorFake<String>(Arrays.asList(result1, result2, result3)).open();

		assertThat(cursor.hasNext(), is(true));
		assertThat(cursor.isFinished(), is(false));
		assertThat(cursor.next(), is("spring"));

		assertThat(cursor.hasNext(), is(true));
		assertThat(cursor.isFinished(), is(false));
		assertThat(cursor.next(), is("data"));

		assertThat(cursor.hasNext(), is(true));
		assertThat(cursor.isFinished(), is(true));
		assertThat(cursor.next(), is("solr"));
	}

	@Test // DATASOLR-162
	public void shouldNotModifyInitialQueryWhenRequestingResults() {

		SolrQuery initialQuery = new SolrQuery("*:*");
		DelegatingCursorFake<String> cursor = new DelegatingCursorFake<String>(initialQuery, null);
		cursor.open();

		SolrQuery executedQuey = cursor.getLastUsedQuery();

		assertThat(executedQuey, not(equalTo(initialQuery)));
		assertThat(executedQuey.getQuery(), equalTo(initialQuery.getQuery()));
		assertThat(executedQuey.get(CursorMarkParams.CURSOR_MARK_PARAM), equalTo(CursorMarkParams.CURSOR_MARK_START));

		assertThat(initialQuery.get(CursorMarkParams.CURSOR_MARK_PARAM), nullValue());
	}

	class DelegatingCursorFake<T> extends DelegatingCursor<T> {

		List<PartialResult<T>> values = new ArrayList<PartialResult<T>>();
		private int requestCounter;
		private SolrQuery lastUsedQuery;

		public DelegatingCursorFake(Collection<PartialResult<T>> results) {
			this(new SolrQuery(), results);
		}

		protected DelegatingCursorFake(SolrQuery query, Collection<PartialResult<T>> results) {
			super(query);
			if (!CollectionUtils.isEmpty(results)) {
				this.values.addAll(results);
			}
			this.requestCounter = 0;
		}

		@Override
		protected org.springframework.data.solr.core.query.result.DelegatingCursor.PartialResult<T> doLoad(
				SolrQuery nativeQuery) {

			this.lastUsedQuery = nativeQuery;
			return ((!values.isEmpty() && requestCounter <= values.size()) ? values.get(requestCounter++) : null);
		}

		public SolrQuery getLastUsedQuery() {
			return lastUsedQuery;
		}

		public int getNrRequestsExecuted() {
			return this.requestCounter;
		}

	}

}
