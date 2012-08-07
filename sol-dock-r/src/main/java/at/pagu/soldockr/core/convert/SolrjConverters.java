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
package at.pagu.soldockr.core.convert;

import java.util.Map;

import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.core.convert.converter.Converter;

final class SolrjConverters {

  private SolrjConverters() {

  }

  public static class ObjectToSolrInputDocumentConverter implements Converter<Object, SolrInputDocument> {

    private final DocumentObjectBinder documentObejctBinder;

    public ObjectToSolrInputDocumentConverter(DocumentObjectBinder binder) {
      if (binder != null) {
        this.documentObejctBinder = binder;
      } else {
        this.documentObejctBinder = new DocumentObjectBinder();
      }
    }

    @Override
    public SolrInputDocument convert(Object source) {
      if (source == null) {
        return null;
      }

      return documentObejctBinder.toSolrInputDocument(source);
    }
  }

  public static class SolrInputDocumentToObjectConverter<T> implements Converter<Map<String, ?>, T> {

    private static final DocumentObjectBinder documentObejctBinder = new DocumentObjectBinder();
    private Class<T> clazz;

    public SolrInputDocumentToObjectConverter(Class<T> clazz) {
      this.clazz = clazz;
    }

    @Override
    public T convert(Map<String, ?> source) {
      if (source == null) {
        return null;
      }
      SolrDocument document = new SolrDocument();
      document.putAll(source);

      return documentObejctBinder.getBean(clazz, document);
    }

  }

}
