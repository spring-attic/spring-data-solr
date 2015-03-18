/*
 * Copyright 2012 - 2015 the original author or authors.
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

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.beans.Field;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.geo.Point;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.result.ScoredPage;
import org.springframework.data.solr.repository.Score;
import org.xml.sax.SAXException;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public class ITestMappingSolrConverter extends AbstractITestWithEmbeddedSolrServer {

	private static final Query DEFAULT_BEAN_OBJECT_QUERY = new SimpleQuery(new Criteria("id").is(DEFAULT_BEAN_ID));
	private static final Query ALL_DOCUMENTS_QUERY = new SimpleQuery(new SimpleStringCriteria("*:*"));

	private SolrTemplate solrTemplate;

	@Before
	public void setUp() throws IOException, ParserConfigurationException, SAXException {
		solrTemplate = new SolrTemplate(solrClient, null);
		solrTemplate.afterPropertiesSet();
	}

	@After
	public void tearDown() {
		solrTemplate.delete(ALL_DOCUMENTS_QUERY);
		solrTemplate.commit();
	}

	/**
	 * @see DATASOLR-142
	 */
	@Test
	public void convertsPointCorrectly() {
		BeanWithPoint bean = new BeanWithPoint();
		bean.id = DEFAULT_BEAN_ID;
		bean.location = new Point(48.30975D, 14.28435D);

		BeanWithPoint loaded = saveAndLoad(bean);

		assertEquals(bean.location.getX(), loaded.location.getX(), 0.0F);
		assertEquals(bean.location.getY(), loaded.location.getY(), 0.0F);
	}

	@Test
	public void convertsJodaDateTimeCorrectly() {
		BeanWithJodaDateTime bean = new BeanWithJodaDateTime();
		bean.id = DEFAULT_BEAN_ID;
		bean.manufactured = new DateTime(2013, 6, 18, 6, 0, 0);

		BeanWithJodaDateTime loaded = saveAndLoad(bean);

		assertThat(loaded.manufactured, equalTo(bean.manufactured));
	}

	@Test
	public void convertsJodaLcoalDateTimeCorrectly() {
		BeanWithJodaLocalDateTime bean = new BeanWithJodaLocalDateTime();
		bean.id = DEFAULT_BEAN_ID;
		bean.manufactured = new LocalDateTime(2013, 6, 18, 6, 0, 0);

		BeanWithJodaLocalDateTime loaded = saveAndLoad(bean);

		assertThat(loaded.manufactured, equalTo(bean.manufactured));
	}

	@Test
	public void testProcessesListCorrectly() {
		BeanWithList bean = new BeanWithList();
		bean.id = DEFAULT_BEAN_ID;
		bean.categories = Arrays.asList("spring", "data", "solr");

		BeanWithList loaded = saveAndLoad(bean);

		assertThat(loaded.categories, equalTo(bean.categories));
	}

	@Test
	public void testProcessesInheritanceCorrectly() {
		BeanWithBaseClass bean = new BeanWithBaseClass();
		bean.id = DEFAULT_BEAN_ID;
		bean.name = "christoph strobl";

		BeanWithBaseClass loaded = saveAndLoad(bean);

		assertEquals(bean.id, loaded.id);
		assertEquals(bean.name, loaded.name);
	}

	@Test
	public void testProcessesEnumCorrectly() {
		BeanWithEnum bean = new BeanWithEnum();
		bean.id = DEFAULT_BEAN_ID;
		bean.enumProperty = LiteralNumberEnum.TWO;

		BeanWithEnum loaded = saveAndLoad(bean);

		assertEquals(bean.id, loaded.id);
		assertEquals(bean.enumProperty, loaded.enumProperty);

		Query query = new SimpleQuery(new Criteria("enumProperty_s").is(LiteralNumberEnum.TWO));

		BeanWithEnum loadedViaProperty = solrTemplate.queryForObject(query, BeanWithEnum.class);
		assertEquals(bean.id, loadedViaProperty.id);
		assertEquals(bean.enumProperty, loadedViaProperty.enumProperty);
	}

	/**
	 * @see DATASOLR-210
	 */
	@Test
	public void testProcessesScoreCorrectly() {

		Collection<BeanWithScore> beans = new ArrayList<BeanWithScore>();
		beans.add(new BeanWithScore("1", "spring"));
		beans.add(new BeanWithScore("2", "spring data solr"));
		beans.add(new BeanWithScore("3", "apache solr"));
		beans.add(new BeanWithScore("4", "apache lucene"));

		solrTemplate.saveBeans(beans);
		solrTemplate.commit();

		ScoredPage<BeanWithScore> page = solrTemplate.queryForPage(new SimpleQuery("description:spring solr"),
				BeanWithScore.class);

		List<BeanWithScore> content = page.getContent();
		assertEquals(3, page.getTotalElements());
		assertEquals(Float.valueOf(0.9105287f), content.get(0).score);
		assertEquals("spring data solr", content.get(0).description);
		assertEquals(Float.valueOf(0.45526436f), content.get(1).score);
		assertEquals("spring", content.get(1).description);
		assertEquals(Float.valueOf(0.28454024f), content.get(2).score);
		assertEquals("apache solr", content.get(2).description);
	}

	/**
	 * @see DATASOLR-202
	 */
	@Test
	public void testDynamicMap() {

		Map<String, String> map = new HashMap<String, String>();
		map.put("key_1", "value 1");
		map.put("key_2", "value 2");
		BeanWithDynamicMap bean = new BeanWithDynamicMap("bean-id", map);

		solrTemplate.saveBean(bean);
		solrTemplate.commit();

		BeanWithDynamicMap loaded = solrTemplate.getById("bean-id", BeanWithDynamicMap.class);
		Assert.assertEquals("value 1", loaded.values.get("key_1"));
		Assert.assertEquals("value 2", loaded.values.get("key_2"));

	}

	@SuppressWarnings("unchecked")
	private <T> T saveAndLoad(T o) {
		solrTemplate.saveBean(o);
		solrTemplate.commit();

		return (T) solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, o.getClass());
	}

	private static class BeanWithPoint {

		@Id//
		@Field private String id;

		@Field("store")//
		private Point location;

	}

	private static class BeanWithJodaDateTime {

		@Id//
		@Field//
		private String id;

		@Field("manufacturedate_dt")//
		private DateTime manufactured;

	}

	private static class BeanWithJodaLocalDateTime {

		@Id//
		@Field//
		private String id;

		@Field("manufacturedate_dt")//
		private LocalDateTime manufactured;

	}

	private static class BeanWithList {

		@Id//
		@Field//
		private String id;

		@Field("cat")//
		private List<String> categories;

	}

	private static class BeanBaseClass {

		@Id @Field//
		protected String id;

	}

	private static class BeanWithBaseClass extends BeanBaseClass {

		@Field("name")//
		private String name;

	}

	private enum LiteralNumberEnum {
		ONE, TWO, THREE;
	}

	private static class BeanWithEnum {

		@Id @Field//
		private String id;

		@Field("enumProperty_s")//
		private LiteralNumberEnum enumProperty;

	}

	private static class BeanWithScore {
		@Id @Field//
		private String id;

		@Indexed(type = "text")//
		private String description;

		@Score//
		private Float score;

		public BeanWithScore(String id, String description) {
			this.id = id;
			this.description = description;
		}

	}

	private static class BeanWithDynamicMap {

		@Id @Field private String id;

		@Dynamic @Field("*_s") private Map<String, String> values;

		public BeanWithDynamicMap(String id, Map<String, String> values) {
			this.id = id;
			this.values = values;
		}

	}

}
