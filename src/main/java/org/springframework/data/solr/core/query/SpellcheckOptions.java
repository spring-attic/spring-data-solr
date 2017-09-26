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
package org.springframework.data.solr.core.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.common.params.SpellingParams;
import org.springframework.lang.Nullable;

/**
 * {@link SpellcheckOptions} allows modification of query parameters targeting the SpellCheck component is designed to
 * provide inline query suggestions based on other, similar, terms.
 *
 * @author Christoph Strobl
 * @since 2.1
 */
public class SpellcheckOptions {

	private @Nullable Query query;
	private Map<String, Object> params;

	private SpellcheckOptions(@Nullable Query query, Map<String, Object> params) {

		this.query = query;
		this.params = new LinkedHashMap<>(params);
	}

	/**
	 * Creates new {@link SpellcheckOptions}.
	 *
	 * @return
	 */
	public static SpellcheckOptions spellcheck() {
		return new SpellcheckOptions(null, new LinkedHashMap<>());
	}

	/**
	 * Creates new {@link SpellcheckOptions} with a given {@link Query}.
	 *
	 * @param q
	 * @return
	 */
	public static SpellcheckOptions spellcheck(Query q) {
		return new SpellcheckOptions(q, new LinkedHashMap<>());
	}

	/**
	 * Get the query to be used for spellchecking.
	 *
	 * @return can be {@literal null}.
	 */
	@Nullable
	public Query getQuery() {
		return query;
	}

	/**
	 * @return never {@literal null}.
	 */
	public Map<String, Object> getParams() {
		return Collections.unmodifiableMap(params);
	}

	/**
	 * If set, Solr creates the dictionary that the SolrSpellChecker will use for spell-checking.
	 *
	 * @return new {@link SpellcheckOptions}
	 */
	public SpellcheckOptions buildDictionary() {
		return createNewAndAppend(SpellingParams.SPELLCHECK_BUILD, true);
	}

	/**
	 * @return can be {@literal null}.
	 */
	public boolean buildDirectory() {
		return params.containsKey(SpellingParams.SPELLCHECK_BUILD) ? (Boolean) params.get(SpellingParams.SPELLCHECK_BUILD)
				: false;
	}

	/**
	 * If set, Solr will take the best suggestion for each token (if one exists) and construct a new query from the
	 * suggestions.
	 *
	 * @return new {@link SpellcheckOptions}
	 */
	public SpellcheckOptions collate() {
		return createNewAndAppend(SpellingParams.SPELLCHECK_COLLATE, true);
	}

	/**
	 * @return can be {@literal null}.
	 */
	public boolean getCollate() {
		return params.containsKey(SpellingParams.SPELLCHECK_COLLATE)
				? (Boolean) params.get(SpellingParams.SPELLCHECK_COLLATE) : false;
	}

	/**
	 * The maximum number of collations to return.
	 *
	 * @param max
	 * @return
	 */
	public SpellcheckOptions maxCollations(long max) {
		return potentiallySetCollate().createNewAndAppend(SpellingParams.SPELLCHECK_MAX_COLLATIONS, max);
	}

	/**
	 * @return can be {@literal null}.
	 */
	@Nullable
	public Long getMaxCollations() {
		return (Long) params.get(SpellingParams.SPELLCHECK_MAX_COLLATIONS);
	}

	/**
	 * This parameter specifies the number of collation possibilities for Solr to try before giving up.
	 *
	 * @param tries
	 * @return
	 */
	public SpellcheckOptions maxCollationTries(long tries) {
		return potentiallySetCollate().createNewAndAppend(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES, tries);
	}

	/**
	 * @return can be {@literal null}.
	 */
	@Nullable
	public Long getMaxCollationTries() {
		return (Long) params.get(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES);
	}

	/**
	 * This parameter specifies the maximum number of word correction combinations to rank and evaluate prior to deciding
	 * which collation candidates to test against the index.
	 *
	 * @param evaluations
	 * @return
	 */
	public SpellcheckOptions maxCollationEvaluations(long evaluations) {
		return potentiallySetCollate().createNewAndAppend(SpellingParams.SPELLCHECK_MAX_COLLATION_EVALUATIONS, evaluations);
	}

	/**
	 * @return can be {@literal null}.
	 */
	@Nullable
	public Long getMaxCollationEvaluations() {
		return (Long) params.get(SpellingParams.SPELLCHECK_MAX_COLLATION_EVALUATIONS);
	}

	/**
	 * Enable the extended response format, which is more complicated but richer. Returns the document frequency for each
	 * suggestion and returns one suggestion block for each term in the query string.
	 *
	 * @return
	 */
	public SpellcheckOptions extendedResults() {
		return createNewAndAppend(SpellingParams.SPELLCHECK_EXTENDED_RESULTS, true);
	}

	/**
	 * @return can be {@literal null}.
	 */
	@Nullable
	public Boolean getExtendedResults() {
		return (Boolean) params.get(SpellingParams.SPELLCHECK_EXTENDED_RESULTS);
	}

	/**
	 * Instructs Solr to return an expanded response format detailing the collations found.
	 *
	 * @return
	 */
	public SpellcheckOptions collateExtendedResults() {
		return potentiallySetCollate().createNewAndAppend(SpellingParams.SPELLCHECK_COLLATE_EXTENDED_RESULTS, true);
	}

	/**
	 * @return
	 */
	public boolean getCollateExtendedResults() {
		return params.containsKey(SpellingParams.SPELLCHECK_COLLATE_EXTENDED_RESULTS)
				? (Boolean) params.get(SpellingParams.SPELLCHECK_COLLATE_EXTENDED_RESULTS) : false;
	}

