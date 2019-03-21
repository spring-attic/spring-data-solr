/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class SchemaDefinition {

	private String collectionName;
	private List<FieldDefinition> fields;
	private String name;
	private Double version;
	private String uniqueKey;

	public SchemaDefinition() {}

	public SchemaDefinition(String collectionName) {
		this.collectionName = collectionName;
		this.fields = new ArrayList<FieldDefinition>();
	}

	public String getCollectionName() {
		return collectionName;
	}

	public List<FieldDefinition> getFields() {
		return fields;
	}

	public void setFields(List<FieldDefinition> fields) {
		this.fields = fields != null ? fields : new ArrayList<FieldDefinition>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getVersion() {
		return version;
	}

	public void setVersion(Double version) {
		this.version = version;
	}

	public String getUniqueKey() {
		return uniqueKey;
	}

	public void setUniqueKey(String uniqueKey) {
		this.uniqueKey = uniqueKey;
	}

	public boolean containsField(String name) {
		return getFieldDefinition(name) != null;
	}

	public FieldDefinition getFieldDefinition(String name) {

		if (CollectionUtils.isEmpty(this.fields)) {
			return null;
		}

		for (FieldDefinition fd : this.fields) {
			if (ObjectUtils.nullSafeEquals(fd.getName(), name)) {
				return fd;
			}
		}

		return null;
	}

	public void addFieldDefinition(FieldDefinition fieldDef) {

		if (this.fields == null) {
			this.fields = new ArrayList<FieldDefinition>();
		}
		this.fields.add(fieldDef);
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	/**
	 * @author Christoph Strobl
	 * @since 1.3
	 */
	public static interface SchemaField {}

	/**
	 * @author Christoph Strobl
	 * @since 1.3
	 */
	public static class FieldDefinition implements SchemaField {

		private String name;
		private String type;
		private boolean stored;
		private boolean indexed;
		private Object defaultValue;
		private List<String> copyFields;
		private List<Filter> filters;
		private List<Tokenizer> tokenizers;
		private boolean multiValued;
		private boolean required;

		public FieldDefinition() {}

		public FieldDefinition(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public boolean isStored() {
			return stored;
		}

		public void setStored(boolean stored) {
			this.stored = stored;
		}

		public boolean isIndexed() {
			return indexed;
		}

		public void setIndexed(boolean indexed) {
			this.indexed = indexed;
		}

		public Object getDefaultValue() {
			return defaultValue;
		}

		public void setDefaultValue(Object defaultValue) {
			this.defaultValue = defaultValue;
		}

		public List<String> getCopyFields() {
			return copyFields;
		}

		public void setCopyFields(Collection<String> copyFields) {
			this.copyFields = new ArrayList<String>(copyFields);
		}

		public List<Filter> getFilters() {
			return filters;
		}

		public void setFilters(List<Filter> filters) {
			this.filters = filters;
		}

		public List<Tokenizer> getTokenizers() {
			return tokenizers;
		}

		public void setTokenizers(List<Tokenizer> tokenizers) {
			this.tokenizers = tokenizers;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isMultiValued() {
			return multiValued;
		}

		public void setMultiValued(boolean multiValued) {
			this.multiValued = multiValued;
		}

		public boolean isRequired() {
			return required;
		}

		public void setRequired(boolean required) {
			this.required = required;
		}

	}

	/**
	 * @author Christoph Strobl
	 * @since 1.3
	 */
	public static class CopyFieldDefinition implements SchemaField {

		String source;
		List<String> destination;
	}

	/**
	 * @author Christoph Strobl
	 * @since 1.3
	 */
	public static class Filter {

		String clazz;
		String pattern;
		String replace;
		String replacement;
	}

	/**
	 * @author Christoph Strobl
	 * @since 1.3
	 */
	public static class Tokenizer {

		String clazz;
	}

	public static class FieldDefinitionBuilder {

		private FieldDefinition fieldDef;

		public FieldDefinitionBuilder() {
			this.fieldDef = new FieldDefinition();
		}

		public FieldDefinition idFieldDefinition(String fieldname, String type) {

			fieldDef.setName(fieldname);
			fieldDef.setType(type);
			fieldDef.setIndexed(true);
			fieldDef.setStored(true);
			fieldDef.setMultiValued(false);

			return fieldDef;
		}
	}

}
