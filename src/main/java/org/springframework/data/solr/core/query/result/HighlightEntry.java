/*
 * Copyright 2012 - 2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.util.Assert;

/**
 * Highlight result entry holding reference to domain object ({@link #getEntity()) as well as the highlights
 * 
 * @author Christoph Strobl
 */
public class HighlightEntry<T> {

	private final T entity;
	private final List<Highlight> highlights = new ArrayList<Highlight>(1);

	/**
	 * @param entity must not be null
	 */
	public HighlightEntry(T entity) {
		Assert.notNull(entity, "Entity must not be null!");
		this.entity = entity;
	}

	/**
	 * Get the entity the highlights are associated to
	 * 
	 * @return
	 */
	public T getEntity() {
		return this.entity;
	}

	/**
	 * @return empty collection if none available
	 */
	public List<Highlight> getHighlights() {
		return Collections.unmodifiableList(this.highlights);
	}

	/**
	 * @param field
	 * @param snipplets
	 */
	public void addSnipplets(Field field, List<String> snipplets) {
		this.highlights.add(new Highlight(field, snipplets));
	}

	/**
	 * @param fieldname
	 * @param snipplets
	 */
	public void addSnipplets(String fieldname, List<String> snipplets) {
		addSnipplets(new SimpleField(fieldname), snipplets);
	}

	/**
	 * Highlight holds reference to the field highlighting was applied to, as well as the snipplets
	 * 
	 * @author Christoph Strobl
	 */
	public static class Highlight {

		private final Field field;
		private final List<String> snipplets;

		/**
		 * @param field must not be null
		 * @param snipplets
		 */
		Highlight(Field field, List<String> snipplets) {
			Assert.notNull(field, "Field must not be null!");

			this.field = field;
			this.snipplets = snipplets;
		}

		Highlight(String fieldname, List<String> snipplets) {
			this(new SimpleField(fieldname), snipplets);
		}

		/**
		 * @return
		 */
		public Field getField() {
			return this.field;
		}

		/**
		 * @return empty list none available
		 */
		public List<String> getSnipplets() {
			return this.snipplets != null ? this.snipplets : Collections.<String> emptyList();
		}

	}

}
