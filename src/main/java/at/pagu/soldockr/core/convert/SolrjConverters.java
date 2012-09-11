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

/**
 * Offers classes that can convert from SolrDocument to any Object and vice versa using the solrj DocumentObjectBinder
 * 
 * @author Christoph Strobl
 */
final class SolrjConverters {

  private SolrjConverters() {

  }

  abstract static class DocumentBinderConverter {
    protected final DocumentObjectBinder documentObejctBinder;

    public DocumentBinderConverter(DocumentObjectBinder binder) {
      this.documentObejctBinder = binder != null ? binder : new DocumentObjectBinder();
    }

  }

  public static class ObjectToSolrInputDocumentConverter extends DocumentBinderConverter implements Converter<Object, SolrInputDocument> {

    public ObjectToSolrInputDocumentConverter(DocumentObjectBinder binder) {
      super(binder);
    }

    @Override
    public SolrInputDocument convert(Object source) {
      if (source == null) {
        return null;
      }

      return documentObejctBinder.toSolrInputDocument(source);
    }
  }

  public static class SolrInputDocumentToObjectConverter<T> extends DocumentBinderConverter implements Converter<Map<String, ?>, T> {

    private Class<T> clazz;

    public SolrInputDocumentToObjectConverter(Class<T> clazz) {
      this(clazz, null);
    }

    public SolrInputDocumentToObjectConverter(Class<T> clazz, DocumentObjectBinder binder) {
      super(binder);
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
