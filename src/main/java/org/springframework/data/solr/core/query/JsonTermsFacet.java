/*
 * Copyright 2012 - 2020 the original author or authors.
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

/**
 * Represents a JSON <a href="https://lucene.apache.org/solr/guide/8_5/json-facet-api.html#terms-facet">terms facet</a>.
 * 
 * @author Joe Linn
 */
public class JsonTermsFacet extends JsonFieldFacet {
	private int offset;
	private int limit = 10;
	private TermsOptions.Sort sort = TermsOptions.DEFAULT_SORT;
	private int overRequest = -1;
	private boolean refine;
	private int overRefine = -1;
	private int minCount = 1;
	private boolean missing;
	private boolean numBuckets;
	private boolean allBuckets;
	private String prefix;
	private Method method = Method.SMART;

	public JsonTermsFacet() {}

	public JsonTermsFacet(String name, String field) {
		super(name, field);
	}

	public int getOffset() {
		return offset;
	}

	public JsonTermsFacet setOffset(int offset) {
		this.offset = offset;
		return this;
	}

	public int getLimit() {
		return limit;
	}

	public JsonTermsFacet setLimit(int limit) {
		this.limit = limit;
		return this;
	}

	public TermsOptions.Sort getSort() {
		return sort;
	}

	public JsonTermsFacet setSort(TermsOptions.Sort sort) {
		this.sort = sort;
		return this;
	}

	public int getOverRequest() {
		return overRequest;
	}

	public JsonTermsFacet setOverRequest(int overRequest) {
		this.overRequest = overRequest;
		return this;
	}

	public boolean isRefine() {
		return refine;
	}

	public JsonTermsFacet setRefine(boolean refine) {
		this.refine = refine;
		return this;
	}

	public int getOverRefine() {
		return overRefine;
	}

	public JsonTermsFacet setOverRefine(int overRefine) {
		this.overRefine = overRefine;
		return this;
	}

	public int getMinCount() {
		return minCount;
	}

	public JsonTermsFacet setMinCount(int minCount) {
		this.minCount = minCount;
		return this;
	}

	public boolean isMissing() {
		return missing;
	}

	public JsonTermsFacet setMissing(boolean missing) {
		this.missing = missing;
		return this;
	}

	public boolean isNumBuckets() {
		return numBuckets;
	}

	public JsonTermsFacet setNumBuckets(boolean numBuckets) {
		this.numBuckets = numBuckets;
		return this;
	}

	public boolean isAllBuckets() {
		return allBuckets;
	}

	public JsonTermsFacet setAllBuckets(boolean allBuckets) {
		this.allBuckets = allBuckets;
		return this;
	}

	public String getPrefix() {
		return prefix;
	}

	public JsonTermsFacet setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public Method getMethod() {
		return method;
	}

	public JsonTermsFacet setMethod(Method method) {
		this.method = method;
		return this;
	}

	@Override
	public String getType() {
		return "terms";
	}

	public enum Method {
		DV, UIF, DVHASH, ENUM, STREAM, SMART;
	}
}
