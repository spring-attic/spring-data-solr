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

/**
 * @author Christoph Strobl
 */
public class TermsOptions {

	public static final Sort DEFAULT_SORT = Sort.COUNT;

	private @Nullable BoundTerm lowerBoundTerm;

	private @Nullable BoundTerm upperBoundTerm;

	private int minCount = -1;

	private int maxCount = -1;

	private @Nullable String prefix;

	private @Nullable String regex;

	private @Nullable RegexFlag regexFlag;

	private int limit = -1;

	private Sort sort = DEFAULT_SORT;

	private boolean raw = false;

	@Nullable
	public BoundTerm getLowerBoundTerm() {
		return lowerBoundTerm;
	}

	public void setLowerBoundTerm(BoundTerm lowerBoundTerm) {
		this.lowerBoundTerm = lowerBoundTerm;
	}

	@Nullable
	public BoundTerm getUpperBoundTerm() {
		return upperBoundTerm;
	}

	public void setUpperBoundTerm(BoundTerm upperBoundTerm) {
		this.upperBoundTerm = upperBoundTerm;
	}

	public int getMinCount() {
		return minCount;
	}

	public void setMinCount(int minCount) {
		this.minCount = minCount;
	}

	public int getMaxCount() {
		return maxCount;
	}

	public void setMaxCount(int maxCount) {
		this.maxCount = maxCount;
	}

	@Nullable
	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Nullable
	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	@Nullable
	public RegexFlag getRegexFlag() {
		return regexFlag;
	}

	public void setRegexFlag(RegexFlag regexFlag) {
		this.regexFlag = regexFlag;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Nullable
	public Sort getSort() {
		return sort;
	}

	public void setSort(Sort sort) {
		this.sort = sort;
	}

	public boolean isRaw() {
		return raw;
	}

	public void setRaw(boolean raw) {
		this.raw = raw;
	}

	/**
	 * @author Christoph Strobl
	 */
	public static class BoundTerm {

		private @Nullable String term;

		private boolean include;

		@Nullable
		public String getTerm() {
			return term;
		}

		public void setTerm(String term) {
			this.term = term;
		}

		public boolean isInclude() {
			return include;
		}

		public void setInclude(boolean include) {
			this.include = include;
		}

	}

	public enum Sort {
		COUNT, INDEX
	}

	public enum RegexFlag {
		CASE_INSENSITIVE, COMMENTS, MULTILINE, LITERAL, DOTALL, UNICODE_CASE, CANON_EQ, UNIX_LINES
	}

}
