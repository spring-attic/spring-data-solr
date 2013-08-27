/*
 * Copyright 2012 - 2013 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.data.solr.core.query.TermsOptions.RegexFlag;
import org.springframework.data.solr.core.query.TermsOptions.Sort;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * 
 */
public class SimpleTermsQuery extends AbstractQuery implements TermsQuery {

	public static final String DEFAULT_REQUEST_HANDLER = "/terms";

	private TermsOptions termsOptions = new TermsOptions();
	private List<Field> fields = new ArrayList<Field>(1);

	public SimpleTermsQuery() {
		super();
	}

	public void addField(Field field) {
		this.fields.add(field);
	}

	public void addField(String fieldname) {
		this.fields.add(new SimpleField(fieldname));
	}

	public void addFields(String... fieldnames) {
		for (String fieldname : fieldnames) {
			addField(fieldname);
		}
	}

	@Override
	public TermsOptions getTermsOptions() {
		return this.termsOptions;
	}

	@Override
	public List<Field> getTermsFields() {
		return Collections.unmodifiableList(this.fields);
	}

	@Override
	public String getRequestHandler() {
		return StringUtils.hasText(super.getRequestHandler()) ? super.getRequestHandler() : DEFAULT_REQUEST_HANDLER;
	}

	public static Builder queryBuilder() {
		return new Builder();
	}

	public static Builder queryBuilder(String... fieldnames) {
		return new Builder(fieldnames);
	}

	public static class Builder {

		private SimpleTermsQuery query;

		public Builder() {
			this.query = new SimpleTermsQuery();
		}

		public Builder(String... fieldnames) {
			this.query = new SimpleTermsQuery();
			this.query.addFields(fieldnames);
		}

		public Builder withCriteria(Criteria criteria) {
			this.query.addCriteria(criteria);
			return this;
		}

		public Builder limit(int limit) {
			this.query.termsOptions.setLimit(limit);
			return this;
		}

		public Builder maxCount(int maxCount) {
			this.query.termsOptions.setMaxCount(maxCount);
			return this;
		}

		public Builder minCount(int minCount) {
			this.query.termsOptions.setMinCount(minCount);
			return this;
		}

		public Builder prefix(String prefix) {
			this.query.termsOptions.setPrefix(prefix);
			return this;
		}

		public Builder regex(String regex) {
			this.query.termsOptions.setRegex(regex);
			return this;
		}

		public Builder sort(Sort sort) {
			this.query.termsOptions.setSort(sort);
			return this;
		}

		public Builder fields(String... fieldnames) {
			this.query.addFields(fieldnames);
			return this;
		}

		public Builder regexFlag(RegexFlag flag) {
			this.query.termsOptions.setRegexFlag(flag);
			return this;
		}

		public Builder handledBy(String requestHandler) {
			this.query.setRequestHandler(requestHandler);
			return this;
		}

		public SimpleTermsQuery build() {
			return this.query;
		}

	}

}
