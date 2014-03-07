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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.beans.Field;
import org.hamcrest.core.IsEqual;
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
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.xml.sax.SAXException;

/**
 * @author Christoph Strobl
 */
public class ITestMappingSolrConverter extends AbstractITestWithEmbeddedSolrServer {

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
	public void convertsGeoLocationCorrectly() {
		BeanWithGeoLocation bean = new BeanWithGeoLocation();
		bean.id = DEFAULT_BEAN_ID;
		bean.location = new GeoLocation(48.30975D, 14.28435D);

		BeanWithGeoLocation loaded = saveAndLoad(bean);

		Assert.assertEquals(bean.location.getLatitude(), loaded.location.getLatitude(), 0.0F);
		Assert.assertEquals(bean.location.getLongitude(), loaded.location.getLongitude(), 0.0F);
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

		Assert.assertEquals(bean.location.getX(), loaded.location.getX(), 0.0F);
		Assert.assertEquals(bean.location.getY(), loaded.location.getY(), 0.0F);
	}

	@Test
	public void convertsJodaDateTimeCorrectly() {
		BeanWithJodaDateTime bean = new BeanWithJodaDateTime();
		bean.id = DEFAULT_BEAN_ID;
		bean.manufactured = new DateTime(2013, 6, 18, 6, 0, 0);

		BeanWithJodaDateTime loaded = saveAndLoad(bean);

		Assert.assertThat(loaded.manufactured, IsEqual.equalTo(bean.manufactured));
	}

	@Test
	public void convertsJodaLcoalDateTimeCorrectly() {
		BeanWithJodaLocalDateTime bean = new BeanWithJodaLocalDateTime();
		bean.id = DEFAULT_BEAN_ID;
		bean.manufactured = new LocalDateTime(2013, 6, 18, 6, 0, 0);

		BeanWithJodaLocalDateTime loaded = saveAndLoad(bean);

		Assert.assertThat(loaded.manufactured, IsEqual.equalTo(bean.manufactured));
	}

	@Test
	public void testProcessesListCorrectly() {
		BeanWithList bean = new BeanWithList();
		bean.id = DEFAULT_BEAN_ID;
		bean.categories = Arrays.asList("spring", "data", "solr");

		BeanWithList loaded = saveAndLoad(bean);

		Assert.assertThat(loaded.categories, IsEqual.equalTo(bean.categories));
	}

	@Test
	public void testProcessesInheritanceCorrectly() {
		BeanWithBaseClass bean = new BeanWithBaseClass();
		bean.id = DEFAULT_BEAN_ID;
		bean.name = "christoph strobl";

		BeanWithBaseClass loaded = saveAndLoad(bean);

		Assert.assertEquals(bean.id, loaded.id);
		Assert.assertEquals(bean.name, loaded.name);
	}

	@Test
	public void testProcessesEnumCorrectly() {
		BeanWithEnum bean = new BeanWithEnum();
		bean.id = DEFAULT_BEAN_ID;
		bean.enumProperty = LiteralNumberEnum.TWO;

		BeanWithEnum loaded = saveAndLoad(bean);

		Assert.assertEquals(bean.id, loaded.id);
		Assert.assertEquals(bean.enumProperty, loaded.enumProperty);

		Query query = new SimpleQuery(new Criteria("enumProperty_s").is(LiteralNumberEnum.TWO));

		BeanWithEnum loadedViaProperty = solrTemplate.queryForObject(query, BeanWithEnum.class);
		Assert.assertEquals(bean.id, loadedViaProperty.id);
		Assert.assertEquals(bean.enumProperty, loadedViaProperty.enumProperty);
	}

	@SuppressWarnings("unchecked")
	private <T> T saveAndLoad(T o) {
		solrTemplate.saveBean(o);
		solrTemplate.commit();

		return (T) solrTemplate.queryForObject(DEFAULT_BEAN_OBJECT_QUERY, o.getClass());
	}

	private static class BeanWithGeoLocation {

		@Id//
		@Field private String id;

		@Field("store")//
		private GeoLocation location;

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

}
