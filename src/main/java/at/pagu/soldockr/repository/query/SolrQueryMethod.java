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

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

import at.pagu.soldockr.repository.Query;

/**
 * Solr specific implementation of {@link QueryMethod} taking care of {@link Query}
 * 
 * @author Christoph Strobl
 */
public class SolrQueryMethod extends QueryMethod {

  private final SolrEntityInformation<?, ?> entityInformation;
  private Method method;

  public SolrQueryMethod(Method method, RepositoryMetadata metadata, SolrEntityInformationCreator solrInformationCreator) {
    super(method, metadata);
    this.method = method;
    this.entityInformation = solrInformationCreator.getEntityInformation(metadata.getReturnedDomainClass(method));
  }

  public boolean hasAnnotatedQuery() {
    return getAnnotatedQuery() != null;
  }

  String getAnnotatedQuery() {
    String query = (String) AnnotationUtils.getValue(getQueryAnnotation());
    return StringUtils.hasText(query) ? query : null;
  }

  private Query getQueryAnnotation() {
    return this.method.getAnnotation(Query.class);
  }

  TypeInformation<?> getReturnType() {
    return ClassTypeInformation.fromReturnTypeOf(method);
  }

  @Override
  public SolrEntityInformation<?, ?> getEntityInformation() {
    return entityInformation;
  }

}
