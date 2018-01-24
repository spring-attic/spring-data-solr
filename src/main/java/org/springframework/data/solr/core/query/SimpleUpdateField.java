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

/**
 * Implementation of {@link UpdateField} to be used with {@link Update}
 * 
 * @author Christoph Strobl
 */
public class SimpleUpdateField extends AbstractValueHoldingField implements UpdateField {

	private static final UpdateAction DEFAULT_ACTION = UpdateAction.SET;
	private UpdateAction action;

	public SimpleUpdateField(String name) {
		this(name, null);
	}

	/**
	 * Creates new instance with {@link #DEFAULT_ACTION}
	 * @param name 
	 * @param value
	 */
	public SimpleUpdateField(String name, Object value) {
		this(name, value, DEFAULT_ACTION);
	}

	/**
	 * @param name
	 * @param value
	 * @param action
	 */
	public SimpleUpdateField(String name, Object value, UpdateAction action) {
		super(name, value);
		this.action = action;
	}

	@Override
	public UpdateAction getAction() {
		return this.action != null ? this.action : DEFAULT_ACTION;
	}

}
