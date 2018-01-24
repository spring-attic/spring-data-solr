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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.FacetParams.FacetRangeInclude;
import org.apache.solr.common.params.FacetParams.FacetRangeOther;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Assert;
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
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;
import org.springframework.data.solr.ExampleSolrBean;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.DistanceField;
import org.springframework.data.solr.core.query.FacetAndHighlightQuery;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetOptions.FacetSort;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithDateRangeParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithFacetParameters;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithNumericRangeParameters;
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
import org.springframework.data.solr.core.query.SimpleFacetAndHighlightQuery;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.SimpleTermsQuery;
import org.springframework.data.solr.core.query.SimpleUpdateField;
import org.springframework.data.solr.core.query.SpellcheckOptions;
import org.springframework.data.solr.core.query.StatsOptions;
import org.springframework.data.solr.core.query.TermsQuery;
import org.springframework.data.solr.core.query.Update;
import org.springframework.data.solr.core.query.UpdateAction;
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
import org.springframework.data.solr.server.support.MulticoreSolrClientFactory;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;

import lombok.Data;

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
		solrTemplate = new SolrTemplate(server, "collection1");
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

		Page<ExampleSolrBean> recalled = solrTemplate
				.queryForPage(new SimpleQuery(new SimpleStringCriteria("popularity:5")), ExampleSolrBean.class);

		Assert.assertEquals(5, recalled.getNumberOfElements());

		for (ExampleSolrBean bean : recalled) {
			Assert.assertEquals("Category must not change on partial update", "category_" + bean.getId(),
					bean.getCategory().get(0));
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

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(q, ExampleSolrBean.class);

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

	@Test // DATSOLR-86
	public void testFacetQueryWithDateFacetRangeField() {
		List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>();
		for (int i = 0; i < 10; i++) {
			final ExampleSolrBean exampleSolrBean = createExampleBeanWithId(Integer.toString(i));
			exampleSolrBean.setLastModified(new GregorianCalendar(2013, Calendar.DECEMBER, (i + 10) / 2).getTime());
			values.add(exampleSolrBean);
		}
		solrTemplate.saveBeans(values);
		solrTemplate.commit();

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
				.queryForFacetPage(q, ExampleSolrBean.class);

		Assert.assertEquals(FacetRangeInclude.LOWER, lastModifiedField.getQueryParameterValue(FACET_RANGE_INCLUDE));

		for (Page<FacetFieldEntry> facetResultPage : page.getFacetResultPages()) {
			Assert.assertEquals(5, facetResultPage.getNumberOfElements());
		}

		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("last_modified"));
		for (FacetFieldEntry entry : facetPage) {
			Assert.assertNotNull(entry.getValue());
			Assert.assertEquals("last_modified", entry.getField().getName());
			Assert.assertEquals(2l, entry.getValueCount());
		}
	}

	@Test // DATSOLR-86
	public void testFacetQueryWithNumericFacetRangeField() {
		List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>();
		for (int i = 0; i < 10; i++) {
			final ExampleSolrBean exampleSolrBean = createExampleBeanWithId(Integer.toString(i));
			exampleSolrBean.setPopularity((i + 1) * 100);
			values.add(exampleSolrBean);
		}
		solrTemplate.saveBeans(values);
		solrTemplate.commit();

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
				.queryForFacetPage(q, ExampleSolrBean.class);

		for (Page<FacetFieldEntry> facetResultPage : page.getFacetResultPages()) {
			Assert.assertEquals(4, facetResultPage.getNumberOfElements());
		}

		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("popularity"));
		for (FacetFieldEntry entry : facetPage) {
			Assert.assertNotNull(entry.getValue());
			Assert.assertEquals("popularity", entry.getField().getName());
			Assert.assertEquals(2l, entry.getValueCount());
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

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(q, ExampleSolrBean.class);

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
		q.setFacetOptions(new FacetOptions(new SimpleQuery(new SimpleStringCriteria("inStock:true")),
				new SimpleQuery(new SimpleStringCriteria("inStock:false"))));

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(q, ExampleSolrBean.class);

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

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(q, ExampleSolrBean.class);
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

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(q, ExampleSolrBean.class);
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

	@Test // DATASOLR-244
	public void testFacetAndHighlightQueryWithFacetFields() {

		List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>();
		for (int i = 0; i < 10; i++) {
			values.add(createExampleBeanWithId(Integer.toString(i)));
		}
		solrTemplate.saveBeans(values);
		solrTemplate.commit();

		FacetAndHighlightQuery q = new SimpleFacetAndHighlightQuery(
				new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
						.setFacetOptions(new FacetOptions().addFacetOnField("name").addFacetOnField("id").setFacetLimit(5));

		q.setHighlightOptions(new HighlightOptions().addField("name"));

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(q, ExampleSolrBean.class);

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

	@Test // DATASOLR-176
	public void testQueryWithJoinFromIndexOperation() {
		ExampleSolrBean belkin = new ExampleSolrBean("belkin", "Belkin", null);
		ExampleSolrBean apple = new ExampleSolrBean("apple", "Apple", null);

		ExampleSolrBean ipod = new ExampleSolrBean("F8V7067-APL-KIT", "Belkin Mobile Power Cord for iPod", null);
		ipod.setManufacturerId(belkin.getId());

		solrTemplate.saveBeans(Arrays.asList(belkin, apple, ipod));
		solrTemplate.commit();

		SimpleQuery query = new SimpleQuery(new SimpleStringCriteria("text:ipod"));
		query.setJoin(Join.from("manu_id_s").fromIndex("collection1").to("id"));

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

		HighlightQueryResult<ExampleSolrBean> page = solrTemplate.queryForHighlightPage(query, ExampleSolrBean.class);
		Assert.assertEquals(2, page.getHighlighted().size());

		Assert.assertEquals("name", page.getHighlighted().get(0).getHighlights().get(0).getField().getName());
		Assert.assertEquals("Test <em>with</em> some GB18030TEST",
				page.getHighlighted().get(0).getHighlights().get(0).getSnipplets().get(0));
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
				.addFilterQuery(new SimpleFilterQuery(new Criteria(QueryFunction.query("{!query v = 'one'}"))));

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(q, ExampleSolrBean.class);
		Assert.assertThat(result.getContent().get(0).getId(), equalTo(bean1.getId()));
	}

	@Test
	public void testFuctionQueryReturnsProperResult() {
		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		solrTemplate.saveBeans(Arrays.asList(bean1, bean2));
		solrTemplate.commit();

		Query q = new SimpleQuery(new Criteria(QueryFunction.query("{!query v='two'}")));

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(q, ExampleSolrBean.class);
		Assert.assertThat(result.getContent().get(0).getId(), is(bean2.getId()));
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
			Assert.assertThat(bean.getDistance(), notNullValue());
		}

	}

	@Test // DATASOLR-162
	public void testDelegatingCursorLoadsAllElements() throws IOException {

		solrTemplate.saveBeans(createBeansWithId(100));
		solrTemplate.commit();

		Cursor<ExampleSolrBean> cursor = solrTemplate
				.queryForCursor(new SimpleQuery("*:*").addSort(new Sort(Direction.DESC, "id")), ExampleSolrBean.class);

		int i = 0;
		while (cursor.hasNext()) {
			cursor.next();
			i++;
		}
		cursor.close();

		Assert.assertThat(i, is(100));
	}

	@Test // DATASOLR-121
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
		groupOptions.addSort(new Sort("name", "id"));
		groupOptions.addGroupByField("name");
		groupOptions.addGroupByFunction(f);
		groupOptions.addGroupByQuery(q1);
		groupOptions.addGroupByQuery(q2);
		groupOptions.setLimit(2);

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
		assertGroupEntry(functionGroupEntriesContent.get(0), 2, "1.0", 2, "id_1", "id_3");
		assertGroupEntry(functionGroupEntriesContent.get(1), 2, "2.0", 2, "id_2", "id_4");

		// asserts first query group
		Page<GroupEntry<ExampleSolrBean>> query1GroupEntries = groupResultQuery1.getGroupEntries();
		Assert.assertEquals(1, query1GroupEntries.getNumberOfElements());
		GroupEntry<ExampleSolrBean> query1GroupEntry = query1GroupEntries.getContent().get(0);
		assertGroupEntry(query1GroupEntry, 3, "cat:category2", 2, "id_2", "id_3");
		assertTrue(query1GroupEntry.getResult().hasNext());

		// asserts second query group
		Page<GroupEntry<ExampleSolrBean>> query2GroupEntries = groupResultQuery2.getGroupEntries();
		Assert.assertEquals(1, query2GroupEntries.getNumberOfElements());
		GroupEntry<ExampleSolrBean> query2GroupEntry = query2GroupEntries.getContent().get(0);
		assertGroupEntry(query2GroupEntry, 1, "cat:category1", 1, "id_1");
		assertFalse(query2GroupEntry.getResult().hasNext());
	}

	@Test // DATASOLR-121
	@Ignore("Seems to be broken on solr side")
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
		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> groupResultPage = solrTemplate
				.queryForFacetPage(groupQuery, ExampleSolrBean.class);

		Page<FacetFieldEntry> facetResultPage = groupResultPage.getFacetResultPage("inStock");

		List<FacetFieldEntry> facetContent = facetResultPage.getContent();

		Assert.assertEquals("true", facetContent.get(0).getValue());
		Assert.assertEquals("false", facetContent.get(1).getValue());
		Assert.assertEquals(2, facetContent.get(0).getValueCount());
		Assert.assertEquals(1, facetContent.get(1).getValueCount());
	}

	private void assertGroupEntryContentIds(GroupEntry<ExampleSolrBean> groupEntry, String... ids) {
		for (int i = 0; i < ids.length; i++) {
			Assert.assertEquals(ids[i], groupEntry.getResult().getContent().get(i).getId());
		}
	}

	private void assertGroupEntry(GroupEntry<ExampleSolrBean> entry, long totalElements, String groupValue,
			int numberOfDocuments, String... ids) {
		Assert.assertEquals(totalElements, entry.getResult().getTotalElements());
		Assert.assertEquals(groupValue, entry.getGroupValue());
		Assert.assertEquals(numberOfDocuments, entry.getResult().getContent().size());
		assertGroupEntryContentIds(entry, ids);
	}

	@Test // DATASOLR-83
	public void testGetById() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		solrTemplate.saveBeans(Arrays.asList(bean1, bean2));

		ExampleSolrBean beanReturned = solrTemplate.getById("id-1", ExampleSolrBean.class);

		Assert.assertEquals(bean1.getId(), beanReturned.getId());
	}

	@Test // DATASOLR-83
	public void testGetByIds() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		solrTemplate.saveBeans(Arrays.asList(bean1, bean2));

		List<String> ids = Arrays.<String> asList("id-1", "id-2");
		Collection<ExampleSolrBean> beansReturned = solrTemplate.getById(ids, ExampleSolrBean.class);
		List<ExampleSolrBean> listBeansReturned = new ArrayList<ExampleSolrBean>(beansReturned);

		Assert.assertEquals(2, beansReturned.size());
		Assert.assertEquals(bean1.getId(), listBeansReturned.get(0).getId());
		Assert.assertEquals(bean2.getId(), listBeansReturned.get(1).getId());
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

		solrTemplate.saveBeans(Arrays.asList(bean1, bean2, bean3));
		solrTemplate.commit();

		StatsOptions statsOptions = new StatsOptions().addField("popularity").addField("price")
				.setSelectiveCalcDistinct(true);

		SimpleQuery statsQuery = new SimpleQuery(new SimpleStringCriteria("*:*"));
		statsQuery.setStatsOptions(statsOptions);
		StatsPage<ExampleSolrBean> statResultPage = solrTemplate.queryForStatsPage(statsQuery, ExampleSolrBean.class);

		FieldStatsResult priceStatResult = statResultPage.getFieldStatsResult("price");
		FieldStatsResult popularityStatResult = statResultPage.getFieldStatsResult("popularity");

		Assert.assertEquals(Long.valueOf(2), priceStatResult.getDistinctCount());
		Collection<Object> distinctValues = priceStatResult.getDistinctValues();
		Assert.assertEquals(2, distinctValues.size());
		Assert.assertTrue(distinctValues.contains(10.0F));
		Assert.assertTrue(distinctValues.contains(20.0F));
		Assert.assertEquals(null, popularityStatResult.getDistinctCount());
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

		solrTemplate.saveBeans(Arrays.asList(bean1, bean2, bean3));
		solrTemplate.commit();

		FacetOptions facetOptions = new FacetOptions()
				.addFacetByRange(new FieldWithNumericRangeParameters("price", 5, 20, 5).setInclude(FacetRangeInclude.ALL));
		facetOptions.setFacetMinCount(0);

		SimpleFacetQuery statsQuery = new SimpleFacetQuery(new SimpleStringCriteria("*:*"));
		statsQuery.setFacetOptions(facetOptions);
		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> statResultPage = solrTemplate
				.queryForFacetPage(statsQuery, ExampleSolrBean.class);

		Page<FacetFieldEntry> priceRangeResult = statResultPage.getRangeFacetResultPage("price");

		List<FacetFieldEntry> content = priceRangeResult.getContent();

		Assert.assertEquals(3, priceRangeResult.getTotalElements());
		Assert.assertEquals(2, content.get(2).getValueCount());
		Assert.assertEquals(1, content.get(1).getValueCount());

		Assert.assertEquals("5.0", content.get(0).getValue());
		Assert.assertEquals("10.0", content.get(1).getValue());
		Assert.assertEquals("15.0", content.get(2).getValue());
	}

	@Test // DATASOLR-248
	public void shouldAllowReadingMultivaluedFieldWithOnlyOneEntryIntoSingleValuedProperty() {

		solrTemplate.execute(new SolrCallback<Object>() {

			@Override
			public Object doInSolr(SolrClient solrClient) throws SolrServerException, IOException {

				SolrInputDocument sid = new SolrInputDocument();
				sid.addField("id", "id-1");
				sid.addField("title", "title");
				solrClient.add(sid).getStatus();
				return solrClient.commit();
			}
		});

		SomeDoc document = solrTemplate.queryForObject(new SimpleQuery("id:id-1"), SomeDoc.class);
		assertThat(document.title, is(equalTo("title")));
	}

	@Test // DATASOLR-248
	public void shouldThrowExceptionReadingMultivaluedFieldWithManyEntriesIntoSingleValuedProperty() {

		solrTemplate.execute(new SolrCallback<Object>() {

			@Override
			public Object doInSolr(SolrClient solrClient) throws SolrServerException, IOException {

				SolrInputDocument sid = new SolrInputDocument();
				sid.addField("id", "id-1");
				sid.addField("title", new String[] { "title-1", "title-2" });
				solrClient.add(sid).getStatus();
				return solrClient.commit();
			}
		});

		exception.expect(MappingException.class);
		exception.expectMessage("title-1");
		exception.expectMessage("title-2");

		solrTemplate.queryForObject(new SimpleQuery("id:id-1"), SomeDoc.class);
	}

	@Test // DATASOLR-248
	public void shouldAllowReadingMultivaluedFieldWithNoEntriesIntoSingleValuedProperty() {

		solrTemplate.execute(new SolrCallback<Object>() {

			@Override
			public Object doInSolr(SolrClient solrClient) throws SolrServerException, IOException {

				SolrInputDocument sid = new SolrInputDocument();
				sid.addField("id", "id-1");
				solrClient.add(sid).getStatus();
				return solrClient.commit();
			}
		});

		SomeDoc document = solrTemplate.queryForObject(new SimpleQuery("id:id-1"), SomeDoc.class);
		assertThat(document.title, is(nullValue()));
	}

	@Test // DATASOLR-137
	public void testFindByNameWithSpellcheckSeggestion() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "green", null);
		solrTemplate.saveBean(bean1);
		solrTemplate.commit();

		SimpleQuery q = new SimpleQuery("name:gren");
		q.setSpellcheckOptions(SpellcheckOptions.spellcheck());
		q.setRequestHandler("/spell");

		SpellcheckedPage<ExampleSolrBean> found = solrTemplate.query(q, ExampleSolrBean.class);
		Assert.assertThat(found.hasContent(), Is.is(false));
		Assert.assertThat(found.getSuggestions().size(), Is.is(Matchers.greaterThan(0)));
		Assert.assertThat(found.getSuggestions(), Matchers.contains("green"));
	}

	@Test // DATSOLR-364
	public void shouldUseBaseUrlInCollectionCallbackWhenExecutingCommands() {

		final HttpSolrClient client = new HttpSolrClient("http://127.0.0.1/solr/");

		SolrTemplate solrTemplate = new SolrTemplate(new MulticoreSolrClientFactory(client), "collection-1");

		solrTemplate.execute("collection-1", new CollectionCallback<Object>() {
			@Override
			public Object doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {

				Assert.assertThat(((HttpSolrClient)solrClient).getBaseURL(), is("http://127.0.0.1/solr"));
				return null;
			}
		});
	}

	private void executeAndCheckStatsRequest(StatsOptions statsOptions) {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		bean1.setPrice(10f);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		bean2.setPrice(20.5f);
		solrTemplate.saveBeans(Arrays.asList(bean1, bean2));
		solrTemplate.commit();

		SimpleQuery statsQuery = new SimpleQuery(new SimpleStringCriteria("*:*"));
		statsQuery.setStatsOptions(statsOptions);
		StatsPage<ExampleSolrBean> statResultPage = solrTemplate.queryForStatsPage(statsQuery, ExampleSolrBean.class);

		FieldStatsResult priceStats = statResultPage.getFieldStatsResult("price");
		Assert.assertEquals(Long.valueOf(2), priceStats.getCount());
		Assert.assertEquals(10D, priceStats.getMin());
		Assert.assertEquals(20.50, priceStats.getMax());
		Assert.assertEquals(Double.valueOf(10), priceStats.getMinAsDouble());
		Assert.assertEquals(Double.valueOf(20.50), priceStats.getMaxAsDouble());
		Assert.assertEquals("10.0", priceStats.getMinAsString());
		Assert.assertEquals("20.5", priceStats.getMaxAsString());
		Assert.assertNull(priceStats.getMinAsDate());
		Assert.assertNull(priceStats.getMaxAsDate());
		Assert.assertEquals(Double.valueOf(15.25), priceStats.getMean());
		Assert.assertEquals(Double.valueOf(30.50), priceStats.getSum());
		Assert.assertEquals(Long.valueOf(0), priceStats.getMissing());
		Assert.assertEquals(Double.valueOf(7.424621202458749), priceStats.getStddev());
		Assert.assertEquals(Double.valueOf(520.25), priceStats.getSumOfSquares());

		Map<String, StatsResult> facetStatsResult = priceStats.getFacetStatsResult(new SimpleField("name"));
		Assert.assertEquals(2, facetStatsResult.size());
		{
			StatsResult nameFacetStatsResult = facetStatsResult.get("one");
			Assert.assertEquals(Long.valueOf(1), nameFacetStatsResult.getCount());
			Assert.assertEquals(10D, nameFacetStatsResult.getMin());
			Assert.assertEquals(10D, nameFacetStatsResult.getMax());
		}
		{
			StatsResult nameFacetStatsResult = facetStatsResult.get("two");
			Assert.assertEquals(Long.valueOf(1), nameFacetStatsResult.getCount());
			Assert.assertEquals(20.5D, nameFacetStatsResult.getMin());
			Assert.assertEquals(20.5D, nameFacetStatsResult.getMax());
		}
	}

	@Data
	static class SomeDoc {

		@Id String id;

		@Field String title;
	}

}
