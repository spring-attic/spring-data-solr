/*
 * Copyright 2012 - 2018 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstraction for solr {@code !join} operation on documents within a single collection.
 *
 * @author Christoph Strobl
 * @author Radek Mensik
 */
public class Join {

	private @Nullable Field from;
	private @Nullable Field to;
	private @Nullable String fromIndex;

	private Join() {
		// hide default constructor
	}

	public Join(Field from, Field to) {
		this(from, to, null);
	}

	/**
	 * Creates new {@link Join} between fields.
	 *
	 * @param from
	 * @param to
	 * @param fromIndex
	 * @since 2.0
	 */
	public Join(Field from, Field to, @Nullable String fromIndex) {
		this.from = from;
		this.to = to;
		this.fromIndex = fromIndex;
	}

	/**
	 * @param from
	 * @return builder allowing completion
	 */
	public static Builder from(Field from) {
		return new Builder(from);
	}

	/**
	 * @param fieldname
	 * @return builder allowing completion
	 */
	public static Builder from(String fieldname) {
		return from(new SimpleField(fieldname));
	}

	/**
	 * @return null if not set
	 */
	@Nullable
	public Field getFrom() {
		return from;
	}

	/**
	 * @return null if not set
	 */
	@Nullable
	public Field getTo() {
		return to;
	}

	/**
	 * @return can be {@literal null}.
	 * @since 2.0
	 */
	@Nullable
	public String getFromIndex() {
		return fromIndex;
	}

	public static class Builder {

		private Join join;

		public Builder(Field from) {
			Assert.notNull(from, "From must not be null!");

			join = new Join();
			join.from = from;
		}

		public Builder(String fieldname) {
			this(new SimpleField(fieldname));
		}

		/**
		 * @param to
		 * @return completed {@link Join}
		 */
		public Join to(Field to) {
			Assert.notNull(to, "To must not be null!");

			join.to = to;
			return this.join;
		}

		/**
		 * @param fieldname
		 * @return completed {@link Join}
		 */
		public Join to(String fieldname) {
			return to(new SimpleField(fieldname));
		}

		/**
		 * @param fromIndex
		 * @return
		 * @since 2.0
		 */
		public Builder fromIndex(String fromIndex) {
			join.fromIndex = fromIndex;
			return this;
		}

	}

}
