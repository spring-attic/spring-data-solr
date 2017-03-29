/*
 * Copyright 2016-2017 the original author or authors.
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

import java.util.Collection;
import java.util.List;

import lombok.Data;

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
	@Data
	class Alternative {

		private final String term;
		private final int termFrequency;
		private final String suggestion;
		private final int suggestionFrequency;
	}
}
