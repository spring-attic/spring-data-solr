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
package org.springframework.data.solr.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.geo.Point;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;
import org.springframework.data.solr.ExampleSolrBean;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.DistanceField;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithFacetParameters;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Function;
import org.springframework.data.solr.core.query.GroupOptions;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.IfFunction;
import org.springframework.data.solr.core.query.Join;
import org.springframework.data.solr.core.query.PartialUpdate;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.Query.Operator;
import org.springframework.data.solr.core.query.QueryFunction;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.SimpleTermsQuery;
import org.springframework.data.solr.core.query.SimpleUpdateField;
import org.springframework.data.solr.core.query.TermsQuery;
import org.springframework.data.solr.core.query.Update;
import org.springframework.data.solr.core.query.UpdateAction;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.FacetPivotFieldEntry;
import org.springframework.data.solr.core.query.result.FacetQueryEntry;
import org.springframework.data.solr.core.query.result.GroupEntry;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.GroupResult;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.core.query.result.TermsFieldEntry;
import org.springframework.data.solr.core.query.result.TermsPage;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;

/**
 * @author Christoph Strobl
 * @author Andrey Paramonov
 * @author Francisco Spaeth
 */
public class ITestSolrTemplate extends AbstractITestWithEmbeddedSolrServer {

	private static final Query DEFAULT_BEAN_OBJECT_QUERY = new SimpleQuery(new Criteria("id").is(DEFAULT_BEAN_ID));
	private static final Query ALL_DOCUMENTS_QUERY = new SimpleQuery(new SimpleStringCriteria("*:*"));

	private SolrTemplate solrTemplate;

	@Before
	public void setUp() throws IOException, ParserConfigurationException, SAXException {
		solrTemplate = new SolrTemplate(solrServer, null);
		solrTemplate.afterPropertiesSet();
	}

	@After
	public void tearDown() {
		solrTemplate.delete(ALL_DOCUMENTS_QUERY);
		solrTemplate.commit();
	}

	@Test
	public void testBeanLifecycle() {
		ExampleSolrBean toInsert = createDefaultExampleBean();

		solrTemplate.saveBean(toInsert);
		ExampleSolrBean recalled = solrTemplate.queryForObject(new SimpleQuery(new Criteria("id").is("1")),
				ExampleSolrBean.class);
		Assert.assertNull(recalled);
		solrTemplate.commit();

		recalled = solrTemplate.queryForObject(new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
		Assert.assertEquals(toInsert.getId(), recalled.getId());

		solrTemplate.deleteById(toInsert.getId());
		recalled = solrTemplate.queryForObject(new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
		Assert.assertEquals(toInsert.getId(), recalled.getId());

		solrTemplate.commit();
		recalled = solrTemplate.queryForObject(new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
		Assert.assertNull(recalled);
	}

	@Test
	public void testPartialUpdateSetSingleValueField() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		solrTemplate.saveBean(toInsert);
		solrTemplate.commit();

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.add("name", "updated-name");
		solrTemplate.saveBean(update);
		solrTemplate.commit();

		Assert.assertEquals(1, solrTemplate.count(ALL_DOCUMENTS_QUERY));

		ExampleSolrBean recalled = solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);

		Assert.assertEquals(toInsert.getId(), recalled.getId());
		Assert.assertEquals("updated-name", recalled.getName());
		Assert.assertEquals(toInsert.getPopularity(), recalled.getPopularity());
	}

	@Test
	public void testPartialUpdateAddSingleValueToMultivalueField() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		toInsert.setCategory(Arrays.asList("nosql"));
		solrTemplate.saveBean(toInsert);
		solrTemplate.commit();

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.add(new SimpleUpdateField("cat", "spring-data-solr", UpdateAction.ADD));
		solrTemplate.saveBean(update);
		solrTemplate.commit();

		Assert.assertEquals(1, solrTemplate.count(ALL_DOCUMENTS_QUERY));

		ExampleSolrBean recalled = solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);

		Assert.assertEquals(toInsert.getId(), recalled.getId());

		Assert.assertEquals(2, recalled.getCategory().size());
		Assert.assertEquals(Arrays.asList("nosql", "spring-data-solr"), recalled.getCategory());

