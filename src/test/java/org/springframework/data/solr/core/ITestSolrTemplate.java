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
package org.springframework.data.solr.core;

import static java.util.Calendar.*;
import static org.apache.solr.common.params.FacetParams.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsEqual.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;

import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.FacetParams;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.geo.Point;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;
import org.springframework.data.solr.ExampleSolrBean;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.FacetOptions.FacetSort;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithDateRangeParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithFacetParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithNumericRangeParameters;
import org.springframework.data.solr.core.query.Query.Operator;
import org.springframework.data.solr.core.query.result.Cursor;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPivotFieldEntry;
import org.springframework.data.solr.core.query.result.FacetQueryEntry;
import org.springframework.data.solr.core.query.result.FieldStatsResult;
import org.springframework.data.solr.core.query.result.GroupEntry;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.GroupResult;
import org.springframework.data.solr.core.query.result.HighlightQueryResult;
import org.springframework.data.solr.core.query.result.SpellcheckedPage;
import org.springframework.data.solr.core.query.result.StatsPage;
import org.springframework.data.solr.core.query.result.StatsResult;
import org.springframework.data.solr.core.query.result.TermsFieldEntry;
import org.springframework.data.solr.core.query.result.TermsPage;
import org.springframework.data.solr.server.support.HttpSolrClientFactory;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;

/**
 * @author Christoph Strobl
 * @author Andrey Paramonov
 * @author Francisco Spaeth
 * @author Radek Mensik
 */
public class ITestSolrTemplate extends AbstractITestWithEmbeddedSolrServer {

	public @Rule ExpectedException exception = ExpectedException.none();

	private static final Query DEFAULT_BEAN_OBJECT_QUERY = new SimpleQuery(new Criteria("id").is(DEFAULT_BEAN_ID));
	private static final Query ALL_DOCUMENTS_QUERY = new SimpleQuery(new SimpleStringCriteria("*:*"));

	private SolrTemplate solrTemplate;

	@Before
	public void setUp() throws IOException, ParserConfigurationException, SAXException {
		solrTemplate = new SolrTemplate(server);
		solrTemplate.afterPropertiesSet();
	}

	@After
	public void tearDown() {
		solrTemplate.delete(COLLECTION_NAME, ALL_DOCUMENTS_QUERY);
		solrTemplate.commit(COLLECTION_NAME);
	}

	@Test
	public void testBeanLifecycle() {
		ExampleSolrBean toInsert = createDefaultExampleBean();

		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME,
				new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
		assertFalse(recalled.isPresent());
		solrTemplate.commit(COLLECTION_NAME);

		recalled = solrTemplate.queryForObject(COLLECTION_NAME, new SimpleQuery(new Criteria("id").is("1")),
				ExampleSolrBean.class);
		assertTrue(recalled.isPresent());
		assertEquals(toInsert.getId(), recalled.get().getId());

		solrTemplate.deleteByIds(COLLECTION_NAME, toInsert.getId());
		recalled = solrTemplate.queryForObject(COLLECTION_NAME, new SimpleQuery(new Criteria("id").is("1")),
				ExampleSolrBean.class);
		assertEquals(toInsert.getId(), recalled.get().getId());

		solrTemplate.commit(COLLECTION_NAME);
		recalled = solrTemplate.queryForObject(COLLECTION_NAME, new SimpleQuery(new Criteria("id").is("1")),
				ExampleSolrBean.class);
		assertFalse(recalled.isPresent());
	}

	@Test
	public void testPartialUpdateSetSingleValueField() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		solrTemplate.commit(COLLECTION_NAME);

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.add("name", "updated-name");
		solrTemplate.saveBean(COLLECTION_NAME, update);
		solrTemplate.commit(COLLECTION_NAME);

