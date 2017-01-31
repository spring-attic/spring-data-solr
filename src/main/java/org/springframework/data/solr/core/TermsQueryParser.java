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
package org.springframework.data.solr.core;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.TermsOptions;
import org.springframework.data.solr.core.query.TermsQuery;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * TermsQueryParser is capable of building {@link SolrQuery} for {@link TermsQuery}
 *
 * @author Christoph Strobl
 */
public class TermsQueryParser extends QueryParserBase<TermsQuery> {

	@Override
	public SolrQuery doConstructSolrQuery(TermsQuery query) {
		Assert.notNull(query, "Cannot construct solrQuery from null value.");

		SolrQuery solrQuery = new SolrQuery();
		String queryString = getQueryString(query);
		if (StringUtils.hasText(queryString)) {
			solrQuery.setParam(CommonParams.Q, queryString);
		}
		appendTermsOptionsToSolrQuery(query.getTermsOptions(), solrQuery);
		processTermsFields(solrQuery, query);
		appendRequestHandler(solrQuery, query.getRequestHandler());
		return solrQuery;
	}

	protected void appendTermsOptionsToSolrQuery(TermsOptions options, SolrQuery solrQuery) {
		solrQuery.setTerms(true);
		if (options.getLimit() >= 0) {
			solrQuery.setTermsLimit(options.getLimit());
		}
		if (options.getMaxCount() >= -1) {
			solrQuery.setTermsMaxCount(options.getMaxCount());
		}
		if (options.getMinCount() >= 0) {
			solrQuery.setTermsMinCount(options.getMinCount());
		}
		if (StringUtils.hasText(options.getPrefix())) {
			solrQuery.setTermsPrefix(options.getPrefix());
		}
		if (StringUtils.hasText(options.getRegex())) {
			solrQuery.setTermsRegex(options.getRegex());
		}
		if (options.getRegexFlag() != null) {
			solrQuery.setTermsRegexFlag(options.getRegexFlag().toString().toLowerCase());
		}
		if (options.getSort() != null) {
			solrQuery.setTermsSortString(options.getSort().toString().toLowerCase());
		}
		if (options.getUpperBoundTerm() != null) {
			solrQuery.setTermsUpper(options.getUpperBoundTerm().getTerm());
			solrQuery.setTermsUpperInclusive(options.getUpperBoundTerm().isInclude());
		}
		if (options.getLowerBoundTerm() != null) {
			solrQuery.setTermsUpper(options.getLowerBoundTerm().getTerm());
			solrQuery.setTermsUpperInclusive(options.getLowerBoundTerm().isInclude());
		}
		if (!options.isRaw()) {
			solrQuery.setTermsRaw(options.isRaw());
		}

	}

	private void processTermsFields(SolrQuery solrQuery, TermsQuery query) {
		for (Field field : query.getTermsFields()) {
			appendTermsFieldToSolrQuery(field, solrQuery);
		}

	}

	protected void appendTermsFieldToSolrQuery(Field field, SolrQuery solrQuery) {
		if (StringUtils.hasText(field.getName())) {
			solrQuery.addTermsField(field.getName());
		}
	}

}
