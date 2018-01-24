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
package org.springframework.data.solr.core.query;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;
import org.springframework.data.solr.ExampleSolrBean;
import org.springframework.data.solr.core.SolrTemplate;
import org.xml.sax.SAXException;

/**
 * @author Christoph Strobl
 */
public class ITestCriteriaExecution extends AbstractITestWithEmbeddedSolrServer {

	private SolrTemplate solrTemplate;

	@Before
	public void setUp() throws IOException, ParserConfigurationException, SAXException {
		solrTemplate = new SolrTemplate(server);
		solrTemplate.afterPropertiesSet();
	}

	@After
	public void tearDown() {
		solrTemplate.delete(COLLECTION_NAME,
				new SimpleQuery(new Criteria(Criteria.WILDCARD).expression(Criteria.WILDCARD)));
		solrTemplate.commit(COLLECTION_NAME);
	}

	@Test
	public void testNegativeNumberCriteria() {
		ExampleSolrBean positivePopularity = createExampleBeanWithId("1");
		positivePopularity.setPopularity(100);

		ExampleSolrBean negativePopularity = createExampleBeanWithId("2");
		negativePopularity.setPopularity(-200);

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(positivePopularity, negativePopularity));
		solrTemplate.commit(COLLECTION_NAME);

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME,
				new SimpleQuery(new Criteria("popularity").is(-200)), ExampleSolrBean.class);
		assertEquals(1, result.getContent().size());
		assertEquals(negativePopularity.getId(), result.getContent().get(0).getId());
	}

	@Test
	public void testNegativeNumberInRange() {
		ExampleSolrBean negative100 = createExampleBeanWithId("1");
		negative100.setPopularity(-100);

		ExampleSolrBean negative200 = createExampleBeanWithId("2");
		negative200.setPopularity(-200);

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(negative100, negative200));
		solrTemplate.commit(COLLECTION_NAME);

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME,
				new SimpleQuery(new Criteria("popularity").between(-150, -50)), ExampleSolrBean.class);
		assertEquals(1, result.getContent().size());
		assertEquals(negative100.getId(), result.getContent().get(0).getId());
	}

	@Test
	public void testDateValue() {
		ExampleSolrBean searchableBean = createExampleBeanWithId("1");
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.set(2012, 7, 23, 6, 10, 0);
		searchableBean.setLastModified(calendar.getTime());

		solrTemplate.saveBean(COLLECTION_NAME, searchableBean);
		solrTemplate.commit(COLLECTION_NAME);

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME,
				new SimpleQuery(new Criteria("last_modified").is(calendar.getTime())), ExampleSolrBean.class);
		assertEquals(1, result.getContent().size());
	}

	@Test
	public void testDateValueInRangeQuery() {
		ExampleSolrBean searchableBeanIn2012 = createExampleBeanWithId("1");
		Calendar calendar2012 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar2012.set(2012, 7, 23, 6, 10, 0);
		searchableBeanIn2012.setLastModified(calendar2012.getTime());

		ExampleSolrBean searchableBeanIn2011 = createExampleBeanWithId("2");
		Calendar calendar2011 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar2011.set(2011, 7, 23, 6, 10, 0);
		searchableBeanIn2011.setLastModified(calendar2011.getTime());

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(searchableBeanIn2012, searchableBeanIn2011));
		solrTemplate.commit(COLLECTION_NAME);

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME,
				new SimpleQuery(new Criteria("last_modified").between(new DateTime(2012, 1, 1, 0, 0, 0, DateTimeZone.UTC),
						new DateTime(2012, 12, 31, 23, 59, 59, DateTimeZone.UTC))),
				ExampleSolrBean.class);
		assertEquals(1, result.getContent().size());

	}

	@Test
	public void testPoint() {
		ExampleSolrBean searchableBeanInBuffalow = createExampleBeanWithId("1");
		searchableBeanInBuffalow.setStore("45.17614,-93.87341");

		ExampleSolrBean searchableBeanInNYC = createExampleBeanWithId("2");
		searchableBeanInNYC.setStore("40.7143,-74.006");

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(searchableBeanInBuffalow, searchableBeanInNYC));
		solrTemplate.commit(COLLECTION_NAME);

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME,
				new SimpleQuery(new Criteria("store").near(new Point(45.15, -93.85), new Distance(5))), ExampleSolrBean.class);

		assertEquals(1, result.getContent().size());
	}

	@Test
	public void testPointWithDistanceInMiles() {
		ExampleSolrBean searchableBeanInBuffalow = createExampleBeanWithId("1");
		searchableBeanInBuffalow.setStore("45.17614,-93.87341");

		ExampleSolrBean searchableBeanInNYC = createExampleBeanWithId("2");
		searchableBeanInNYC.setStore("40.7143,-74.006");

		solrTemplate.saveBeans(COLLECTION_NAME, Arrays.asList(searchableBeanInBuffalow, searchableBeanInNYC));
		solrTemplate.commit(COLLECTION_NAME);

		Page<ExampleSolrBean> result = solrTemplate.queryForPage(COLLECTION_NAME,
				new SimpleQuery(new Criteria("store").near(new Point(45.15, -93.85), new Distance(3.106856, Metrics.MILES))),
				ExampleSolrBean.class);

		assertEquals(1, result.getContent().size());
	}
}
