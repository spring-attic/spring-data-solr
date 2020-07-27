/*
 * Copyright 2016-2020 the original author or authors.
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
package org.springframework.data.solr.core.query.result;

import java.util.Collection;
import java.util.List;

import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @since 2.1
 */
public interface SpellcheckQueryResult {

	/**
	 * Get all {@link Alternative}s;
	 *
	 * @return never {@literal null}.
	 */
	Collection<Alternative> getAlternatives();

	/**
	 * Get the {@link Alternative}s for a given term.
	 *
	 * @param term must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	Collection<Alternative> getAlternatives(String term);

	/**
	 * Get all suggestions.
	 *
	 * @return never {@literal null}.
	 */
	Collection<String> getSuggestions();

	/**
	 * Get the suggestions for a given term.
	 *
	 * @param term must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	Collection<String> getSuggestions(String term);

	void addSuggestions(String term, List<Alternative> suggestions);

	/**
	 * @author Christoph Strobl
	 * @since 2.1
	 */
	class Alternative {

		private final String term;
		private final int termFrequency;
		private final String suggestion;
		private final int suggestionFrequency;

		public Alternative(String term, int termFrequency, String suggestion, int suggestionFrequency) {
			this.term = term;
			this.termFrequency = termFrequency;
			this.suggestion = suggestion;
			this.suggestionFrequency = suggestionFrequency;
		}

		public String getTerm() {
			return this.term;
		}

		public int getTermFrequency() {
			return this.termFrequency;
		}

		public String getSuggestion() {
			return this.suggestion;
		}

		public int getSuggestionFrequency() {
			return this.suggestionFrequency;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Alternative)) {
				return false;
			}
			Alternative that = (Alternative) o;
			if (termFrequency != that.termFrequency) {
				return false;
			}
			if (suggestionFrequency != that.suggestionFrequency) {
				return false;
			}
			if (!ObjectUtils.nullSafeEquals(term, that.term)) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(suggestion, that.suggestion);
		}

		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(term);
			result = 31 * result + termFrequency;
			result = 31 * result + ObjectUtils.nullSafeHashCode(suggestion);
			result = 31 * result + suggestionFrequency;
			return result;
		}

		@Override
		public String toString() {
			return "Alternative{" + "term='" + term + '\'' + ", termFrequency=" + termFrequency + ", suggestion='"
					+ suggestion + '\'' + ", suggestionFrequency=" + suggestionFrequency + '}';
		}
	}
}
