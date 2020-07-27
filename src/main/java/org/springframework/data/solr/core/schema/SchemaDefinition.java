/*
 * Copyright 2014-2020 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class SchemaDefinition {

	private @Nullable String collectionName;
	private List<FieldDefinition> fields = new ArrayList<>();
	private List<CopyFieldDefinition> copyFields = new ArrayList<>();
	private @Nullable String name;
	private @Nullable Double version;
	private @Nullable String uniqueKey;

	public SchemaDefinition() {}

	public SchemaDefinition(String collectionName) {
		this.collectionName = collectionName;
		this.fields = new ArrayList<>();
	}

	@Nullable
	public String getCollectionName() {
		return collectionName;
	}

	public List<FieldDefinition> getFields() {
		return fields;
	}

	public void setFields(@Nullable List<FieldDefinition> fields) {
		this.fields = fields != null ? fields : new ArrayList<>();
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Nullable
	public Double getVersion() {
		return version;
	}

	public void setVersion(Double version) {
		this.version = version;
	}

	@Nullable
	public String getUniqueKey() {
		return uniqueKey;
	}

	public void setUniqueKey(String uniqueKey) {
		this.uniqueKey = uniqueKey;
	}

	public boolean containsField(String name) {
		return getFieldDefinition(name) != null;
	}

	@Nullable
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
		this.fields.add(fieldDef);
	}

	public void addCopyField(CopyFieldDefinition copyField) {
		this.copyFields.add(copyField);
	}

	public List<CopyFieldDefinition> getCopyFields() {
		return this.copyFields;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	/**
	 * @author Christoph Strobl
	 * @since 1.3
	 */
	public interface SchemaField {}

	/**
	 * @author Christoph Strobl
	 * @since 1.3
	 */
	public static class FieldDefinition implements SchemaField {

		private @Nullable String name;
		private @Nullable String type;
		private boolean stored;
		private boolean indexed;
		private @Nullable Object defaultValue;
		private List<String> copyFields = Collections.emptyList();
		private List<Filter> filters = Collections.emptyList();
		private List<Tokenizer> tokenizers = Collections.emptyList();
		private boolean multiValued;
		private boolean required;

		public FieldDefinition(String name) {
			this.name = name;
		}

		public FieldDefinition() {}

		public void setCopyFields(Collection<String> copyFields) {
			this.copyFields = new ArrayList<>(copyFields);
		}

		/**
		 * @return
		 * @since 2.1
		 */
		public Map<String, Object> asMap() {

			Map<String, Object> values = new LinkedHashMap<>();
			addIfNotNull("name", name, values);
			addIfNotNull("type", type, values);
			addIfNotNull("indexed", indexed, values);
			addIfNotNull("stored", stored, values);
			addIfNotNull("multiValued", multiValued, values);
			addIfNotNull("default", defaultValue, values);
			addIfNotNull("required", required, values);
			return values;
		}

		private void addIfNotNull(String key, @Nullable Object value, Map<String, Object> dest) {
			if (value != null) {
				dest.put(key, value);
			}
		}

		/**
		 * @param source
		 * @return
		 * @since 2.1
		 */
		public static FieldDefinition fromMap(Map<String, Object> source) {

			FieldDefinition fd = new FieldDefinition();
			if (!CollectionUtils.isEmpty(source)) {

				fd.name = valueFromMap("name", source, null);
				fd.type = valueFromMap("type", source, null);
				fd.indexed = valueFromMap("indexed", source, false);
				fd.stored = valueFromMap("stored", source, false);
				fd.multiValued = valueFromMap("multiValued", source, false);
				fd.required = valueFromMap("required", source, false);
				fd.defaultValue = valueFromMap("default", source, null);
			}
			return fd;
		}

		private static <T> T valueFromMap(String key, Map<String, Object> source, @Nullable T defaultValue) {

			if (source.containsKey(key)) {
				return (T) source.get(key);
			} else {
				return defaultValue;
			}
		}

		@Nullable
		public String getName() {
			return this.name;
		}

		@Nullable
		public String getType() {
			return this.type;
		}

		public boolean isStored() {
			return this.stored;
		}

		public boolean isIndexed() {
			return this.indexed;
		}

		@Nullable
		public Object getDefaultValue() {
			return this.defaultValue;
		}

		public List<String> getCopyFields() {
			return this.copyFields;
		}

		public List<Filter> getFilters() {
			return this.filters;
		}

		public List<Tokenizer> getTokenizers() {
			return this.tokenizers;
		}

		public boolean isMultiValued() {
			return this.multiValued;
		}

		public boolean isRequired() {
			return this.required;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}

		public void setStored(boolean stored) {
			this.stored = stored;
		}

		public void setIndexed(boolean indexed) {
			this.indexed = indexed;
		}

		public void setDefaultValue(@Nullable Object defaultValue) {
			this.defaultValue = defaultValue;
		}

		public void setFilters(List<Filter> filters) {
			this.filters = filters;
		}

		public void setTokenizers(List<Tokenizer> tokenizers) {
			this.tokenizers = tokenizers;
		}

		public void setMultiValued(boolean multiValued) {
			this.multiValued = multiValued;
		}

		public void setRequired(boolean required) {
			this.required = required;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof FieldDefinition)) {
				return false;
			}
			FieldDefinition that = (FieldDefinition) o;
			if (stored != that.stored) {
				return false;
			}
			if (indexed != that.indexed) {
				return false;
			}
			if (multiValued != that.multiValued) {
				return false;
			}
			if (required != that.required) {
				return false;
			}
			if (!ObjectUtils.nullSafeEquals(name, that.name)) {
				return false;
			}
			if (!ObjectUtils.nullSafeEquals(type, that.type)) {
				return false;
			}
			if (!ObjectUtils.nullSafeEquals(defaultValue, that.defaultValue)) {
				return false;
			}
			if (!ObjectUtils.nullSafeEquals(copyFields, that.copyFields)) {
				return false;
			}
			if (!ObjectUtils.nullSafeEquals(filters, that.filters)) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(tokenizers, that.tokenizers);
		}

		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(name);
			result = 31 * result + ObjectUtils.nullSafeHashCode(type);
			result = 31 * result + (stored ? 1 : 0);
			result = 31 * result + (indexed ? 1 : 0);
			result = 31 * result + ObjectUtils.nullSafeHashCode(defaultValue);
			result = 31 * result + ObjectUtils.nullSafeHashCode(copyFields);
			result = 31 * result + ObjectUtils.nullSafeHashCode(filters);
			result = 31 * result + ObjectUtils.nullSafeHashCode(tokenizers);
			result = 31 * result + (multiValued ? 1 : 0);
			result = 31 * result + (required ? 1 : 0);
			return result;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof FieldDefinition;
		}

		public String toString() {
			return "SchemaDefinition.FieldDefinition(name=" + this.name + ", type=" + this.type + ", stored=" + this.stored
					+ ", indexed=" + this.indexed + ", defaultValue=" + this.defaultValue + ", copyFields=" + this.copyFields
					+ ", filters=" + this.filters + ", tokenizers=" + this.tokenizers + ", multiValued=" + this.multiValued
					+ ", required=" + this.required + ")";
		}

		/**
		 * @author Christoph Strobl
		 * @since 2.1
		 */
		public static class Builder {

			FieldDefinition fd;

			public Builder() {
				fd = new FieldDefinition();
			}

			public Builder named(String name) {
				fd.setName(name);
				return this;
			}

			public Builder stored() {
				fd.setStored(true);
				return this;
			}

			public Builder indexed() {
				fd.setIndexed(true);
				return this;
			}

			public Builder muliValued() {
				fd.setMultiValued(true);
				return this;
			}

			public Builder copyTo(String... fields) {
				fd.setCopyFields(Arrays.asList(fields));
				return this;
			}

			public Builder required() {
				fd.setRequired(true);
				return this;
			}

			public Builder typedAs(String type) {
				fd.setType(type);
				return this;
			}

			public Builder defaultedTo(Object value) {
				fd.setDefaultValue(value);
				return this;
			}

			public FieldDefinition create() {
				return fd;
			}

		}

		/**
		 * @return
		 * @since 2.1
		 */
		public static Builder newFieldDefinition() {
			return new Builder();
		}

	}

	/**
	 * @author Christoph Strobl
	 * @since 1.3
	 */
	public static class CopyFieldDefinition implements SchemaField {

		@Nullable String source;
		List<String> destination = Collections.emptyList();

		public CopyFieldDefinition() {}

		public static CopyFieldDefinition fromMap(Map<String, Object> fieldValueMap) {

			CopyFieldDefinition cfd = new CopyFieldDefinition();
			cfd.source = (String) fieldValueMap.get("source");

			Object dest = fieldValueMap.get("dest");
			if (dest instanceof Collection) {
				cfd.destination = new ArrayList<>((Collection<String>) dest);
			} else if (fieldValueMap.get("dest") instanceof String) {
				cfd.destination = Collections.singletonList(dest.toString());
			} else {
				cfd.destination = Collections.emptyList();
			}

			return cfd;
		}

		/**
		 * @return
		 * @since 2.1
		 */
		public static Builder newCopyFieldDefinition() {
			return new Builder();
		}

		@Nullable
		public String getSource() {
			return this.source;
		}

		public List<String> getDestination() {
			return this.destination;
		}

		public void setSource(@Nullable String source) {
			this.source = source;
		}

		public void setDestination(List<String> destination) {
			this.destination = destination;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof CopyFieldDefinition)) {
				return false;
			}
			CopyFieldDefinition that = (CopyFieldDefinition) o;
			if (!ObjectUtils.nullSafeEquals(source, that.source)) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(destination, that.destination);
		}

		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(source);
			result = 31 * result + ObjectUtils.nullSafeHashCode(destination);
			return result;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof CopyFieldDefinition;
		}

		public String toString() {
			return "SchemaDefinition.CopyFieldDefinition(source=" + this.source + ", destination=" + this.destination + ")";
		}

		/**
		 * @author Christoph Strobl
		 * @since 2.1
		 */
		public static class Builder {

			CopyFieldDefinition cf;

			public Builder() {
				cf = new CopyFieldDefinition();
			}

			public Builder copyFrom(String source) {
				cf.setSource(source);
				return this;
			}

			public Builder to(String... destinations) {

				if (cf.getDestination() == null) {
					cf.setDestination(Arrays.asList(destinations));
				} else {
					ArrayList<String> values = new ArrayList<>(cf.getDestination());
					CollectionUtils.mergeArrayIntoCollection(destinations, values);
					cf.setDestination(values);
				}
				return this;
			}

			public CopyFieldDefinition create() {
				return cf;
			}
		}
	}

	/**
	 * @author Christoph Strobl
	 * @since 1.3
	 */
	public static class Filter {

		@Nullable String clazz;
		@Nullable String pattern;
		@Nullable String replace;
		@Nullable String replacement;
	}

	/**
	 * @author Christoph Strobl
	 * @since 1.3
	 */
	public static class Tokenizer {

		@Nullable String clazz;
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
