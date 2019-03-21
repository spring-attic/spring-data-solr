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
package org.springframework.data.solr.core;

import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

/**
 * @author Francisco Spaeth
 * @author Christoph Strobl
 */
@SolrDocument(boost = 0.8f)
public class SimpleBoostedJavaObject extends SimpleJavaObject {

	private @Indexed(boost = 0.7f) String boostedField;

	public SimpleBoostedJavaObject() {
		super();
	}

	public SimpleBoostedJavaObject(String id, Long value, String stringValue) {
		super(id, value);
		this.boostedField = stringValue;
	}

	public String getBoostedField() {
		return boostedField;
	}

	public void setBoostedField(String boostedField) {
		this.boostedField = boostedField;
	}

}