		Assert.assertEquals(toInsert.getName(), recalled.getName());
		Assert.assertEquals(toInsert.getPopularity(), recalled.getPopularity());
	}

	@Test
	public void testPartialUpdateIncSingleValue() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		solrTemplate.saveBean(toInsert);
		solrTemplate.commit();

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.add(new SimpleUpdateField("popularity", 1, UpdateAction.INC));
		solrTemplate.saveBean(update);
		solrTemplate.commit();

		Assert.assertEquals(1, solrTemplate.count(ALL_DOCUMENTS_QUERY));

		ExampleSolrBean recalled = solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);

		Assert.assertEquals(toInsert.getId(), recalled.getId());

		Assert.assertEquals(1, recalled.getCategory().size());

		Assert.assertEquals(toInsert.getName(), recalled.getName());
		Assert.assertEquals(Integer.valueOf(11), recalled.getPopularity());
	}

	@Test
	public void testPartialUpdateSetMultipleValuesToMultivaluedField() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		solrTemplate.saveBean(toInsert);
		solrTemplate.commit();

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setValueOfField("cat", Arrays.asList("spring", "data", "solr"));
		solrTemplate.saveBean(update);
		solrTemplate.commit();

		Assert.assertEquals(1, solrTemplate.count(ALL_DOCUMENTS_QUERY));

		ExampleSolrBean recalled = solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);

		Assert.assertEquals(toInsert.getId(), recalled.getId());

		Assert.assertEquals(3, recalled.getCategory().size());
		Assert.assertEquals(Arrays.asList("spring", "data", "solr"), recalled.getCategory());

		Assert.assertEquals(toInsert.getName(), recalled.getName());
		Assert.assertEquals(toInsert.getPopularity(), recalled.getPopularity());
	}

	@Test
	public void testPartialUpdateSetEmptyCollectionToMultivaluedFieldRemovesValuesFromDocument() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setCategory(Arrays.asList("spring", "data", "solr"));
		toInsert.setPopularity(10);
		solrTemplate.saveBean(toInsert);
		solrTemplate.commit();

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setValueOfField("cat", Collections.emptyList());

		solrTemplate.saveBean(update);
		solrTemplate.commit();

		Assert.assertEquals(1, solrTemplate.count(ALL_DOCUMENTS_QUERY));

		ExampleSolrBean recalled = solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);
		Assert.assertEquals(toInsert.getId(), recalled.getId());

		Assert.assertEquals(0, recalled.getCategory().size());

		Assert.assertEquals(toInsert.getName(), recalled.getName());
		Assert.assertEquals(toInsert.getPopularity(), recalled.getPopularity());
	}

	@Test
	public void testPartialUpdateAddMultipleValuesToMultivaluedField() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		solrTemplate.saveBean(toInsert);
		solrTemplate.commit();

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.addValueToField("cat", Arrays.asList("spring", "data", "solr"));

		solrTemplate.saveBean(update);
		solrTemplate.commit();

		Assert.assertEquals(1, solrTemplate.count(ALL_DOCUMENTS_QUERY));

		ExampleSolrBean recalled = solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);

		Assert.assertEquals(toInsert.getId(), recalled.getId());

		Assert.assertEquals(4, recalled.getCategory().size());
		Assert.assertEquals(Arrays.asList(toInsert.getCategory().get(0), "spring", "data", "solr"), recalled.getCategory());

		Assert.assertEquals(toInsert.getName(), recalled.getName());
		Assert.assertEquals(toInsert.getPopularity(), recalled.getPopularity());
	}

	@Test
	public void testPartialUpdateWithMultipleDocuments() {
		List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>(10);
		for (int i = 0; i < 10; i++) {
			ExampleSolrBean toBeInserted = createExampleBeanWithId(Integer.toString(i));
			toBeInserted.setPopularity(10);
			values.add(toBeInserted);
		}
		solrTemplate.saveBeans(values);
		solrTemplate.commit();

		List<Update> updates = new ArrayList<Update>(5);
		for (int i = 0; i < 5; i++) {
			PartialUpdate update = new PartialUpdate("id", Integer.toString(i));
			update.add("popularity", 5);
			updates.add(update);
		}
		solrTemplate.saveBeans(updates);
		solrTemplate.commit();

		Assert.assertEquals(10, solrTemplate.count(ALL_DOCUMENTS_QUERY));

		Page<ExampleSolrBean> recalled = solrTemplate.queryForPage(
				new SimpleQuery(new SimpleStringCriteria("popularity:5")), ExampleSolrBean.class);

		Assert.assertEquals(5, recalled.getNumberOfElements());

		for (ExampleSolrBean bean : recalled) {
			Assert.assertEquals("Category must not change on partial update", "category_" + bean.getId(), bean.getCategory()
					.get(0));
		}
	}

	@Test
	public void testPartialUpdateSetWithNullAtTheEnd() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		toInsert.setCategory(Arrays.asList("cat-1"));
		solrTemplate.saveBean(toInsert);
		solrTemplate.commit();

		ExampleSolrBean loaded = solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);

		Assert.assertEquals(1, loaded.getCategory().size());

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setValueOfField("popularity", 500);
		update.setValueOfField("cat", Arrays.asList("cat-1", "cat-2", "cat-3"));
		update.setValueOfField("name", null);

		solrTemplate.saveBean(update);
		solrTemplate.commit();

		loaded = solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);
		Assert.assertEquals(Integer.valueOf(500), loaded.getPopularity());
		Assert.assertEquals(3, loaded.getCategory().size());
		Assert.assertNull(loaded.getName());
	}

	@Test
	public void testPartialUpdateSetWithNullInTheMiddle() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		toInsert.setCategory(Arrays.asList("cat-1"));
		solrTemplate.saveBean(toInsert);
		solrTemplate.commit();

		ExampleSolrBean loaded = solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);

		Assert.assertEquals(1, loaded.getCategory().size());

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setValueOfField("popularity", 500);
		update.setValueOfField("name", null);
		update.setValueOfField("cat", Arrays.asList("cat-1", "cat-2", "cat-3"));

		solrTemplate.saveBean(update);
		solrTemplate.commit();

		loaded = solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);
		Assert.assertEquals(Integer.valueOf(500), loaded.getPopularity());
		Assert.assertNull(loaded.getName());
		Assert.assertEquals(3, loaded.getCategory().size());
	}

	@Test
	public void testPartialUpdateWithVersion() {
		ExampleSolrBean toInsert = createDefaultExampleBean();

		solrTemplate.saveBean(toInsert);
		solrTemplate.commit();

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setVersion(1L);
		update.setValueOfField("popularity", 500);

		solrTemplate.saveBean(update);
		solrTemplate.commit();

		ExampleSolrBean loaded = solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);
		Assert.assertEquals(Integer.valueOf(500), loaded.getPopularity());
	}

	@Test(expected = UncategorizedSolrException.class)
	public void testPartialUpdateWithInvalidVersion() {
		ExampleSolrBean toInsert = createDefaultExampleBean();

		solrTemplate.saveBean(toInsert);
		solrTemplate.commit();

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setVersion(2L);
		update.setValueOfField("popularity", 500);

		solrTemplate.saveBean(update);
	}

	@Test
	public void testPing() throws SolrServerException, IOException {
		solrTemplate.ping();
	}

	@Test
	public void testRollback() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		solrTemplate.saveBean(toInsert);
		ExampleSolrBean recalled = solrTemplate.queryForObject(new SimpleQuery(new Criteria("id").is("1")),
				ExampleSolrBean.class);
		Assert.assertNull(recalled);

		solrTemplate.rollback();
		recalled = solrTemplate.queryForObject(new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
		Assert.assertNull(recalled);
	}

	@Test
	public void testFacetQueryWithFacetFields() {
		List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>();
		for (int i = 0; i < 10; i++) {
			values.add(createExampleBeanWithId(Integer.toString(i)));
		}
		solrTemplate.saveBeans(values);
		solrTemplate.commit();

		FacetQuery q = new SimpleFacetQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
				.setFacetOptions(new FacetOptions().addFacetOnField("name").addFacetOnField("id").setFacetLimit(5));

		FacetPage<ExampleSolrBean> page = solrTemplate.queryForFacetPage(q, ExampleSolrBean.class);

		for (Page<FacetFieldEntry> facetResultPage : page.getFacetResultPages()) {
			Assert.assertEquals(5, facetResultPage.getNumberOfElements());
		}

		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("name"));
		for (FacetFieldEntry entry : facetPage) {
			Assert.assertNotNull(entry.getValue());
			Assert.assertEquals("name", entry.getField().getName());
			Assert.assertEquals(1l, entry.getValueCount());
		}

		facetPage = page.getFacetResultPage(new SimpleField("id"));
		for (FacetFieldEntry entry : facetPage) {
			Assert.assertNotNull(entry.getValue());
			Assert.assertEquals("id", entry.getField().getName());
			Assert.assertEquals(1l, entry.getValueCount());
		}
	}

	@Test
	public void testFacetQueryWithPivotFields() {
		List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>();
		for (int i = 0; i < 10; i++) {
			values.add(createExampleBeanWithId(Integer.toString(i)));
		}
		solrTemplate.saveBeans(values);
		solrTemplate.commit();

		FacetQuery q = new SimpleFacetQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
				.setFacetOptions(new FacetOptions().addFacetOnPivot("cat", "name"));

		FacetPage<ExampleSolrBean> page = solrTemplate.queryForFacetPage(q, ExampleSolrBean.class);

		List<FacetPivotFieldEntry> pivotEntries = page.getPivot("cat,name");
		Assert.assertNotNull(pivotEntries);
		Assert.assertEquals(10, pivotEntries.size());

		for (FacetPivotFieldEntry entry1 : pivotEntries) {
			Assert.assertNotNull(entry1.getValue());
			Assert.assertEquals("cat", entry1.getField().getName());
			Assert.assertEquals(1l, entry1.getValueCount());
			for (FacetPivotFieldEntry entry2 : entry1.getPivot()) {
				Assert.assertNotNull(entry2.getValue());
				Assert.assertEquals("name", entry2.getField().getName());
				Assert.assertEquals(1l, entry2.getValueCount());
			}
		}

	}

	@Test
	public void testFacetQueryWithFacetQueries() {
		List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>();
		for (int i = 0; i < 10; i++) {
			ExampleSolrBean bean = createExampleBeanWithId(Integer.toString(i));
			bean.setInStock(i % 2 == 0);
			values.add(bean);
		}
		solrTemplate.saveBeans(values);
		solrTemplate.commit();

		FacetQuery q = new SimpleFacetQuery(new SimpleStringCriteria("*:*"));
		q.setFacetOptions(new FacetOptions(new SimpleQuery(new SimpleStringCriteria("inStock:true")), new SimpleQuery(
				new SimpleStringCriteria("inStock:false"))));

		FacetPage<ExampleSolrBean> page = solrTemplate.queryForFacetPage(q, ExampleSolrBean.class);

		Page<FacetQueryEntry> facetQueryResultPage = page.getFacetQueryResult();
		Assert.assertEquals(2, facetQueryResultPage.getContent().size());
		Assert.assertEquals("inStock:true", facetQueryResultPage.getContent().get(0).getValue());
		Assert.assertEquals(5, facetQueryResultPage.getContent().get(0).getValueCount());

		Assert.assertEquals("inStock:false", facetQueryResultPage.getContent().get(1).getValue());
		Assert.assertEquals(5, facetQueryResultPage.getContent().get(1).getValueCount());

		Assert.assertEquals(1, page.getAllFacets().size());
	}

	@Test
	public void testFacetQueryWithFacetPrefix() {
		ExampleSolrBean season = new ExampleSolrBean("1", "spring", "season");
		ExampleSolrBean framework = new ExampleSolrBean("2", "spring", "framework");
		ExampleSolrBean island = new ExampleSolrBean("3", "java", "island");
		ExampleSolrBean language = new ExampleSolrBean("4", "java", "language");

		solrTemplate.saveBeans(Arrays.asList(season, framework, island, language));
		solrTemplate.commit();

		FacetQuery q = new SimpleFacetQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
				.setFacetOptions(new FacetOptions().addFacetOnField("name").setFacetLimit(5).setFacetPrefix("spr"));

		FacetPage<ExampleSolrBean> page = solrTemplate.queryForFacetPage(q, ExampleSolrBean.class);
		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("name"));
		for (FacetFieldEntry entry : facetPage) {
			Assert.assertEquals("spring", entry.getValue());
			Assert.assertEquals("name", entry.getField().getName());
			Assert.assertEquals(2l, entry.getValueCount());
		}
	}

	@Test
	public void testFacetQueryWithFieldFacetPrefix() {
		ExampleSolrBean season = new ExampleSolrBean("1", "spring", "season");
		ExampleSolrBean framework = new ExampleSolrBean("2", "spring", "framework");
		ExampleSolrBean island = new ExampleSolrBean("3", "java", "island");
		ExampleSolrBean language = new ExampleSolrBean("4", "java", "language");

		solrTemplate.saveBeans(Arrays.asList(season, framework, island, language));
		solrTemplate.commit();
		FacetQuery q = new SimpleFacetQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
				.setFacetOptions(new FacetOptions().addFacetOnField(new FieldWithFacetParameters("name").setPrefix("spr"))
						.addFacetOnField("cat").setFacetPrefix("lan").setFacetLimit(5));

		FacetPage<ExampleSolrBean> page = solrTemplate.queryForFacetPage(q, ExampleSolrBean.class);
		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("name"));
		for (FacetFieldEntry entry : facetPage) {
			Assert.assertEquals("spring", entry.getValue());
			Assert.assertEquals("name", entry.getField().getName());
			Assert.assertEquals(2l, entry.getValueCount());
		}

		facetPage = page.getFacetResultPage(new SimpleField("cat"));
		for (FacetFieldEntry entry : facetPage) {
			Assert.assertEquals("language", entry.getValue());
			Assert.assertEquals("cat", entry.getField().getName());
			Assert.assertEquals(1l, entry.getValueCount());
		}
	}

	@Test
	public void testQueryWithSort() {
		List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>();
		for (int i = 0; i < 10; i++) {
			values.add(createExampleBeanWithId(Integer.toString(i)));
		}
		solrTemplate.saveBeans(values);
		solrTemplate.commit();

		Query query = new SimpleQuery(new SimpleStringCriteria("*:*")).addSort(new Sort(Sort.Direction.DESC, "name"));
		Page<ExampleSolrBean> page = solrTemplate.queryForPage(query, ExampleSolrBean.class);

		ExampleSolrBean prev = page.getContent().get(0);
		for (int i = 1; i < page.getContent().size(); i++) {
			ExampleSolrBean cur = page.getContent().get(i);
			Assert.assertTrue(Long.valueOf(cur.getId()) < Long.valueOf(prev.getId()));
			prev = cur;
		}
	}

	@Test
	public void testQueryWithMultiSort() {
		List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>();
		for (int i = 0; i < 10; i++) {
			ExampleSolrBean bean = createExampleBeanWithId(Integer.toString(i));
			bean.setInStock(i % 2 == 0);
			values.add(bean);
		}
		solrTemplate.saveBeans(values);
		solrTemplate.commit();

		Query query = new SimpleQuery(new SimpleStringCriteria("*:*")).addSort(new Sort(Sort.Direction.DESC, "inStock"))
				.addSort(new Sort(Sort.Direction.ASC, "name"));
		Page<ExampleSolrBean> page = solrTemplate.queryForPage(query, ExampleSolrBean.class);

		ExampleSolrBean prev = page.getContent().get(0);
		for (int i = 1; i < 5; i++) {
			ExampleSolrBean cur = page.getContent().get(i);
			Assert.assertTrue(cur.isInStock());
			Assert.assertTrue(Long.valueOf(cur.getId()) > Long.valueOf(prev.getId()));
			prev = cur;
		}

		prev = page.getContent().get(5);
		for (int i = 6; i < page.getContent().size(); i++) {
			ExampleSolrBean cur = page.getContent().get(i);
			Assert.assertFalse(cur.isInStock());
			Assert.assertTrue(Long.valueOf(cur.getId()) > Long.valueOf(prev.getId()));
			prev = cur;
		}
	}

	@Test
	public void testQueryWithDefaultOperator() {
		List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>();
		for (int i = 0; i < 10; i++) {
			ExampleSolrBean bean = createExampleBeanWithId(Integer.toString(i));
			bean.setInStock(i % 2 == 0);
			values.add(bean);
		}
		solrTemplate.saveBeans(values);
		solrTemplate.commit();

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("inStock:(true false)"));
		query.setDefaultOperator(Operator.AND);
		Page<ExampleSolrBean> page = solrTemplate.queryForPage(query, ExampleSolrBean.class);
		Assert.assertEquals(0, page.getContent().size());

		query.setDefaultOperator(Operator.OR);
		page = solrTemplate.queryForPage(query, ExampleSolrBean.class);
		Assert.assertEquals(10, page.getContent().size());
	}

	@Test
	public void testQueryWithDefType() {
		List<ExampleSolrBean> values = createBeansWithIdAndPrefix(5, "id-");
		solrTemplate.saveBeans(values);
		solrTemplate.commit();

		SimpleQuery query = new SimpleQuery(new Criteria("id").in("id-1", "id-2", "id-3"));
		query.setDefType("lucene");
		query.setDefaultOperator(Operator.OR);

		Page<ExampleSolrBean> page = solrTemplate.queryForPage(query, ExampleSolrBean.class);
		Assert.assertEquals(3, page.getContent().size());
	}

	@Test
	public void testQueryWithRequestHandler() {
		List<ExampleSolrBean> values = createBeansWithIdAndPrefix(2, "rh-");
		values.get(0).setInStock(false);
		values.get(1).setInStock(true);

		solrTemplate.saveBeans(values);
		solrTemplate.commit();

		SimpleQuery query = new SimpleQuery(new Criteria("id").in("rh-1", "rh-2"));
		Page<ExampleSolrBean> page = solrTemplate.queryForPage(query, ExampleSolrBean.class);
		Assert.assertEquals(2, page.getContent().size());

		query = new SimpleQuery(new Criteria("id").in("rh-1", "rh-2"));
		query.setRequestHandler("/instock");
		page = solrTemplate.queryForPage(query, ExampleSolrBean.class);
		Assert.assertEquals(1, page.getContent().size());
		Assert.assertEquals("rh-2", page.getContent().get(0).getId());
	}

	@Test
	public void testQueryWithJoinOperation() {
		ExampleSolrBean belkin = new ExampleSolrBean("belkin", "Belkin", null);
		ExampleSolrBean apple = new ExampleSolrBean("apple", "Apple", null);

		ExampleSolrBean ipod = new ExampleSolrBean("F8V7067-APL-KIT", "Belkin Mobile Power Cord for iPod", null);
		ipod.setManufacturerId(belkin.getId());

		solrTemplate.saveBeans(Arrays.asList(belkin, apple, ipod));
		solrTemplate.commit();

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("text:ipod"));
		query.setJoin(Join.from("manu_id_s").to("id"));

		Page<ExampleSolrBean> page = solrTemplate.queryForPage(query, ExampleSolrBean.class);
		Assert.assertEquals(1, page.getContent().size());
		Assert.assertEquals(belkin.getId(), page.getContent().get(0).getId());
	}

	@Test
	public void testQueryWithHighlights() {
		ExampleSolrBean belkin = new ExampleSolrBean("GB18030TEST", "Test with some GB18030TEST", null);
		ExampleSolrBean apple = new ExampleSolrBean("UTF8TEST", "Test with some UTF8TEST", null);

		solrTemplate.saveBeans(Arrays.asList(belkin, apple));
		solrTemplate.commit();

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("name:with"));
		query.setHighlightOptions(new HighlightOptions());

		HighlightPage<ExampleSolrBean> page = solrTemplate.queryForHighlightPage(query, ExampleSolrBean.class);
		Assert.assertEquals(2, page.getHighlighted().size());

		Assert.assertEquals("name", page.getHighlighted().get(0).getHighlights().get(0).getField().getName());
		Assert.assertEquals("Test <em>with</em> some GB18030TEST", page.getHighlighted().get(0).getHighlights().get(0)
				.getSnipplets().get(0));
	}

	@Test
	public void testTermsQuery() {
		TermsQuery query = SimpleTermsQuery.queryBuilder().fields("name").build();

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one two three", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two three", null);
		ExampleSolrBean bean3 = new ExampleSolrBean("id-3", "three", null);

		solrTemplate.saveBeans(Arrays.asList(bean1, bean2, bean3));
		solrTemplate.commit();

		TermsPage page = solrTemplate.queryForTermsPage(query);

		ArrayList<TermsFieldEntry> values = Lists.newArrayList(page.getTermsForField("name"));
		Assert.assertEquals("three", values.get(0).getValue());
		Assert.assertEquals(3, values.get(0).getValueCount());

		Assert.assertEquals("two", values.get(1).getValue());
		Assert.assertEquals(2, values.get(1).getValueCount());

		Assert.assertEquals("one", values.get(2).getValue());
		Assert.assertEquals(1, values.get(2).getValueCount());
	}

	@Test
	public void testFuctionQueryInFilterReturnsProperResult() {
		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		solrTemplate.saveBeans(Arrays.asList(bean1, bean2));
		solrTemplate.commit();

		Query q = new SimpleQuery("*:*")
				.addFilterQuery(new SimpleFilterQuery(new Criteria(QueryFunction.query("name:o*"))));

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(q, ExampleSolrBean.class);
		Assert.assertThat(result.getNumberOfElements(), IsEqual.equalTo(1));
	}

	@Test
	public void testFuctionQueryReturnsProperResult() {
		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		solrTemplate.saveBeans(Arrays.asList(bean1, bean2));
		solrTemplate.commit();

		Query q = new SimpleQuery(new Criteria(QueryFunction.query("name:o*")));

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(q, ExampleSolrBean.class);
		Assert.assertThat(result.getNumberOfElements(), IsEqual.equalTo(1));
	}

	@Test
	public void testFunctionQueryInFieldProjection() {
		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		bean1.setStore("45.17614,-93.87341");
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "one two", null);
		bean2.setStore("40.7143,-74.006");

		solrTemplate.saveBeans(Arrays.asList(bean1, bean2));
		solrTemplate.commit();

		Query q = new SimpleQuery("*:*");
		q.addProjectionOnField(new DistanceField("distance", "store", new Point(45.15, -93.85)));
		Page<ExampleSolrBean> result = solrTemplate.queryForPage(q, ExampleSolrBean.class);
		for (ExampleSolrBean bean : result) {
			Assert.assertThat(bean.getDistance(), IsNull.notNullValue());
		}

	}

	/**
	 * @see DATASOLR-162
	 */
	@Test
	public void testDelegatingCursorLoadsAllElements() throws IOException {

		solrTemplate.saveBeans(createBeansWithId(100));
		solrTemplate.commit();

		Cursor<ExampleSolrBean> cursor = solrTemplate.queryForCursor(
				new SimpleQuery("*:*").addSort(new Sort(Direction.DESC, "id")), ExampleSolrBean.class);

		int i = 0;
		while (cursor.hasNext()) {
			cursor.next();
			i++;
		}
		cursor.close();

		Assert.assertThat(i, Is.is(100));
	}
	
	/**
	 * @see DATASOLR-121
	 */
	@Test
	public void testRegularGroupQuery() {
		solrTemplate.saveBean(new ExampleSolrBean("id_1", "name1", "category1", 2, true));
		solrTemplate.saveBean(new ExampleSolrBean("id_2", "name1", "category2"));
		solrTemplate.saveBean(new ExampleSolrBean("id_3", "name2", "category2", 1, true));
		solrTemplate.saveBean(new ExampleSolrBean("id_4", "name3", "category2"));
		solrTemplate.commit();

		Function f = IfFunction.when("inStock").then("1").otherwise("2");
		Query q1 = new SimpleQuery("cat:category2");
		Query q2 = new SimpleQuery("cat:category1");
		
		SimpleQuery groupQuery = new SimpleQuery(new SimpleStringCriteria("*:*"));
		GroupOptions groupOptions = new GroupOptions();
		groupQuery.setGroupOptions(groupOptions);
		groupOptions.addSort(new Sort("name","id"));
		groupOptions.addGroupByField("name");
		groupOptions.addGroupByFunction(f);
		groupOptions.addGroupByQuery(q1);
		groupOptions.addGroupByQuery(q2);
		groupOptions.setGroupLimit(2);

		// asserts result page
		GroupPage<ExampleSolrBean> groupResultPage = solrTemplate.queryForGroupPage(groupQuery, ExampleSolrBean.class);
		GroupResult<ExampleSolrBean> groupResultName = groupResultPage.getGroupResult("name");
		GroupResult<ExampleSolrBean> groupResultFunction = groupResultPage.getGroupResult(f);
		GroupResult<ExampleSolrBean> groupResultQuery1 = groupResultPage.getGroupResult(q1);
		GroupResult<ExampleSolrBean> groupResultQuery2 = groupResultPage.getGroupResult(q2);
		
		// asserts field group
		Page<GroupEntry<ExampleSolrBean>> nameGroupEntries = groupResultName.getGroupEntries();
		Assert.assertEquals(3, nameGroupEntries.getTotalElements());
		List<GroupEntry<ExampleSolrBean>> nameGroupEntriesContent = nameGroupEntries.getContent();
		assertGroupEntry(nameGroupEntriesContent.get(0), 2, "name1", 2, "id_1", "id_2");
		assertGroupEntry(nameGroupEntriesContent.get(1), 1, "name2", 1, "id_3");
		assertGroupEntry(nameGroupEntriesContent.get(2), 1, "name3", 1, "id_4");

		// asserts function group
		Page<GroupEntry<ExampleSolrBean>> functionGroupEntries = groupResultFunction.getGroupEntries();
		Assert.assertEquals(2, functionGroupEntries.getNumberOfElements());
		List<GroupEntry<ExampleSolrBean>> functionGroupEntriesContent = functionGroupEntries.getContent();
		assertGroupEntry(functionGroupEntriesContent.get(0), 2, "1.0", 2, "id_1","id_3");
		assertGroupEntry(functionGroupEntriesContent.get(1), 2, "2.0", 2, "id_2","id_4");

		// asserts first query group
		Page<GroupEntry<ExampleSolrBean>> query1GroupEntries = groupResultQuery1.getGroupEntries();
		Assert.assertEquals(1, query1GroupEntries.getNumberOfElements());
		GroupEntry<ExampleSolrBean> query1GroupEntry = query1GroupEntries.getContent().get(0);
		assertGroupEntry(query1GroupEntry, 3, "cat:category2", 2, "id_2","id_3");
		assertTrue(query1GroupEntry.getResult().hasNext());

		// asserts second query group
		Page<GroupEntry<ExampleSolrBean>> query2GroupEntries = groupResultQuery2.getGroupEntries();
		Assert.assertEquals(1, query2GroupEntries.getNumberOfElements());
		GroupEntry<ExampleSolrBean> query2GroupEntry = query2GroupEntries.getContent().get(0);
		assertGroupEntry(query2GroupEntry, 1, "cat:category1", 1, "id_1");
		assertFalse(query2GroupEntry.getResult().hasNext());
	}

	/**
	 * @see DATASOLR-121
	 */
	@Test
	public void testGroupQueryWithFacets() {
		solrTemplate.saveBean(new ExampleSolrBean("id_1", "name1", "category1", 2, true));
		solrTemplate.saveBean(new ExampleSolrBean("id_2", "name1", "category1", 2, true));
		solrTemplate.saveBean(new ExampleSolrBean("id_3", "name1", "category1", 1, true));
		solrTemplate.saveBean(new ExampleSolrBean("id_4", "name2", "category2", 1, false));
		solrTemplate.saveBean(new ExampleSolrBean("id_5", "name2", "category2", 2, false));
		solrTemplate.saveBean(new ExampleSolrBean("id_6", "name2", "category1", 1, true));
		solrTemplate.commit();

		SimpleFacetQuery groupQuery = new SimpleFacetQuery(new SimpleStringCriteria("*:*"));
		GroupOptions groupOptions = new GroupOptions();
		groupQuery.setGroupOptions(groupOptions);
		groupQuery.setFacetOptions(new FacetOptions("inStock"));
		groupOptions.addGroupByField("name");
		groupOptions.setGroupFacets(true);
		FacetPage<ExampleSolrBean> groupResultPage = solrTemplate.queryForFacetPage(groupQuery, ExampleSolrBean.class);

		Page<FacetFieldEntry> facetResultPage = groupResultPage.getFacetResultPage("inStock");
		
		List<FacetFieldEntry> facetContent = facetResultPage.getContent();
		
		Assert.assertEquals("true",facetContent.get(0).getValue());
		Assert.assertEquals("false",facetContent.get(1).getValue());
		Assert.assertEquals(2,facetContent.get(0).getValueCount());
		Assert.assertEquals(1,facetContent.get(1).getValueCount());
	}
	
	private void assertGroupEntryContentIds(GroupEntry<ExampleSolrBean> groupEntry, String ... ids) {
		for (int i = 0; i < ids.length; i++) {
			Assert.assertEquals(ids[i], groupEntry.getResult().getContent().get(i).getId());
		}
	}

	private void assertGroupEntry(GroupEntry<ExampleSolrBean> entry, long totalElements,
			String groupValue, int numberOfDocuments, String ... ids) {
		Assert.assertEquals(totalElements, entry.getResult().getTotalElements());
		Assert.assertEquals(groupValue, entry.getGroupValue());
		Assert.assertEquals(numberOfDocuments, entry.getResult().getContent().size());
		assertGroupEntryContentIds(entry, ids);
	}

}
