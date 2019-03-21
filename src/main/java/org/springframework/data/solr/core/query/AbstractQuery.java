/*
 * Copyright 2012 - 2014 the original author or authors.
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
package org.springframework.data.solr.core.query;

import org.springframework.util.Assert;

/**
 * Base QueryImplementation <br />
 * Construct Query by adding {@link Criteria}.
 * 
 * @author Christoph Strobl
 */
class AbstractQuery {

	private Criteria criteria;
	private Join join;
	private String requestHandler;

	AbstractQuery() {}

	AbstractQuery(Criteria criteria) {
		this.addCriteria(criteria);
	}

	/**
	 * Add an criteria to the query. The criteria will be connected using 'AND'.
	 * 
	 * @param criteria
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final <T extends SolrDataQuery> T addCriteria(Criteria criteria) {
		Assert.notNull(criteria, "Cannot add null criteria.");

		if (this.criteria == null) {
			this.criteria = criteria;
		} else {
			if (this.criteria instanceof Crotch) {
				((Crotch) this.criteria).add(criteria);
			} else {
				Crotch tree = new Crotch();
				tree.add(this.criteria);
				tree.add(criteria);
				this.criteria = tree;
			}
		}
		return (T) this;
	}

	/**
	 * @return null if not set
	 */
	public Criteria getCriteria() {
		return this.criteria;
	}

	/**
	 * Set values for join {@code !join from=inner_id to=outer_id}
	 * 
	 * @param from
	 * @param to
	 */
	public void setJoin(Join join) {
		this.join = join;
	}

	/**
	 * @return null if not set
	 */
	public Join getJoin() {
		return join;
	}

	public String getRequestHandler() {
		return requestHandler;
	}

	public void setRequestHandler(String requestHandler) {
		this.requestHandler = requestHandler;
	}

}
