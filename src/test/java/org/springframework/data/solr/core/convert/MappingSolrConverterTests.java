/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsArrayContainingInAnyOrder;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.hamcrest.collection.IsMapContaining;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNull;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.geo.Point;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.query.PartialUpdate;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public class MappingSolrConverterTests {

	private MappingSolrConverter converter;
	private SimpleSolrMappingContext mappingContext;

	@Before
	public void setUp() {
		mappingContext = new SimpleSolrMappingContext();

		converter = new MappingSolrConverter(mappingContext);
		converter.afterPropertiesSet();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testWrite() {
		BeanWithDefaultTypes bean = new BeanWithDefaultTypes();
		bean.stringProperty = "j73x73r";
		bean.intProperty = 1979;
		bean.listOfString = Arrays.asList("one", "two", "three");
		bean.arrayOfString = new String[] { "three", "two", "one" };
		bean.dateProperty = new Date(60264684000000L);

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertEquals(bean.stringProperty, solrDocument.getFieldValue("stringProperty"));
		Assert.assertEquals(bean.intProperty, solrDocument.getFieldValue("intProperty"));
		Assert.assertThat(solrDocument.getFieldValues("listOfString"), IsEqual.equalTo((Collection) bean.listOfString));
		Assert.assertThat(solrDocument.getFieldValues("arrayOfString"),
				IsEqual.equalTo((Collection) Arrays.asList(bean.arrayOfString)));
		Assert.assertEquals(bean.dateProperty, solrDocument.getFieldValue("dateProperty"));
	}

	@Test
	public void testWriteWithCustomType() {
		BeanWithCustomTypes bean = new BeanWithCustomTypes();
		bean.location = new Point(48.362893D, 14.534437D);

		SolrInputDocument document = new SolrInputDocument();
		converter.write(bean, document);

		Assert.assertEquals("48.362893,14.534437", document.getFieldValue("location"));
	}

	@Test
	public void testWriteWithCustomTypeList() {
		BeanWithCustomTypes bean = new BeanWithCustomTypes();
		bean.locations = Arrays.asList(new Point(48.362893D, 14.534437D), new Point(48.208602D, 16.372996D));

		SolrInputDocument document = new SolrInputDocument();
		converter.write(bean, document);

		Assert.assertEquals(Arrays.asList("48.362893,14.534437", "48.208602,16.372996"),
				document.getFieldValues("locations"));
	}

	@Test
	public void testWriteWithNamedProperty() {
		BeanWithNamedFields bean = new BeanWithNamedFields();
		bean.name = "j73x73r(at)gmail(dot)com";

		SolrInputDocument document = new SolrInputDocument();
		converter.write(bean, document);

		Assert.assertEquals(bean.name, document.getFieldValue("namedProperty"));
	}

	@Test
	public void testWriteWithSimpleTypes() {
		BeanWithSimpleTypes bean = new BeanWithSimpleTypes();
		bean.simpleBoolProperty = true;
		bean.simpleFloatProperty = 10f;
		bean.simpleIntProperty = 3;

		SolrInputDocument document = new SolrInputDocument();
		converter.write(bean, document);

		Assert.assertEquals(bean.simpleBoolProperty, document.getFieldValue("simpleBoolProperty"));
		Assert.assertEquals(bean.simpleFloatProperty, document.getFieldValue("simpleFloatProperty"));
		Assert.assertEquals(bean.simpleIntProperty, document.getFieldValue("simpleIntProperty"));
	}

	@Test
	public void testWriteWithNulls() {
		BeanWithDefaultTypes bean = new BeanWithDefaultTypes();
		bean.stringProperty = null;
		bean.intProperty = null;
		bean.listOfString = null;

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertNull(solrDocument.getFieldValue("stringProperty"));
		Assert.assertNull(solrDocument.getFieldValue("intProperty"));
		Assert.assertNull(solrDocument.getFieldValues("listOfString"));
	}

	@Test
	public void testWriteWithoutFieldAnnotation() {
		BeanWithoutAnnotatedFields bean = new BeanWithoutAnnotatedFields();
		bean.notIndexedProperty = "!do not index!";

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertNull(solrDocument.getFieldValue("notIndexedProperty"));
	}

	@Test
	public void testWriteWithFieldsExcludedFromIndexing() {
		BeanWithFieldsExcludedFromIndexing bean = new BeanWithFieldsExcludedFromIndexing();
		bean.transientField = "must not be indexed";
		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertNull(solrDocument.getFieldValue("transientField"));
	}

	@Test
	public void testWriteMappedProperty() {
		Map<String, String> values = new HashMap<String, String>(2);
		values.put("key_1", "value_1");
		values.put("key_2", "value_2");

		BeanWithWildcards bean = new BeanWithWildcards();
		bean.flatMapWithLeadingWildcard = values;

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertEquals(values.get("key_1"), solrDocument.getFieldValue("key_1"));
		Assert.assertEquals(values.get("key_2"), solrDocument.getFieldValue("key_2"));
	}

	@Test
	public void testWriteMappedListProperty() {
		Map<String, List<String>> values = new HashMap<String, List<String>>(2);
		values.put("key_1", Arrays.asList("value_1"));
		values.put("key_2", Arrays.asList("value_2", "value_3"));

		BeanWithWildcards bean = new BeanWithWildcards();
		bean.multivaluedFieldMapWithLeadingWildcardList = values;

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertEquals(values.get("key_1"), solrDocument.getFieldValues("key_1"));
		Assert.assertEquals(values.get("key_2"), solrDocument.getFieldValues("key_2"));
	}

	@Test
	public void testWriteMappedArrayProperty() {
		Map<String, String[]> values = new HashMap<String, String[]>(2);
		values.put("key_1", new String[] { "value_1" });
		values.put("key_2", new String[] { "value_2", "value_3" });

		BeanWithWildcards bean = new BeanWithWildcards();
		bean.multivaluedFieldMapWithLeadingWildcardArray = values;

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertEquals(Arrays.asList(values.get("key_1")), solrDocument.getFieldValues("key_1"));
		Assert.assertEquals(Arrays.asList(values.get("key_2")), solrDocument.getFieldValues("key_2"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWriteSingeValuedFieldWithLeadingWildcard() {
		BeanWithWildcards bean = new BeanWithWildcards();
		bean.fieldWithLeadingWildcard = "leading_1";

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWriteSingeValuedFieldWithTrailingWildcard() {
		BeanWithWildcards bean = new BeanWithWildcards();
		bean.filedWithTrailingWildcard = "trailing_1";

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWriteListWithLeadingWildcard() {
		BeanWithWildcards bean = new BeanWithWildcards();
		bean.listFieldWithLeadingWildcard = Arrays.asList("leading_1");

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWriteArrayWithLeadingWildcard() {
		BeanWithWildcards bean = new BeanWithWildcards();
		bean.arrayFieldWithLeadingWildcard = new String[] { "leading_1" };

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWriteListWithTrailingWildcard() {
		BeanWithWildcards bean = new BeanWithWildcards();
		bean.listFieldWithTrailingWildcard = Arrays.asList("trailing_1");

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWriteArrayWithTailingWildcard() {
		BeanWithWildcards bean = new BeanWithWildcards();
		bean.arrayFieldWithTrailingWildcard = new String[] { "trailing_1" };

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWriteListWithWildcard() {
		BeanWithWildcards bean = new BeanWithWildcards();
		bean.listFieldWithLeadingWildcard = Arrays.asList("leading_1");
		bean.arrayFieldWithLeadingWildcard = new String[] { "leading_2" };

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);
	}

	@Test
	public void testWriteNonMapPropertyWithWildcardWhenNotIndexed() {
		BeanWithCatchAllField bean = new BeanWithCatchAllField();
		bean.allStringProperties = new String[] { "value_1", "value_2" };

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);
		Assert.assertTrue(solrDocument.isEmpty());
	}

	@Test
	public void testWriteWithFieldAnnotationOnSetter() {
		BeanWithFieldAnnotationOnSetter bean = new BeanWithFieldAnnotationOnSetter();
		bean.name = "value_1";
		bean.namedField = "value_2";

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertEquals(bean.name, solrDocument.getFieldValue("name"));
		Assert.assertEquals(bean.namedField, solrDocument.getFieldValue("namedFieldFieldName"));
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
	public void testWriteNullToSolrInputDocumentColletion() {
		Collection<?> result = converter.write((Iterable<?>) null);
		Assert.assertNotNull(result);
		Assert.assertThat(result, IsEmptyCollection.empty());
	}

	@Test
	public void testWriteEmptyCollectionToSolrInputDocumentColletion() {
		Collection<?> result = converter.write(new ArrayList<Object>());
		Assert.assertNotNull(result);
		Assert.assertThat(result, IsEmptyCollection.empty());
	}

	@Test
	public void testWriteCollectionToSolrInputDocumentColletion() {
		BeanWithDefaultTypes bean1 = new BeanWithDefaultTypes();
		bean1.stringProperty = "solr";

		BeanWithDefaultTypes bean2 = new BeanWithDefaultTypes();
		bean2.intProperty = 10;

		Collection<SolrInputDocument> result = converter.write(Arrays.asList(bean1, bean2));
		Assert.assertNotNull(result);
		Assert.assertThat(result, IsCollectionWithSize.hasSize(2));
	}

	@Test
	public void testWriteBeanWithInheritance() {
		BeanWithInteritance bean = new BeanWithInteritance();
		bean.stringProperty = "some string";
		bean.intProperty = 10;

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertEquals(bean.stringProperty, solrDocument.getFieldValue("stringProperty"));
		Assert.assertEquals(bean.intProperty, solrDocument.getFieldValue("intProperty"));
	}

	@Test
	public void testRead() {
		SolrDocument document = new SolrDocument();
		document.addField("stringProperty", "christoph");
		document.addField("intProperty", 32);
		document.addField("listOfString", Arrays.asList("one", "two", "three"));
		document.addField("arrayOfString", new String[] { "spring", "data", "solr" });
		document.addField("dateProperty", new Date(60264684000000L));

		BeanWithDefaultTypes target = converter.read(BeanWithDefaultTypes.class, document);

		Assert.assertEquals(document.getFieldValue("stringProperty"), target.stringProperty);
		Assert.assertEquals(document.getFieldValue("intProperty"), target.intProperty);
		Assert.assertThat(target.listOfString, IsEqual.equalTo(document.getFieldValue("listOfString")));
		Assert.assertArrayEquals(document.getFieldValues("arrayOfString").toArray(), target.arrayOfString);
		Assert.assertEquals(document.getFieldValue("dateProperty"), target.dateProperty);
	}

	@Test
	public void testReadSingleValuedArray() {
		SolrDocument document = new SolrDocument();
		document.setField("arrayOfString", "christoph");

		BeanWithDefaultTypes target = converter.read(BeanWithDefaultTypes.class, document);
		Assert.assertArrayEquals(document.getFieldValues("arrayOfString").toArray(), target.arrayOfString);
	}

	@Test
	public void testReadWithCustomTypes() {
		SolrDocument document = new SolrDocument();
		document.addField("location", "48.362893,14.534437");
		document.addField("locations", Arrays.asList("48.362893,14.534437", "13.923404,142.177731"));

		BeanWithCustomTypes target = converter.read(BeanWithCustomTypes.class, document);

		Assert.assertEquals(48.362893D, target.location.getX(), 0.0f);
		Assert.assertEquals(14.534437D, target.location.getY(), 0.0f);

		Assert.assertEquals(48.362893D, target.locations.get(0).getX(), 0.0f);
		Assert.assertEquals(14.534437D, target.locations.get(0).getY(), 0.0f);
		Assert.assertEquals(13.923404D, target.locations.get(1).getX(), 0.0f);
		Assert.assertEquals(142.177731D, target.locations.get(1).getY(), 0.0f);
	}

	@Test
	public void testReadWithNamedProperty() {
		SolrDocument document = new SolrDocument();
		document.addField("namedProperty", "strobl");

		BeanWithNamedFields target = converter.read(BeanWithNamedFields.class, document);

		Assert.assertEquals(document.getFieldValue("namedProperty"), target.name);
	}

	@Test
	public void testReadWithSimpleTypes() {
		SolrDocument document = new SolrDocument();
		document.addField("simpleIntProperty", 1);
		document.addField("simpleBoolProperty", true);
		document.addField("simpleFloatProperty", 10f);

		BeanWithSimpleTypes target = converter.read(BeanWithSimpleTypes.class, document);

		Assert.assertEquals(document.getFieldValue("simpleIntProperty"), target.simpleIntProperty);
		Assert.assertEquals(document.getFieldValue("simpleBoolProperty"), target.simpleBoolProperty);
		Assert.assertEquals(document.getFieldValue("simpleFloatProperty"), target.simpleFloatProperty);
	}

	@Test
	public void testReadWithNullFields() {
		SolrDocument document = new SolrDocument();
		document.addField("stringProperty", null);
		document.addField("intProperty", null);
		document.addField("listOfString", null);
		document.addField("arrayOfString", null);
		document.addField("date", null);

		BeanWithDefaultTypes target = converter.read(BeanWithDefaultTypes.class, document);

		Assert.assertNull(target.stringProperty);
		Assert.assertNull(target.intProperty);
		Assert.assertNull(target.listOfString);
		Assert.assertNull(target.arrayOfString);
		Assert.assertNull(target.dateProperty);
	}

	@Test
	public void testReadWithPropertiesNotInDocument() {
		SolrDocument document = new SolrDocument();
		document.addField("stringProperty", null);

		BeanWithDefaultTypes target = converter.read(BeanWithDefaultTypes.class, document);

		Assert.assertEquals(document.getFieldValue("stringProperty"), target.stringProperty);
		Assert.assertNull(target.intProperty);
		Assert.assertNull(target.listOfString);
	}

	@Test
	public void testReadToFieldExcludedFromIndexing() {
		SolrDocument document = new SolrDocument();
		document.addField("transientField", "value computed on solr server");

		BeanWithFieldsExcludedFromIndexing target = converter.read(BeanWithFieldsExcludedFromIndexing.class, document);
		Assert.assertEquals(document.getFieldValue("transientField"), target.transientField);
	}

	@Test
	public void testReadWithTargetConstructorCall() {
		SolrDocument document = new SolrDocument();
		document.addField("stringProperty", null);
		document.addField("intProperty", null);

		BeanWithDefaultTypes target = converter.read(BeanWithDefaultTypes.class, document);

		Assert.assertEquals(document.getFieldValue("stringProperty"), target.stringProperty);
		Assert.assertEquals(document.getFieldValue("intProperty"), target.intProperty);
	}

	@Test
	public void testReadWithCatchAllField() {
		SolrDocument document = new SolrDocument();
		document.addField("stringProperty_ci", "case-insensitive-string");
		document.addField("stringProperty_multi", new String[] { "first", "second", "third" });

		BeanWithCatchAllField target = converter.read(BeanWithCatchAllField.class, document);
		Assert.assertEquals(4, target.allStringProperties.length);
		Assert.assertThat(target.allStringProperties,
				IsArrayContainingInAnyOrder.arrayContainingInAnyOrder("case-insensitive-string", "first", "second", "third"));
	}

	@Test
	public void testReadPropertyWithTrailingWildcard() {
		SolrDocument document = new SolrDocument();
		document.addField("field_with_trailing_wildcard_ci", "trailing");

		BeanWithWildcards target = converter.read(BeanWithWildcards.class, document);
		Assert.assertEquals("trailing", target.filedWithTrailingWildcard);
	}

	@Test
	public void testReadPropertyWithTrailingWildcardToCollection() {
		SolrDocument document = new SolrDocument();
		document.addField("listFieldWithTrailingWildcard_1", "trailing_1");
		document.addField("listFieldWithTrailingWildcard_2", "trailing_2");

		BeanWithWildcards target = converter.read(BeanWithWildcards.class, document);
		Assert.assertEquals(Arrays.asList("trailing_1", "trailing_2"), target.listFieldWithTrailingWildcard);
	}

	@Test
	public void testReadPropertyWithTrailingWildcardToArray() {
		SolrDocument document = new SolrDocument();
		document.addField("arrayFieldWithTrailingWildcard_1", "trailing_1");
		document.addField("arrayFieldWithTrailingWildcard_2", "trailing_2");

		BeanWithWildcards target = converter.read(BeanWithWildcards.class, document);
		Assert.assertArrayEquals(new String[] { "trailing_1", "trailing_2" }, target.arrayFieldWithTrailingWildcard);
	}

	@Test
	public void testReadPropertyWithLeadingWildcard() {
		SolrDocument document = new SolrDocument();
		document.addField("ci_field_with_leading_wildcard", "leading");

		BeanWithWildcards target = converter.read(BeanWithWildcards.class, document);
		Assert.assertEquals("leading", target.fieldWithLeadingWildcard);
	}

	@Test
	public void testReadPropertyWithLeadingWildcardToCollection() {
		SolrDocument document = new SolrDocument();
		document.addField("1_listFieldWithLeadingWildcard", "leading_1");
		document.addField("2_listFieldWithLeadingWildcard", "leading_2");

		BeanWithWildcards target = converter.read(BeanWithWildcards.class, document);
		Assert.assertEquals(Arrays.asList("leading_1", "leading_2"), target.listFieldWithLeadingWildcard);
	}

	@Test
	public void testReadPropertyWithLeadingWildcardToArray() {
		SolrDocument document = new SolrDocument();
		document.addField("1_arrayFieldWithTrailingWildcard", "leading_1");
		document.addField("2_arrayFieldWithTrailingWildcard", "leading_2");

		BeanWithWildcards target = converter.read(BeanWithWildcards.class, document);
		Assert.assertArrayEquals(new String[] { "leading_1", "leading_2" }, target.arrayFieldWithLeadingWildcard);
	}

	@Test
	public void testReadFieldWithLeadingWildcardToMap() {
		SolrDocument document = new SolrDocument();
		document.addField("1_flatMapWithLeadingWildcard", "leading-map-value-1");
		document.addField("2_flatMapWithLeadingWildcard", "leading-map-value-2");

		BeanWithWildcards target = converter.read(BeanWithWildcards.class, document);
		Assert.assertEquals(2, target.flatMapWithLeadingWildcard.size());
		Assert.assertThat(target.flatMapWithLeadingWildcard,
				Matchers.allOf(Matchers.hasEntry("1_flatMapWithLeadingWildcard", "leading-map-value-1"),
						Matchers.hasEntry("2_flatMapWithLeadingWildcard", "leading-map-value-2")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReadMultivaluedFieldWithLeadingWildcardToMapWithSingleEntry() {
		SolrDocument document = new SolrDocument();
		document.addField("1_flatMapWithLeadingWildcard", Arrays.asList("leading-map-value-1", "leading-map-value-2"));
		converter.read(BeanWithWildcards.class, document);
	}

	@Test
	public void testReadFieldWithTrailingWildcardToMap() {
		SolrDocument document = new SolrDocument();
		document.addField("flatMapWithTrailingWildcard_1", "trailing-map-value-1");
		document.addField("flatMapWithTrailingWildcard_2", "trailing-map-value-2");

		BeanWithWildcards target = converter.read(BeanWithWildcards.class, document);
		Assert.assertEquals(2, target.flatMapWithTrailingWildcard.size());
		Assert.assertThat(target.flatMapWithTrailingWildcard,
				Matchers.allOf(Matchers.hasEntry("flatMapWithTrailingWildcard_1", "trailing-map-value-1"),
						Matchers.hasEntry("flatMapWithTrailingWildcard_2", "trailing-map-value-2")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReadMultivaluedFieldWithTrailingWildcardToMapWithSingleEntry() {
		SolrDocument document = new SolrDocument();
		document.addField("flatMapWithTrailingWildcard_1", Arrays.asList("trailing-map-value-1", "trailing-map-value-2"));
		converter.read(BeanWithWildcards.class, document);
	}

	@Test
	public void testReadMultivaluedFieldWithLeadingWildcardToArrayInMap() {
		SolrDocument document = new SolrDocument();
		document.addField("1_multivaluedFieldMapWithLeadingWildcard", "leading-map-value-1");
		document.addField("2_multivaluedFieldMapWithLeadingWildcard",
				Arrays.asList("leading-map-value-2", "leading-map-value-3"));

		BeanWithWildcards target = converter.read(BeanWithWildcards.class, document);
		Assert.assertEquals(2, target.multivaluedFieldMapWithLeadingWildcardArray.size());
		Assert.assertThat(target.multivaluedFieldMapWithLeadingWildcardArray,
				Matchers.allOf(
						Matchers.hasEntry("1_multivaluedFieldMapWithLeadingWildcard", new String[] { "leading-map-value-1" }),
						Matchers.hasEntry("2_multivaluedFieldMapWithLeadingWildcard",
								new String[] { "leading-map-value-2", "leading-map-value-3" })));
	}

	@Test
	public void testReadMultivaluedFieldWithLeadingWildcardToListInMap() {
		SolrDocument document = new SolrDocument();
		document.addField("1_multivaluedFieldMapWithLeadingWildcard", "leading-map-value-1");
		document.addField("2_multivaluedFieldMapWithLeadingWildcard",
				Arrays.asList("leading-map-value-2", "leading-map-value-3"));

		BeanWithWildcards target = converter.read(BeanWithWildcards.class, document);
		Assert.assertEquals(2, target.multivaluedFieldMapWithLeadingWildcardList.size());
		Assert.assertThat(target.multivaluedFieldMapWithLeadingWildcardList,
				Matchers.allOf(
						Matchers.hasEntry("1_multivaluedFieldMapWithLeadingWildcard", Arrays.asList("leading-map-value-1")),
						Matchers.hasEntry("2_multivaluedFieldMapWithLeadingWildcard",
								Arrays.asList("leading-map-value-2", "leading-map-value-3"))));
	}

	@Test
	public void testReadMultivaluedFieldWithTrailingWildcardToArrayInMap() {
		SolrDocument document = new SolrDocument();
		document.addField("multivaluedFieldMapWithTrailingWildcard_1", "trailing-map-value-1");
		document.addField("multivaluedFieldMapWithTrailingWildcard_2",
				Arrays.asList("trailing-map-value-2", "trailing-map-value-3"));

		BeanWithWildcards target = converter.read(BeanWithWildcards.class, document);
		Assert.assertEquals(2, target.multivaluedFieldMapWithTrailingWildcardArray.size());
		Assert.assertThat(target.multivaluedFieldMapWithTrailingWildcardArray,
				Matchers.allOf(
						Matchers.hasEntry("multivaluedFieldMapWithTrailingWildcard_1", new String[] { "trailing-map-value-1" }),
						Matchers.hasEntry("multivaluedFieldMapWithTrailingWildcard_2",
								new String[] { "trailing-map-value-2", "trailing-map-value-3" })));
	}

	@Test
	public void testReadMultivaluedFieldWithTrailingWildcardToListInMap() {
		SolrDocument document = new SolrDocument();
		document.addField("multivaluedFieldMapWithTrailingWildcard_1", "trailing-map-value-1");
		document.addField("multivaluedFieldMapWithTrailingWildcard_2",
				Arrays.asList("trailing-map-value-2", "trailing-map-value-3"));

		BeanWithWildcards target = converter.read(BeanWithWildcards.class, document);
		Assert.assertEquals(2, target.multivaluedFieldMapWithTrailingWildcardArray.size());
		Assert.assertThat(target.multivaluedFieldMapWithTrailingWildcardList,
				Matchers.allOf(
						Matchers.hasEntry("multivaluedFieldMapWithTrailingWildcard_1", Arrays.asList("trailing-map-value-1")),
						Matchers.hasEntry("multivaluedFieldMapWithTrailingWildcard_2",
								Arrays.asList("trailing-map-value-2", "trailing-map-value-3"))));
	}

	@Test // DATASOLR-202
	public void testWriteDynamicMappedPropertyWithLeadingWildcard() {

		Map<String, String> values = new HashMap<String, String>(2);
		values.put("key_1", "value_1");
		values.put("key_2", "value_2");

		BeanWithDynamicMapsWildcards bean = new BeanWithDynamicMapsWildcards();
		bean.flatMapWithLeadingWildcard = values;

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertNotNull(solrDocument.getFieldValue("key_1_flatMapWithLeadingWildcard"));
		Assert.assertNotNull(solrDocument.getFieldValue("key_2_flatMapWithLeadingWildcard"));
		Assert.assertEquals(values.get("key_1"), solrDocument.getFieldValue("key_1_flatMapWithLeadingWildcard"));
		Assert.assertEquals(values.get("key_2"), solrDocument.getFieldValue("key_2_flatMapWithLeadingWildcard"));
	}

	@Test // DATASOLR-202
	public void testWriteDynamicMappedListPropertyWithLeadingWildcard() {

		Map<String, List<String>> values = new HashMap<String, List<String>>(2);
		values.put("key_1", Arrays.asList("value_1"));
		values.put("key_2", Arrays.asList("value_2", "value_3"));

		BeanWithDynamicMapsWildcards bean = new BeanWithDynamicMapsWildcards();
		bean.multivaluedFieldMapWithLeadingWildcardList = values;

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertNotNull(solrDocument.getFieldValue("key_1_multivaluedFieldMapWithLeadingWildcard"));
		Assert.assertNotNull(solrDocument.getFieldValue("key_2_multivaluedFieldMapWithLeadingWildcard"));
		Assert.assertEquals(values.get("key_1"),
				solrDocument.getFieldValues("key_1_multivaluedFieldMapWithLeadingWildcard"));
		Assert.assertEquals(values.get("key_2"),
				solrDocument.getFieldValues("key_2_multivaluedFieldMapWithLeadingWildcard"));
	}

	@Test // DATASOLR-202
	public void testWriteDynamicMappedArrayPropertyWithLeadingWildcard() {

		Map<String, String[]> values = new HashMap<String, String[]>(2);
		values.put("key_1", new String[] { "value_1" });
		values.put("key_2", new String[] { "value_2", "value_3" });

		BeanWithDynamicMapsWildcards bean = new BeanWithDynamicMapsWildcards();
		bean.multivaluedFieldMapWithLeadingWildcardArray = values;

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertNotNull(solrDocument.getFieldValue("key_1_multivaluedFieldMapWithLeadingWildcard"));
		Assert.assertNotNull(solrDocument.getFieldValue("key_2_multivaluedFieldMapWithLeadingWildcard"));
		Assert.assertEquals(Arrays.asList(values.get("key_1")),
				solrDocument.getFieldValues("key_1_multivaluedFieldMapWithLeadingWildcard"));
		Assert.assertEquals(Arrays.asList(values.get("key_2")),
				solrDocument.getFieldValues("key_2_multivaluedFieldMapWithLeadingWildcard"));
	}

	@Test // DATASOLR-308
	public void testWriteDynamicMappedPropertyWithTrailingWildcard() {

		Map<String, String> values = new HashMap<String, String>(2);
		values.put("key_1", "value_1");
		values.put("key_2", "value_2");

		BeanWithDynamicMapsWildcards bean = new BeanWithDynamicMapsWildcards();
		bean.flatMapWithTrailingWildcard = values;

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertNotNull(solrDocument.getFieldValue("flatMapWithTrailingWildcard_key_1"));
		Assert.assertNotNull(solrDocument.getFieldValue("flatMapWithTrailingWildcard_key_2"));
		Assert.assertEquals(values.get("key_1"), solrDocument.getFieldValue("flatMapWithTrailingWildcard_key_1"));
		Assert.assertEquals(values.get("key_2"), solrDocument.getFieldValue("flatMapWithTrailingWildcard_key_2"));
	}

	@Test // DATASOLR-308
	public void testWriteDynamicMappedListPropertyWithTrailingWildcard() {

		Map<String, List<String>> values = new HashMap<String, List<String>>(2);
		values.put("key_1", Arrays.asList("value_1"));
		values.put("key_2", Arrays.asList("value_2", "value_3"));

		BeanWithDynamicMapsWildcards bean = new BeanWithDynamicMapsWildcards();
		bean.multivaluedFieldMapWithTrailingWildcardList = values;

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertNotNull(solrDocument.getFieldValue("multivaluedFieldMapWithTrailingWildcard_key_1"));
		Assert.assertNotNull(solrDocument.getFieldValue("multivaluedFieldMapWithTrailingWildcard_key_2"));
		Assert.assertEquals(values.get("key_1"),
				solrDocument.getFieldValues("multivaluedFieldMapWithTrailingWildcard_key_1"));
		Assert.assertEquals(values.get("key_2"),
				solrDocument.getFieldValues("multivaluedFieldMapWithTrailingWildcard_key_2"));
	}

	@Test // DATASOLR-308
	public void testWriteDynamicMappedArrayPropertyWithTrailingWildcard() {

		Map<String, String[]> values = new HashMap<String, String[]>(2);
		values.put("key_1", new String[] { "value_1" });
		values.put("key_2", new String[] { "value_2", "value_3" });

		BeanWithDynamicMapsWildcards bean = new BeanWithDynamicMapsWildcards();
		bean.multivaluedFieldMapWithTrailingWildcardArray = values;

		SolrInputDocument solrDocument = new SolrInputDocument();
		converter.write(bean, solrDocument);

		Assert.assertNotNull(solrDocument.getFieldValue("multivaluedFieldMapWithTrailingWildcard_key_1"));
		Assert.assertNotNull(solrDocument.getFieldValue("multivaluedFieldMapWithTrailingWildcard_key_2"));
		Assert.assertEquals(Arrays.asList(values.get("key_1")),
				solrDocument.getFieldValues("multivaluedFieldMapWithTrailingWildcard_key_1"));
		Assert.assertEquals(Arrays.asList(values.get("key_2")),
				solrDocument.getFieldValues("multivaluedFieldMapWithTrailingWildcard_key_2"));
	}

	@Test // DATASOLR-202
	public void testReadFieldWithLeadingWildcardToDynamicMap() {
		SolrDocument document = new SolrDocument();
		document.addField("1_flatMapWithLeadingWildcard", "leading-map-value-1");
		document.addField("2_flatMapWithLeadingWildcard", "leading-map-value-2");

		BeanWithDynamicMapsWildcards target = converter.read(BeanWithDynamicMapsWildcards.class, document);

		Assert.assertEquals(2, target.flatMapWithLeadingWildcard.size());
		Assert.assertThat(target.flatMapWithLeadingWildcard,
				Matchers.allOf(Matchers.hasEntry("1", "leading-map-value-1"), Matchers.hasEntry("2", "leading-map-value-2")));
	}

	@Test(expected = IllegalArgumentException.class) // DATASOLR-202
	public void testReadMultivaluedFieldWithLeadingWildcardToDynamicMapWithSingleEntry() {

		SolrDocument document = new SolrDocument();
		document.addField("1_flatMapWithLeadingWildcard", Arrays.asList("leading-map-value-1", "leading-map-value-2"));
		converter.read(BeanWithDynamicMapsWildcards.class, document);
	}

	@Test // DATASOLR-202
	public void testReadFieldWithTrailingWildcardToDynamicMap() {

		SolrDocument document = new SolrDocument();
		document.addField("flatMapWithTrailingWildcard_1", "trailing-map-value-1");
		document.addField("flatMapWithTrailingWildcard_2", "trailing-map-value-2");

		BeanWithDynamicMapsWildcards target = converter.read(BeanWithDynamicMapsWildcards.class, document);

		Assert.assertEquals(2, target.flatMapWithTrailingWildcard.size());
		Assert.assertThat(target.flatMapWithTrailingWildcard,
				Matchers.allOf(Matchers.hasEntry("1", "trailing-map-value-1"), Matchers.hasEntry("2", "trailing-map-value-2")));
	}

	@Test(expected = IllegalArgumentException.class) // DATASOLR-202
	public void testReadMultivaluedFieldWithTrailingWildcardToDynamicMapWithSingleEntry() {

		SolrDocument document = new SolrDocument();
		document.addField("flatMapWithTrailingWildcard_1", Arrays.asList("trailing-map-value-1", "trailing-map-value-2"));
		converter.read(BeanWithDynamicMapsWildcards.class, document);
	}

	@Test
	public void testReadMultivaluedFieldWithLeadingWildcardToArrayInDynamicMap() {

		SolrDocument document = new SolrDocument();
		document.addField("1_multivaluedFieldMapWithLeadingWildcard", "leading-map-value-1");
		document.addField("2_multivaluedFieldMapWithLeadingWildcard",
				Arrays.asList("leading-map-value-2", "leading-map-value-3"));

		BeanWithDynamicMapsWildcards target = converter.read(BeanWithDynamicMapsWildcards.class, document);

		Assert.assertEquals(2, target.multivaluedFieldMapWithLeadingWildcardArray.size());
		Assert.assertThat(target.multivaluedFieldMapWithLeadingWildcardArray,
				Matchers.allOf(Matchers.hasEntry("1", new String[] { "leading-map-value-1" }),
						Matchers.hasEntry("2", new String[] { "leading-map-value-2", "leading-map-value-3" })));
	}

	@Test // DATASOLR-202
	public void testReadMultivaluedFieldWithLeadingWildcardToListInDynamicMap() {

		SolrDocument document = new SolrDocument();
		document.addField("1_multivaluedFieldMapWithLeadingWildcard", "leading-map-value-1");
		document.addField("2_multivaluedFieldMapWithLeadingWildcard",
				Arrays.asList("leading-map-value-2", "leading-map-value-3"));

		BeanWithDynamicMapsWildcards target = converter.read(BeanWithDynamicMapsWildcards.class, document);

		Assert.assertEquals(2, target.multivaluedFieldMapWithLeadingWildcardList.size());
		Assert.assertThat(target.multivaluedFieldMapWithLeadingWildcardList,
				Matchers.allOf(Matchers.hasEntry("1", Arrays.asList("leading-map-value-1")),
						Matchers.hasEntry("2", Arrays.asList("leading-map-value-2", "leading-map-value-3"))));
	}

	@Test // DATASOLR-202
	public void testReadMultivaluedFieldWithTrailingWildcardToArrayInDynamicMap() {

		SolrDocument document = new SolrDocument();
		document.addField("multivaluedFieldMapWithTrailingWildcard_1", "trailing-map-value-1");
		document.addField("multivaluedFieldMapWithTrailingWildcard_2",
				Arrays.asList("trailing-map-value-2", "trailing-map-value-3"));

		BeanWithDynamicMapsWildcards target = converter.read(BeanWithDynamicMapsWildcards.class, document);

		Assert.assertEquals(2, target.multivaluedFieldMapWithTrailingWildcardArray.size());
		Assert.assertThat(target.multivaluedFieldMapWithTrailingWildcardArray,
				Matchers.allOf(Matchers.hasEntry("1", new String[] { "trailing-map-value-1" }),
						Matchers.hasEntry("2", new String[] { "trailing-map-value-2", "trailing-map-value-3" })));
	}

	@Test // DATASOLR-202
	public void testReadMultivaluedFieldWithTrailingWildcardToListInDynamicMap() {

		SolrDocument document = new SolrDocument();
		document.addField("multivaluedFieldMapWithTrailingWildcard_1", "trailing-map-value-1");
		document.addField("multivaluedFieldMapWithTrailingWildcard_2",
				Arrays.asList("trailing-map-value-2", "trailing-map-value-3"));

		BeanWithDynamicMapsWildcards target = converter.read(BeanWithDynamicMapsWildcards.class, document);

		Assert.assertEquals(2, target.multivaluedFieldMapWithTrailingWildcardArray.size());
		Assert.assertThat(target.multivaluedFieldMapWithTrailingWildcardList,
				Matchers.allOf(Matchers.hasEntry("1", Arrays.asList("trailing-map-value-1")),
						Matchers.hasEntry("2", Arrays.asList("trailing-map-value-2", "trailing-map-value-3"))));
	}

	@Test
	public void testReadWithWithFieldAnnotationOnSetters() {
		SolrDocument document = new SolrDocument();
		document.addField("name", "value_1");
		document.addField("namedFieldFieldName", "value_2");

		BeanWithFieldAnnotationOnSetter target = converter.read(BeanWithFieldAnnotationOnSetter.class, document);
		Assert.assertEquals("value_1", target.name);
		Assert.assertEquals("value_2", target.namedField);
	}

	@Test
	public void testReadBeanWithInheritance() {
		SolrDocument document = new SolrDocument();
		document.addField("stringProperty", "some string");
		document.addField("intProperty", 10);

		BeanWithInteritance target = converter.read(BeanWithInteritance.class, document);

		Assert.assertEquals(document.getFieldValue("stringProperty"), target.stringProperty);
		Assert.assertEquals(document.getFieldValue("intProperty"), target.intProperty);
	}

	@Test
	public void testReadToMap() {
		SolrDocument document = new SolrDocument();
		document.addField("map_1", "value-1");
		document.addField("map_2", "value-2");

		BeanWithDifferentMaps target = converter.read(BeanWithDifferentMaps.class, document);
		Assert.assertThat(target.mapProperty, IsInstanceOf.instanceOf(HashMap.class));
	}

	@Test
	public void testReadToHashMapMap() {
		SolrDocument document = new SolrDocument();
		document.addField("hashMap_1", "value-1");
		document.addField("hashMap_2", "value-2");

		BeanWithDifferentMaps target = converter.read(BeanWithDifferentMaps.class, document);
		Assert.assertThat(target.hashMapProperty, IsInstanceOf.instanceOf(HashMap.class));
	}

	@Test
	public void testReadToLinkedHashMapMap() {
		SolrDocument document = new SolrDocument();
		document.addField("linkedHashMap_1", "value-1");
		document.addField("linkedHashMap_2", "value-2");

		BeanWithDifferentMaps target = converter.read(BeanWithDifferentMaps.class, document);
		Assert.assertThat(target.linkedHashMapProperty, IsInstanceOf.instanceOf(LinkedHashMap.class));
	}

	@Test
	public void testReadOverlappingWildcradsIgnoresNoWildcardFields() {
		SolrDocument document = new SolrDocument();
		document.addField("acme_s_com", "value_1");

		BeanWithOverlappingWildcards target = converter.read(BeanWithOverlappingWildcards.class, document);
		Assert.assertThat(target.justAString, IsEqual.equalTo("value_1"));
		Assert.assertThat(target.keys, IsNull.nullValue(Map.class));
		Assert.assertThat(target.strings, IsNull.nullValue(Map.class));
	}

	@Test
	public void testReadOverlappingWildcradsShouldPlaceStringInMultipleMatchingFields() {
		SolrDocument document = new SolrDocument();
		document.addField("some_key_s", "value_1");

		BeanWithOverlappingWildcards target = converter.read(BeanWithOverlappingWildcards.class, document);
		Assert.assertThat(target.justAString, IsNull.nullValue(String.class));
		Assert.assertThat(target.keys, IsMapContaining.hasEntry("some_key_s", "value_1"));
		Assert.assertThat(target.strings, IsMapContaining.hasEntry("some_key_s", "value_1"));
	}

	@Test
	public void testReadOverlappingWildcradsShouldPlaceStringInOnlyOneMatchingFieldWhenNoFullMatch() {
		SolrDocument document = new SolrDocument();
		document.addField("some_different_s", "value_1");

		BeanWithOverlappingWildcards target = converter.read(BeanWithOverlappingWildcards.class, document);
		Assert.assertThat(target.justAString, IsNull.nullValue(String.class));
		Assert.assertThat(target.keys, IsNull.nullValue(Map.class));
		Assert.assertThat(target.strings, IsMapContaining.hasEntry("some_different_s", "value_1"));
	}

	@Test
	public void testReadOverlappingWildcardsShouldMatchWildCardAtEndOfPattern() {
		SolrDocument document = new SolrDocument();
		document.addField("_s_prefixed", "value_1");

		BeanWithOverlappingWildcards target = converter.read(BeanWithOverlappingWildcards.class, document);
		Assert.assertThat(target.justAString, IsNull.nullValue(String.class));
		Assert.assertThat(target.keys, IsNull.nullValue(Map.class));
		Assert.assertThat(target.strings, IsNull.nullValue(Map.class));
		Assert.assertThat(target.stringWithPrefix, IsEqual.equalTo("value_1"));
	}

	@Test
	public void shouldConvertTypesWithinCollectionsOfMapCorrectly() {

		SolrDocument document = new SolrDocument();
		document.addField("fieldWithDateTimeInListOfMap_d", new Date(60264684000000L));

		BeanWithWildcardsOnTypesThatRequireConversion target = converter
				.read(BeanWithWildcardsOnTypesThatRequireConversion.class, document);
		Assert.assertThat(target.fieldWithDateTimeInListOfMap.get("fieldWithDateTimeInListOfMap_d"),
				IsInstanceOf.instanceOf(List.class));
		Assert.assertThat(target.fieldWithDateTimeInListOfMap.get("fieldWithDateTimeInListOfMap_d").get(0),
				IsInstanceOf.instanceOf(DateTime.class));
	}

	@SuppressWarnings("unchecked")
	@Test // DATASOLR-171
	public void shouldUseConstructorCorrectlyWhenMultivaluedConvertedToArray() {

		SolrDocument document = new SolrDocument();
		document.addField("array", Arrays.asList("v-1", "v-2"));

		BeanWithArrayConstructor target = converter.read(BeanWithArrayConstructor.class, document);
		Assert.assertThat(target.fields, IsEqual.equalTo(((List<String>) document.getFieldValue("array")).toArray()));
	}

	@Test // DATASOLR-235
	public void testRegularFieldBoosting() {

		BeanWithBoost bean = new BeanWithBoost();
		bean.boostedRegularField = "value";
		bean.regularField = "value";

		Map<String, SolrInputField> target = new HashMap<String, SolrInputField>();
		converter.write(bean, target);

		// configured boost
		Assert.assertEquals(0.5f, target.get("boostedRegularField").getBoost(), 0);
		// default boost
		Assert.assertEquals(1, target.get("regularField").getBoost(), 0);
	}

	@Test // DATASOLR-235
	public void testMapWildcardFieldBoosting() {

		BeanWithBoost bean = new BeanWithBoost();

		bean.boostedMapWildcardField = new HashMap<String, String>();
		bean.boostedMapWildcardField.put("val1_boostedMapWildcardField", "value");
		bean.boostedMapWildcardField.put("val2_boostedMapWildcardField", "value");

		bean.mapWildcardField = new HashMap<String, String>();
		bean.mapWildcardField.put("val1_mapWildcardField", "value");
		bean.mapWildcardField.put("val2_mapWildcardField", "value");

		Map<String, SolrInputField> target = new HashMap<String, SolrInputField>();
		converter.write(bean, target);

		// configured boost
		Assert.assertEquals(0.5f, target.get("val1_boostedMapWildcardField").getBoost(), 0);
		Assert.assertEquals(0.5f, target.get("val2_boostedMapWildcardField").getBoost(), 0);
		// default boost
		Assert.assertEquals(1, target.get("val1_mapWildcardField").getBoost(), 0);
		Assert.assertEquals(1, target.get("val2_mapWildcardField").getBoost(), 0);
	}

	@Test // DATASOLR-235
	public void testDocumentBoosting() {
		SolrInputDocument boostedDocument = new SolrInputDocument();
		SolrInputDocument regularDocument = new SolrInputDocument();

		converter.write(new BeanWithBoost(), boostedDocument);
		converter.write(new BeanBase(), regularDocument);

		// configured boost
		Assert.assertEquals(0.5f, boostedDocument.getDocumentBoost(), 0);
		// default boost
		Assert.assertEquals(1, regularDocument.getDocumentBoost(), 0);
	}

	@Test // DATASOLR-375
	public void writeEnumValues() {

		BeanWithDefaultTypes source = new BeanWithDefaultTypes();
		source.enumProperty = SomeEnum.E2;

		SolrInputDocument sink = new SolrInputDocument();
		converter.write(source, sink);

		Assert.assertThat(sink.getFieldValue("enumProperty"), IsEqual.equalTo((Object) SomeEnum.E2.name()));
	}

	@Test // DATASOLR-407
	public void writeListOfEnumValues() {

		BeanWithDefaultTypes source = new BeanWithDefaultTypes();
		source.enumList = Arrays.asList(SomeEnum.E2, SomeEnum.E1);

		SolrInputDocument sink = new SolrInputDocument();
		converter.write(source, sink);

		Assert.assertThat(sink.getFieldValues("enumList"), IsIterableContainingInOrder.<Object>contains("E2", "E1"));
	}

	@Test // DATASOLR-407
	public void readListOfEnumValues() {

		SolrDocument source = new SolrDocument();
		source.addField("enumList", Arrays.asList("E2", "E1"));

		BeanWithDefaultTypes target = converter.read(BeanWithDefaultTypes.class, source);

		Assert.assertThat(target.enumList, IsIterableContainingInOrder.contains(SomeEnum.E2, SomeEnum.E1));
	}

	public static class BeanWithoutAnnotatedFields {

		String notIndexedProperty;

	}

	public static class BeanWithCustomTypes {

		@Field Point location;

		@Field List<Point> locations;

	}

	public static class BeanWithSimpleTypes {

		@Field int simpleIntProperty;

		@Field boolean simpleBoolProperty;

		@Field float simpleFloatProperty;

	}

	public static class BeanWithNamedFields {

		@Field("namedProperty") String name;

	}

	public static class BeanWithFieldAnnotationOnSetter {

		String name;

		String namedField;

		@Field
		public void setName(String name) {
			this.name = name;
		}

		@Field("namedFieldFieldName")
		public void setNamedField(String namedField) {
			this.namedField = namedField;
		}

	}

	public static class BeanWithDefaultTypes {

		@Field String stringProperty;

		@Field Integer intProperty;

		@Field List<String> listOfString;

		@Field String[] arrayOfString;

		@Field Date dateProperty;

		@Field SomeEnum enumProperty;

		@Field List<SomeEnum> enumList;

	}

	enum SomeEnum {
		E1, E2
	}

	public static class BeanWithCatchAllField {

		@Indexed(readonly = true) @Field("stringProperty_*") String[] allStringProperties;

	}

	public static class BeanWithConstructor {

		@Field String stringProperty;

		@Field Integer intProperty;

		public BeanWithConstructor(String stringProperty, Integer intProperty) {
			super();
			this.stringProperty = stringProperty;
			this.intProperty = intProperty;
		}

	}

	public static class BeanWithWildcardsOnTypesThatRequireConversion {

		@Field("fieldWithDateTimeInListOfMap_*") Map<String, List<DateTime>> fieldWithDateTimeInListOfMap;
	}

	public class BeanWithWildcards {

		@Field("field_with_trailing_wildcard_*") String filedWithTrailingWildcard;

		@Field("*_field_with_leading_wildcard") String fieldWithLeadingWildcard;

		@Field("listFieldWithTrailingWildcard_*") List<String> listFieldWithTrailingWildcard;

		@Field("arrayFieldWithTrailingWildcard_*") String[] arrayFieldWithTrailingWildcard;

		@Field("*_listFieldWithLeadingWildcard") List<String> listFieldWithLeadingWildcard;

		@Field("*_arrayFieldWithTrailingWildcard") String[] arrayFieldWithLeadingWildcard;

		@Field("*_flatMapWithLeadingWildcard") Map<String, String> flatMapWithLeadingWildcard;

		@Field("flatMapWithTrailingWildcard_*") Map<String, String> flatMapWithTrailingWildcard;

		@Field("*_multivaluedFieldMapWithLeadingWildcard") Map<String, String[]> multivaluedFieldMapWithLeadingWildcardArray;

		@Field("*_multivaluedFieldMapWithLeadingWildcard") Map<String, List<String>> multivaluedFieldMapWithLeadingWildcardList;

		@Field("multivaluedFieldMapWithTrailingWildcard_*") Map<String, String[]> multivaluedFieldMapWithTrailingWildcardArray;

		@Field("multivaluedFieldMapWithTrailingWildcard_*") Map<String, List<String>> multivaluedFieldMapWithTrailingWildcardList;

	}

	public class BeanWithDynamicMapsWildcards {

		@Field("*_flatMapWithLeadingWildcard") @Dynamic Map<String, String> flatMapWithLeadingWildcard;

		@Field("flatMapWithTrailingWildcard_*") @Dynamic Map<String, String> flatMapWithTrailingWildcard;

		@Field("*_multivaluedFieldMapWithLeadingWildcard") @Dynamic Map<String, String[]> multivaluedFieldMapWithLeadingWildcardArray;

		@Field("*_multivaluedFieldMapWithLeadingWildcard") @Dynamic Map<String, List<String>> multivaluedFieldMapWithLeadingWildcardList;

		@Field("multivaluedFieldMapWithTrailingWildcard_*") @Dynamic Map<String, String[]> multivaluedFieldMapWithTrailingWildcardArray;

		@Field("multivaluedFieldMapWithTrailingWildcard_*") @Dynamic Map<String, List<String>> multivaluedFieldMapWithTrailingWildcardList;

	}

	public static class BeanWithFieldsExcludedFromIndexing {

		@Indexed(readonly = true) @Field String transientField;

	}

	public static class BeanBase {

		@Field String stringProperty;

	}

	public static class BeanWithInteritance extends BeanBase {

		@Field Integer intProperty;

	}

	public static class BeanWithDifferentMaps {

		@Field("map_*") Map<String, String> mapProperty;

		@Field("hashMap_*") HashMap<String, String> hashMapProperty;

		@Field("linkedHashMap_*") LinkedHashMap<String, String> linkedHashMapProperty;
	}

	public static class BeanWithOverlappingWildcards {

		@Field("*_key_s") Map<String, String> keys;

		@Field("*_s") Map<String, String> strings;

		@Field("acme_s_com") String justAString;

		@Field("_s*") String stringWithPrefix;

	}

	public static class BeanWithArrayConstructor {

		@Field("array") String[] fields;

		public BeanWithArrayConstructor(String[] fields) {
			this.fields = fields;
		}

	}

	@org.springframework.data.solr.core.mapping.SolrDocument(boost = 0.5f)
	public static class BeanWithBoost {

		@Indexed(boost = 0.5f) String boostedRegularField;

		@Indexed(name = "*_boostedMapWildcardField", boost = 0.5f) Map<String, String> boostedMapWildcardField;

		@Indexed String regularField;

		@Indexed(name = "*_mapWildcardField") Map<String, String> mapWildcardField;
	}

}
