/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.solr.core.query.Field.*;

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

import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.FacetParams.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
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
import org.springframework.data.solr.core.query.result.*;
import org.springframework.data.solr.server.support.HttpSolrClientFactory;

import com.google.common.collect.Lists;

/**
 * @author Christoph Strobl
 * @author Andrey Paramonov
 * @author Francisco Spaeth
 * @author Radek Mensik
 */
public class ITestSolrTemplate extends AbstractITestWithEmbeddedSolrServer {

	private static final Query DEFAULT_BEAN_OBJECT_QUERY = new SimpleQuery(new Criteria("id").is(DEFAULT_BEAN_ID));
	private static final Query ALL_DOCUMENTS_QUERY = new SimpleQuery(new SimpleStringCriteria("*:*"));

	private SolrTemplate solrTemplate;

	@Before
	public void setUp() {
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
		assertThat(recalled.isPresent()).isFalse();
		solrTemplate.commit(COLLECTION_NAME);

		recalled = solrTemplate.queryForObject(COLLECTION_NAME, new SimpleQuery(new Criteria("id").is("1")),
				ExampleSolrBean.class);
		assertThat(recalled.isPresent()).isTrue();
		assertThat(recalled.get().getId()).isEqualTo(toInsert.getId());

		solrTemplate.deleteByIds(COLLECTION_NAME, toInsert.getId());
		recalled = solrTemplate.queryForObject(COLLECTION_NAME, new SimpleQuery(new Criteria("id").is("1")),
				ExampleSolrBean.class);
		assertThat(recalled.get().getId()).isEqualTo(toInsert.getId());

		solrTemplate.commit(COLLECTION_NAME);
		recalled = solrTemplate.queryForObject(COLLECTION_NAME, new SimpleQuery(new Criteria("id").is("1")),
				ExampleSolrBean.class);
		assertThat(recalled.isPresent()).isFalse();
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

		assertThat(solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY)).isEqualTo(1);

		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertThat(recalled.get().getId()).isEqualTo(toInsert.getId());
		assertThat(recalled.get().getName()).isEqualTo("updated-name");
		assertThat(recalled.get().getPopularity()).isEqualTo(toInsert.getPopularity());
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

