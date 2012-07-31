/*
 * Copyright (C) 2012 sol-dock-r authors.
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
package at.pagu.soldockr.repository.query;

import java.util.Iterator;

import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;

import at.pagu.soldockr.ApiUsageException;
import at.pagu.soldockr.core.mapping.SolrPersistentProperty;
import at.pagu.soldockr.core.query.Criteria;
import at.pagu.soldockr.core.query.Query;
import at.pagu.soldockr.core.query.SimpleQuery;

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
    return new SimpleQuery(from(part.getType(), new Criteria(path.toDotPath(SolrPersistentProperty.PropertyToFieldNameConverter.INSTANCE)),
        iterator));
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
    return query;
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
        return criteria.isNot(parameters.next());
      case REGEX:
        return criteria.expression(parameters.next().toString());
      case LIKE:
        return criteria.startsWith(parameters.next().toString());
      case NEAR:
        return criteria.fuzzy(parameters.next().toString());
    }
    throw new ApiUsageException("Illegal criteria found '" + type + "'.");
  }

}
