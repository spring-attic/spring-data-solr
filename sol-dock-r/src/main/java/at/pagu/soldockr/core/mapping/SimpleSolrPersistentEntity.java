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
package at.pagu.soldockr.core.mapping;

import java.util.Locale;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

public class SimpleSolrPersistentEntity<T> extends BasicPersistentEntity<T, SolrPersistentProperty> implements SolrPersistentEntity<T>, ApplicationContextAware {

  private final StandardEvaluationContext context;
  private String solrCoreName;

  public SimpleSolrPersistentEntity(TypeInformation<T> typeInformation) {
    super(typeInformation);
    this.context = new StandardEvaluationContext();
    this.solrCoreName = derivateSolrCoreNameFromClass(typeInformation.getType());
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    context.addPropertyAccessor(new BeanFactoryAccessor());
    context.setBeanResolver(new BeanFactoryResolver(applicationContext));
    context.setRootObject(applicationContext);
  }

  private String derivateSolrCoreNameFromClass(Class<?> clazz) {
    String derivativeSolrCoreName = clazz.getSimpleName().toLowerCase(Locale.ENGLISH);
    if (clazz.isAnnotationPresent(SolrDocument.class)) {
      SolrDocument solrDocument = clazz.getAnnotation(SolrDocument.class);
      if (StringUtils.hasText(solrDocument.solrCoreName())) {
        derivativeSolrCoreName = solrDocument.solrCoreName();
      }
    }
    return derivativeSolrCoreName;
  }

  @Override
  public String getSolrCoreName() {
    return this.solrCoreName;
  }

}
