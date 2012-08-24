/*
 * Copyright (C) 2012 j73x73r@gmail.com.
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
package at.pagu.data.solr.example.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.repository.NoRepositoryBean;

import at.pagu.data.solr.example.model.Product;
import at.pagu.soldockr.core.query.Criteria;
import at.pagu.soldockr.core.query.FacetOptions;
import at.pagu.soldockr.core.query.FacetQuery;
import at.pagu.soldockr.core.query.Query;
import at.pagu.soldockr.core.query.SimpleFacetQuery;
import at.pagu.soldockr.core.query.SimpleField;
import at.pagu.soldockr.core.query.SimpleQuery;
import at.pagu.soldockr.core.query.result.FacetPage;
import at.pagu.soldockr.repository.SimpleSolrRepository;

/**
 * @author Christoph Strobl
 */
@NoRepositoryBean
public class SolrProductRepository extends SimpleSolrRepository<Product> implements ProductRepository {

  @Override
  public Page<Product> findByPopularity(Integer popularity) {
    Query query = new SimpleQuery(new Criteria(SolrSearchableFields.POPULARITY).is(popularity));
    return getSolrOperations().executeListQuery(query, Product.class);
  }

  @Override
  public FacetPage<Product> findByNameStartingWithAndFacetOnAvailable(String namePrefix) {
    FacetQuery query = new SimpleFacetQuery(new Criteria(SolrSearchableFields.NAME).startsWith(namePrefix));
    query.setFacetOptions(new FacetOptions(SolrSearchableFields.AVAILABLE));
    return getSolrOperations().executeFacetQuery(query, Product.class);
  }

  @Override
  public Page<Product> findByAvailableTrue() {
    Query query = new SimpleQuery(new Criteria(new SimpleField(Criteria.WILDCARD)).expression(Criteria.WILDCARD));
    query.addFilterQuery(new SimpleQuery(new Criteria(SolrSearchableFields.AVAILABLE).is(true)));

    return getSolrOperations().executeListQuery(query, Product.class);
  }
}
