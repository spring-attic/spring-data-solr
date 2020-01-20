/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.solr.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.solr.AbstractITestWithEmbeddedSolrServer;
import org.springframework.data.solr.ExampleSolrBean;
import org.springframework.data.solr.core.SolrTemplate;

/**
 * @author Christoph Strobl
 * @author Mayank Kumar
 */
public class ITestSimpleSolrRepository extends AbstractITestWithEmbeddedSolrServer {

	private ExampleSolrBeanRepository repository;

	@Before
	public void setUp() {

		SolrTemplate template = new SolrTemplate(server);
		template.afterPropertiesSet();

		repository = new ExampleSolrBeanRepository(template, ExampleSolrBean.class);
	}

	@Test
	public void testBeanLifecyle() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		ExampleSolrBean savedBean = repository.save(toInsert);

		assertThat(savedBean).isSameAs(toInsert);

		assertThat(repository.existsById(savedBean.getId())).isTrue();

		ExampleSolrBean retrieved = repository.findById(savedBean.getId()).get();
		assertThat(retrieved).isNotNull();
		assertThat(EqualsBuilder.reflectionEquals(savedBean, retrieved, new String[] { "version" })).isTrue();

		assertThat(repository.count()).isEqualTo(1);

		assertThat(repository.existsById(savedBean.getId())).isTrue();

		repository.delete(savedBean);

		assertThat(repository.count()).isEqualTo(0);
		assertThat(repository.findById(savedBean.getId()).isPresent()).isFalse();
	}

	@Test
	public void testListFunctions() {
		int objectCount = 100;
		List<ExampleSolrBean> toInsert = new ArrayList<>(objectCount);
		for (int i = 0; i < 100; i++) {
			toInsert.add(createExampleBeanWithId(Integer.toString(i)));
		}

		repository.saveAll(toInsert);

		assertThat(repository.count()).isEqualTo(objectCount);

		int counter = 0;
		for (ExampleSolrBean retrievedBean : repository.findAll()) {
			assertThat(EqualsBuilder.reflectionEquals(toInsert.get(counter), retrievedBean, new String[] { "version" }))
					.isTrue();

			counter++;
			if (counter > objectCount) {
				fail("More beans return than added!");
			}
		}

		repository.delete(toInsert.get(0));
		assertThat(repository.count()).isEqualTo(99);

		repository.deleteAll();

		assertThat(repository.count()).isEqualTo(0);
	}

	@Test // DATASOLR-332
	public void testBeanLifecyleWithCommitWithin() {
		ExampleSolrBean toInsert = createDefaultExampleBean();
		ExampleSolrBean savedBean = repository.save(toInsert, Duration.ofSeconds(10));

		assertThat(savedBean).isSameAs(toInsert);

		assertThat(repository.existsById(savedBean.getId())).isTrue();

		ExampleSolrBean retrieved = repository.findById(savedBean.getId()).get();
		assertThat(retrieved).isNotNull();
		assertThat(EqualsBuilder.reflectionEquals(savedBean, retrieved, new String[] { "version" })).isTrue();

		assertThat(repository.count()).isEqualTo(1);

		assertThat(repository.existsById(savedBean.getId())).isTrue();

		repository.delete(savedBean);

		assertThat(repository.count()).isEqualTo(0);
		assertThat(repository.findById(savedBean.getId()).isPresent()).isFalse();
	}
}