		assertThat(solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY)).isEqualTo(1);

		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertThat(recalled.get().getId()).isEqualTo(toInsert.getId());

		assertThat(recalled.get().getCategory().size()).isEqualTo(2);
		assertThat(recalled.get().getCategory()).isEqualTo(Arrays.asList("nosql", "spring-data-solr"));

		assertThat(recalled.get().getName()).isEqualTo(toInsert.getName());
		assertThat(recalled.get().getPopularity()).isEqualTo(toInsert.getPopularity());
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

		assertThat(solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY)).isEqualTo(1);

		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertThat(recalled.isPresent()).isTrue();
		assertThat(recalled.get().getId()).isEqualTo(toInsert.getId());

		assertThat(recalled.get().getCategory().size()).isEqualTo(1);

		assertThat(recalled.get().getName()).isEqualTo(toInsert.getName());
		assertThat(recalled.get().getPopularity()).isEqualTo(Integer.valueOf(11));
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

		assertThat(solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY)).isEqualTo(1);

		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertThat(recalled.isPresent()).isTrue();
		assertThat(recalled.get().getId()).isEqualTo(toInsert.getId());

		assertThat(recalled.get().getCategory().size()).isEqualTo(3);
		assertThat(recalled.get().getCategory()).isEqualTo(Arrays.asList("spring", "data", "solr"));

		assertThat(recalled.get().getName()).isEqualTo(toInsert.getName());
		assertThat(recalled.get().getPopularity()).isEqualTo(toInsert.getPopularity());
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

		assertThat(solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY)).isEqualTo(1);

		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertThat(recalled.isPresent()).isTrue();
		assertThat(recalled.get().getId()).isEqualTo(toInsert.getId());

		assertThat(recalled.get().getCategory().size()).isEqualTo(0);

		assertThat(recalled.get().getName()).isEqualTo(toInsert.getName());
		assertThat(recalled.get().getPopularity()).isEqualTo(toInsert.getPopularity());
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

		assertThat(solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY)).isEqualTo(1);

		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY,
				ExampleSolrBean.class);

		assertThat(recalled.get().getId()).isEqualTo(toInsert.getId());

		assertThat(recalled.get().getCategory().size()).isEqualTo(4);
		assertThat(recalled.get().getCategory())
				.isEqualTo(Arrays.asList(toInsert.getCategory().get(0), "spring", "data", "solr"));

		assertThat(recalled.get().getName()).isEqualTo(toInsert.getName());
		assertThat(recalled.get().getPopularity()).isEqualTo(toInsert.getPopularity());
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

		assertThat(solrTemplate.count(COLLECTION_NAME, ALL_DOCUMENTS_QUERY)).isEqualTo(10);

		Page<ExampleSolrBean> recalled = solrTemplate.queryForPage(COLLECTION_NAME,
				new SimpleQuery(new SimpleStringCriteria("popularity:5")), ExampleSolrBean.class);

		assertThat(recalled.getNumberOfElements()).isEqualTo(5);

		for (ExampleSolrBean bean : recalled) {
			assertThat(bean.getCategory().get(0)).as("Category must not change on partial update")
					.isEqualTo("category_" + bean.getId());
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

		assertThat(loaded.get().getCategory().size()).isEqualTo(1);

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setValueOfField("popularity", 500);
		update.setValueOfField("cat", Arrays.asList("cat-1", "cat-2", "cat-3"));
		update.setValueOfField("name", null);

		solrTemplate.saveBean(COLLECTION_NAME, update);
		solrTemplate.commit(COLLECTION_NAME);

		loaded = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);
		assertThat(loaded.get().getPopularity()).isEqualTo(Integer.valueOf(500));
		assertThat(loaded.get().getCategory().size()).isEqualTo(3);
		assertThat(loaded.get().getName()).isNull();
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

		assertThat(loaded.get().getCategory().size()).isEqualTo(1);

		PartialUpdate update = new PartialUpdate("id", DEFAULT_BEAN_ID);
		update.setValueOfField("popularity", 500);
		update.setValueOfField("name", null);
		update.setValueOfField("cat", Arrays.asList("cat-1", "cat-2", "cat-3"));

		solrTemplate.saveBean(COLLECTION_NAME, update);
		solrTemplate.commit(COLLECTION_NAME);

		loaded = solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY, ExampleSolrBean.class);
		assertThat(loaded.get().getPopularity()).isEqualTo(Integer.valueOf(500));
		assertThat(loaded.get().getName()).isNull();
		assertThat(loaded.get().getCategory().size()).isEqualTo(3);
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
		assertThat(loaded.get().getPopularity()).isEqualTo(Integer.valueOf(500));
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
	public void testPing() {
		solrTemplate.ping();
	}

	@Test
	public void testRollback() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		solrTemplate.saveBean(COLLECTION_NAME, toInsert);
		Optional<ExampleSolrBean> recalled = solrTemplate.queryForObject(COLLECTION_NAME,
				new SimpleQuery(new Criteria("id").is("1")), ExampleSolrBean.class);
		assertThat(recalled.isPresent()).isFalse();

		solrTemplate.rollback(COLLECTION_NAME);
		recalled = solrTemplate.queryForObject(COLLECTION_NAME, new SimpleQuery(new Criteria("id").is("1")),
				ExampleSolrBean.class);
		assertThat(recalled.isPresent()).isFalse();
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
			assertThat(facetResultPage.getNumberOfElements()).isEqualTo(5);
		}

		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("name"));
		for (FacetFieldEntry entry : facetPage) {
			assertThat(entry.getValue()).isNotNull();
			assertThat(entry.getField().getName()).isEqualTo("name");
			assertThat(entry.getValueCount()).isEqualTo(1l);
		}

		facetPage = page.getFacetResultPage(new SimpleField("id"));
		for (FacetFieldEntry entry : facetPage) {
			assertThat(entry.getValue()).isNotNull();
			assertThat(entry.getField().getName()).isEqualTo("id");
			assertThat(entry.getValueCount()).isEqualTo(1l);
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
								.setPageable(PageRequest.of(1, 10)));

		org.springframework.data.solr.core.query.result.FacetQueryResult<ExampleSolrBean> page = solrTemplate
				.queryForFacetPage(COLLECTION_NAME, q, ExampleSolrBean.class);

		assertThat(lastModifiedField.<Object> getQueryParameterValue(FACET_RANGE_INCLUDE))
				.isEqualTo(FacetRangeInclude.LOWER);

		for (Page<FacetFieldEntry> facetResultPage : page.getFacetResultPages()) {
			assertThat(facetResultPage.getNumberOfElements()).isEqualTo(5);
		}

		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("last_modified"));
		for (FacetFieldEntry entry : facetPage) {
			assertThat(entry.getValue()).isNotNull();
			assertThat(entry.getField().getName()).isEqualTo("last_modified");
			assertThat(entry.getValueCount()).isEqualTo(2l);
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
			assertThat(facetResultPage.getNumberOfElements()).isEqualTo(4);
		}

		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("popularity"));
		for (FacetFieldEntry entry : facetPage) {
			assertThat(entry.getValue()).isNotNull();
			assertThat(entry.getField().getName()).isEqualTo("popularity");
			assertThat(entry.getValueCount()).isEqualTo(2l);
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
		assertThat(pivotEntries).isNotNull();
		assertThat(pivotEntries.size()).isEqualTo(10);

		for (FacetPivotFieldEntry entry1 : pivotEntries) {
			assertThat(entry1.getValue()).isNotNull();
			assertThat(entry1.getField().getName()).isEqualTo("cat");
			assertThat(entry1.getValueCount()).isEqualTo(1l);
			for (FacetPivotFieldEntry entry2 : entry1.getPivot()) {
				assertThat(entry2.getValue()).isNotNull();
				assertThat(entry2.getField().getName()).isEqualTo("name");
				assertThat(entry2.getValueCount()).isEqualTo(1l);
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
		assertThat(facetQueryResultPage.getContent().size()).isEqualTo(2);
		assertThat(facetQueryResultPage.getContent().get(0).getValue()).isEqualTo("inStock:true");
		assertThat(facetQueryResultPage.getContent().get(0).getValueCount()).isEqualTo(5);

		assertThat(facetQueryResultPage.getContent().get(1).getValue()).isEqualTo("inStock:false");
		assertThat(facetQueryResultPage.getContent().get(1).getValueCount()).isEqualTo(5);

		assertThat(page.getAllFacets().size()).isEqualTo(1);
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
			assertThat(entry.getValue()).isEqualTo("spring");
			assertThat(entry.getField().getName()).isEqualTo("name");
			assertThat(entry.getValueCount()).isEqualTo(2l);
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
			assertThat(entry.getValue()).isEqualTo("spring");
			assertThat(entry.getField().getName()).isEqualTo("name");
			assertThat(entry.getValueCount()).isEqualTo(2l);
		}

		facetPage = page.getFacetResultPage(new SimpleField("cat"));
		for (FacetFieldEntry entry : facetPage) {
			assertThat(entry.getValue()).isEqualTo("language");
			assertThat(entry.getField().getName()).isEqualTo("cat");
			assertThat(entry.getValueCount()).isEqualTo(1l);
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
			assertThat(facetResultPage.getNumberOfElements()).isEqualTo(5);
		}

		Page<FacetFieldEntry> facetPage = page.getFacetResultPage(new SimpleField("name"));
		for (FacetFieldEntry entry : facetPage) {
			assertThat(entry.getValue()).isNotNull();
			assertThat(entry.getField().getName()).isEqualTo("name");
			assertThat(entry.getValueCount()).isEqualTo(1l);
		}

		facetPage = page.getFacetResultPage(new SimpleField("id"));
		for (FacetFieldEntry entry : facetPage) {
			assertThat(entry.getValue()).isNotNull();
			assertThat(entry.getField().getName()).isEqualTo("id");
			assertThat(entry.getValueCount()).isEqualTo(1l);
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

		Query query = new SimpleQuery(new SimpleStringCriteria("*:*")).addSort(Sort.by(Sort.Direction.DESC, "name"));
		Page<ExampleSolrBean> page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);

		ExampleSolrBean prev = page.getContent().get(0);
		for (int i = 1; i < page.getContent().size(); i++) {
			ExampleSolrBean cur = page.getContent().get(i);
			assertThat(Long.valueOf(cur.getId()) < Long.valueOf(prev.getId())).isTrue();
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

		Query query = new SimpleQuery(new SimpleStringCriteria("*:*")).addSort(Sort.by(Sort.Direction.DESC, "inStock"))
				.addSort(Sort.by(Sort.Direction.ASC, "name"));
		Page<ExampleSolrBean> page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);

		ExampleSolrBean prev = page.getContent().get(0);
		for (int i = 1; i < 5; i++) {
			ExampleSolrBean cur = page.getContent().get(i);
			assertThat(cur.isInStock()).isTrue();
			assertThat(Long.valueOf(cur.getId()) > Long.valueOf(prev.getId())).isTrue();
			prev = cur;
		}

		prev = page.getContent().get(5);
		for (int i = 6; i < page.getContent().size(); i++) {
			ExampleSolrBean cur = page.getContent().get(i);
			assertThat(cur.isInStock()).isFalse();
			assertThat(Long.valueOf(cur.getId()) > Long.valueOf(prev.getId())).isTrue();
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
		assertThat(page.getContent().size()).isEqualTo(0);

		query.setDefaultOperator(Operator.OR);
		page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);
		assertThat(page.getContent().size()).isEqualTo(10);
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
		assertThat(page.getContent().size()).isEqualTo(3);
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
		assertThat(page.getContent().size()).isEqualTo(2);

		query = new SimpleQuery(new Criteria("id").in("rh-1", "rh-2"));
		query.setRequestHandler("/instock");
		page = solrTemplate.queryForPage(COLLECTION_NAME, query, ExampleSolrBean.class);
		assertThat(page.getContent().size()).isEqualTo(1);
		assertThat(page.getContent().get(0).getId()).isEqualTo("rh-2");
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
		assertThat(page.getContent().size()).isEqualTo(1);
		assertThat(page.getContent().get(0).getId()).isEqualTo(belkin.getId());
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
		assertThat(page.getContent().size()).isEqualTo(1);
		assertThat(page.getContent().get(0).getId()).isEqualTo(belkin.getId());
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
		assertThat(page.getHighlighted().size()).isEqualTo(2);

		assertThat(page.getHighlighted().get(0).getHighlights().get(0).getField().getName()).isEqualTo("name");
		assertThat(page.getHighlighted().get(0).getHighlights().get(0).getSnipplets().get(0))
				.isEqualTo("Test <em>with</em> some GB18030TEST");
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
		assertThat(values.get(0).getValue()).isEqualTo("three");
		assertThat(values.get(0).getValueCount()).isEqualTo(3);

		assertThat(values.get(1).getValue()).isEqualTo("two");
		assertThat(values.get(1).getValueCount()).isEqualTo(2);

		assertThat(values.get(2).getValue()).isEqualTo("one");
		assertThat(values.get(2).getValueCount()).isEqualTo(1);
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
		assertThat(result.getContent().get(0).getId()).isEqualTo(bean1.getId());
	}

	@Test
	public void testFuctionQueryReturnsProperResult() {
		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2));
		solrTemplate.commit(COLLECTION_NAME);

		Query q = new SimpleQuery(new Criteria(QueryFunction.query("{!query v='two'}")));

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME, q, ExampleSolrBean.class);
		assertThat(result.getContent().get(0).getId()).isEqualTo(bean2.getId());
	}

	@Test
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
			assertThat(bean.getDistance()).isNotNull();
		}
	}

	@Test // DATASOLR-511
	public void testFunctionQueryInFieldProjectionWhenUsingQuery() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		bean1.setStore("45.17614,-93.87341");
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "one two", null);
		bean2.setStore("40.7143,-74.006");

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2));
		solrTemplate.commit(COLLECTION_NAME);

		Query q = Query.query(Criteria.where(GeoDistanceFunction.distanceFrom("store").to(new Point(45.15, -93.85)))) //
				.projectAllFields() //
				.addProjectionOnField(distance("distance"));

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME, q, ExampleSolrBean.class);
		for (ExampleSolrBean bean : result) {
			assertThat(bean.getDistance()).isNotNull();
		}
	}

	@Test // DATASOLR-511
	public void testFunctionQueryInFieldProjectionWhenUsingFilterQuery() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		bean1.setStore("45.17614,-93.87341");
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "one two", null);
		bean2.setStore("40.7143,-74.006");

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2));
		solrTemplate.commit(COLLECTION_NAME);

		Query q = Query.all() //
				.addFilterQuery(FilterQuery.geoFilter("store", new Point(45.15, -93.85))) //
				.projectAllFields() //
				.addProjectionOnField(distance("distance")); //

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME, q, ExampleSolrBean.class);
		for (ExampleSolrBean bean : result) {
			assertThat(bean.getDistance()).isNotNull();
		}
	}

	@Test // DATASOLR-511
	public void testGeoFunctionQueryWithSort() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		bean1.setStore("45.17614,-93.87341");
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "one two", null);
		bean2.setStore("40.7143,-74.006");

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2));
		solrTemplate.commit(COLLECTION_NAME);

		Query q = Query.all() //
				.addFilterQuery(FilterQuery.geoFilter("store", new Point(45.15, -93.85))) //
				.addSort(Sort.by(Direction.DESC, "geodist()"));

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME, q, ExampleSolrBean.class);
		assertThat(result.getContent().get(0).getId()).isEqualTo(bean2.getId());
		assertThat(result.getContent().get(1).getId()).isEqualTo(bean1.getId());
	}

	@Test // DATASOLR-162
	public void testDelegatingCursorLoadsAllElements() throws IOException {

		solrTemplate.saveBeans(COLLECTION_NAME, createBeansWithId(100));
		solrTemplate.commit(COLLECTION_NAME);

		Cursor<ExampleSolrBean> cursor = solrTemplate.queryForCursor(COLLECTION_NAME,
				new SimpleQuery("*:*").addSort(Sort.by(Direction.DESC, "id")), ExampleSolrBean.class);

		int i = 0;
		while (cursor.hasNext()) {
			cursor.next();
			i++;
		}
		cursor.close();

		assertThat(i).isEqualTo(100);
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
		groupOptions.addSort(Sort.by("name", "id"));
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
		assertThat(nameGroupEntries.getTotalElements()).isEqualTo(3);
		List<GroupEntry<ExampleSolrBean>> nameGroupEntriesContent = nameGroupEntries.getContent();
		assertGroupEntry(nameGroupEntriesContent.get(0), 2, "name1", 2, "id_1", "id_2");
		assertGroupEntry(nameGroupEntriesContent.get(1), 1, "name2", 1, "id_3");
		assertGroupEntry(nameGroupEntriesContent.get(2), 1, "name3", 1, "id_4");

		// asserts function group
		Page<GroupEntry<ExampleSolrBean>> functionGroupEntries = groupResultFunction.getGroupEntries();
		assertThat(functionGroupEntries.getNumberOfElements()).isEqualTo(2);
		List<GroupEntry<ExampleSolrBean>> functionGroupEntriesContent = functionGroupEntries.getContent();
		assertGroupEntry(functionGroupEntriesContent.get(0), 2, "1.0", 2, "id_1", "id_3");
		assertGroupEntry(functionGroupEntriesContent.get(1), 2, "2.0", 2, "id_2", "id_4");

		// asserts first query group
		Page<GroupEntry<ExampleSolrBean>> query1GroupEntries = groupResultQuery1.getGroupEntries();
		assertThat(query1GroupEntries.getNumberOfElements()).isEqualTo(1);
		GroupEntry<ExampleSolrBean> query1GroupEntry = query1GroupEntries.getContent().get(0);
		assertGroupEntry(query1GroupEntry, 3, "cat:category2", 2, "id_2", "id_3");
		assertThat(query1GroupEntry.getResult().hasNext()).isTrue();

		// asserts second query group
		Page<GroupEntry<ExampleSolrBean>> query2GroupEntries = groupResultQuery2.getGroupEntries();
		assertThat(query2GroupEntries.getNumberOfElements()).isEqualTo(1);
		GroupEntry<ExampleSolrBean> query2GroupEntry = query2GroupEntries.getContent().get(0);
		assertGroupEntry(query2GroupEntry, 1, "cat:category1", 1, "id_1");
		assertThat(query2GroupEntry.getResult().hasNext()).isFalse();
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

		assertThat(facetContent.get(0).getValue()).isEqualTo("true");
		assertThat(facetContent.get(1).getValue()).isEqualTo("false");
		assertThat(facetContent.get(0).getValueCount()).isEqualTo(2);
		assertThat(facetContent.get(1).getValueCount()).isEqualTo(1);
	}

	private void assertGroupEntryContentIds(GroupEntry<ExampleSolrBean> groupEntry, String... ids) {
		for (int i = 0; i < ids.length; i++) {
			assertThat(groupEntry.getResult().getContent().get(i).getId()).isEqualTo(ids[i]);
		}
	}

	private void assertGroupEntry(GroupEntry<ExampleSolrBean> entry, long totalElements, String groupValue,
			int numberOfDocuments, String... ids) {
		assertThat(entry.getResult().getTotalElements()).isEqualTo(totalElements);
		assertThat(entry.getGroupValue()).isEqualTo(groupValue);
		assertThat(entry.getResult().getContent().size()).isEqualTo(numberOfDocuments);
		assertGroupEntryContentIds(entry, ids);
	}

	@Test // DATASOLR-83
	public void testGetById() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2));

		Optional<ExampleSolrBean> beanReturned = solrTemplate.getById(COLLECTION_NAME, "id-1", ExampleSolrBean.class);

		assertThat(beanReturned.get().getId()).isEqualTo(bean1.getId());
	}

	@Test // DATASOLR-83
	public void testGetByIds() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "one", null);
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "two", null);
		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2));

		List<String> ids = Arrays.<String> asList("id-1", "id-2");
		Collection<ExampleSolrBean> beansReturned = solrTemplate.getByIds(COLLECTION_NAME, ids, ExampleSolrBean.class);
		List<ExampleSolrBean> listBeansReturned = new ArrayList<>(beansReturned);

		assertThat(beansReturned.size()).isEqualTo(2);
		assertThat(listBeansReturned.get(0).getId()).isEqualTo(bean1.getId());
		assertThat(listBeansReturned.get(1).getId()).isEqualTo(bean2.getId());
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

		assertThat(priceStatResult.getDistinctCount()).isEqualTo(Long.valueOf(2));
		Collection<Object> distinctValues = priceStatResult.getDistinctValues();
		assertThat(distinctValues.size()).isEqualTo(2);
		assertThat(distinctValues.contains(10.0F)).isTrue();
		assertThat(distinctValues.contains(20.0F)).isTrue();
		assertThat(popularityStatResult.getDistinctCount()).isEqualTo(null);
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

		assertThat(priceRangeResult.getTotalElements()).isEqualTo(3);
		assertThat(content.get(2).getValueCount()).isEqualTo(2);
		assertThat(content.get(1).getValueCount()).isEqualTo(1);

		assertThat(content.get(0).getValue()).isEqualTo("5.0");
		assertThat(content.get(1).getValue()).isEqualTo("10.0");
		assertThat(content.get(2).getValue()).isEqualTo("15.0");
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
		assertThat(document.get().title).isEqualTo("title");
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

		assertThatExceptionOfType(MappingException.class)
				.isThrownBy(() -> solrTemplate.queryForObject(COLLECTION_NAME, new SimpleQuery("id:id-1"), SomeDoc.class))
				.withMessageContaining("title-1").withMessageContaining("title-2");
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
		assertThat(document.get().title).isNull();
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
		assertThat(found.hasContent()).isEqualTo(false);
		assertThat(found.getSuggestions().size()).isGreaterThan(0);
		assertThat(found.getSuggestions()).containsExactly("green");
	}

	@Test // DATSOLR-364
	public void shouldUseBaseUrlInCollectionCallbackWhenExecutingCommands() {

		final HttpSolrClient client = new HttpSolrClient.Builder().withBaseSolrUrl("http://127.0.0.1/solr/").build();

		SolrTemplate solrTemplate = new SolrTemplate(new HttpSolrClientFactory(client));

		solrTemplate.execute(solrClient -> {

			assertThat(((HttpSolrClient) solrClient).getBaseURL()).isEqualTo("http://127.0.0.1/solr");
			return null;
		});
	}

	@Test // DATASOLR-466
	public void countShouldUseMappedFieldName() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "name1", null);
		bean1.setManufacturerId("man-1");
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "name2", null);
		bean2.setManufacturerId("man-2");
		ExampleSolrBean bean3 = new ExampleSolrBean("id-3", "name3", null);
		bean3.setManufacturerId("man-1");

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2, bean3));
		solrTemplate.commit(COLLECTION_NAME);

		assertThat(solrTemplate.count(COLLECTION_NAME, new SimpleQuery(Criteria.where("manufacturerId").is("man-1")),
				ExampleSolrBean.class)).isEqualTo(2L);
	}

	@Test // DATASOLR-466
	public void deleteShouldUseMappedFieldName() {

		ExampleSolrBean bean1 = new ExampleSolrBean("id-1", "name1", null);
		bean1.setManufacturerId("man-1");
		ExampleSolrBean bean2 = new ExampleSolrBean("id-2", "name2", null);
		bean2.setManufacturerId("man-2");
		ExampleSolrBean bean3 = new ExampleSolrBean("id-3", "name3", null);
		bean3.setManufacturerId("man-1");

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(bean1, bean2, bean3));
		solrTemplate.commit(COLLECTION_NAME);

		solrTemplate.delete(COLLECTION_NAME, new SimpleQuery(Criteria.where("manufacturerId").is("man-1")),
				ExampleSolrBean.class);
		solrTemplate.commit(COLLECTION_NAME);

		assertThat(solrTemplate.count(COLLECTION_NAME, new SimpleQuery(AnyCriteria.any()))).isEqualTo(1L);
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
		assertThat(priceStats.getCount()).isEqualTo(Long.valueOf(2));
		assertThat(priceStats.getMin()).isEqualTo(10D);
		assertThat(priceStats.getMax()).isEqualTo(20.50);
		assertThat(priceStats.getMinAsDouble()).isEqualTo(Double.valueOf(10));
		assertThat(priceStats.getMaxAsDouble()).isEqualTo(Double.valueOf(20.50));
		assertThat(priceStats.getMinAsString()).isEqualTo("10.0");
		assertThat(priceStats.getMaxAsString()).isEqualTo("20.5");
		assertThat(priceStats.getMinAsDate()).isNull();
		assertThat(priceStats.getMaxAsDate()).isNull();
		assertThat(priceStats.getMean()).isEqualTo(15.25);
		assertThat(priceStats.getSum()).isEqualTo(30.50);
		assertThat(priceStats.getMissing()).isEqualTo(Long.valueOf(0));
		assertThat(priceStats.getStddev()).isEqualTo(Double.valueOf(7.424621202458749));
		assertThat(priceStats.getSumOfSquares()).isEqualTo(Double.valueOf(520.25));

		Map<String, StatsResult> facetStatsResult = priceStats.getFacetStatsResult(new SimpleField("name"));
		assertThat(facetStatsResult.size()).isEqualTo(2);
		{
			StatsResult nameFacetStatsResult = facetStatsResult.get("one");
			assertThat(nameFacetStatsResult.getCount()).isEqualTo(Long.valueOf(1));
			assertThat(nameFacetStatsResult.getMin()).isEqualTo(10D);
			assertThat(nameFacetStatsResult.getMax()).isEqualTo(10D);
		}
		{
			StatsResult nameFacetStatsResult = facetStatsResult.get("two");
			assertThat(nameFacetStatsResult.getCount()).isEqualTo(Long.valueOf(1));
			assertThat(nameFacetStatsResult.getMin()).isEqualTo(20.5D);
			assertThat(nameFacetStatsResult.getMax()).isEqualTo(20.5D);
		}
	}

	@Data
	static class SomeDoc {

		@Id String id;

		@Field String title;
	}

}