	/**
	 * This parameter specifies the maximum number of documents that should be collect when testing potential collations
	 * against the index.
	 *
	 * @param nr
	 * @return
	 */
	public SpellcheckOptions maxCollationCollectDocs(long nr) {
		return potentiallySetCollate().createNewAndAppend(SpellingParams.SPELLCHECK_COLLATE_MAX_COLLECT_DOCS, nr);
	}

	/**
	 * @return can be {@literal null}.
	 */
	@Nullable
	public Long getMaxCollationCollectDocs() {
		return (Long) params.get(SpellingParams.SPELLCHECK_COLLATE_MAX_COLLECT_DOCS);
	}

	/**
	 * This parameter prefix can be used to specify any additional parameters that you wish to the Spellchecker to use
	 * when internally validating collation queries.
	 *
	 * @param param
	 * @param value
	 * @return
	 */
	public SpellcheckOptions collateParam(String param, Object value) {
		return potentiallySetCollate().createNewAndAppend(SpellingParams.SPELLCHECK_COLLATE_PARAM_OVERRIDE + param, value);
	}

	/**
	 * @return can be {@literal null}.
	 */
	public Map<String, Object> getCollateParams() {

		Map<String, Object> tmp = new LinkedHashMap<>();

		for (Entry<String, Object> entry : params.entrySet()) {
			if (entry.getKey().startsWith(SpellingParams.SPELLCHECK_COLLATE_PARAM_OVERRIDE)) {
				tmp.put(entry.getKey().substring(SpellingParams.SPELLCHECK_COLLATE_PARAM_OVERRIDE.length()), entry.getValue());
			}
		}
		return tmp;
	}

	/**
	 * Specifies the maximum number of spelling suggestions to be returned.
	 *
	 * @param nr
	 * @return
	 */
	public SpellcheckOptions count(long nr) {
		return createNewAndAppend(SpellingParams.SPELLCHECK_COUNT, nr);
	}

	/**
	 * @return can be {@literal null}.
	 */
	@Nullable
	public Long getCount() {
		return (Long) params.get(SpellingParams.SPELLCHECK_COUNT);
	}

	/**
	 * This parameter causes Solr to use the dictionary named in the parameter's argument. The default setting is
	 * "default". This parameter can be used to invoke a specific spellchecker on a per request basis.
	 *
	 * @return
	 */
	public SpellcheckOptions dictionaries(String... names) {
		return createNewAndAppend(SpellingParams.SPELLCHECK_DICT, names);
	}

	/**
	 * @return can be {@literal null}.
	 */
	@Nullable
	public String[] getDictionary() {
		return (String[]) params.get(SpellingParams.SPELLCHECK_DICT);
	}

	/**
	 * Limits spellcheck responses to queries that are more popular than the original query.
	 *
	 * @return
	 */
	public SpellcheckOptions onlyMorePopular() {
		return createNewAndAppend(SpellingParams.SPELLCHECK_ONLY_MORE_POPULAR, true);
	}

	/**
	 * @return never {@literal null}.
	 */
	public boolean getOnlyMorePopular() {
		return params.containsKey(SpellingParams.SPELLCHECK_ONLY_MORE_POPULAR)
				? (Boolean) params.get(SpellingParams.SPELLCHECK_ONLY_MORE_POPULAR) : false;
	}

	/**
	 * The maximum number of hits the request can return in order to both generate spelling suggestions and set the
	 * {@literal correctlySpelled} element to {@literal false}.
	 *
	 * @param nr
	 * @return
	 */
	public SpellcheckOptions maxResultsForSuggest(long nr) {
		return createNewAndAppend(SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST, nr);
	}

	/**
	 * @return can be {@literal null}.
	 */
	@Nullable
	public Long getMaxResultsForSuggest() {
		return (Long) params.get(SpellingParams.SPELLCHECK_MAX_RESULTS_FOR_SUGGEST);
	}

	/**
	 * The count of suggestions to return for each query term existing in the index and/or dictionary.
	 *
	 * @param nr
	 * @return
	 */
	public SpellcheckOptions alternativeTermCount(long nr) {
		return createNewAndAppend(SpellingParams.SPELLCHECK_ALTERNATIVE_TERM_COUNT, nr);
	}

	/**
	 * @return can be {@literal null}.
	 */
	@Nullable
	public Long getAlternativeTermCount() {
		return (Long) params.get(SpellingParams.SPELLCHECK_ALTERNATIVE_TERM_COUNT);
	}

	/**
	 * Specifies an accuracy value to be used by the spell checking implementation to decide whether a result is
	 * worthwhile or not. The value is a float between 0 and 1.
	 * 
	 * @param nr
	 * @return
	 */
	public SpellcheckOptions accuracy(float nr) {
		return createNewAndAppend(SpellingParams.SPELLCHECK_ACCURACY, nr);
	}

	/**
	 * @return can be {@literal null}.
	 */
	@Nullable
	public Float getAccuracy() {
		return (Float) params.get(SpellingParams.SPELLCHECK_ACCURACY);
	}

	private SpellcheckOptions potentiallySetCollate() {

		if (params.containsKey(SpellingParams.SPELLCHECK_COLLATE)) {
			return this;
		}

		return collate();
	}

	private SpellcheckOptions createNewAndAppend(String key, Object value) {

		SpellcheckOptions so = new SpellcheckOptions(query, params);
		so.params.put(key, value);
		return so;
	}
}
