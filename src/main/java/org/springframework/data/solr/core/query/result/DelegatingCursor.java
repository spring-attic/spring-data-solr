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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CursorMarkParams;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link DelegatingCursor} is a base {@link Cursor} implementation that temporarily holds data fetched in one run and
 * delegates iteration.
 * 
 * @author Christoph Strobl
 * @param <T>
 */
public abstract class DelegatingCursor<T> implements Cursor<T> {

	private State state;
	private @Nullable String cursorMark;
	private long position;
	private Iterator<T> delegate;
	private final SolrQuery referenceQuery;

	protected DelegatingCursor(SolrQuery query) {
		this(query, CursorMarkParams.CURSOR_MARK_START);
	}

	protected DelegatingCursor(SolrQuery query, String initalCursorMark) {

		this.referenceQuery = query;
		this.cursorMark = StringUtils.hasText(initalCursorMark) ? initalCursorMark : CursorMarkParams.CURSOR_MARK_START;
		this.state = State.READY;
		this.delegate = Collections.<T> emptyList().iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {

		validateState();

		if (!delegate.hasNext() && !isFinished()) {
			load(getCursorMark());
		}

		if (delegate.hasNext()) {
			return true;
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	@Override
	public T next() {

		validateState();

		if (!hasNext()) {
			throw new NoSuchElementException("No more elements available for cursor " + getCursorMark() + ".");
		}

		T next = moveNext(delegate);
		position++;
		return next;
	}

	/**
	 * Move one position next in given source.
	 * 
	 * @param source
	 * @return
	 */
	protected T moveNext(Iterator<T> source) {
		return source.next();
	}

	private void load(@Nullable String cursorMark) {

		SolrQuery query = referenceQuery.getCopy();
		query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);

		PartialResult<T> result = doLoad(query);
		process(result);
	}

	/**
	 * Read data from Solr.
	 * 
	 * @param nativeQuery The query to execute already positioned at the next cursor mark.
	 * @return
	 */
	protected abstract PartialResult<T> doLoad(SolrQuery nativeQuery);

	private void process(@Nullable PartialResult<T> result) {

		if (result == null) {
			this.delegate = Collections.<T> emptyList().iterator();
			this.state = State.FINISHED;
			return;
		}

		if (getCursorMark().equals(result.getNextCursorMark())) {
			this.state = State.FINISHED;
		}

		this.cursorMark = result.getNextCursorMark();

		if (!CollectionUtils.isEmpty(result.getItems())) {
			delegate = result.iterator();
		} else {
			Collections.<T> emptyList().iterator();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.Cursor#open()
	 */
	@Override
	public DelegatingCursor<T> open() {

		if (!isReady()) {
			throw new InvalidDataAccessApiUsageException("Cursor already " + state + ". Cannot (re)open it.");
		}

		this.state = State.OPEN;
		doOpen(this.getCursorMark());
		return this;
	}

	/**
	 * Customization hook for {@link #open()}.
	 * 
	 * @param cursorMark
	 */
	protected void doOpen(@Nullable String cursorMark) {
		load(cursorMark);
	}

	/*
	 * (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {

		try {
			doClose();
		} finally {
			this.state = State.CLOSED;
		}

	}

	/**
	 * Customization hook for clean up operations
	 */
	protected void doClose() {
		this.delegate = Collections.<T> emptyList().iterator();
		this.referenceQuery.clear();
		this.position = -1;
		this.cursorMark = null;
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException("Removing elements from cursor is not supported");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.Cursor#getPosition()
	 */
	@Override
	public long getPosition() {
		return this.position;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.Cursor#getCursorMark()
	 */
	@Nullable
	@Override
	public String getCursorMark() {
		return this.cursorMark;
	}

	/**
	 * @return true if {@link State#REDAY}
	 */
	public boolean isReady() {
		return State.REDAY.equals(state) || State.READY.equals(state);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.Cursor#isOpen()
	 */
	public boolean isOpen() {
		return State.OPEN.equals(state);
	}

	/**
	 * @return true if {@link State#FINISHED}
	 */
	public boolean isFinished() {
		return State.FINISHED.equals(state);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.query.result.Cursor#isClosed()
	 */
	@Override
	public boolean isClosed() {
		return State.CLOSED.equals(state);
	}

	private void validateState() {
		if (isReady() || isClosed()) {
			throw new InvalidDataAccessApiUsageException("Cannot access closed cursor. Did you forget to call open()?");
		}
	}

	/**
	 * {@link PartialResult} provided by a round trip to SolrClient loading data for an iteration. Also holds the cursor
	 * mark to use next.
	 * 
	 * @author Christoph Strobl
	 * @param <T>
	 */
	public static class PartialResult<T> implements Iterable<T> {

		private String nextCursorMark;
		private Collection<T> items;

		public PartialResult(String nextCursorMark, @Nullable Collection<T> items) {
			this.nextCursorMark = nextCursorMark;
			this.items = (items != null ? new ArrayList<>(items) : Collections.<T> emptyList());
		}

		/**
		 * Get the next cursor mark to use.
		 * 
		 * @return
		 */
		public String getNextCursorMark() {
			return nextCursorMark;
		}

		/**
		 * Get items returned from server. <br/>
		 * 
		 * @return never {@literal null}
		 */
		public Collection<T> getItems() {
			return items;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		public Iterator<T> iterator() {
			return items.iterator();
		}
	}

}
