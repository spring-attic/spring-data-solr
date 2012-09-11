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
package at.pagu.soldockr.repository.support;

import static org.springframework.data.querydsl.QueryDslUtils.QUERY_DSL_PRESENT;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

import at.pagu.soldockr.core.SolrOperations;
import at.pagu.soldockr.repository.SimpleSolrRepository;
import at.pagu.soldockr.repository.SolrRepository;
import at.pagu.soldockr.repository.query.PartTreeSolrQuery;
import at.pagu.soldockr.repository.query.SolrEntityInformation;
import at.pagu.soldockr.repository.query.SolrEntityInformationCreator;
import at.pagu.soldockr.repository.query.SolrQueryMethod;
import at.pagu.soldockr.repository.query.StringBasedSolrQuery;

/**
 * Factory to create {@link SolrRepository}
 * 
 * @author Christoph Strobl
 */
public class SolrRepositoryFactory extends RepositoryFactorySupport {

  private final SolrOperations solrOperations;
  private final SolrEntityInformationCreator entityInformationCreator;

  public SolrRepositoryFactory(SolrOperations solrOperations) {
    Assert.notNull(solrOperations);
    this.solrOperations = solrOperations;
    this.entityInformationCreator = new SolrEntityInformationCreatorImpl(solrOperations.getConverter().getMappingContext());
  }

  @Override
  public <T, ID extends Serializable> SolrEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
    return entityInformationCreator.getEntityInformation(domainClass);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  protected Object getTargetRepository(RepositoryMetadata metadata) {
    SimpleSolrRepository repository = new SimpleSolrRepository(getEntityInformation(metadata.getDomainType()), solrOperations);
    repository.setEntityClass(metadata.getDomainType());
    return repository;
  }

  @Override
  protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
    if (isQueryDslRepository(metadata.getRepositoryInterface())) {
      throw new IllegalArgumentException("QueryDsl Support has not been implemented yet.");
    }
    return SimpleSolrRepository.class;
  }

  private static boolean isQueryDslRepository(Class<?> repositoryInterface) {
    return QUERY_DSL_PRESENT && QueryDslPredicateExecutor.class.isAssignableFrom(repositoryInterface);
  }

  @Override
  protected QueryLookupStrategy getQueryLookupStrategy(Key key) {
    return new SolrQueryLookupStrategy();
  }

  private class SolrQueryLookupStrategy implements QueryLookupStrategy {

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, NamedQueries namedQueries) {

      SolrQueryMethod queryMethod = new SolrQueryMethod(method, metadata, entityInformationCreator);
      String namedQueryName = queryMethod.getNamedQueryName();

      if (namedQueries.hasQuery(namedQueryName)) {
        String namedQuery = namedQueries.getQuery(namedQueryName);
        return new StringBasedSolrQuery(namedQuery, queryMethod, solrOperations);
      } else if (queryMethod.hasAnnotatedQuery()) {
        return new StringBasedSolrQuery(queryMethod, solrOperations);
      } else {
        return new PartTreeSolrQuery(queryMethod, solrOperations);
      }
    }

  }

}
