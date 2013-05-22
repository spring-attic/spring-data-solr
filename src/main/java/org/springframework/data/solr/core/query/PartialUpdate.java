/*
 * Copyright 2012 - 2013 the original author or authors.
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
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link Update} to be used when performing atomic updates against solr. <br />
 * Update can directly be saved via {@link org.springframework.data.solr.core.SolrOperations#saveBean(Object)}
 * 
 * 
 * @author Christoph Strobl
 */
public class PartialUpdate implements Update {

	private final ValueHoldingField idField;
	private Object version;
	private final List<UpdateField> updates = new ArrayList<UpdateField>();

	public PartialUpdate(String idFieldName, Object idFieldValue) {
		this(new IdField(idFieldName, idFieldValue));
	}

	public PartialUpdate(Field idField, Object idFieldValue) {
		this(new IdField(idField.getName(), idFieldValue));
	}

	PartialUpdate(IdField idField) {
		this.idField = idField;
	}

	@Override
	public ValueHoldingField getIdField() {
		return this.idField;
	}

	/**
	 * Add field with given name and value to the fields to be updated. Default {@link UpateAction} will be
	 * {@link UpateAction.SET}.
	 * 
	 * @param fieldName
	 * @param value
	 */
	public void add(String fieldName, Object value) {
		add(new SimpleUpdateField(fieldName, value));
	}

	/**
	 * Add {@link UpdateField} to the list of fields to be updated
	 * 
	 * @param field
	 */
	public void add(UpdateField field) {
		this.updates.add(field);
	}

	/**
	 * Add field with given name and value using {@link UpateAction.ADD} to the fields to be updated.
	 * 
	 * @param fieldName
	 * @param value
	 */
	public void addValueToField(String fieldName, Object value) {
		add(new SimpleUpdateField(fieldName, value, UpdateAction.ADD));
	}

	/**
	 * Add field with given name and value using {@link UpateAction.SET} to the fields to be updated.
	 * 
	 * @param fieldName
	 * @param value
	 */
	public void setValueOfField(String fieldName, Object value) {
		add(new SimpleUpdateField(fieldName, value, UpdateAction.SET));
	}

	/**
	 * Add field with given name and value using {@link UpateAction.INC} to the fields to be updated.
	 * 
	 * @param fieldName
	 * @param value
	 */
	public void increaseValueOfField(String fieldName, Object value) {
		add(new SimpleUpdateField(fieldName, value, UpdateAction.INC));
	}

	@Override
	public List<UpdateField> getUpdates() {
		return Collections.unmodifiableList(updates);
	}

	@Override
	public Object getVersion() {
		return this.version;
	}

	/**
	 * set {@code _version_} of document to apply update to. Use null to skip version check in solr.
	 * 
	 * @param documentVersion
	 */
	public void setVersion(Object documentVersion) {
		this.version = documentVersion;
	}

	static class IdField extends AbstractValueHoldingField {

		public IdField(String fieldName, Object fieldValue) {
			super(fieldName, fieldValue);
		}

	}

}
