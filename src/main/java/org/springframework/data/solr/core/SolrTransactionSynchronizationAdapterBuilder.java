/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @since 1.2
 */
public class SolrTransactionSynchronizationAdapterBuilder {

	@Nullable SolrTransactionSynchronizationAdapter adapter;
	@Nullable private String collectionName;

	/**
	 * @param solrOperations must not be {@literal null}
	 * @return
	 */
	public static SolrTransactionSynchronizationAdapterBuilder forOperations(SolrOperations solrOperations) {

		Assert.notNull(solrOperations, "SolrOperations for transaction syncronisation must not be 'null'");
		SolrTransactionSynchronizationAdapterBuilder builder = new SolrTransactionSynchronizationAdapterBuilder();
		builder.adapter = new SolrTransactionSynchronizationAdapter(solrOperations);
		return builder;
	}

	/**
	 * @param collection
	 * @return
	 */
	public SolrTransactionSynchronizationAdapterBuilder onCollection(String collection) {

		this.collectionName = collection;
		return this;
	}

	/**
	 * Creates a {@link SolrTransactionSynchronizationAdapter} reacting on
	 * {@link TransactionSynchronization#STATUS_COMMITTED} and {@link TransactionSynchronization#STATUS_ROLLED_BACK}.
	 * 
	 * @return
	 */
	public SolrTransactionSynchronizationAdapter withDefaultBehaviour() {

		this.adapter.registerCompletionDelegate(TransactionSynchronization.STATUS_COMMITTED,
				new SolrTransactionSynchronizationAdapter.CommitTransaction(this.collectionName));
		this.adapter.registerCompletionDelegate(TransactionSynchronization.STATUS_ROLLED_BACK,
				new SolrTransactionSynchronizationAdapter.RollbackTransaction(this.collectionName));

		return this.adapter;
	}

}
