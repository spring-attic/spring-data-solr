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
package org.springframework.data.solr.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.FacetParams;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;
import org.springframework.data.solr.ExampleSolrBean;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.FacetOptions.FieldWithFacetParameters;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.FacetRangeOptions;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.Join;
import org.springframework.data.solr.core.query.PartialUpdate;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.Query.Operator;
import org.springframework.data.solr.core.query.SimpleFacetQuery;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.SimpleTermsQuery;
import org.springframework.data.solr.core.query.SimpleUpdateField;
import org.springframework.data.solr.core.query.TermsQuery;
import org.springframework.data.solr.core.query.Update;
import org.springframework.data.solr.core.query.UpdateAction;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.FacetPivotFieldEntry;
import org.springframework.data.solr.core.query.result.FacetQueryEntry;
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
    public void testFacetQueryWithDateFacetRangeField() {
        List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>();
        for (int i = 0; i < 10; i++) {
            final ExampleSolrBean exampleSolrBean=createExampleBeanWithId(Integer.toString(i));
            exampleSolrBean.setLastModified(new GregorianCalendar(2013, Calendar.DECEMBER, (i + 10) / 2).getTime());
            values.add(exampleSolrBean);
        }
        solrTemplate.saveBeans(values);
        solrTemplate.commit();

        final FacetRangeOptions.FieldWithDateFacetRangeParameters lastModifiedField =
        new FacetRangeOptions.FieldWithDateFacetRangeParameters("last_modified");
        lastModifiedField.setSort(FacetOptions.FacetSort.COUNT);
        lastModifiedField.setStart(new GregorianCalendar(2013, Calendar.NOVEMBER, 30).getTime());
        lastModifiedField.setEnd(new GregorianCalendar(2014, Calendar.JANUARY, 1).getTime());
        lastModifiedField.setGap("+1DAY");

        FacetQuery q = new SimpleFacetQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
                .setFacetRangeOptions(new FacetRangeOptions().addFacetRangeOnField(lastModifiedField).setFacetLimit(5));

        FacetPage<ExampleSolrBean> page = solrTemplate.queryForFacetPage(q, ExampleSolrBean.class);

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

    @Test
    public void testFacetQueryWithNumericFacetRangeField() {
        List<ExampleSolrBean> values = new ArrayList<ExampleSolrBean>();
        for (int i = 0; i < 10; i++) {
            final ExampleSolrBean exampleSolrBean=createExampleBeanWithId(Integer.toString(i));
            exampleSolrBean.setPopularity((i+1)*100);
            values.add(exampleSolrBean);
        }
        solrTemplate.saveBeans(values);
        solrTemplate.commit();

        final FacetRangeOptions.FieldWithNumericFacetRangeParameters popularityField =
                new FacetRangeOptions.FieldWithNumericFacetRangeParameters("popularity");
        popularityField.setSort(FacetOptions.FacetSort.INDEX);
        popularityField.setStart(100);
        popularityField.setEnd(800);
        popularityField.setGap(200);
        popularityField.setOther(FacetParams.FacetRangeOther.AFTER);
        popularityField.setHardEnd(false);

        FacetQuery q = new SimpleFacetQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD))
                .setFacetRangeOptions(new FacetRangeOptions().addFacetRangeOnField(popularityField).setFacetLimit(5));

        FacetPage<ExampleSolrBean> page = solrTemplate.queryForFacetPage(q, ExampleSolrBean.class);

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

}
