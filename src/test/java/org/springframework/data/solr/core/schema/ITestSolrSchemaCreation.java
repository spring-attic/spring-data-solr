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
package org.springframework.data.solr.core.schema;

import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator.Feature;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 */
public class ITestSolrSchemaCreation {

	private SolrTemplate template;

	@Before
	public void setUp() {
		this.template = new SolrTemplate(new HttpSolrServer("http://localhost:8983/solr"));
		template.setSchemaCreationFeatures(Collections.singletonList(Feature.CREATE_MISSING_FIELDS));
		template.afterPropertiesSet();

		assertServerPresent(template);

		template.delete(new SimpleQuery("*:*"));
		template.commit();
	}

	// TODO: move this to @Rule
	private void assertServerPresent(SolrTemplate template) {

		String errMsg = "";
		try {
			String schemaName = template.getSchemaName("collection1");
			if (!schemaName.equalsIgnoreCase("example-schemaless")) {
				errMsg = "Expected to run in schemaless mode";
			}
		} catch (Exception e) {
			errMsg = "Solr Server not running - " + e.getMessage();
		}

		if (StringUtils.hasText(errMsg)) {
			throw new AssumptionViolatedException(errMsg);
		}
	}

	@Test
	public void beanShouldBeSavedCorrectly() {

		Foo foo = new Foo();
		foo.id = "1";
		template.saveBean(foo);
	}

	public static class Foo {

		@Indexed @Id String id;

		@Indexed String indexedStringWithoutType;

		@Indexed(name = "namedField", type = "string", indexed = false) String justAStoredField;

		@Indexed List<String> listField;

		@Indexed(type = "tdouble") Double someDoubleValue;

	}

}
