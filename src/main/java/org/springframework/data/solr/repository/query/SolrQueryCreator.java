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
package org.springframework.data.solr.repository.query;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.solr.core.geo.BoundingBox;
import org.springframework.data.solr.core.geo.Distance;
import org.springframework.data.solr.core.geo.GeoLocation;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import sun.security.x509.CertAttrSet;

/**
 * Solr specific implmentation of an {@link AbstractQueryCreator} that constructs {@link Query}
 * 
 * @author Christoph Strobl
 */
class SolrQueryCreator extends AbstractQueryCreator<Query, Query> {

	private final MappingContext<?, SolrPersistentProperty> context;

	public SolrQueryCreator(PartTree tree, MappingContext<?, SolrPersistentProperty> context) {
		super(tree);
		this.context = context;
	}

	public SolrQueryCreator(PartTree tree, ParameterAccessor parameters, MappingContext<?, SolrPersistentProperty> context) {
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
		return new SimpleQuery(base.getCriteria().or(query.getCriteria()));
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
			return criteria.is(parameters.next());
		case NEGATING_SIMPLE_PROPERTY:
			return criteria.is(parameters.next()).not();
		case REGEX:
			return criteria.expression(parameters.next().toString());
		case LIKE:
		case STARTING_WITH:
			return criteria.startsWith(parameters.next().toString());
		case NOT_LIKE:
			return criteria.startsWith(parameters.next().toString()).not();
		case ENDING_WITH:
			return criteria.endsWith(parameters.next().toString());
		case CONTAINING:
			return criteria.contains(parameters.next().toString());
		case AFTER:
		case GREATER_THAN:
			return criteria.greaterThan(parameters.next());
		case GREATER_THAN_EQUAL:
			return criteria.greaterThanEqual(parameters.next());
		case BEFORE:
		case LESS_THAN:
			return criteria.lessThan(parameters.next());
		case LESS_THAN_EQUAL:
			return criteria.lessThanEqual(parameters.next());
		case BETWEEN:
			return criteria.between(parameters.next(), parameters.next());
		case IN:
			return criteria.in(asArray(parameters.next()));
		case NOT_IN:
			return criteria.in(asArray(parameters.next())).not();
		case NEAR:
			Object value = parameters.next();
			if (value instanceof BoundingBox) {
				return criteria.near((BoundingBox) value);
			} else {
				return criteria.near((GeoLocation) value, (Distance) parameters.next());
			}
        case WITHIN:
            return criteria.within((GeoLocation) parameters.next(), (Distance) parameters.next());
		default:
			throw new InvalidDataAccessApiUsageException("Illegal criteria found '" + type + "'.");
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

}
