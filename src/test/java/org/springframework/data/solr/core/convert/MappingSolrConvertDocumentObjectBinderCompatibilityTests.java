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
package org.springframework.data.solr.core.convert;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.util.NamedList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;

/**
 * Test compatibility with {@link org.apache.solr.client.solrj.beans.DocumentObjectBinder} by running tests against both
 * {@link MappingSolrConverter} and {@link SolrJConverter} <br/>
 * Borrowed and modified from: <a href=
 * "https://svn.apache.org/repos/asf/lucene/dev/trunk/solr/solrj/src/test/org/apache/solr/client/solrj/beans/TestDocumentObjectBinder.java"
 * >org/apache/solr/client/solrj/beans/TestDocumentObjectBinder.java</a> <br />
 *
 * @author Christoph Strobl
 */
@RunWith(Parameterized.class)
public class MappingSolrConvertDocumentObjectBinderCompatibilityTests {

	private SolrConverter converter;

	public MappingSolrConvertDocumentObjectBinderCompatibilityTests(SolrConverter converter) {
		this.converter = converter;
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { { new SolrJConverter() },
				{ new MappingSolrConverter(new SimpleSolrMappingContext()) } };
		return Arrays.asList(data);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSimple() throws Exception {
		XMLResponseParser parser = new XMLResponseParser();
		NamedList<Object> nl = parser.processResponse(new StringReader(xml));
		QueryResponse res = new QueryResponse(nl, null);

		SolrDocumentList solDocList = res.getResults();
		List<Item> l = getBeans(solDocList);
		assertEquals(solDocList.size(), l.size());
		assertEquals(solDocList.get(0).getFieldValue("features"), l.get(0).features);

		Item item = new Item();
		item.id = "aaa";
		item.categories = new String[] { "aaa", "bbb", "ccc" };
		SolrInputDocument out = new SolrInputDocument();
		converter.write(item, out);

		assertEquals(item.id, out.getFieldValue("id"));
		SolrInputField catfield = out.getField("cat");
		assertEquals(3, catfield.getValueCount());

		List<String> catValues = (List<String>) catfield.getValue();
		assertEquals("aaa", catValues.get(0));
		assertEquals("bbb", catValues.get(1));
		assertEquals("ccc", catValues.get(2));
	}

	@Test
	public void testSingleVal4Array() {
		SolrDocument d = new SolrDocument();
		d.setField("cat", "hello");
		Item item = converter.read(Item.class, d);
		assertEquals("hello", item.categories[0]);
	}

	@Test
	public void testDynamicFieldBinding() {
		XMLResponseParser parser = new XMLResponseParser();
		NamedList<Object> nl = parser.processResponse(new StringReader(xml));
		QueryResponse res = new QueryResponse(nl, null);

		List<Item> l = getBeans(res.getResults());

		Item item = l.get(3);

		Assert.assertArrayEquals(new String[] { "Mobile Store", "iPod Store", "CCTV Store" }, item.getAllSuppliers());
		Assert.assertTrue(item.supplier.containsKey("supplier_1"));
		Assert.assertTrue(item.supplier.containsKey("supplier_2"));
		assertEquals(2, item.supplier.size());

		List<String> supplierOne = item.supplier.get("supplier_1");
		assertEquals("Mobile Store", supplierOne.get(0));
		assertEquals("iPod Store", supplierOne.get(1));

		List<String> supplierTwo = item.supplier.get("supplier_2");
		assertEquals("CCTV Store", supplierTwo.get(0));
	}

	@Test // DATASOLR-87, DATASOLR-309
	public void testToAndFromSolrDocument() {

		Item item = new Item();
		item.id = "one";
		item.inStock = false;
		item.categories = new String[] { "aaa", "bbb", "ccc" };
		item.features = Arrays.asList(item.categories);
		List<String> supA = Arrays.asList("supA1", "supA2", "supA3");
		List<String> supB = Arrays.asList("supB1", "supB2", "supB3");
		item.supplier = new HashMap<>();
		item.supplier.put("supplier_supA", supA);
		item.supplier.put("supplier_supB", supB);

		item.supplier_simple = new HashMap<>();
		item.supplier_simple.put("sup_simple_supA", "supA_val");
		item.supplier_simple.put("sup_simple_supB", "supB_val");

		SolrInputDocument doc = new SolrInputDocument();

		converter.write(item, doc);

		SolrDocumentList docs = new SolrDocumentList();
		docs.add(toSolrDocument(doc));
		Item out = converter.read(Item.class, docs.get(0));

		// make sure it came out the same
		assertEquals(item.id, out.id);
		assertEquals(item.inStock, out.inStock);
		assertEquals(item.categories.length, out.categories.length);
		assertEquals(item.features, out.features);
		assertEquals(supA, out.supplier.get("supplier_supA"));
		assertEquals(supB, out.supplier.get("supplier_supB"));
		assertEquals(item.supplier_simple.get("sup_simple_supB"), out.supplier_simple.get("sup_simple_supB"));

		// put back "out" as Bean, to see if both ways work as you would expect
		// but the Field that "allSuppliers" need to be cleared, as it is just for
		// retrieving data, not to post data
		out.allSuppliers = null;
		SolrInputDocument doc1 = new SolrInputDocument();
		converter.write(out, doc1);

		SolrDocumentList docs1 = new SolrDocumentList();
		docs1.add(toSolrDocument(doc1));
		Item out1 = converter.read(Item.class, docs1.get(0));

		assertEquals(item.id, out1.id);
		assertEquals(item.inStock, out1.inStock);
		assertEquals(item.categories.length, out1.categories.length);
		assertEquals(item.features, out1.features);

		assertEquals(item.supplier_simple.get("sup_simple_supB"), out1.supplier_simple.get("sup_simple_supB"));

		assertEquals(supA, out1.supplier.get("supplier_supA"));
		assertEquals(supB, out1.supplier.get("supplier_supB"));
	}

	private List<Item> getBeans(SolrDocumentList solDocList) {
		return converter.read(solDocList, Item.class);
	}

	@Test // DATASOLR-394
	public void testChild() throws Exception {

		SingleValueChild in = new SingleValueChild();
		in.id = "1";
		in.child = new Child();
		in.child.id = "1.0";
		in.child.name = "Name One";

		SolrInputDocument solrInputDoc = new SolrInputDocument();
		new SolrJConverter().write(in, solrInputDoc);
		SolrDocument solrDoc = toSolrDocument(solrInputDoc);

		assertEquals(1, solrInputDoc.getChildDocuments().size());
		assertEquals(1, solrDoc.getChildDocuments().size());

		SingleValueChild out = converter.read(SingleValueChild.class, toSolrDocument(solrInputDoc));

		assertEquals(in.id, out.id);
		assertEquals(in.child.id, out.child.id);
		assertEquals(in.child.name, out.child.name);

		ListChild listIn = new ListChild();
		listIn.id = "2";
		Child child = new Child();
		child.id = "1.1";
		child.name = "Name Two";
		listIn.child = Arrays.asList(in.child, child);

		solrInputDoc = new SolrInputDocument();
		converter.write(listIn, solrInputDoc);

		solrDoc = toSolrDocument(solrInputDoc);

		assertEquals(2, solrInputDoc.getChildDocuments().size());
		assertEquals(2, solrDoc.getChildDocuments().size());

		ListChild listOut = converter.read(ListChild.class, toSolrDocument(solrInputDoc));

		assertEquals(listIn.id, listOut.id);
		assertEquals(listIn.child.get(0).id, listOut.child.get(0).id);
		assertEquals(listIn.child.get(0).name, listOut.child.get(0).name);
		assertEquals(listIn.child.get(1).id, listOut.child.get(1).id);
		assertEquals(listIn.child.get(1).name, listOut.child.get(1).name);

		ArrayChild arrIn = new ArrayChild();
		arrIn.id = "3";
		arrIn.child = new Child[] { in.child, child };

		solrInputDoc = new SolrInputDocument();
		converter.write(arrIn, solrInputDoc);

		solrDoc = toSolrDocument(solrInputDoc);

		assertEquals(2, solrInputDoc.getChildDocuments().size());
		assertEquals(2, solrDoc.getChildDocuments().size());

		ArrayChild arrOut = converter.read(ArrayChild.class, solrDoc);

		assertEquals(arrIn.id, arrOut.id);
		assertEquals(arrIn.child[0].id, arrOut.child[0].id);
		assertEquals(arrIn.child[0].name, arrOut.child[0].name);
		assertEquals(arrIn.child[1].id, arrOut.child[1].id);
		assertEquals(arrIn.child[1].name, arrOut.child[1].name);

	}

	public static class Child {

		@Field String id;

		@Field String name;

	}

	public static class SingleValueChild {

		@Field String id;

		@Field(child = true) Child child;
	}

	public static class ListChild {

		@Field String id;

		@Field(child = true) List<Child> child;

	}

	public static class ArrayChild {

		@Field String id;

		@Field(child = true) Child[] child;
	}

	private static SolrDocument toSolrDocument(SolrInputDocument d) {

		SolrDocument doc = new SolrDocument();
		for (SolrInputField field : d) {
			doc.setField(field.getName(), field.getValue());
		}
		if (d.getChildDocuments() != null) {
			for (SolrInputDocument in : d.getChildDocuments()) {
				doc.addChildDocument(toSolrDocument(in));
			}
		}
		return doc;
	}

	public static class Item {
		@Field String id;

		@Field("cat") //
		String[] categories;

		@Field //
		List<String> features;

		@Field //
		Date timestamp;

		@Field("highway_mileage") //
		int mwyMileage;

		boolean inStock;

		@Field("supplier_*") //
		Map<String, List<String>> supplier;

		@Field("sup_simple_*") //
		Map<String, String> supplier_simple;

		@Indexed(readonly = true) //
		private String[] allSuppliers;

		@Field("supplier_*")
		public void setAllSuppliers(String[] allSuppliers) {
			this.allSuppliers = allSuppliers;
		}

		public String[] getAllSuppliers() {
			return this.allSuppliers;
		}

		@Field
		public void setInStock(Boolean b) {
			inStock = b;
		}

		// required if you want to fill SolrDocuments with the same annotaion...
		public boolean isInStock() {
			return inStock;
		}
	}

	public static class NotGettableItem {
		@Field //
		String id;

		@SuppressWarnings("unused") //
		private boolean inStock;

		private String aaa;

		@Field
		public void setInStock(Boolean b) {
			inStock = b;
		}

		public String getAaa() {
			return aaa;
		}

		@Field
		public void setAaa(String aaa) {
			this.aaa = aaa;
		}
	}

	public static final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<response>"
			+ "<lst name=\"responseHeader\"><int name=\"status\">0</int><int name=\"QTime\">0</int><lst name=\"params\"><str name=\"start\">0</str><str name=\"q\">*:*\n"
			+ "</str><str name=\"version\">2.2</str><str name=\"rows\">4</str></lst></lst><result name=\"response\" numFound=\"26\" start=\"0\"><doc><arr name=\"cat\">"
			+ "<str>electronics</str><str>hard drive</str></arr><arr name=\"features\"><str>7200RPM, 8MB cache, IDE Ultra ATA-133</str>"
			+ "<str>NoiseGuard, SilentSeek technology, Fluid Dynamic Bearing (FDB) motor</str></arr><str name=\"id\">SP2514N</str>"
			+ "<bool name=\"inStock\">true</bool><str name=\"manu\">Samsung Electronics Co. Ltd.</str><str name=\"name\">Samsung SpinPoint P120 SP2514N - hard drive - 250 GB - ATA-133</str>"
			+ "<int name=\"popularity\">6</int><float name=\"price\">92.0</float><str name=\"sku\">SP2514N</str><date name=\"timestamp\">2008-04-16T10:35:57.078Z</date></doc>"
			+ "<doc><arr name=\"cat\"><str>electronics</str><str>hard drive</str></arr><arr name=\"features\"><str>SATA 3.0Gb/s, NCQ</str><str>8.5ms seek</str>"
			+ "<str>16MB cache</str></arr><str name=\"id\">6H500F0</str><bool name=\"inStock\">true</bool><str name=\"manu\">Maxtor Corp.</str>"
			+ "<str name=\"name\">Maxtor DiamondMax 11 - hard drive - 500 GB - SATA-300</str><int name=\"popularity\">6</int><float name=\"price\">350.0</float>"
			+ "<str name=\"sku\">6H500F0</str><date name=\"timestamp\">2008-04-16T10:35:57.109Z</date></doc><doc><arr name=\"cat\"><str>electronics</str>"
			+ "<str>connector</str></arr><arr name=\"features\"><str>car power adapter, white</str></arr><str name=\"id\">F8V7067-APL-KIT</str>"
			+ "<bool name=\"inStock\">false</bool><str name=\"manu\">Belkin</str><str name=\"name\">Belkin Mobile Power Cord for iPod w/ Dock</str>"
			+ "<int name=\"popularity\">1</int><float name=\"price\">19.95</float><str name=\"sku\">F8V7067-APL-KIT</str>"
			+ "<date name=\"timestamp\">2008-04-16T10:35:57.140Z</date><float name=\"weight\">4.0</float></doc><doc>"
			+ "<arr name=\"cat\"><str>electronics</str><str>connector</str></arr><arr name=\"features\">"
			+ "<str>car power adapter for iPod, white</str></arr><str name=\"id\">IW-02</str><bool name=\"inStock\">false</bool>"
			+ "<str name=\"manu\">Belkin</str><str name=\"name\">iPod &amp; iPod Mini USB 2.0 Cable</str>"
			+ "<int name=\"popularity\">1</int><float name=\"price\">11.5</float><str name=\"sku\">IW-02</str>"
			+ "<str name=\"supplier_1\">Mobile Store</str><str name=\"supplier_1\">iPod Store</str><str name=\"supplier_2\">CCTV Store</str>"
			+ "<date name=\"timestamp\">2008-04-16T10:35:57.140Z</date><float name=\"weight\">2.0</float></doc></result>\n"
			+ "</response>";
}
