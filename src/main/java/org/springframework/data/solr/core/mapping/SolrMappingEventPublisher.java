/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.solr.core.mapping;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mapping.context.MappingContextEvent;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator;

/**
 * @author Christoph Strobl
 * @since 1.3
 */
public class SolrMappingEventPublisher implements ApplicationEventPublisher {

	SolrPersistentEntitySchemaCreator schmeaCreator;

	public SolrMappingEventPublisher(SolrPersistentEntitySchemaCreator schemaCreator) {
		this.schmeaCreator = schemaCreator;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationEventPublisher#publishEvent(org.springframework.context.ApplicationEvent)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void publishEvent(ApplicationEvent event) {

		if (event instanceof MappingContextEvent) {
			this.schmeaCreator
					.onApplicationEvent((MappingContextEvent<SolrPersistentEntity<?>, SolrPersistentProperty>) event);
		}
	}
}
