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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 1.2
 */
public class Crotch extends Criteria {

	private List<Criteria> siblings = new ArrayList<>();
	private @Nullable Node mostRecentSibling = null;

	Crotch() {}

	@Override
	public Field getField() {
		if (this.mostRecentSibling instanceof Criteria) {
			return ((Criteria) this.mostRecentSibling).getField();
		}
		return null;
	}

	@Override
	public Crotch is(@Nullable Object o) {
		mostRecentSibling.is(o);
		return this;
	}

	@Override
	public Crotch boost(float boost) {
		mostRecentSibling.boost(boost);
		return this;
	}

	@Override
	public Crotch not() {

		mostRecentSibling.not();
		return this;
	}

	@Override
	public Crotch notOperator() {

		if (this.isRoot()) {
			this.setNegating(true);
		} else {
			super.notOperator();
		}
		return this;
	}

	@Override
	public Crotch endsWith(String postfix) {
		mostRecentSibling.endsWith(postfix);
		return this;
	}

	@Override
	public Crotch startsWith(String prefix) {
		mostRecentSibling.startsWith(prefix);
		return this;
	}

	@Override
	public Crotch contains(String value) {
		mostRecentSibling.contains(value);
		return this;
	}

	@Override
	public Crotch is(Object... values) {
		mostRecentSibling.is(values);
		return this;
	}

	@Override
	public Crotch is(Iterable<?> values) {
		mostRecentSibling.is(values);
		return this;
	}

	@Override
	public Crotch isNull() {
		mostRecentSibling.isNull();
		return this;
	}

	@Override
	public Crotch isNotNull() {
		mostRecentSibling.isNotNull();
		return this;
	}

	@Override
	public Crotch contains(String... values) {
		mostRecentSibling.contains(values);
		return this;
	}

	@Override
	public Crotch contains(Iterable<String> values) {
		mostRecentSibling.contains(values);
		return this;
	}

	@Override
	public Crotch startsWith(String... values) {
		mostRecentSibling.startsWith(values);
		return this;
	}

	@Override
	public Crotch startsWith(Iterable<String> values) {
		mostRecentSibling.startsWith(values);
		return this;
	}

	@Override
	public Crotch endsWith(String... values) {
		mostRecentSibling.endsWith(values);
		return this;
	}

	@Override
	public Crotch endsWith(Iterable<String> values) {
		mostRecentSibling.endsWith(values);
		return this;
	}

	@Override
	public Crotch fuzzy(String value) {
		mostRecentSibling.fuzzy(value);
		return this;
	}

	@Override
	public Crotch fuzzy(String values, float levenshteinDistance) {
		mostRecentSibling.fuzzy(values, levenshteinDistance);
		return this;
	}

	@Override
	public Crotch sloppy(String phrase, int distance) {
		mostRecentSibling.sloppy(phrase, distance);
		return this;
	}

	@Override
	public Crotch expression(String nativeSolrQueryExpression) {
		mostRecentSibling.expression(nativeSolrQueryExpression);
		return this;
	}

	@Override
	public Crotch between(@Nullable Object lowerBound, @Nullable Object upperBound) {
		mostRecentSibling.between(lowerBound, upperBound);
		return this;
	}

	@Override
	public Crotch between(@Nullable Object lowerBound, @Nullable Object upperBound, boolean includeLowerBound, boolean includeUpperBound) {
		mostRecentSibling.between(lowerBound, upperBound, includeLowerBound, includeUpperBound);
		return this;
	}

	@Override
	public Crotch lessThan(Object upperBound) {
		mostRecentSibling.lessThan(upperBound);
		return this;
	}

	@Override
	public Crotch lessThanEqual(Object upperBound) {
		mostRecentSibling.lessThanEqual(upperBound);
		return this;
	}

	@Override
	public Crotch greaterThan(Object lowerBound) {
		mostRecentSibling.greaterThan(lowerBound);
		return this;
	}

	@Override
	public Crotch greaterThanEqual(Object lowerBound) {
		mostRecentSibling.greaterThanEqual(lowerBound);
		return this;
	}

	@Override
	public Crotch in(Object... values) {
		mostRecentSibling.in(values);
		return this;
	}

	@Override
	public Crotch in(Iterable<?> values) {
		mostRecentSibling.in(values);
		return this;
	}

	@Override
	public Crotch within(Point location, @Nullable Distance distance) {
		mostRecentSibling.within(location, distance);
		return this;
	}

	@Override
	public Crotch near(Box box) {
		mostRecentSibling.near(box);
		return this;
	}

	@Override
	public Crotch near(Point location, @Nullable Distance distance) {
		mostRecentSibling.near(location, distance);
		return this;
	}

	@Override
	public Crotch function(Function function) {
		mostRecentSibling.function(function);
		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(this.isOr() ? " OR " : " AND ");
		sb.append('(');
		boolean first = true;
		for (Node node : this.siblings) {
			String s = node.toString();
			if (first) {
				s = s.replaceFirst("OR", "").replaceFirst("AND", "");
				first = false;
			}
			sb.append(s);
		}
		sb.append(')');
		return sb.toString();
	}

	// ------- NODE STUFF --------
	void add(Node node) {

		if (!(node instanceof Criteria)) {
			throw new IllegalArgumentException("Can only add instances of Criteria");
		}

		node.setParent(this);

		boolean containsNearFunction = this.siblings.stream().anyMatch(criteria -> criteria.getPredicates().stream()
				.anyMatch(predicate -> predicate.getKey().equalsIgnoreCase("$within")));

		Criteria criteria = (Criteria) node;
		if (containsNearFunction) {
			this.siblings.add(0, criteria);
		} else {
			this.siblings.add(criteria);
		}

		this.mostRecentSibling = node;
	}

	@Override
	public Collection<Criteria> getSiblings() {
		return Collections.unmodifiableCollection(siblings);
	}

	@Override
	public Crotch and(Node part) {
		add(part);
		return this;
	}

	@Override
	public Crotch or(Node part) {
		part.setPartIsOr(true);
		add(part);
		return this;
	}

	@Override
	public Crotch and(String fieldname) {
		if (this.mostRecentSibling instanceof Crotch) {
			((Crotch) mostRecentSibling).add(new Criteria(fieldname));
		} else {
			and(new Criteria(fieldname));
		}
		return this;
	}

	@Override
	public Crotch or(String fieldname) {
		Criteria criteria = new Criteria(fieldname);
		criteria.setPartIsOr(true);

		if (this.mostRecentSibling instanceof Crotch) {
			((Crotch) mostRecentSibling).add(criteria);
		} else {
			or(new Criteria(fieldname));
		}
		return this;
	}
}
