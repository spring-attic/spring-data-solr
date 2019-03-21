/*
 * Copyright 2012 - 2015 the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Christoph Strobl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("TransactionalSolrRepositoryTest-context.xml")
@Transactional
@TransactionConfiguration(defaultRollback = true, transactionManager = "transactionManager")
public class ITestTransactionalSolrRepositoryDeleteOperationRollbackTrue extends TransactionalIntegrationTestsBase {

	private static final String ID = "id-tansaction-rolled-back";

	@Autowired private ProductRepository repo;

	@Autowired private SolrClient solrClientMock;

	@BeforeTransaction
	public void resetMock() {
		Mockito.reset(solrClientMock);
	}

	@AfterTransaction
	public void checkIfDeleted() throws SolrServerException, IOException {
		Mockito.verify(solrClientMock, Mockito.times(1)).rollback();
		Mockito.verify(solrClientMock, Mockito.never()).commit();
	}

	@Test
	public void testDeleteById() {
		repo.delete(ID);
	}

	@Test
	public void testDelete() {
		ProductBean bean = new ProductBean();
		bean.setId(ID);
		repo.delete(bean);
	}

	@Test
	public void testDeleteMultipleObjects() {
		ProductBean bean = new ProductBean();
		bean.setId(ID);
		repo.delete(Arrays.asList(bean));
	}

	@Test
	public void testDeleteAll() {
		repo.deleteAll();
	}

}
