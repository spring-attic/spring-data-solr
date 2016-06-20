/*
 * Copyright 2016 the original author or authors.
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

import org.springframework.data.solr.core.schema.SchemaDefinition.SchemaField;

/**
 * Operations interface for executing modification on a managed schema.
 *
 * @author Christoph Strobl
 * @since 2.1
 */
public interface SchemaOperations {

	/**
	 * Get the current schema name.
	 *
	 * @return
	 */
	String getSchemaName();

	/**
	 * Get the current schema version.
	 *
	 * @return
	 */
	Double getSchemaVersion();

	/**
	 * Read back the {@link SchemaDefinition} from server.
	 *
	 * @return
	 */
	SchemaDefinition readSchema();

	/**
	 * Add given {@link SchemaField}.
	 *
	 * @param field must not be {@literal null}.
	 * @throws SchemaModificationException
	 */
	void addField(SchemaField field);

	/**
	 * Remove the field with given name.
	 *
	 * @param name must not be {@literal null}.
	 */
	void removeField(String name);

}
