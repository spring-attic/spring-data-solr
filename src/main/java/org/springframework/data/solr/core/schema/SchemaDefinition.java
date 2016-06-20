/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class SchemaDefinition {

	private String collectionName;
	private List<FieldDefinition> fields;
	private List<CopyFieldDefinition> copyFields;
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

	public void addCopyField(CopyFieldDefinition copyField) {

		if (this.copyFields == null) {
			this.copyFields = new ArrayList<CopyFieldDefinition>();
		}
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
	public static interface SchemaField {}

	/**
	 * @author Christoph Strobl
	 * @since 1.3
	 */
	@Data
	@NoArgsConstructor
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

		public FieldDefinition(String name) {
			this.name = name;
		}

		public void setCopyFields(Collection<String> copyFields) {
			this.copyFields = new ArrayList<String>(copyFields);
		}

		/**
		 * @return
		 * @since 2.1
		 */
		public Map<String, Object> asMap() {

			Map<String, Object> values = new LinkedHashMap<String, Object>();
			addIfNotNull("name", name, values);
			addIfNotNull("type", type, values);
			addIfNotNull("indexed", indexed, values);
			addIfNotNull("stored", stored, values);
			addIfNotNull("multiValued", multiValued, values);
			addIfNotNull("default", defaultValue, values);
			addIfNotNull("required", required, values);
			return values;
		}

		private void addIfNotNull(String key, Object value, Map<String, Object> dest) {
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

		private static <T> T valueFromMap(String key, Map<String, Object> source, T defaultValue) {

			if (source.containsKey(key)) {
				return (T) source.get(key);
			} else {
				return defaultValue;
			}
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
	@Data
	public static class CopyFieldDefinition implements SchemaField {

		String source;
		List<String> destination;

		public static CopyFieldDefinition fromMap(Map<String, Object> fieldValueMap) {

			CopyFieldDefinition cfd = new CopyFieldDefinition();
			cfd.source = (String) fieldValueMap.get("source");

			Object dest = fieldValueMap.get("dest");
			if (dest instanceof Collection) {
				cfd.destination = new ArrayList<String>((Collection<String>) dest);
			} else if (fieldValueMap.get("dest") instanceof String) {
				cfd.destination = Collections.<String> singletonList(dest.toString());
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
					ArrayList<String> values = new ArrayList<String>(cf.getDestination());
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
