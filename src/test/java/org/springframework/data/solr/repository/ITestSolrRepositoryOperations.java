/*
 * Copyright 2012 the original author or authors.
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
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Christoph Strobl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ITestSolrRepositoryOperations {

	@Autowired
	private ProductRepository repo;

	@Before
	public void setUp() {
		repo.deleteAll();
	}

	@After
	public void tearDown() {
		repo.deleteAll();
	}

	@Test
	public void testFindByNamedQuery() {
		ProductBean popularBean = createProductBean("1");
		popularBean.setPopularity(5);

		ProductBean unpopularBean = createProductBean("2");
		unpopularBean.setPopularity(1);

		repo.save(Arrays.asList(popularBean, unpopularBean));

		List<ProductBean> found = repo.findByNamedQuery(5);
		Assert.assertEquals(1, found.size());
		Assert.assertEquals(popularBean.getId(), found.get(0).getId());
	}

	private ProductBean createProductBean(String id) {
		ProductBean initial = new ProductBean();
		initial.setId(id);
		initial.setAvailable(true);
		initial.setName("name-" + id);
		return initial;
	}
}
