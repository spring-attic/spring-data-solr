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
package at.pagu.soldockr.repository.cdi;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.util.Assert;

import at.pagu.soldockr.core.SolrOperations;
import at.pagu.soldockr.repository.support.SolrRepositoryFactory;

/**
 * Uses CdiRepositoryBean to create SolrRepository instances.
 * 
 * @author Christoph Strobl
 */
public class SolrRepositoryBean<T> extends CdiRepositoryBean<T> {

  private final Bean<SolrOperations> solrOperationsBean;

  public SolrRepositoryBean(Bean<SolrOperations> operations, Set<Annotation> qualifiers, Class<T> repositoryType, BeanManager beanManager) {
    super(qualifiers, repositoryType, beanManager);

    Assert.notNull(operations, "Cannot create repository with 'null' for SolrOperations.");
    this.solrOperationsBean = operations;
  }

  @Override
  protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType) {
    SolrOperations solrOperations = getDependencyInstance(solrOperationsBean, SolrOperations.class);
    return new SolrRepositoryFactory(solrOperations).getRepository(repositoryType);
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return solrOperationsBean.getScope();
  }

}
