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

import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @since 1.1
 */
public class SimpleCalculatedField implements CalculatedField {

	private Function function;
	private String alias;

	public SimpleCalculatedField(Function function) {
		this(null, function);
	}

	public SimpleCalculatedField(String alias, Function function) {
		Assert.notNull(function, "Function cannot be empty.");

		this.alias = alias;
		this.function = function;
	}

	@Override
	public String getName() {
		return this.alias;
	}

	@Override
	public Function getFunction() {
		return this.function;
	}

	@Override
	public String getAlias() {
		return this.alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

}
