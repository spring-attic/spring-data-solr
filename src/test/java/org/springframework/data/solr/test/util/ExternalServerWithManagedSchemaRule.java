/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.solr.test.util;

import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 */
public class ExternalServerWithManagedSchemaRule implements TestRule {

	private String errMsg;
	private String baseUrl;

	public ExternalServerWithManagedSchemaRule(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public ExternalServerWithManagedSchemaRule init() {
		SolrTemplate template = new SolrTemplate(new HttpSolrServer(baseUrl));

		try {
			String schemaName = template.getSchemaName("collection1");
			if (!schemaName.equalsIgnoreCase("example-schemaless")) {
				errMsg = "Expected to run in schemaless mode";
			}
		} catch (Exception e) {
			errMsg = "Solr Server not running - " + e.getMessage();
		}

		return this;
	}

	public static ExternalServerWithManagedSchemaRule onLocalhost() {
		return new ExternalServerWithManagedSchemaRule("http://localhost:8983/solr").init();
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				if (StringUtils.hasText(errMsg)) {
					throw new AssumptionViolatedException(errMsg);
				}
			}
		};
	}

}
