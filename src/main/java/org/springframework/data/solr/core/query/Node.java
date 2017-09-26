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
package org.springframework.data.solr.core.query;

import java.util.Collection;
import java.util.Collections;

import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 1.2
 */
public abstract class Node {

	private @Nullable Node parent;
	private boolean isOr = false;
	private boolean negating = false;

	protected Node() {}

	// ------- TREE ---------
	protected void setParent(@Nullable Node parent) {
		this.parent = parent;
	}

	/**
	 * Define {@literal or} nature of {@link Node}
	 * 
	 * @param isOr
	 */
	public void setPartIsOr(boolean isOr) {
		this.isOr = isOr;
	}

	/**
	 * @return true in case {@link Node} has no parent.
	 */
	public boolean isRoot() {
		return this.parent == null;
	}

	/**
	 * @return true in case {@link Node} has {@literal or} nature.
	 */
	public boolean isOr() {
		return this.isOr;
	}

	/**
	 * Get parent {@link Node}.
	 * 
	 * @return null in case no parent set.
	 */
	@Nullable
	public Node getParent() {
		return this.parent;
	}

	/**
	 * @return true if {@link Node} has siblings.
	 */
	public boolean hasSiblings() {
		return !getSiblings().isEmpty();
	}

	/**
	 * @return empty collection if {@link Node} does not have siblings.
	 */
	public Collection<Criteria> getSiblings() {
		return Collections.emptyList();
	}

	/**
	 * @return true if {@code not()} criteria
	 * @since 1.4
	 */
	public boolean isNegating() {
		return this.negating;
	}

	/**
	 * @param negating
	 * @since 1.4
	 */
	protected void setNegating(boolean negating) {
		this.negating = negating;
	}

	// ------ CONJUNCTIONS --------
	/**
	 * Combine two {@link Node}s using {@literal and}.
	 * 
	 * @param part
	 * @return
	 */
	public abstract <T extends Node> T and(Node part);

	/**
	 * Combine node with new {@link Node} for given {@literal fieldname} using {@literal and}.
	 * 
	 * @return
	 */
	public abstract <T extends Node> T and(String fieldname);

	/**
	 * Combine two {@link Node}s using {@literal or}.
	 * 
	 * @param part
	 * @return
	 */
	public abstract <T extends Node> T or(Node part);

	/**
	 * Combine node with new {@link Node} for given {@literal fieldname} using {@literal and}.
	 * 
	 * @return
	 */
	public abstract <T extends Node> T or(String fieldname);

	// ------- COMMANDS ----------
	public abstract Node is(Object value);

	public abstract Node is(Object... values);

	public abstract Node is(Iterable<?> values);

	public abstract Node isNull();

	public abstract Node isNotNull();

	public abstract Node contains(String value);

	public abstract Node contains(String... values);

	public abstract Node contains(Iterable<String> values);

	public abstract Node startsWith(String prefix);

	public abstract Node startsWith(String... values);

	public abstract Node startsWith(Iterable<String> values);

	public abstract Node endsWith(String postfix);

	public abstract Node endsWith(String... values);

	public abstract Node endsWith(Iterable<String> values);

	public abstract Node not();

	public abstract Node fuzzy(String value);

	public abstract Node fuzzy(String values, float levenshteinDistance);

	public abstract Node sloppy(String phrase, int distance);

	public abstract Node expression(String nativeSolrQueryExpression);

	public abstract Node boost(float value);

	public abstract Node between(Object lowerBound, Object upperBound);

	public abstract Node between(Object lowerBound, Object upperBound, boolean includeLowerBound,
			boolean includeUpperBound);

	public abstract Node lessThan(Object upperBound);

	public abstract Node lessThanEqual(Object upperBound);

	public abstract Node greaterThan(Object lowerBound);

	public abstract Node greaterThanEqual(Object lowerBound);

	public abstract Node in(Object... values);

	public abstract Node in(Iterable<?> values);

	public abstract Node within(Point location, Distance distance);

	public abstract Node near(Box box);

	public abstract Node near(Point location, Distance distance);

	public abstract Node function(Function function);

}
