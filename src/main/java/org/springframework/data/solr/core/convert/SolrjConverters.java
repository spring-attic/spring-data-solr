/*
 * Copyright 2012 - 2018 the original author or authors.
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
package org.springframework.data.solr.core.convert;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.solr.core.query.Update;
import org.springframework.data.solr.core.query.UpdateField;
import org.springframework.data.solr.core.query.ValueHoldingField;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Offers classes that can convert from SolrDocument to any Object and vice versa using the solrj DocumentObjectBinder
 *
 * @author Christoph Strobl
 */
final class SolrjConverters {

	private SolrjConverters() {

	}

	abstract static class DocumentBinderConverter {
		protected final DocumentObjectBinder documentObjectBinder;

		public DocumentBinderConverter(@Nullable DocumentObjectBinder binder) {
			this.documentObjectBinder = binder != null ? binder : new DocumentObjectBinder();
		}

	}

	/**
	 * Converts any {@link Object} to {@link SolrInputDocument}
	 *
	 * @author Christoph Strobl
	 */
	@WritingConverter
	public static class ObjectToSolrInputDocumentConverter extends DocumentBinderConverter
			implements Converter<Object, SolrInputDocument> {

		public ObjectToSolrInputDocumentConverter(DocumentObjectBinder binder) {
			super(binder);
		}

		@Override
		public SolrInputDocument convert(@Nullable Object source) {
			if (source == null) {
				return null;
			}

			return documentObjectBinder.toSolrInputDocument(source);
		}
	}

	/**
	 * Converts any {@link Update} to {@link SolrInputDocument} for atomic update.
	 *
	 * @author Christoph Strobl
	 */
	@WritingConverter
	public static class UpdateToSolrInputDocumentConverter implements Converter<Update, SolrInputDocument> {

		private static final String VERSION_FIELD_ID = "_version_";

		public UpdateToSolrInputDocumentConverter() {
			super();
		}

		@Override
		public SolrInputDocument convert(@Nullable Update source) {
			if (source == null) {
				return null;
			}
			Assert.notNull(source.getIdField(), "Id field must not be null!");
			Assert.hasText(source.getIdField().getName(), "Name of Id field must not be null nor empty!");

			SolrInputDocument solrInputDocument = new SolrInputDocument();
			solrInputDocument.addField(source.getIdField().getName(), source.getIdField().getValue());
			if (source.getVersion() != null) {
				solrInputDocument.addField(VERSION_FIELD_ID, source.getVersion());
			}

			for (UpdateField field : source.getUpdates()) {
				HashMap<String, Object> mapValue = new HashMap<>(1);
				mapValue.put(field.getAction().getSolrOperation(), getUpdateValue(field));
				solrInputDocument.addField(field.getName(), mapValue);
			}

			return solrInputDocument;
		}

		@Nullable
		private Object getUpdateValue(ValueHoldingField field) {
			// Solr removes all values from document in case of empty colleciton
			// therefore those values have to be set to null.
			if (field.getValue() instanceof Collection) {
				if (((Collection<?>) field.getValue()).isEmpty()) {
					return null;
				}
			}

			return field.getValue();
		}

	}

	/**
	 * Convert any {@link SolrDocument} to object of given {@link Class} using {@link DocumentObjectBinder}
	 *
	 * @author Christoph Strobl
	 * @param <T>
	 */
	@ReadingConverter
	public static class SolrInputDocumentToObjectConverter<T> extends DocumentBinderConverter
			implements Converter<Map<String, ?>, T> {

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
			if (source instanceof SolrDocument && ((SolrDocument) source).hasChildDocuments()) {
				document.addChildDocuments(((SolrDocument) source).getChildDocuments());
			}

			return documentObjectBinder.getBean(clazz, document);
		}

	}

}
