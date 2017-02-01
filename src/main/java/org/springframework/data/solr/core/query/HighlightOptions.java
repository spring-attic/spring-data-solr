/*
 * Copyright 2012 - 2017 the original author or authors.
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
package org.springframework.data.solr.core.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.solr.common.params.HighlightParams;
import org.springframework.util.Assert;

/**
 * Empty Options indicate to set {@code hl=true}. As long as there are no fields defined {@code *} will be used. Some
 * options like {@see HighlightOptions#setFormatter(String)} can be set directly. Any option can be set via
 * {@see HighlightOptions#addHighlightParameter(HighlightParameter)}.
 * 
 * @author Christoph Strobl
 */
public class HighlightOptions {

	public static final Field ALL_FIELDS = new Field() {

		@Override
		public String getName() {
			return Criteria.WILDCARD;
		}
	};

	private final ParameterHolder<HighlightParameter> parameterHolder = new ParameterHolder<HighlightParameter>();
	private FilterQuery query;
	private final List<Field> fields = new ArrayList<Field>(1);

	/**
	 * Add field to highlight
	 * 
	 * @param field
	 * @return
	 */
	public HighlightOptions addField(Field field) {
		Assert.notNull(field, "Field must not be null!");
		this.fields.add(field);
		return this;
	}

	/**
	 * Add name of field to highlight on
	 * 
	 * @param fieldname
	 * @return
	 */
	public HighlightOptions addField(String fieldname) {
		Assert.hasText(fieldname, "Fieldname must not be null nor empty!");
		return addField(new SimpleField(fieldname));
	}

	/**
	 * Add names of fields to highlight on
	 * 
	 * @param fieldnames
	 * @return
	 */
	public HighlightOptions addField(String... fieldnames) {
		Assert.notNull(fieldnames, "Fieldnames must not be null!");

		for (String fieldname : fieldnames) {
			addField(fieldname);
		}
		return this;
	}

	/**
	 * Add names of fields to highlight on
	 * 
	 * @param fieldnames
	 * @return
	 */
	public HighlightOptions addFields(Collection<String> fieldnames) {
		Assert.notNull(fieldnames, "Fieldnames must not be null!");

		for (String fieldname : fieldnames) {
			addField(fieldname);
		}
		return this;
	}

	/**
	 * @return null if not set
	 */
	public FilterQuery getQuery() {
		return this.query;
	}

	/**
	 * Set {@see FilterQuery} to be used for {@code hl.q}
	 * 
	 * @param query
	 */
	public HighlightOptions setQuery(FilterQuery query) {
		this.query = query;
		return this;
	}

	/**
	 * @return null if not set
	 */
	public Integer getFragsize() {
		return this.parameterHolder.getParameterValue(HighlightParams.FRAGSIZE);
	}

	/**
	 * set fragsize {@code hl.fragsize}.
	 * 
	 * @param fragsize
	 */
	public HighlightOptions setFragsize(Integer fragsize) {
		addHighlightParameter(HighlightParams.FRAGSIZE, fragsize);
		return this;
	}

	/**
	 * @return null if not set
	 */
	public String getFormatter() {
		return this.parameterHolder.getParameterValue(HighlightParams.FORMATTER);
	}

	/**
	 * set formatter {@code hl.formatter}
	 * 
	 * @param formatter
	 */
	public HighlightOptions setFormatter(String formatter) {
		addHighlightParameter(HighlightParams.FORMATTER, formatter);
		return this;
	}

	/**
	 * @return null if not set
	 */
	public Integer getNrSnipplets() {
		return this.parameterHolder.getParameterValue(HighlightParams.SNIPPETS);
	}

	/**
	 * set {@code hl.snippets}
	 * 
	 * @param nrSnipplets
	 */
	public HighlightOptions setNrSnipplets(Integer nrSnipplets) {
		addHighlightParameter(HighlightParams.SNIPPETS, nrSnipplets);
		return this;
	}

	/**
	 * set {@code hl.simple.pre}
	 * 
	 * @param prefix
	 */
	public HighlightOptions setSimplePrefix(String prefix) {
		addHighlightParameter(HighlightParams.SIMPLE_PRE, prefix);
		return this;
	}

	/**
	 * @return
	 */
	public String getSimplePrefix() {
		return this.parameterHolder.getParameterValue(HighlightParams.SIMPLE_PRE);
	}

	/**
	 * set {@code hl.simple.post}
	 * 
	 * @param postfix
	 */
	public HighlightOptions setSimplePostfix(String postfix) {
		addHighlightParameter(HighlightParams.SIMPLE_POST, postfix);
		return this;
	}

	/**
	 * @return
	 */
	public String getSimplePostfix() {
		return this.parameterHolder.getParameterValue(HighlightParams.SIMPLE_POST);
	}

	/**
	 * @return unmodifiable list of fields
	 */
	public List<Field> getFields() {
		return Collections.unmodifiableList(fields);
	}

