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
package org.springframework.data.solr.core.convert;

import java.util.Map;

import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.core.query.PartialUpdate;

/**
 * @author Christoph Strobl
 */
public class SolrJConverterTests {

	private SolrJConverter converter;

	@Before
	public void setUp() {
		converter = new SolrJConverter();
	}

	@Test
	public void testWrite() {
		ConvertableBean convertable = new ConvertableBean("j73x73r", 1979);
		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(convertable, solrDocument);

		Assert.assertEquals(convertable.getStringProperty(), solrDocument.getFieldValue("stringProperty"));
		Assert.assertEquals(convertable.getIntProperty(), solrDocument.getFieldValue("intProperty"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testWriteUpdate() {
		PartialUpdate update = new PartialUpdate("id", "123");
		update.add("language", "java");
		update.add("since", 1995);

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(update, solrDocument);

		Assert.assertEquals(update.getIdField().getValue(), solrDocument.getFieldValue(update.getIdField().getName()));
		Assert.assertTrue(solrDocument.getFieldValue("since") instanceof Map);
		Assert.assertEquals(1995, ((Map<String, Object>) solrDocument.getFieldValue("since")).get("set"));
	}

	@Test
	public void testRead() {
		SolrDocument document = new SolrDocument();
		document.addField("stringProperty", "christoph");
		document.addField("intProperty", 32);

		ConvertableBean convertable = converter.read(ConvertableBean.class, (Map<String, Object>) document);

		Assert.assertEquals(document.getFieldValue("stringProperty"), convertable.getStringProperty());
		Assert.assertEquals(document.getFieldValue("intProperty"), convertable.getIntProperty());
	}

	public static class ConvertableBean {

		@Field
		String stringProperty;

		@Field
		Integer intProperty;

		public ConvertableBean() {
		}

		public ConvertableBean(String stringProperty, Integer intProperty) {
			super();
			this.stringProperty = stringProperty;
			this.intProperty = intProperty;
		}

		String getStringProperty() {
			return stringProperty;
		}

		void setStringProperty(String stringProperty) {
			this.stringProperty = stringProperty;
		}

		Integer getIntProperty() {
			return intProperty;
		}

		void setIntProperty(Integer intProperty) {
			this.intProperty = intProperty;
		}

	}

}