		assertEquals(1, solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY));

		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertEquals(toInsert.getId(), recalled.get().getId());
		assertEquals("updated-name", recalled.get().getName());
		assertEquals(toInsert.getPopularity(), recalled.get().getPopularity());
	}

	@Test
	public void testPartialUpdateAddSingleValueToMultivalueField() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		toInsert.setCategory(Collections.singletonList("nosql"));
		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		solrTemplate.commit(COLLECTION_NAME);

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.add(new SimpleUpdateField("cat", "spring-data-solr", UpdateAction.ADD));
		solrTemplate.saveBean(COLLECTION_NAME, update);
		solrTemplate.commit(COLLECTION_NAME);

		assertEquals(1, solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY));

		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertEquals(toInsert.getId(), recalled.get().getId());

		assertEquals(2, recalled.get().getCategory().size());
		assertEquals(Arrays.asList("nosql", "spring-data-solr"), recalled.get().getCategory());

		assertEquals(toInsert.getName(), recalled.get().getName());
		assertEquals(toInsert.getPopularity(), recalled.get().getPopularity());
	}

	@Test
	public void testPartialUpdateIncSingleValue() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		solrTemplate.commit(COLLECTION_NAME);

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.add(new SimpleUpdateField("popularity", 1, UpdateAction.INC));
		solrTemplate.saveBean(COLLECTION_NAME, update);
		solrTemplate.commit(COLLECTION_NAME);

		assertEquals(1, solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY));

		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertTrue(recalled.isPresent());
		assertEquals(toInsert.getId(), recalled.get().getId());

		assertEquals(1, recalled.get().getCategory().size());

		assertEquals(toInsert.getName(), recalled.get().getName());
		assertEquals(Integer.valueOf(11), recalled.get().getPopularity());
	}

	@Test
	public void testPartialUpdateSetMultipleValuesToMultivaluedField() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		solrTemplate.commit(COLLECTION_NAME);

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setValueOfField("cat", Arrays.asList("spring", "data", "solr"));
		solrTemplate.saveBean(COLLECTION_NAME, update);
		solrTemplate.commit(COLLECTION_NAME);

		assertEquals(1, solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY));

		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertTrue(recalled.isPresent());
		assertEquals(toInsert.getId(), recalled.get().getId());

		assertEquals(3, recalled.get().getCategory().size());
		assertEquals(Arrays.asList("spring", "data", "solr"), recalled.get().getCategory());

		assertEquals(toInsert.getName(), recalled.get().getName());
		assertEquals(toInsert.getPopularity(), recalled.get().getPopularity());
	}

	@Test
	public void testPartialUpdateSetEmptyCollectionToMultivaluedFieldRemovesValuesFromDocument() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setCategory(Arrays.asList("spring", "data", "solr"));
		toInsert.setPopularity(10);
		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		solrTemplate.commit(COLLECTION_NAME);

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setValueOfField("cat", Collections.emptyList());

		solrTemplate.saveBean(COLLECTION_NAME, update);
		solrTemplate.commit(COLLECTION_NAME);

		assertEquals(1, solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY));

		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertTrue(recalled.isPresent());
		assertEquals(toInsert.getId(), recalled.get().getId());

		assertEquals(0, recalled.get().getCategory().size());

		assertEquals(toInsert.getName(), recalled.get().getName());
		assertEquals(toInsert.getPopularity(), recalled.get().getPopularity());
	}

	@Test
	public void testPartialUpdateAddMultipleValuesToMultivaluedField() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		solrTemplate.commit(COLLECTION_NAME);

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.addValueToField("cat", Arrays.asList("spring", "data", "solr"));

		solrTemplate.saveBean(COLLECTION_NAME, update);
		solrTemplate.commit(COLLECTION_NAME);

		assertEquals(1, solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY));

		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertEquals(toInsert.getId(), recalled.get().getId());

		assertEquals(4, recalled.get().getCategory().size());
		assertEquals(Arrays.asList(toInsert.getCategory().get(0), "spring", "data", "solr"), recalled.get().getCategory());

		assertEquals(toInsert.getName(), recalled.get().getName());
		assertEquals(toInsert.getPopularity(), recalled.get().getPopularity());
	}

	@Test
	public void testPartialUpdateWithMultipleDocuments() {
		List<ExampleSolrBean> values = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			ExampleSolrBean toBeInserted = createExampleBeanWithId(Integer.toString(i));
			toBeInserted.setPopularity(10);
			values.add(toBeInserted);
		}
		solrTemplate.saveBeans(COLLECTION_NAME, values);
		solrTemplate.commit(COLLECTION_NAME);

		List<Update> updates = new ArrayList<>(5);
		for (int i = 0; i < 5; i++) {
			PartialUpdate update = new PartialUpdate("id", Integer.toString(i));
			update.add("popularity", 5);
			updates.add(update);
		}
		solrTemplate.saveBeans(COLLECTION_NAME, updates);
		solrTemplate.commit(COLLECTION_NAME);

		assertEquals(10, solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY));

		Page<ExampleSolrBean> recalled = solrTemplate.queryForPage(COLLECTION_NAME,
				new SimpleQuery(new SimpleStringCriteria("popularity:5")), ExampleSolrBean.class);

		assertEquals(5, recalled.getNumberOfElements());

		for (ExampleSolrBean bean : recalled) {
			assertEquals("Category must not change on partial update", "category_" + bean.getId(), bean.getCategory().get(0));
		}
	}

	@Test
	public void testPartialUpdateSetWithNullAtTheEnd() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		toInsert.setCategory(Collections.singletonList("cat-1"));
		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		solrTemplate.commit(COLLECTION_NAME);

		Optional<ExampleSolrBean> loaded = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertEquals(1, loaded.get().getCategory().size());

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setValueOfField("popularity", 500);
		update.setValueOfField("cat", Arrays.asList("cat-1", "cat-2", "cat-3"));
		update.setValueOfField("name", null);

		solrTemplate.saveBean(COLLECTION_NAME, update);
		solrTemplate.commit(COLLECTION_NAME);

		loaded = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);
		assertEquals(Integer.valueOf(500), loaded.get().getPopularity());
		assertEquals(3, loaded.get().getCategory().size());
		assertNull(loaded.get().getName());
	}

	@Test
	public void testPartialUpdateSetWithNullInTheMiddle() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		toInsert.setPopularity(10);
		toInsert.setCategory(Collections.singletonList("cat-1"));
		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		solrTemplate.commit(COLLECTION_NAME);

		Optional<ExampleSolrBean> loaded = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertEquals(1, loaded.get().getCategory().size());

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setValueOfField("popularity", 500);
		update.setValueOfField("name", null);
		update.setValueOfField("cat", Arrays.asList("cat-1", "cat-2", "cat-3"));

		solrTemplate.saveBean(COLLECTION_NAME, update);
		solrTemplate.commit(COLLECTION_NAME);

		loaded = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);
		assertEquals(Integer.valueOf(500), loaded.get().getPopularity());
		assertNull(loaded.get().getName());
		assertEquals(3, loaded.get().getCategory().size());
	}

	@Test
	public void testPartialUpdateWithVersion() {
		ExampleSolrBean toInsert = createDefaultExampleBean();

		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		solrTemplate.commit(COLLECTION_NAME);

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setVersion(1L);
		update.setValueOfField("popularity", 500);

		solrTemplate.saveBean(COLLECTION_NAME, update);
		solrTemplate.commit(COLLECTION_NAME);

		Optional<ExampleSolrBean> loaded = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);
		assertEquals(Integer.valueOf(500), loaded.get().getPopularity());
	}

	@Test(expected = UncategorizedSolrException.class)
	public void testPartialUpdateWithInvalidVersion() {
		ExampleSolrBean toInsert = createDefaultExampleBean();

		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		solrTemplate.commit(COLLECTION_NAME);

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setVersion(2L);
		update.setValueOfField("popularity", 500);

		solrTemplate.saveBean(COLLECTION_NAME, update);
	}

	@Test
	public void testPing() throws SolrServerException, IOException {
		solrTemplate.ping();
	}

	@Test
	public void testRollback() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME,
				new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
		assertFalse(recalled.isPresent());

		solrTemplate.rollback(COLLECTION_NAME);
		recalled = solrTemplate.queryForObject(COLLECTION_NAME, new SimpleQuery(new Criteria("id").is("1")),
				ExampleSolrBean.class);
		assertFalse(recalled.isPresent());
	}

	@Test
	public void testFacetQueryWithFacetFields() {
		List<ExampleSolrBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			values.add(createExampleBeanWithId(Integer.toString(i)));
		}
		solrTemplate.saveBeans(COLLECTION_NAME, values);
		solrTemplate.commit(COLLECTION_NAME);

		FacetQuery q = new SimpleFacetQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
				.setFacetOptions(new FacetOptions().addFacetOnField("name").addFacetOnField("id").setFacetLimit(5));

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(COLLECTION_NAME, q, ExampleSolrBean.class);

		for (Page<FacetFieldEntry> facetResultPage : page.getFacetResultPages()) {
			assertEquals(5, facetResultPage.getNumberOfElements());
		}

		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("name"));
		for (FacetFieldEntry entry : facetPage) {
			assertNotNull(entry.getValue());
			assertEquals("name", entry.getField().getName());
			assertEquals(1l, entry.getValueCount());
		}

		facetPage = page.getFacetResultPage(new SimpleField("id"));
		for (FacetFieldEntry entry : facetPage) {
			assertNotNull(entry.getValue());
			assertEquals("id", entry.getField().getName());
			assertEquals(1l, entry.getValueCount());
		}
	}

	@Test // DATSOLR-86
	public void testFacetQueryWithDateFacetRangeField() {
		List<ExampleSolrBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			final ExampleSolrBean exampleSolrBean = createExampleBeanWithId(Integer.toString(i));
			exampleSolrBean.setLastModified(new GregorianCalendar(2013, Calendar.DECEMBER, (i + 10) / 2).getTime());
			values.add(exampleSolrBean);
		}
		solrTemplate.saveBeans(COLLECTION_NAME, values);
		solrTemplate.commit(COLLECTION_NAME);

		final FieldWithDateRangeParameters lastModifiedField = new FieldWithDateRangeParameters("last_modified",
				new GregorianCalendar(2013, NOVEMBER, 30).getTime(), new GregorianCalendar(2014, JANUARY, 1).getTime(), "+1DAY") //
						.setOther(FacetRangeOther.ALL) //
						.setInclude(FacetRangeInclude.LOWER);

		FacetQuery q = new SimpleFacetQuery(new SimpleStringCriteria("*:*")) //
				.setFacetOptions(//
						new FacetOptions() //
								.addFacetByRange(lastModifiedField) //
								.setFacetLimit(5) //
								.setFacetMinCount(1) //
								.setFacetSort(FacetSort.COUNT) //
								.setPageable(new PageRequest(1, 10)));

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(COLLECTION_NAME, q, ExampleSolrBean.class);

		assertEquals(FacetRangeInclude.LOWER, lastModifiedField.getQueryParameterValue(FACET_RANGE_INCLUDE));

		for (Page<FacetFieldEntry> facetResultPage : page.getFacetResultPages()) {
			assertEquals(5, facetResultPage.getNumberOfElements());
		}

		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("last_modified"));
		for (FacetFieldEntry entry : facetPage) {
			assertNotNull(entry.getValue());
			assertEquals("last_modified", entry.getField().getName());
			assertEquals(2l, entry.getValueCount());
		}
	}

	@Test // DATSOLR-86
	public void testFacetQueryWithNumericFacetRangeField() {
		List<ExampleSolrBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			final ExampleSolrBean exampleSolrBean = createExampleBeanWithId(Integer.toString(i));
			exampleSolrBean.setPopularity((i + 1) * 100);
			values.add(exampleSolrBean);
		}
		solrTemplate.saveBeans(COLLECTION_NAME, values);
		solrTemplate.commit(COLLECTION_NAME);

		final FieldWithNumericRangeParameters popularityField = new FieldWithNumericRangeParameters("popularity", 100, 800,
				200) //
						.setOther(FacetParams.FacetRangeOther.ALL) //
						.setHardEnd(false) //
						.setInclude(FacetRangeInclude.LOWER);

		FacetQuery q = new SimpleFacetQuery(new SimpleStringCriteria("*:*")) //
				.setFacetOptions( //
						new FacetOptions() //
								.addFacetByRange(popularityField) //
								.setFacetLimit(5) //
								.setFacetMinCount(1) //
								.setFacetSort(FacetSort.COUNT));

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(COLLECTION_NAME, q, ExampleSolrBean.class);

		for (Page<FacetFieldEntry> facetResultPage : page.getFacetResultPages()) {
			assertEquals(4, facetResultPage.getNumberOfElements());
		}

		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("popularity"));
		for (FacetFieldEntry entry : facetPage) {
			assertNotNull(entry.getValue());
			assertEquals("popularity", entry.getField().getName());
			assertEquals(2l, entry.getValueCount());
		}
	}

	@Test
	public void testFacetQueryWithPivotFields() {
		List<ExampleSolrBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			values.add(createExampleBeanWithId(Integer.toString(i)));
		}
		solrTemplate.saveBeans(COLLECTION_NAME, values);
		solrTemplate.commit(COLLECTION_NAME);

		FacetQuery q = new SimpleFacetQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
				.setFacetOptions(new FacetOptions().addFacetOnPivot("cat", "name"));

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(COLLECTION_NAME, q, ExampleSolrBean.class);

		List<FacetPivotFieldEntry> pivotEntries = page.getPivot("cat,name");
		assertNotNull(pivotEntries);
		assertEquals(10, pivotEntries.size());

		for (FacetPivotFieldEntry entry1 : pivotEntries) {
			assertNotNull(entry1.getValue());
			assertEquals("cat", entry1.getField().getName());
			assertEquals(1l, entry1.getValueCount());
			for (FacetPivotFieldEntry entry2 : entry1.getPivot()) {
				assertNotNull(entry2.getValue());
				assertEquals("name", entry2.getField().getName());
				assertEquals(1l, entry2.getValueCount());
			}
		}

	}

	@Test
	public void testFacetQueryWithFacetQueries() {
		List<ExampleSolrBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ExampleSolrBean bean = createExampleBeanWithId(Integer.toString(i));
			bean.setInStock(i % 2 == 0);
			values.add(bean);
		}
		solrTemplate.saveBeans(COLLECTION_NAME, values);
		solrTemplate.commit(COLLECTION_NAME);

		FacetQuery q = new SimpleFacetQuery(new SimpleStringCriteria("*:*"));
		q.setFacetOptions(new FacetOptions(new SimpleQuery(new SimpleStringCriteria("inStock:true")),
				new SimpleQuery(new SimpleStringCriteria("inStock:false"))));

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(COLLECTION_NAME, q, ExampleSolrBean.class);

		Page<FacetQueryEntry> facetQueryResultPage = page.getFacetQueryResult();
		assertEquals(2, facetQueryResultPage.getContent().size());
		assertEquals("inStock:true", facetQueryResultPage.getContent().get(0).getValue());
		assertEquals(5, facetQueryResultPage.getContent().get(0).getValueCount());

		assertEquals("inStock:false", facetQueryResultPage.getContent().get(1).getValue());
		assertEquals(5, facetQueryResultPage.getContent().get(1).getValueCount());

		assertEquals(1, page.getAllFacets().size());
	}

	@Test
	public void testFacetQueryWithFacetPrefix() {
		ExampleSolrBean season = new ExampleSolrBean("1", "spring", "season");
		ExampleSolrBean framework = new ExampleSolrBean("2", "spring", "framework");
		ExampleSolrBean island = new ExampleSolrBean("3", "java", "island");
		ExampleSolrBean language = new ExampleSolrBean("4", "java", "language");

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(season, framework, island, language));
		solrTemplate.commit(COLLECTION_NAME);

		FacetQuery q = new SimpleFacetQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
				.setFacetOptions(new FacetOptions().addFacetOnField("name").setFacetLimit(5).setFacetPrefix("spr"));

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(COLLECTION_NAME, q, ExampleSolrBean.class);
		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("name"));
		for (FacetFieldEntry entry : facetPage) {
			assertEquals("spring", entry.getValue());
			assertEquals("name", entry.getField().getName());
			assertEquals(2l, entry.getValueCount());
		}
	}

	@Test
	public void testFacetQueryWithFieldFacetPrefix() {
		ExampleSolrBean season = new ExampleSolrBean("1", "spring", "season");
		ExampleSolrBean framework = new ExampleSolrBean("2", "spring", "framework");
		ExampleSolrBean island = new ExampleSolrBean("3", "java", "island");
		ExampleSolrBean language = new ExampleSolrBean("4", "java", "language");

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(season, framework, island, language));
		solrTemplate.commit(COLLECTION_NAME);
		FacetQuery q = new SimpleFacetQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
				.setFacetOptions(new FacetOptions().addFacetOnField(new FieldWithFacetParameters("name").setPrefix("spr"))
						.addFacetOnField("cat").setFacetPrefix("lan").setFacetLimit(5));

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(COLLECTION_NAME, q, ExampleSolrBean.class);
		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("name"));
		for (FacetFieldEntry entry : facetPage) {
			assertEquals("spring", entry.getValue());
			assertEquals("name", entry.getField().getName());
			assertEquals(2l, entry.getValueCount());
		}

		facetPage = page.getFacetResultPage(new SimpleField("cat"));
		for (FacetFieldEntry entry : facetPage) {
			assertEquals("language", entry.getValue());
			assertEquals("cat", entry.getField().getName());
			assertEquals(1l, entry.getValueCount());
		}
	}

	@Test // DATASOLR-244
	public void testFacetAndHighlightQueryWithFacetFields() {

		List<ExampleSolrBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			values.add(createExampleBeanWithId(Integer.toString(i)));
		}
		solrTemplate.saveBeans(COLLECTION_NAME, values);
		solrTemplate.commit(COLLECTION_NAME);

		FacetAndHighlightQuery q = new SimpleFacetAndHighlightQuery(
				new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
						.setFacetOptions(new FacetOptions().addFacetOnField("name").addFacetOnField("id").setFacetLimit(5));

		q.setHighlightOptions(new HighlightOptions().addField("name"));

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(COLLECTION_NAME, q, ExampleSolrBean.class);

		for (Page<FacetFieldEntry> facetResultPage : page.getFacetResultPages()) {
			assertEquals(5, facetResultPage.getNumberOfElements());
		}

		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("name"));
		for (FacetFieldEntry entry : facetPage) {
			assertNotNull(entry.getValue());
			assertEquals("name", entry.getField().getName());
			assertEquals(1l, entry.getValueCount());
		}

		facetPage = page.getFacetResultPage(new SimpleField("id"));
		for (FacetFieldEntry entry : facetPage) {
			assertNotNull(entry.getValue());
			assertEquals("id", entry.getField().getName());
			assertEquals(1l, entry.getValueCount());
		}
	}

	@Test
	public void testQueryWithSort() {
		List<ExampleSolrBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			values.add(createExampleBeanWithId(Integer.toString(i)));
		}
		solrTemplate.saveBeans(COLLECTION_NAME, values);
		solrTemplate.commit(COLLECTION_NAME);

		Query query = new SimpleQuery(new SimpleStringCriteria("*:*")).addSort(new Sort(Sort.Direction.DESC, "name"));
		Page<ExampleSolrBean> page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);

		ExampleSolrBean prev = page.getContent().get(0);
		for (int i = 1; i < page.getContent().size(); i++) {
			ExampleSolrBean cur = page.getContent().get(i);
			assertTrue(Long.valueOf(cur.getId()) < Long.valueOf(prev.getId()));
			prev = cur;
		}
	}

	@Test
	public void testQueryWithMultiSort() {
		List<ExampleSolrBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ExampleSolrBean bean = createExampleBeanWithId(Integer.toString(i));
			bean.setInStock(i % 2 == 0);
			values.add(bean);
		}
		solrTemplate.saveBeans(COLLECTION_NAME, values);
		solrTemplate.commit(COLLECTION_NAME);

		Query query = new SimpleQuery(new SimpleStringCriteria("*:*")).addSort(new Sort(Sort.Direction.DESC, "inStock"))
				.addSort(new Sort(Sort.Direction.ASC, "name"));
		Page<ExampleSolrBean> page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);

		ExampleSolrBean prev = page.getContent().get(0);
		for (int i = 1; i < 5; i++) {
			ExampleSolrBean cur = page.getContent().get(i);
			assertTrue(cur.isInStock());
			assertTrue(Long.valueOf(cur.getId()) > Long.valueOf(prev.getId()));
			prev = cur;
		}

		prev = page.getContent().get(5);
		for (int i = 6; i < page.getContent().size(); i++) {
			ExampleSolrBean cur = page.getContent().get(i);
			assertFalse(cur.isInStock());
			assertTrue(Long.valueOf(cur.getId()) > Long.valueOf(prev.getId()));
			prev = cur;
		}
	}

	@Test
	@Ignore("https://issues.apache.org/jira/browse/SOLR-12069")
	public void testQueryWithDefaultOperator() {
		List<ExampleSolrBean> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ExampleSolrBean bean = createExampleBeanWithId(Integer.toString(i));
			bean.setInStock(i % 2 == 0);
			values.add(bean);
		}
		solrTemplate.saveBeans(COLLECTION_NAME, values);
		solrTemplate.commit(COLLECTION_NAME);

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("inStock:(true false)"));
		query.setDefaultOperator(Operator.AND);
		Page<ExampleSolrBean> page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);
		assertEquals(0, page.getContent().size());

		query.setDefaultOperator(Operator.OR);
		page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);
		assertEquals(10, page.getContent().size());
	}

	@Test
	public void testQueryWithDefType() {
		List<ExampleSolrBean> values = createBeansWithIdAndPrefix(5, "id-");
		solrTemplate.saveBeans(COLLECTION_NAME, values);
		solrTemplate.commit(COLLECTION_NAME);

		SimpleQuery query = new SimpleQuery(new Criteria("id").in("id-1", "id-2", "id-3"));
		query.setDefType("lucene");
		query.setDefaultOperator(Operator.OR);

		Page<ExampleSolrBean> page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);
		assertEquals(3, page.getContent().size());
	}

	@Test
	public void testQueryWithRequestHandler() {
		List<ExampleSolrBean> values = createBeansWithIdAndPrefix(2, "rh-");
		values.get(0).setInStock(false);
		values.get(1).setInStock(true);

		solrTemplate.saveBeans(COLLECTION_NAME, values);
		solrTemplate.commit(COLLECTION_NAME);

		SimpleQuery query = new SimpleQuery(new Criteria("id").in("rh-1", "rh-2"));
		Page<ExampleSolrBean> page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);
		assertEquals(2, page.getContent().size());

		query = new SimpleQuery(new Criteria("id").in("rh-1", "rh-2"));
		query.setRequestHandler("/instock");
		page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);
		assertEquals(1, page.getContent().size());
		assertEquals("rh-2", page.getContent().get(0).getId());
	}

	@Test
	public void testQueryWithJoinOperation() {
		ExampleSolrBean belkin = new ExampleSolrBean("belkin", "Belkin", null);
		ExampleSolrBean apple = new ExampleSolrBean("apple", "Apple", null);

		ExampleSolrBean ipod = new ExampleSolrBean("F8V7067-APL-KIT", "Belkin Mobile Power Cord for iPod", null);
		ipod.setManufacturerId(belkin.getId());

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(belkin, apple, ipod));
		solrTemplate.commit(COLLECTION_NAME);

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("text:ipod"));
		query.setJoin(Join.from("manu_id_s").to("id"));

		Page<ExampleSolrBean> page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);
		assertEquals(1, page.getContent().size());
		assertEquals(belkin.getId(), page.getContent().get(0).getId());
	}

	@Test // DATASOLR-176
	public void testQueryWithJoinFromIndexOperation() {
		ExampleSolrBean belkin = new ExampleSolrBean("belkin", "Belkin", null);
		ExampleSolrBean apple = new ExampleSolrBean("apple", "Apple", null);

		ExampleSolrBean ipod = new ExampleSolrBean("F8V7067-APL-KIT", "Belkin Mobile Power Cord for iPod", null);
		ipod.setManufacturerId(belkin.getId());

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(belkin, apple, ipod));
		solrTemplate.commit(COLLECTION_NAME);

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("text:ipod"));
		query.setJoin(Join.from("manu_id_s").fromIndex("collection1").to("id"));

		Page<ExampleSolrBean> page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);
		assertEquals(1, page.getContent().size());
		assertEquals(belkin.getId(), page.getContent().get(0).getId());
	}

	@Test
	public void testQueryWithHighlights() {
		ExampleSolrBean belkin = new ExampleSolrBean("GB18030TEST", "Test with some GB18030TEST", null);
		ExampleSolrBean apple = new ExampleSolrBean("UTF8TEST", "Test with some UTF8TEST", null);

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(belkin, apple));
		solrTemplate.commit(COLLECTION_NAME);

		SimpleHighlightQuery query = new SimpleHighlightQuery(new SimpleStringCriteria("name:with"));
		query.setHighlightOptions(new HighlightOptions());

		HighlightQueryResult<ExampleSolrBean> page = solrTemplate.queryForHighlightPage(COLLECTION_NAME, query,
				ExampleSolrBean.class);
		assertEquals(2, page.getHighlighted().size());

		assertEquals("name", page.getHighlighted().get(0).getHighlights().get(0).getField().getName());
		assertEquals("Test <em>with</em> some GB18030TEST",
				page.getHighlighted().get(0).getHighlights().get(0).getSnipplets().get(0));
	}

	@Test
	public void testTermsQuery() {
		TermsQuery query = SimpleTermsQuery.queryBuilder().fields("name").build();

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one two three", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two three", null);
		ExampleSolrBean bean3 = new ExampleSolrBean("id-3", "three", null);

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2, bean3));
		solrTemplate.commit(COLLECTION_NAME);

		TermsPage page = solrTemplate.queryForTermsPage(COLLECTION_NAME, query);

		ArrayList<TermsFieldEntry> values = Lists.newArrayList(page.getTermsForField("name"));
		assertEquals("three", values.get(0).getValue());
		assertEquals(3, values.get(0).getValueCount());

		assertEquals("two", values.get(1).getValue());
		assertEquals(2, values.get(1).getValueCount());

		assertEquals("one", values.get(2).getValue());
		assertEquals(1, values.get(2).getValueCount());
	}

	@Test
	public void testFuctionQueryInFilterReturnsProperResult() {
		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2));
		solrTemplate.commit(COLLECTION_NAME);

		Query q = new SimpleQuery("*:*")
				.addFilterQuery(new SimpleFilterQuery(new Criteria(QueryFunction.query("{!query v = 'one'}"))));

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME, q, ExampleSolrBean.class);
		assertThat(result.getContent().get(0).getId(), equalTo(bean1.getId()));
	}

	@Test
	public void testFuctionQueryReturnsProperResult() {
		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2));
		solrTemplate.commit(COLLECTION_NAME);

		Query q = new SimpleQuery(new Criteria(QueryFunction.query("{!query v='two'}")));

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME, q, ExampleSolrBean.class);
		assertThat(result.getContent().get(0).getId(), is(bean2.getId()));
	}

	@Test
	@Ignore("No longer supported in Solr 6 - A ValueSource isn't directly available from this field. Instead try a query using the distance as the score.")
	public void testFunctionQueryInFieldProjection() {
		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		bean1.setStore("45.17614,-93.87341");
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "one two", null);
		bean2.setStore("40.7143,-74.006");

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2));
		solrTemplate.commit(COLLECTION_NAME);

		Query q = new SimpleQuery("*:*");
		q.addProjectionOnField(new DistanceField("distance", "store", new Point(45.15, -93.85)));
		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME, q, ExampleSolrBean.class);
		for (ExampleSolrBean bean : result) {
			assertThat(bean.getDistance(), notNullValue());
		}

	}

	@Test // DATASOLR-162
	public void testDelegatingCursorLoadsAllElements() throws IOException {

		solrTemplate.saveBeans(COLLECTION_NAME, createBeansWithId(100));
		solrTemplate.commit(COLLECTION_NAME);

		Cursor<ExampleSolrBean> cursor = solrTemplate.queryForCursor(COLLECTION_NAME,
				new SimpleQuery("*:*").addSort(new Sort(Direction.DESC, "id")), ExampleSolrBean.class);

		int i = 0;
		while (cursor.hasNext()) {
			cursor.next();
			i++;
		}
		cursor.close();

		assertThat(i, is(100));
	}

	@Test // DATASOLR-121
	public void testRegularGroupQuery() {
		solrTemplate.saveBean(COLLECTION_NAME, new ExampleSolrBean("id_1", "name1", "category1", 2, true));
		solrTemplate.saveBean(COLLECTION_NAME, new ExampleSolrBean("id_2", "name1", "category2"));
		solrTemplate.saveBean(COLLECTION_NAME, new ExampleSolrBean("id_3", "name2", "category2", 1, true));
		solrTemplate.saveBean(COLLECTION_NAME, new ExampleSolrBean("id_4", "name3", "category2"));
		solrTemplate.commit(COLLECTION_NAME);

		Function f = IfFunction.when("inStock").then("1").otherwise("2");
		Query q1 = new SimpleQuery("cat:category2");
		Query q2 = new SimpleQuery("cat:category1");

		SimpleQuery groupQuery = new SimpleQuery(new SimpleStringCriteria("*:*"));
		GroupOptions groupOptions = new GroupOptions();
		groupQuery.setGroupOptions(groupOptions);
		groupOptions.addSort(new Sort("name", "id"));
		groupOptions.addGroupByField("name");
		groupOptions.addGroupByFunction(f);
		groupOptions.addGroupByQuery(q1);
		groupOptions.addGroupByQuery(q2);
		groupOptions.setLimit(2);

		// asserts result page
		GroupPage<ExampleSolrBean> groupResultPage = solrTemplate.queryForGroupPage(COLLECTION_NAME, groupQuery,
				ExampleSolrBean.class);
		GroupResult<ExampleSolrBean> groupResultName = groupResultPage.getGroupResult("name");
		GroupResult<ExampleSolrBean> groupResultFunction = groupResultPage.getGroupResult(f);
		GroupResult<ExampleSolrBean> groupResultQuery1 = groupResultPage.getGroupResult(q1);
		GroupResult<ExampleSolrBean> groupResultQuery2 = groupResultPage.getGroupResult(q2);

		// asserts field group
		Page<GroupEntry<ExampleSolrBean>> nameGroupEntries = groupResultName.getGroupEntries();
		assertEquals(3, nameGroupEntries.getTotalElements());
		List<GroupEntry<ExampleSolrBean>> nameGroupEntriesContent = nameGroupEntries.getContent();
		assertGroupEntry(nameGroupEntriesContent.get(0), 2, "name1", 2, "id_1", "id_2");
		assertGroupEntry(nameGroupEntriesContent.get(1), 1, "name2", 1, "id_3");
		assertGroupEntry(nameGroupEntriesContent.get(2), 1, "name3", 1, "id_4");

		// asserts function group
		Page<GroupEntry<ExampleSolrBean>> functionGroupEntries = groupResultFunction.getGroupEntries();
		assertEquals(2, functionGroupEntries.getNumberOfElements());
		List<GroupEntry<ExampleSolrBean>> functionGroupEntriesContent = functionGroupEntries.getContent();
		assertGroupEntry(functionGroupEntriesContent.get(0), 2, "1.0", 2, "id_1", "id_3");
		assertGroupEntry(functionGroupEntriesContent.get(1), 2, "2.0", 2, "id_2", "id_4");

		// asserts first query group
		Page<GroupEntry<ExampleSolrBean>> query1GroupEntries = groupResultQuery1.getGroupEntries();
		assertEquals(1, query1GroupEntries.getNumberOfElements());
		GroupEntry<ExampleSolrBean> query1GroupEntry = query1GroupEntries.getContent().get(0);
		assertGroupEntry(query1GroupEntry, 3, "cat:category2", 2, "id_2", "id_3");
		assertTrue(query1GroupEntry.getResult().hasNext());

		// asserts second query group
		Page<GroupEntry<ExampleSolrBean>> query2GroupEntries = groupResultQuery2.getGroupEntries();
		assertEquals(1, query2GroupEntries.getNumberOfElements());
		GroupEntry<ExampleSolrBean> query2GroupEntry = query2GroupEntries.getContent().get(0);
		assertGroupEntry(query2GroupEntry, 1, "cat:category1", 1, "id_1");
		assertFalse(query2GroupEntry.getResult().hasNext());
	}

	@Test // DATASOLR-121
	@Ignore("Seems to be broken on solr side")
	public void testGroupQueryWithFacets() {
		solrTemplate.saveBean(COLLECTION_NAME, new ExampleSolrBean("id_1", "name1", "category1", 2, true));
		solrTemplate.saveBean(COLLECTION_NAME, new ExampleSolrBean("id_2", "name1", "category1", 2, true));
		solrTemplate.saveBean(COLLECTION_NAME, new ExampleSolrBean("id_3", "name1", "category1", 1, true));
		solrTemplate.saveBean(COLLECTION_NAME, new ExampleSolrBean("id_4", "name2", "category2", 1, false));
		solrTemplate.saveBean(COLLECTION_NAME, new ExampleSolrBean("id_5", "name2", "category2", 2, false));
		solrTemplate.saveBean(COLLECTION_NAME, new ExampleSolrBean("id_6", "name2", "category1", 1, true));
		solrTemplate.commit(COLLECTION_NAME);

		SimpleFacetQuery groupQuery = new SimpleFacetQuery(new SimpleStringCriteria("*:*"));
		GroupOptions groupOptions = new GroupOptions();
		groupQuery.setGroupOptions(groupOptions);
		groupQuery.setFacetOptions(new FacetOptions("inStock"));
		groupOptions.addGroupByField("name");
		groupOptions.setGroupFacets(true);
		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> groupResultPage = solrTemplate
				.queryForFacetPage(COLLECTION_NAME, groupQuery, ExampleSolrBean.class);

		Page<FacetFieldEntry> facetResultPage = groupResultPage.getFacetResultPage("inStock");

		List<FacetFieldEntry> facetContent = facetResultPage.getContent();

		assertEquals("true", facetContent.get(0).getValue());
		assertEquals("false", facetContent.get(1).getValue());
		assertEquals(2, facetContent.get(0).getValueCount());
		assertEquals(1, facetContent.get(1).getValueCount());
	}

	private void assertGroupEntryContentIds(GroupEntry<ExampleSolrBean> groupEntry, String... ids) {
		for (int i = 0; i < ids.length; i++) {
			assertEquals(ids[i], groupEntry.getResult().getContent().get(i).getId());
		}
	}

	private void assertGroupEntry(GroupEntry<ExampleSolrBean> entry, long totalElements, String groupValue,
			int numberOfDocuments, String... ids) {
		assertEquals(totalElements, entry.getResult().getTotalElements());
		assertEquals(groupValue, entry.getGroupValue());
		assertEquals(numberOfDocuments, entry.getResult().getContent().size());
		assertGroupEntryContentIds(entry, ids);
	}

	@Test // DATASOLR-83
	public void testGetById() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2));

		Optional<ExampleSolrBean> beanReturned = solrTemplate.getById(COLLECTION_NAME, "id-1", ExampleSolrBean.class);

		assertEquals(bean1.getId(), beanReturned.get().getId());
	}

	@Test // DATASOLR-83
	public void testGetByIds() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2));

		List<String> ids = Arrays.<String> asList("id-1", "id-2");
		Collection<ExampleSolrBean> beansReturned = solrTemplate.getByIds(COLLECTION_NAME, ids, ExampleSolrBean.class);
		List<ExampleSolrBean> listBeansReturned = new ArrayList<>(beansReturned);

		assertEquals(2, beansReturned.size());
		assertEquals(bean1.getId(), listBeansReturned.get(0).getId());
		assertEquals(bean2.getId(), listBeansReturned.get(1).getId());
	}

	@Test // DATASOLR-160
	public void testQueryWithFieldsStatsAndFaceting() {
		StatsOptions statsOptions = new StatsOptions().addField("price").addFacet("name");
		executeAndCheckStatsRequest(statsOptions);
	}

	@Test // DATASOLR-160
	public void testQueryWithFieldsStatsAndSelectiveFaceting() {

		StatsOptions statsOptions = new StatsOptions().addField("price").addSelectiveFacet("name");
		executeAndCheckStatsRequest(statsOptions);
	}

	@Test // DATASOLR-160
	public void testDistinctStatsRequest() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "name1", null);
		bean1.setPrice(10);
		bean1.setPopularity(1);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "name2", null);
		bean2.setPrice(20);
		bean1.setPopularity(1);
		ExampleSolrBean bean3 = new ExampleSolrBean("id-3", "name3", null);
		bean3.setPrice(20);
		bean1.setPopularity(2);

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2, bean3));
		solrTemplate.commit(COLLECTION_NAME);

		StatsOptions statsOptions = new StatsOptions().addField("popularity").addField("price")
				.setSelectiveCalcDistinct(true);

		SimpleQuery statsQuery = new SimpleQuery(new SimpleStringCriteria("*:*"));
		statsQuery.setStatsOptions(statsOptions);
		StatsPage<ExampleSolrBean> statResultPage = solrTemplate.queryForStatsPage(COLLECTION_NAME, statsQuery,
				ExampleSolrBean.class);

		FieldStatsResult priceStatResult = statResultPage.getFieldStatsResult("price");
		FieldStatsResult popularityStatResult = statResultPage.getFieldStatsResult("popularity");

		assertEquals(Long.valueOf(2), priceStatResult.getDistinctCount());
		Collection<Object> distinctValues = priceStatResult.getDistinctValues();
		assertEquals(2, distinctValues.size());
		assertTrue(distinctValues.contains(10.0F));
		assertTrue(distinctValues.contains(20.0F));
		assertEquals(null, popularityStatResult.getDistinctCount());
	}

	@Test // DATASOLR-86
	public void testRangeFacetRequest() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "name1", null);
		bean1.setPrice(10);
		bean1.setPopularity(1);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "name2", null);
		bean2.setPrice(20);
		bean1.setPopularity(1);
		ExampleSolrBean bean3 = new ExampleSolrBean("id-3", "name3", null);
		bean3.setPrice(20);
		bean1.setPopularity(2);

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2, bean3));
		solrTemplate.commit(COLLECTION_NAME);

		FacetOptions facetOptions = new FacetOptions()
				.addFacetByRange(new FieldWithNumericRangeParameters("price", 5, 20, 5).setInclude(FacetRangeInclude.ALL));
		facetOptions.setFacetMinCount(0);

		SimpleFacetQuery statsQuery = new SimpleFacetQuery(new SimpleStringCriteria("*:*"));
		statsQuery.setFacetOptions(facetOptions);
		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> statResultPage = solrTemplate
				.queryForFacetPage(COLLECTION_NAME, statsQuery, ExampleSolrBean.class);

		Page<FacetFieldEntry> priceRangeResult = statResultPage.getRangeFacetResultPage("price");

		List<FacetFieldEntry> content = priceRangeResult.getContent();

		assertEquals(3, priceRangeResult.getTotalElements());
		assertEquals(2, content.get(2).getValueCount());
		assertEquals(1, content.get(1).getValueCount());

		assertEquals("5.0", content.get(0).getValue());
		assertEquals("10.0", content.get(1).getValue());
		assertEquals("15.0", content.get(2).getValue());
	}

	@Test // DATASOLR-248
	public void shouldAllowReadingMultivaluedFieldWithOnlyOneEntryIntoSingleValuedProperty() {

		solrTemplate.execute((SolrCallback<Object>) solrClient -> {

			SolrInputDocument sid = new SolrInputDocument();
			sid.addField("id", "id-1");
			sid.addField("title", "title");
			solrClient.add(sid).getStatus();
			return solrClient.commit(COLLECTION_NAME);
		});

		Optional<SomeDoc> document = solrTemplate.queryForObject(COLLECTION_NAME, new SimpleQuery("id:id-1"),
				SomeDoc.class);
		assertThat(document.get().title, is(equalTo("title")));
	}

	@Test // DATASOLR-248
	public void shouldThrowExceptionReadingMultivaluedFieldWithManyEntriesIntoSingleValuedProperty() {

		solrTemplate.execute((SolrCallback<Object>) solrClient -> {

			SolrInputDocument sid = new SolrInputDocument();
			sid.addField("id", "id-1");
			sid.addField("title", new String[] { "title-1", "title-2" });
			solrClient.add(sid).getStatus();
			return solrClient.commit(COLLECTION_NAME);
		});

		exception.expect(MappingException.class);
		exception.expectMessage("title-1");
		exception.expectMessage("title-2");

		solrTemplate.queryForObject(COLLECTION_NAME, new SimpleQuery("id:id-1"), SomeDoc.class);
	}

	@Test // DATASOLR-248
	public void shouldAllowReadingMultivaluedFieldWithNoEntriesIntoSingleValuedProperty() {

		solrTemplate.execute((SolrCallback<Object>) solrClient -> {

			SolrInputDocument sid = new SolrInputDocument();
			sid.addField("id", "id-1");
			solrClient.add(sid).getStatus();
			return solrClient.commit(COLLECTION_NAME);
		});

		Optional<SomeDoc> document = solrTemplate.queryForObject(COLLECTION_NAME, new SimpleQuery("id:id-1"),
				SomeDoc.class);
		assertThat(document.get().title, is(nullValue()));
	}

	@Test // DATASOLR-137
	public void testFindByNameWithSpellcheckSeggestion() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "green", null);
		solrTemplate.saveBean(COLLECTION_NAME, bean1);
		solrTemplate.commit(COLLECTION_NAME);

		SimpleQuery q = new SimpleQuery("name:gren");
		q.setSpellcheckOptions(SpellcheckOptions.spellcheck());
		q.setRequestHandler("/spell");

		SpellcheckedPage<ExampleSolrBean> found = solrTemplate.query(COLLECTION_NAME, q, ExampleSolrBean.class);
		assertThat(found.hasContent(), Is.is(false));
		assertThat(found.getSuggestions().size(), Is.is(Matchers.greaterThan(0)));
		assertThat(found.getSuggestions(), Matchers.contains("green"));
	}

	@Test // DATSOLR-364
	public void shouldUseBaseUrlInCollectionCallbackWhenExecutingCommands() {

		final HttpSolrClient client = new HttpSolrClient.Builder().withBaseSolrUrl("http://127.0.0.1/solr/").build();

		SolrTemplate solrTemplate = new SolrTemplate(new HttpSolrClientFactory(client));

		solrTemplate.execute(solrClient -> {

			assertThat(((HttpSolrClient) solrClient).getBaseURL(), is("http://127.0.0.1/solr"));
			return null;
		});
	}

	private void executeAndCheckStatsRequest(StatsOptions statsOptions) {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		bean1.setPrice(10f);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		bean2.setPrice(20.5f);
		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2));
		solrTemplate.commit(COLLECTION_NAME);

		SimpleQuery statsQuery = new SimpleQuery(new SimpleStringCriteria("*:*"));
		statsQuery.setStatsOptions(statsOptions);
		StatsPage<ExampleSolrBean> statResultPage = solrTemplate.queryForStatsPage(COLLECTION_NAME, statsQuery,
				ExampleSolrBean.class);

		FieldStatsResult priceStats = statResultPage.getFieldStatsResult("price");
		assertEquals(Long.valueOf(2), priceStats.getCount());
		assertEquals(10D, priceStats.getMin());
		assertEquals(20.50, priceStats.getMax());
		assertEquals(Double.valueOf(10), priceStats.getMinAsDouble());
		assertEquals(Double.valueOf(20.50), priceStats.getMaxAsDouble());
		assertEquals("10.0", priceStats.getMinAsString());
		assertEquals("20.5", priceStats.getMaxAsString());
		assertNull(priceStats.getMinAsDate());
		assertNull(priceStats.getMaxAsDate());
		assertEquals(15.25, priceStats.getMean());
		assertEquals(30.50, priceStats.getSum());
		assertEquals(Long.valueOf(0), priceStats.getMissing());
		assertEquals(Double.valueOf(7.424621202458749), priceStats.getStddev());
		assertEquals(Double.valueOf(520.25), priceStats.getSumOfSquares());

		Map<String, StatsResult> facetStatsResult = priceStats.getFacetStatsResult(new SimpleField("name"));
		assertEquals(2, facetStatsResult.size());
		{
			StatsResult nameFacetStatsResult = facetStatsResult.get("one");
			assertEquals(Long.valueOf(1), nameFacetStatsResult.getCount());
			assertEquals(10D, nameFacetStatsResult.getMin());
			assertEquals(10D, nameFacetStatsResult.getMax());
		}
		{
			StatsResult nameFacetStatsResult = facetStatsResult.get("two");
			assertEquals(Long.valueOf(1), nameFacetStatsResult.getCount());
			assertEquals(20.5D, nameFacetStatsResult.getMin());
			assertEquals(20.5D, nameFacetStatsResult.getMax());
		}
	}

	@Data
	static class SomeDoc {

		@Id String id;

		@Field String title;
	}

}
