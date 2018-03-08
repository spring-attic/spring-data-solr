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

import java.io.Closeable;
import java.io.Serializable;
import java.util.Iterator;

import org.springframework.lang.Nullable;

/**
 * {@link Cursor} provides a lazy loading abstraction for fetching documents.
 * 
 * @author Christoph Strobl
 * @param <T>
 */
public interface Cursor<T> extends Iterator<T>, Closeable {

	enum State {

		/**
		 * @deprecated since 3.1. Please use READY instead.
		 */
		@Deprecated
		REDAY, READY, OPEN, FINISHED, CLOSED
	}

	/**
	 * Get the current set cursorMark
	 * 
	 * @return
	 */
	@Nullable
	Serializable getCursorMark();

	/**
	 * Opens the cursor. <br />
	 * Only {@link State#READY} cursors can be opened.
	 * 
	 * @return
	 */
	Cursor<T> open();

	/**
	 * @return the current position starting a zero.
	 */
	long getPosition();

	/**
	 * @return true if {@link State#OPEN}
	 */
	boolean isOpen();

	/**
	 * @return true if {@link State#CLOSED}
	 */
	boolean isClosed();

}
