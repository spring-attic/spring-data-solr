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

import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 * @since 1.1
 */
public abstract class AbstractFunction implements Function {

	@SuppressWarnings("rawtypes")
	private final List arguments;

	@SuppressWarnings("rawtypes")
	protected AbstractFunction() {
		this(new ArrayList(0));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected AbstractFunction(List<?> arguments) {
		this.arguments = new ArrayList();
		if (!CollectionUtils.isEmpty(arguments)) {
			this.arguments.addAll(arguments);
		}
	}

	@SuppressWarnings("unchecked")
	protected void addArgument(Object argument) {
		this.arguments.add(argument);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<?> getArguments() {
		return Collections.unmodifiableList(this.arguments);
	}

	@Override
	public boolean hasArguments() {
		return !CollectionUtils.isEmpty(arguments);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [arguments=" + arguments + "]";
	}

}
