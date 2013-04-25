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

import org.springframework.data.repository.CrudRepository;

/**
 * @author Christoph Strobl
 */
class TransactionalIntegrationTestsBase {

	protected void safeDelete(CrudRepository<?, ?> repo) {
		doDelete(repo, 1);
	}

	/**
	 * For some reason transaction log might not be ready when performing delete.
	 * 
	 * <pre>
	 * java.lang.AssertionError
	 * 	at org.apache.solr.update.TransactionLog.init(TransactionLog.java:172)
	 * 	at org.apache.solr.update.TransactionLog.init(TransactionLog.java:140)
	 * </pre>
	 * 
	 * So will have to retry 3 times to give solr the time it needs
	 * 
	 * @param repo
	 * @param tryCount
	 */
	private void doDelete(CrudRepository<?, ?> repo, int tryCount) {
		try {
			repo.deleteAll();
		} catch (java.lang.AssertionError e) {
			if (tryCount > 3) {
				// just give up and print error
				System.err.println(e);
				return;
			}
			wait(250);
			doDelete(repo, tryCount + 1);
		}
	}

	protected void wait(int timeout) {
		try {
			Thread.sleep(timeout);
		} catch (InterruptedException e) {
			// do noting
		}
	}
}
