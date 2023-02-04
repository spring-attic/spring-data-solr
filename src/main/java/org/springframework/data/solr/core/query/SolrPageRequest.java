/*
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.lang.Nullable;

/**
 * Solr specific implementation of {@code Pageable} allowing zero sized pages.
 *
 * @author Christoph Strobl
 */
public class SolrPageRequest implements Pageable {

	private @Nullable Sort sort;
	private int page;
	private int size;

	/**
	 * Creates a new {@link SolrPageRequest}. Pages are zero indexed.
	 *
	 * @param page zero-based page index.
	 * @param size the size of the page to be returned.
	 */
	public SolrPageRequest(int page, int size) {
		this(page, size, Sort.unsorted());
	}

	/**
	 * Creates a new {@link SolrPageRequest} with sort parameters applied.
	 *
	 * @param page zero-based page index.
	 * @param size the size of the page to be returned.
	 * @param direction the direction of the {@link Sort} to be specified, can be {@literal null}.
	 * @param properties the properties to sort by, must not be {@literal null} or empty.
	 */
	public SolrPageRequest(int page, int size, Direction direction, String... properties) {
		this(page, size, Sort.by(direction, properties));
	}

	/**
	 * Creates a new {@link SolrPageRequest} with sort parameters applied.
	 *
	 * @param page zero-based page index.
	 * @param size the size of the page to be returned.
	 * @param sort can be {@literal null}.
	 */
	public SolrPageRequest(int page, int size, @Nullable Sort sort) {
		this.page = page;
		this.size = size;
		this.sort = sort;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getPageNumber()
	 */
	@Override
	public int getPageNumber() {
		return page;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getPageSize()
	 */
	@Override
	public int getPageSize() {
		return this.size;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getOffset()
	 */
	@Override
	public long getOffset() {
		return page * size;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getSort()
	 */
	@Override
	public Sort getSort() {
		return sort != null ? sort : Sort.unsorted();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#next()
	 */
	@Override
	public Pageable next() {
		return new SolrPageRequest(getPageNumber() + 1, getPageSize(), getSort());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#previousOrFirst()
	 */
	@Override
	public Pageable previousOrFirst() {
		return hasPrevious() ? previous() : first();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#first()
	 */
	@Override
	public Pageable first() {
		return new SolrPageRequest(0, getPageSize(), getSort());
	}

	@Override
	public Pageable withPage(final int pageNumber) {
		return new SolrPageRequest(pageNumber, this.getPageSize(), this.getSort());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#hasPrevious()
	 */
	@Override
	public boolean hasPrevious() {
		return page > 0;
	}

	/**
	 * Returns the {@link Pageable} requesting the previous {@link Page}.
	 *
	 * @return
	 */
	public Pageable previous() {
		return getPageNumber() == 0 ? this : new SolrPageRequest(getPageNumber() - 1, getPageSize(), getSort());
	}

	@Override
	public int hashCode() {
		int result = sort.hashCode();
		result = 31 * result + page;
		result = 31 * result + (size ^ size >>> 32);
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Pageable)) {
			return false;
		}

		Pageable other = (Pageable) obj;
		if (page != other.getPageNumber()) {
			return false;
		}
		if (size != other.getPageSize()) {
			return false;
		}
		if (sort == null) {
			if (other.getSort() != null) {
				return false;
			}
		} else if (!sort.equals(other.getSort())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SolrPageRequest [number=" + page + ", size=" + size + ", sort=" + sort + "]";
	}

}
