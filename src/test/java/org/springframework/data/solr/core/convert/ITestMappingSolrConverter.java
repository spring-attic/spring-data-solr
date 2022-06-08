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
package org.springframework.data.solr.core.convert;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Offset.offset;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.geo.Point;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.mapping.ChildDocument;
import org.springframework.data.solr.core.mapping.Dynamic;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.Score;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.result.ScoredPage;

/**
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public class ITestMappingSolrConverter extends AbstractITestWithEmbeddedSolrServer {

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

	@Test // DATASOLR-142
	public void convertsPointCorrectly() {
		BeanWithPoint bean = new BeanWithPoint();
		bean.id = DEFAULT_BEAN_ID;
		bean.location = new Point(48.30975D, 14.28435D);

		BeanWithPoint loaded = saveAndLoad(bean);

		assertThat(loaded.location.getX()).isCloseTo(bean.location.getX(), offset(0.0D));
		assertThat(loaded.location.getY()).isCloseTo(bean.location.getY(), offset(0.0D));
	}

	@Test
	public void convertsJodaDateTimeCorrectly() {
		BeanWithJodaDateTime bean = new BeanWithJodaDateTime();
		bean.id = DEFAULT_BEAN_ID;
		bean.manufactured = new DateTime(2013, 6, 18, 6, 0, 0);

		BeanWithJodaDateTime loaded = saveAndLoad(bean);

		assertThat(loaded.manufactured).isEqualTo(bean.manufactured);
	}

	@Test
	public void convertsJodaLcoalDateTimeCorrectly() {
		BeanWithJodaLocalDateTime bean = new BeanWithJodaLocalDateTime();
		bean.id = DEFAULT_BEAN_ID;
		bean.manufactured = new LocalDateTime(2013, 6, 18, 6, 0, 0);

		BeanWithJodaLocalDateTime loaded = saveAndLoad(bean);

		assertThat(loaded.manufactured).isEqualTo(bean.manufactured);
	}

	@Test
	public void testProcessesListCorrectly() {
		BeanWithList bean = new BeanWithList();
		bean.id = DEFAULT_BEAN_ID;
		bean.categories = Arrays.asList("spring", "data", "solr");

		BeanWithList loaded = saveAndLoad(bean);

		assertThat(loaded.categories).isEqualTo(bean.categories);
	}

	@Test
	public void testProcessesInheritanceCorrectly() {
		BeanWithBaseClass bean = new BeanWithBaseClass();
		bean.id = DEFAULT_BEAN_ID;
		bean.name = "christoph strobl";

		BeanWithBaseClass loaded = saveAndLoad(bean);

		assertThat(loaded.id).isEqualTo(bean.id);
		assertThat(loaded.name).isEqualTo(bean.name);
	}

	@Test
	public void testProcessesEnumCorrectly() {
		BeanWithEnum bean = new BeanWithEnum();
		bean.id = DEFAULT_BEAN_ID;
		bean.enumProperty = LiteralNumberEnum.TWO;

		BeanWithEnum loaded = saveAndLoad(bean);

		assertThat(loaded.id).isEqualTo(bean.id);
		assertThat(loaded.enumProperty).isEqualTo(bean.enumProperty);

		Query query = new SimpleQuery(new Criteria("enumProperty_s").is(LiteralNumberEnum.TWO));

		BeanWithEnum loadedViaProperty = solrTemplate.queryForObject(COLLECTION_NAME, query, BeanWithEnum.class).get();
		assertThat(loadedViaProperty.id).isEqualTo(bean.id);
		assertThat(loadedViaProperty.enumProperty).isEqualTo(bean.enumProperty);
	}

	@Test // DATASOLR-210, DATASOLR-309
	public void testProcessesScoreCorrectly() {

		Collection<BeanWithScore> beans = new ArrayList<>();
		beans.add(new BeanWithScore("1", "spring"));
		beans.add(new BeanWithScore("2", "spring data solr"));
		beans.add(new BeanWithScore("3", "apache solr"));
		beans.add(new BeanWithScore("4", "apache lucene"));

		solrTemplate.saveBeans(COLLECTION_NAME, beans);
		solrTemplate.commit(COLLECTION_NAME);

		ScoredPage<BeanWithScore> page = solrTemplate.queryForPage(COLLECTION_NAME,
				new SimpleQuery("description:spring solr"), BeanWithScore.class);

		List<BeanWithScore> content = page.getContent();
		assertThat(page.getTotalElements()).isEqualTo(3);

		assertThat(content.get(0).score).isNotNull();
		assertThat(content.get(0).description).isEqualTo("spring data solr");
		assertThat(content.get(1).score).isNotNull();
		assertThat(content.get(1).description).isEqualTo("spring");
		assertThat(content.get(2).score).isNotNull();
		assertThat(content.get(2).description).isEqualTo("apache solr");
	}

	@Test // DATASOLR-471
	public void testProcessesDeprecatedScoreCorrectly() {

		Collection<BeanWithDeprecatedScore> beans = new ArrayList<>();
		beans.add(new BeanWithDeprecatedScore("1", "spring"));
		beans.add(new BeanWithDeprecatedScore("2", "spring data solr"));
		beans.add(new BeanWithDeprecatedScore("3", "apache solr"));
		beans.add(new BeanWithDeprecatedScore("4", "apache lucene"));

		solrTemplate.saveBeans(COLLECTION_NAME, beans);
		solrTemplate.commit(COLLECTION_NAME);

		ScoredPage<BeanWithDeprecatedScore> page = solrTemplate.queryForPage(COLLECTION_NAME,
				new SimpleQuery("description:spring solr"), BeanWithDeprecatedScore.class);

		List<BeanWithDeprecatedScore> content = page.getContent();
		assertThat(page.getTotalElements()).isEqualTo(3);

		assertThat(content.get(0).score).isNotNull();
		assertThat(content.get(0).description).isEqualTo("spring data solr");
		assertThat(content.get(1).score).isNotNull();
		assertThat(content.get(1).description).isEqualTo("spring");
		assertThat(content.get(2).score).isNotNull();
		assertThat(content.get(2).description).isEqualTo("apache solr");
	}

	@Test // DATASOLR-202
	public void testDynamicMap() {

		Map<String, String> map = new HashMap<>();
		map.put("key_1", "value 1");
		map.put("key_2", "value 2");
		BeanWithDynamicMap bean = new BeanWithDynamicMap("bean-id", map);

		solrTemplate.saveBean(COLLECTION_NAME, bean);
		solrTemplate.commit(COLLECTION_NAME);

		BeanWithDynamicMap loaded = solrTemplate.getById(COLLECTION_NAME, "bean-id", BeanWithDynamicMap.class).get();
		assertThat(loaded.values.get("key_1")).isEqualTo("value 1");
		assertThat(loaded.values.get("key_2")).isEqualTo("value 2");

	}

	@Test // DATASOLR-308
	public void testDynamicMapList() {

		Map<String, List<String>> map = new HashMap<>();
		map.put("key_1", Arrays.asList("value 11", "value 12"));
		map.put("key_2", Arrays.asList("value 21", "value 22"));
		BeanWithDynamicMapList bean = new BeanWithDynamicMapList("bean-id", map);

		solrTemplate.saveBean(COLLECTION_NAME, bean);
		solrTemplate.commit(COLLECTION_NAME);

		BeanWithDynamicMapList loaded = solrTemplate.getById(COLLECTION_NAME, "bean-id", BeanWithDynamicMapList.class)
				.get();
		assertThat(loaded.values.get("key_1")).isEqualTo(Arrays.asList("value 11", "value 12"));
		assertThat(loaded.values.get("key_2")).isEqualTo(Arrays.asList("value 21", "value 22"));

	}

	@Test // DATASOLR-394
	public void writeAndReadDocumentWithNestedChildObjectsCorrectly() throws IOException, SolrServerException {

		Book theWayOfKings = new Book();
		theWayOfKings.id = "book1";
		theWayOfKings.type = "book";
		theWayOfKings.title = "The Way of Kings";
		theWayOfKings.author = "Brandon Sanderson";
		theWayOfKings.category = "fantasy";
		theWayOfKings.publicationYear = 2010;
		theWayOfKings.publisher = "Tor";
		theWayOfKings.reviews = new ArrayList<>();

		BookReview review1 = new BookReview();
		review1.id = "review1";
		review1.type = "review";
		review1.author = "yonik";
		review1.date = new Date();
		review1.stars = 5;
		review1.comment = "A great start to what looks like an epic series";
		theWayOfKings.reviews.add(review1);

		BookReview review2 = new BookReview();
		review2.id = "review2";
		review2.type = "review";
		review2.author = "dan";
		review2.date = new Date();
		review2.stars = 3;
		review2.comment = "This book was too long.";
		theWayOfKings.reviews.add(review2);

		solrTemplate.saveBean(COLLECTION_NAME, theWayOfKings);
		solrTemplate.commit(COLLECTION_NAME);

		assertThat(solrTemplate.getSolrClient().query(new SolrQuery("*:*")).getResults()).hasSize(3);

		Query query = new SimpleQuery();
		query.addCriteria(Criteria.where("id").is(theWayOfKings.id));
		query.addProjectionOnField(new SimpleField("*"));
		query.addProjectionOnField(new SimpleField("[child parentFilter=type_s:book]"));

		Optional<Book> result = solrTemplate.queryForObject(COLLECTION_NAME, query, Book.class);
		assertThat(result.orElse(null)).isEqualTo(theWayOfKings);
	}

	@SuppressWarnings("unchecked")
	private <T> T saveAndLoad(T o) {
		solrTemplate.saveBean(COLLECTION_NAME, o);
		solrTemplate.commit(COLLECTION_NAME);

		return (T) solrTemplate.queryForObject(COLLECTION_NAME, DEFAULT_BEAN_OBJECT_QUERY, o.getClass()).orElse(null);
	}

	private static class BeanWithPoint {

		@Id //
		@Field private String id;

		@Field("store") //
		private Point location;

	}

	private static class BeanWithJodaDateTime {

		@Id //
		@Field //
		private String id;

		@Field("manufacturedate_dt") //
		private DateTime manufactured;

	}

	private static class BeanWithJodaLocalDateTime {

		@Id //
		@Field //
		private String id;

		@Field("manufacturedate_dt") //
		private LocalDateTime manufactured;

	}

	private static class BeanWithList {

		@Id //
		@Field //
		private String id;

		@Field("cat") //
		private List<String> categories;

	}

	private static class BeanBaseClass {

		@Id @Field //
		protected String id;

	}

	private static class BeanWithBaseClass extends BeanBaseClass {

		@Field("name") //
		private String name;

	}

	private enum LiteralNumberEnum {
		ONE, TWO, THREE
	}

	private static class BeanWithEnum {

		@Id @Field //
		private String id;

		@Field("enumProperty_s") //
		private LiteralNumberEnum enumProperty;

	}

	private static class BeanWithScore {

		@Id @Field //
		private String id;

		@Indexed(type = "text") //
		private String description;

		@Score //
		private Float score;

		public BeanWithScore(String id, String description) {
			this.id = id;
			this.description = description;
		}
	}

	@RequiredArgsConstructor
	private static class BeanWithDeprecatedScore {

		@Id @Field //
		private final String id;

		@Indexed(type = "text") //
		private final String description;

		@org.springframework.data.solr.repository.Score //
		private Float score;

	}

	private static class BeanWithDynamicMap {

		@Id @Field private String id;

		@Dynamic @Field("*_s") private Map<String, String> values;

		public BeanWithDynamicMap(String id, Map<String, String> values) {
			this.id = id;
			this.values = values;
		}

	}

	private static class BeanWithDynamicMapList {

		@Id @Field private String id;

		@Dynamic @Field("*_ss") private Map<String, List<String>> values;

		public BeanWithDynamicMapList(String id, Map<String, List<String>> values) {
			this.id = id;
			this.values = values;
		}

	}

	@Data
	static class Book {

		@Id String id;
		@Indexed("type_s") String type;
		@Indexed("title_t") String title;
		@Indexed("author_s") String author;
		@Indexed("cat_s") String category;
		@Indexed("pubyear_i") int publicationYear;
		@Indexed("publisher_s") String publisher;

		@ChildDocument List<BookReview> reviews;
	}

	@Data
	static class BookReview {

		String id;
		@Indexed("type_s") String type;
		@Indexed("review_dt") Date date;
		@Indexed("stars_i") int stars;
		@Indexed("author_s") String author;
		@Indexed("comment_t") String comment;
	}

}
