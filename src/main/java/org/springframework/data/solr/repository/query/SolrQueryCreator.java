/*
 * Copyright 2012 - 2014 the original author or authors.
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
package org.springframework.data.solr.repository.query;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;

/**
 * Solr specific implmentation of an {@link AbstractQueryCreator} that constructs {@link Query}
 * 
 * @author Christoph Strobl
 * @author John Dorman
 */
class SolrQueryCreator extends AbstractQueryCreator<Query, Query> {

	private final MappingContext<?, SolrPersistentProperty> context;

	public SolrQueryCreator(PartTree tree, SolrParameterAccessor parameters,
			MappingContext<?, SolrPersistentProperty> context) {
		super(tree, parameters);
		this.context = context;
	}

	@Override
	protected Query create(Part part, Iterator<Object> iterator) {
		PersistentPropertyPath<SolrPersistentProperty> path = context.getPersistentPropertyPath(part.getProperty());
		return new SimpleQuery(from(part.getType(),
				new Criteria(path.toDotPath(SolrPersistentProperty.PropertyToFieldNameConverter.INSTANCE)), iterator));
	}

	@Override
	protected Query and(Part part, Query base, Iterator<Object> iterator) {
		if (base == null) {
			return create(part, iterator);
		}
		PersistentPropertyPath<SolrPersistentProperty> path = context.getPersistentPropertyPath(part.getProperty());
		return base.addCriteria(from(part.getType(),
				new Criteria(path.toDotPath(SolrPersistentProperty.PropertyToFieldNameConverter.INSTANCE)), iterator));
	}

	@Override
	protected Query or(Query base, Query query) {
		Criteria part = query.getCriteria();
		part.setPartIsOr(true);
		if (part.hasSiblings()) {
			boolean first = true;
			for (Criteria nested : part.getSiblings()) {
				if (first) {
					nested.setPartIsOr(true);
					first = false;
				}
				base.addCriteria(nested);
			}
		} else {
			base.addCriteria(part);
		}
		return base;
	}

	@Override
	protected Query complete(Query query, Sort sort) {
		if (query == null) {
			return null;
		}
		return query.addSort(sort);
	}

	private Criteria from(Type type, Criteria instance, Iterator<?> parameters) {
		Criteria criteria = instance;
		if (criteria == null) {
			criteria = new Criteria();
		}

		switch (type) {
			case TRUE:
				return criteria.is(true);
			case FALSE:
				return criteria.is(false);
			case SIMPLE_PROPERTY:
				return criteria.is(appendBoostAndGetParameterValue(criteria, parameters));
			case NEGATING_SIMPLE_PROPERTY:
				return criteria.is(appendBoostAndGetParameterValue(criteria, parameters)).not();
			case IS_NULL:
				return criteria.isNull();
			case IS_NOT_NULL:
				return criteria.isNotNull();
			case REGEX:
				return criteria.expression(appendBoostAndGetParameterValue(criteria, parameters).toString());
			case LIKE:
			case STARTING_WITH:
				return criteria.startsWith(asStringArray(appendBoostAndGetParameterValue(criteria, parameters)));
			case NOT_LIKE:
				return criteria.startsWith(asStringArray(appendBoostAndGetParameterValue(criteria, parameters))).not();
			case ENDING_WITH:
				return criteria.endsWith(asStringArray(appendBoostAndGetParameterValue(criteria, parameters)));
			case CONTAINING:
				return criteria.contains(asStringArray(appendBoostAndGetParameterValue(criteria, parameters)));
			case AFTER:
			case GREATER_THAN:
				return criteria.greaterThan(appendBoostAndGetParameterValue(criteria, parameters));
			case GREATER_THAN_EQUAL:
				return criteria.greaterThanEqual(appendBoostAndGetParameterValue(criteria, parameters));
			case BEFORE:
			case LESS_THAN:
				return criteria.lessThan(appendBoostAndGetParameterValue(criteria, parameters));
			case LESS_THAN_EQUAL:
				return criteria.lessThanEqual(appendBoostAndGetParameterValue(criteria, parameters));
			case BETWEEN:
				return criteria.between(appendBoostAndGetParameterValue(criteria, parameters),
						appendBoostAndGetParameterValue(criteria, parameters));
			case IN:
				return criteria.in(asArray(appendBoostAndGetParameterValue(criteria, parameters)));
			case NOT_IN:
				return criteria.in(asArray(appendBoostAndGetParameterValue(criteria, parameters))).not();
			case NEAR:
				return createNearCriteria(parameters, criteria);
			case WITHIN:
				return criteria.within((Point) getBindableValue((BindableSolrParameter) parameters.next()),
						(Distance) getBindableValue((BindableSolrParameter) parameters.next()));
			default:
				throw new InvalidDataAccessApiUsageException("Illegal criteria found '" + type + "'.");
		}
	}

	private Object appendBoostAndGetParameterValue(Criteria criteria, Iterator<?> iterator) {
		Object param = iterator.next();
		if (param instanceof BindableSolrParameter) {
			BindableSolrParameter bindable = (BindableSolrParameter) param;
			appendBoost(criteria, bindable);
			return bindable.getValue();
		}
		return param;
	}

	private Criteria appendBoost(Criteria criteria, BindableSolrParameter parameter) {
		if (!Float.isNaN(parameter.getBoost())) {
			criteria.boost(parameter.getBoost());
		}
		return criteria;
	}

	private Object getBindableValue(BindableSolrParameter parameter) {
		if (parameter == null) {
			return null;
		}
		return parameter.getValue();
	}

	private Criteria createNearCriteria(Iterator<?> parameters, Criteria criteria) {
		Object value = getBindableValue((BindableSolrParameter) parameters.next());
		if (value instanceof Box) {
			return criteria.near((Box) value);
		} else {
			return criteria.near((Point) value, (Distance) getBindableValue((BindableSolrParameter) parameters.next()));
		}
	}

	private Object[] asArray(Object o) {
		if (o instanceof Collection) {
			return ((Collection<?>) o).toArray();
		} else if (o.getClass().isArray()) {
			return (Object[]) o;
		}
		return new Object[] { o };
	}

	@SuppressWarnings("unchecked")
	private String[] asStringArray(Object o) {
		if (o instanceof Collection) {
			Collection<?> col = (Collection<?>) o;
			if (col.isEmpty()) {
				return new String[0];
			} else {
				if (!(col.iterator().next() instanceof String)) {
					throw new IllegalArgumentException("Parameter has to be a collection of String.");
				}
				return ((Collection<String>) o).toArray(new String[col.size()]);
			}
		} else if (o.getClass().isArray()) {
			if (!(o instanceof String[])) {
				throw new IllegalArgumentException("Parameter has to be an array of String.");
			}
			return (String[]) o;
		}
		return new String[] { o.toString() };
	}

}