	/**
	 * @return collection of all parameters
	 */
	public Collection<HighlightParameter> getHighlightParameters() {
		return this.parameterHolder.getParameters();
	}

	/**
	 * Add parameter by name
	 * 
	 * @param parameterName must not be null
	 * @param value
	 * @return
	 */
	public HighlightOptions addHighlightParameter(String parameterName, Object value) {
		return addHighlightParameter(new HighlightParameter(parameterName, value));
	}

	/**
	 * Add parameter
	 * 
	 * @param parameter must not be null
	 * @return
	 */
	public HighlightOptions addHighlightParameter(HighlightParameter parameter) {
		Assert.notNull(parameter, "Parameter must not be null!");
		this.parameterHolder.add(parameter);
		return this;
	}

	/**
	 * Get value of parameter with given type
	 * 
	 * @param parameterName
	 * @return null if not present
	 */
	public <S> S getHighlightParameterValue(String parameterName) {
		return this.parameterHolder.getParameterValue(parameterName);
	}

	/**
	 * Get Collection of fields that have field specific highlight options.
	 * 
	 * @return
	 */
	public Collection<FieldWithHighlightParameters> getFieldsWithHighlightParameters() {

		List<FieldWithHighlightParameters> result = new ArrayList<FieldWithHighlightParameters>();
		for (Field candidate : fields) {

			if (candidate instanceof FieldWithHighlightParameters) {
				result.add((FieldWithHighlightParameters) candidate);
			}
		}

		return result;
	}

	/**
	 * @return true if query is not null
	 */
	public boolean hasQuery() {
		return this.query != null;
	}

	/**
	 * @return true if at least one field available
	 */
	public boolean hasFields() {
		return !this.fields.isEmpty();
	}

	/**
	 * Query Parameter to be used for highlighting
	 * 
	 * @author Christoph Strobl
	 */
	public static class HighlightParameter extends QueryParameterImpl {

		public HighlightParameter(String parameter, Object value) {
			super(parameter, value);
		}

	}

	/**
	 * Field with hightlight query parameters
	 * 
	 * @author Christoph Strobl
	 */
	public static class FieldWithHighlightParameters extends FieldWithQueryParameters<HighlightParameter> {

		/**
		 * @param fieldname must not be null/blank
		 */
		public FieldWithHighlightParameters(String fieldname) {
			super(fieldname);
		}

		/**
		 * @return null if not set
		 */
		public Integer getNrSnipplets() {
			return getQueryParameterValue(HighlightParams.SNIPPETS);
		}

		/**
		 * set fragsize {@code hl.fragsize}.
		 * 
		 * @param fragsize
		 */
		public FieldWithHighlightParameters setNrSnipplets(Integer nrSnipplets) {
			addHighlightParameter(HighlightParams.SNIPPETS, nrSnipplets);
			return this;
		}

		/**
		 * @return null if not set
		 */
		public Integer getFragsize() {
			return getQueryParameterValue(HighlightParams.FRAGSIZE);
		}

		/**
		 * set fragsize {@code f.&lt;fieldname&gt;.hl.fragsize}.
		 * 
		 * @param fragsize
		 */
		public FieldWithHighlightParameters setFragsize(Integer fragsize) {
			addHighlightParameter(HighlightParams.FRAGSIZE, fragsize);
			return this;
		}

		/**
		 * @return null if not set
		 */
		public Boolean isMergeContigous() {
			return getQueryParameterValue(HighlightParams.MERGE_CONTIGUOUS_FRAGMENTS);
		}

		/**
		 * set fragsize {@code f.&lt;fieldname&gt;.hl.fragsize}.
		 * 
		 * @param fragsize
		 */
		public FieldWithHighlightParameters setMergeContigous(Boolean mergeContigous) {
			addHighlightParameter(HighlightParams.MERGE_CONTIGUOUS_FRAGMENTS, mergeContigous);
			return this;
		}

		/**
		 * @return null if not set
		 */
		public String getFormatter() {
			return getQueryParameterValue(HighlightParams.FORMATTER);
		}

		/**
		 * set fragsize {@code f.&lt;formatter&gt;.hl.fragsize}.
		 * 
		 * @param fragsize
		 */
		public FieldWithHighlightParameters setFormatter(String formatter) {
			addHighlightParameter(HighlightParams.FORMATTER, formatter);
			return this;
		}

		/**
		 * Add field specific parameter by name
		 * 
		 * @param parameterName
		 * @param value
		 */
		public FieldWithHighlightParameters addHighlightParameter(String parameterName, Object value) {
			return this.addHighlightParameter(new HighlightParameter(parameterName, value));
		}

		/**
		 * Add field specific highlight parameter
		 * 
		 * @param parameter
		 * @return
		 */
		public FieldWithHighlightParameters addHighlightParameter(HighlightParameter parameter) {
			this.addQueryParameter(parameter);
			return this;
		}

	}

}
