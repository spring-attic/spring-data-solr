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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.StringUtils;

public class SimpleSolrPersistentProperty extends AnnotationBasedPersistentProperty<SolrPersistentProperty> implements
    SolrPersistentProperty {

  private static final Set<Class<?>> SUPPORTED_ID_TYPES = new HashSet<Class<?>>();
  private static final Set<String> SUPPORTED_ID_PROPERTY_NAMES = new HashSet<String>();

  static {
    SUPPORTED_ID_TYPES.add(String.class);
    SUPPORTED_ID_PROPERTY_NAMES.add("id");
  }

  public SimpleSolrPersistentProperty(Field field, PropertyDescriptor propertyDescriptor,
      PersistentEntity<?, SolrPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
    super(field, propertyDescriptor, owner, simpleTypeHolder);
  }

  @Override
  public String getFieldName() {
    org.apache.solr.client.solrj.beans.Field annotation = getField().getAnnotation(org.apache.solr.client.solrj.beans.Field.class);

    if (StringUtils.hasText(annotation.value()) && !("#default".equals(annotation.value()))) {
      return annotation.value();
    }
    return field.getName();
  }

  @Override
  public boolean isIdProperty() {
    if (super.isIdProperty()) {
      return true;
    }

    return SUPPORTED_ID_PROPERTY_NAMES.contains(getFieldName());
  }

  @Override
  protected Association<SolrPersistentProperty> createAssociation() {
    return null;
  }
  
}
