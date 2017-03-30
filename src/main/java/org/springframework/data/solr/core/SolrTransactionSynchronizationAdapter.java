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

import java.util.HashMap;
import java.util.Map;

import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Christoph Strobl
 * @since 1.2
 */
public class SolrTransactionSynchronizationAdapter extends TransactionSynchronizationAdapter {

	private Map<Integer, CompletionDelegate> delegates = new HashMap<>(2);
	private final SolrOperations solrOperations;

	SolrTransactionSynchronizationAdapter(SolrOperations solrOperations) {
		super();
		this.solrOperations = solrOperations;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.transaction.support.TransactionSynchronizationAdapter#afterCompletion(int)
	 */
	@Override
	public void afterCompletion(int status) {

		CompletionDelegate delegate = this.delegates.get(status);
		if (delegate != null) {
			delegate.execute(this.solrOperations);
		}
	}

	public void register() {
		TransactionSynchronizationManager.registerSynchronization(this);
	}

	public void registerCompletionDelegate(int transactionStatus, CompletionDelegate completionDelegate) {
		this.delegates.put(transactionStatus, completionDelegate);
	}

	public interface CompletionDelegate {

		void execute(SolrOperations solrOperations);

	}

	public static class CommitTransaction implements CompletionDelegate {

		private final String collectionName;

		CommitTransaction(String collectionName) {
			this.collectionName = collectionName;
		}

		@Override
		public void execute(SolrOperations solrOperations) {
			solrOperations.commit(collectionName);
		}

	}

	public static class RollbackTransaction implements CompletionDelegate {

		private final String collectionName;

		RollbackTransaction(String collectionName) {
			this.collectionName = collectionName;
		}

		@Override
		public void execute(SolrOperations solrOperations) {
			solrOperations.rollback(collectionName);
		}

	}

}
