/*
 * Copyright 2012-2016 the original author or authors.
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
package org.springframework.data.solr.repository.cdi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.apache.webbeans.cditest.CdiTestContainer;
import org.apache.webbeans.cditest.CdiTestContainerLoader;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.solr.repository.ProductBean;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class ITestCdiRepository {

	private static CdiTestContainer cdiContainer;
	private CdiProductRepository repository;
	private SamplePersonRepository samplePersonRepository;
	private QualifiedProductRepository qualifiedProductRepository;

	@BeforeClass
	public static void init() throws Exception {

		cdiContainer = CdiTestContainerLoader.getCdiContainer();
		cdiContainer.startApplicationScope();
		cdiContainer.bootContainer();
	}

	@AfterClass
	public static void shutdown() throws Exception {

		cdiContainer.stopContexts();
		cdiContainer.shutdownContainer();
	}

	@Before
	public void setUp() {

		CdiRepositoryClient client = cdiContainer.getInstance(CdiRepositoryClient.class);
		repository = client.getRepository();
		samplePersonRepository = client.getSamplePersonRepository();
		qualifiedProductRepository = client.getQualifiedProductRepository();
	}

	@Test
	public void testCdiRepository() {

		assertNotNull(repository);

		ProductBean bean = new ProductBean();
		bean.setId("id-1");
		bean.setName("cidContainerTest-1");

		repository.save(bean);

		assertTrue(repository.exists(bean.getId()));

		ProductBean retrieved = repository.findOne(bean.getId());
		assertNotNull(retrieved);
		assertEquals(bean.getId(), retrieved.getId());
		assertEquals(bean.getName(), retrieved.getName());

		assertEquals(1, repository.count());

		assertTrue(repository.exists(bean.getId()));

		repository.delete(bean);

		assertEquals(0, repository.count());
		retrieved = repository.findOne(bean.getId());
		assertNull(retrieved);
	}

	/**
	 * @see DATASOLR-187
	 */
	@Test
	public void returnOneFromCustomImpl() {

		assertThat(samplePersonRepository.returnOne(), is(1));
	}

	/**
	 * @see DATASOLR-276
	 */
	@Test
	public void testQualifiedCdiRepository() {

		ProductBean bean = new ProductBean();
		bean.setId("id-1");
		bean.setName("cidContainerTest-1");

		qualifiedProductRepository.save(bean);

		qualifiedProductRepository.delete(bean);

		assertEquals(0, qualifiedProductRepository.count());
	}
}
