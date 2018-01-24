/*
 * Copyright 2012 - 2014 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.solr.core.convert.SolrjConverters.UpdateToSolrInputDocumentConverter;
import org.springframework.data.solr.core.query.PartialUpdate;

/**
 * @author Christoph Strobl
 */
public class UpdateToSolrInputDocumentConverterTests {

	private UpdateToSolrInputDocumentConverter converter = new UpdateToSolrInputDocumentConverter();

	@Test
	public void testConvertWithIdAndVersion() {
		PartialUpdate update = new PartialUpdate("id", "1");
		update.setVersion(1);

		SolrInputDocument document = converter.convert(update);

		Assert.assertEquals(update.getIdField().getValue(), document.getFieldValue(update.getIdField().getName()));
		Assert.assertEquals(update.getVersion(), document.getFieldValue("_version_"));
	}

	@Test
	public void testConvertWithIdNoVersion() {
		PartialUpdate update = new PartialUpdate("id", "1");

		SolrInputDocument document = converter.convert(update);

		Assert.assertEquals(update.getIdField().getValue(), document.getFieldValue(update.getIdField().getName()));
		Assert.assertNull(document.getFieldValue("_version_"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConvertWhenSettingValue() {
		PartialUpdate update = new PartialUpdate("id", "1");
		update.setValueOfField("field_1", "valueToSet");

		SolrInputDocument document = converter.convert(update);

		Assert.assertTrue(document.getFieldValue("field_1") instanceof Map);
		Assert.assertEquals("valueToSet", ((Map<String, Object>) document.getFieldValue("field_1")).get("set"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConvertWhenAddingValue() {
		PartialUpdate update = new PartialUpdate("id", "1");
		update.addValueToField("field_1", "valueToAdd");

		SolrInputDocument document = converter.convert(update);

		Assert.assertTrue(document.getFieldValue("field_1") instanceof Map);
		Assert.assertEquals("valueToAdd", ((Map<String, Object>) document.getFieldValue("field_1")).get("add"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConvertWhenIncreasingValue() {
		PartialUpdate update = new PartialUpdate("id", "1");
		update.increaseValueOfField("field_1", 1);

		SolrInputDocument document = converter.convert(update);

		Assert.assertTrue(document.getFieldValue("field_1") instanceof Map);
		Assert.assertEquals(1, ((Map<String, Object>) document.getFieldValue("field_1")).get("inc"));
	}

	@Test
	public void testConvertNull() {
		Assert.assertNull(converter.convert(null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertNullIdField() {
		converter.convert(new PartialUpdate((String) null, "1"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertBlankIdField() {
		converter.convert(new PartialUpdate("  ", "1"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConvertFieldWithEmtpyCollectionsAddsNullValueForUpdate() {
		PartialUpdate update = new PartialUpdate("id", "1");
		update.add("field_1", Collections.emptyList());

		SolrInputDocument document = converter.convert(update);
		Assert.assertThat((Map<String, Object>)document.getFieldValue("field_1"), IsMapContaining.hasEntry("set", null));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConvertFieldWithNullValueIsTransformedCorrectly() {
		PartialUpdate update = new PartialUpdate("id", "1");
		update.add("field_1", null);

		SolrInputDocument document = converter.convert(update);
		Assert.assertThat((Map<String, Object>)document.getFieldValue("field_1"), IsMapContaining.hasEntry("set", null));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testConvertFieldWithCollectionsIsTransformedCorrectlyToSolrInputDocument() {
		List<String> values = Arrays.asList("go", "pivotal");
		PartialUpdate update = new PartialUpdate("id", "1");
		update.add("field_1", values);

		SolrInputDocument document = converter.convert(update);
		Assert.assertThat((Map<String, List<String>>)document.getFieldValue("field_1"), IsMapContaining.hasEntry("set", values));
	}
}
