/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.solr.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.solr.core.query.result.SpellcheckedPage;

/**
 * Enable Solr spellcheck component for a repository query method.<br />
 * Use {@link SpellcheckedPage} as method return to access suggestions.
 *
 * @author Christoph Strobl
 * @since 2.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Spellcheck {

	/**
	 * Specifies the maximum number of spelling suggestions to be returned.
	 *
	 * @return
	 */
	long count() default -1;

	/**
	 * This parameter causes Solr to use the dictionary named in the parameter's argument. The default setting is
	 * "default". This parameter can be used to invoke a specific spellchecker on a per request basis.
	 *
	 * @return
	 */
	String[] dictionaries() default {};

	/**
	 * This parameter enables/disables the extended response format. The default values is {@literal false}. If enabled
	 * the response returns the document frequency for each suggestion and returns one suggestion block for each term in
	 * the query string.
	 *
	 * @return
	 */
	boolean extendedResults() default false;

	/**
	 * The maximum number of hits the request can return in order to both generate spelling suggestions and set the
	 * {@literal correctlySpelled} element to {@literal false}.
	 *
	 * @return
	 */
	long maxResultsForSuggest() default -1;

	/**
	 * The count of suggestions to return for each query term existing in the index and/or dictionary.
	 *
	 * @return
	 */
	long alternativeTermCount() default -1;

	/**
	 * Specifies an accuracy value to be used by the spell checking implementation to decide whether a result is
	 * worthwhile or not. The value is a float between 0 and 1.
	 *
	 * @return
	 */
	float accuracy() default -1F;

	/**
	 * Limits spellcheck responses to queries that are more popular than the original query.
	 *
	 * @return
	 */
	boolean onlyMorePopular() default false;

	/**
	 * If set, Solr creates the dictionary that the SolrSpellChecker will use for spell-checking.
	 *
	 * @return
	 */
	boolean buildDictionary() default false;

	/**
	 * If set, Solr will take the best suggestion for each token (if one exists) and construct a new query from the
	 * suggestions.
	 *
	 * @return
	 */
	boolean collate() default false;

	/**
	 * The maximum number of collations to return.
	 *
	 * @return
	 */
	long maxCollations() default -1;

	/**
	 * This parameter specifies the number of collation possibilities for Solr to try before giving up.
	 *
	 * @return
	 */
	long maxCollationsTries() default -1;

	/**
	 * This parameter specifies the maximum number of word correction combinations to rank and evaluate prior to deciding
	 * which collation candidates to test against the index.
	 *
	 * @return
	 */
	long maxCollationEvaluations() default -1;

	/**
	 * This parameter specifies the maximum number of documents that should be collect when testing potential collations
	 * against the index.
	 *
	 * @return
	 */
	long maxCollationCollectDocs() default -1;

	/**
	 * Instructs Solr to return an expanded response format detailing the collations found.
	 *
	 * @return
	 */
	boolean collateExtendedResults() default false;

}
