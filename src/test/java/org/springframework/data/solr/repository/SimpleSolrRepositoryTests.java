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
package org.springframework.data.solr.repository;

import java.util.Arrays;

import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.ExampleSolrBean;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.repository.support.SimpleSolrRepository;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleSolrRepositoryTests {

	private SimpleSolrRepository<ExampleSolrBean, String> repository;

	@Mock private SolrOperations solrOperationsMock;

	@Before
	public void setUp() {
		repository = new SimpleSolrRepository<ExampleSolrBean, String>(solrOperationsMock, ExampleSolrBean.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInitRepositoryWithNullSolrOperations() {
		new SimpleSolrRepository<ExampleSolrBean, String>(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInitRepositoryWithNullEntityClass() {
		new SimpleSolrRepository<ExampleSolrBean, String>(new SolrTemplate(
				new HttpSolrClient("http://localhost:8080/solr"), null), null);
	}

	@Test
	public void testInitRepository() {
		repository = new SimpleSolrRepository<ExampleSolrBean, String>(new SolrTemplate(new HttpSolrClient(
				"http://localhost:8080/solr"), null), ExampleSolrBean.class);
		Assert.assertEquals(ExampleSolrBean.class, repository.getEntityClass());
	}

	@Test
	public void testFindAllByIdQuery() {
		Mockito.when(solrOperationsMock.count(Mockito.any(SolrDataQuery.class))).thenReturn(12345l);

		repository.findAll(Arrays.asList("id-1", "id-2", "id-3"));
		ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

		Mockito.verify(solrOperationsMock, Mockito.times(1)).count(captor.capture());
		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForPage(captor.capture(),
				Mockito.eq(ExampleSolrBean.class));

		Assert.assertNull(captor.getAllValues().get(0).getPageRequest());
		Assert.assertEquals(12345, captor.getAllValues().get(1).getPageRequest().getPageSize());
	}

	@Test
	public void testFindAllByIdQueryForBeanWithLongIdType() {
		Mockito.when(solrOperationsMock.count(Mockito.any(SolrDataQuery.class))).thenReturn(12345l);
		SimpleSolrRepository<BeanWithLongIdType, Long> repoWithNonStringIdType = new SimpleSolrRepository<BeanWithLongIdType, Long>(
				solrOperationsMock, BeanWithLongIdType.class);

		repoWithNonStringIdType.findAll(Arrays.asList(1L, 2L, 3L));
		ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);

		Mockito.verify(solrOperationsMock, Mockito.times(1)).count(captor.capture());
		Mockito.verify(solrOperationsMock, Mockito.times(1)).queryForPage(captor.capture(),
				Mockito.eq(BeanWithLongIdType.class));

		Assert.assertNull(captor.getAllValues().get(0).getPageRequest());
		Assert.assertEquals(12345, captor.getAllValues().get(1).getPageRequest().getPageSize());
	}

	static class BeanWithLongIdType {

		@Id private Long id;

		@Field private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
