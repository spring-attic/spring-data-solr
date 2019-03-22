/*
 * Copyright 2012 - 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.query;

import java.util.Arrays;

import org.springframework.util.Assert;

/**
 * Implementation of {@code termfreq(field,term)}
 *
 * @author Christoph Strobl
 * @since 1.1
 */
public class TermFrequencyFunction extends AbstractFunction {

	private static final String OPERATION = "termfreq";

	private TermFrequencyFunction(Field field, String term) {
		super(Arrays.asList(field, term));
	}

	/**
	 * @param term
	 * @return
	 */
	public static Builder termFequency(String term) {
		return new Builder(term);
	}

	@Override
	public String getOperation() {
		return OPERATION;
	}

	public static class Builder {

		private final String term;

		public Builder(String term) {
			this.term = term;
		}

		/**
		 * @param fieldName must not be empty
		 * @return
		 */
		public TermFrequencyFunction inField(String fieldName) {

			Assert.hasText(fieldName, "fieldName for termfrequency must not be 'empty'.");
			return inField(new SimpleField(fieldName));
		}

		/**
		 * @param field must not be null
		 * @return
		 */
		public TermFrequencyFunction inField(Field field) {

			Assert.notNull(field, "Field for termfrequency must not be 'null'.");
			return new TermFrequencyFunction(field, this.term);
		}
	}
}
