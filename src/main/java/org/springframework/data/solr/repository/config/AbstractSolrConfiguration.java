/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.solr.repository.config;

import java.util.Collection;
import java.util.Collections;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.solr.core.RequestMethod;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.convert.MappingSolrConverter;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.core.convert.SolrCustomConversions;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator;
import org.springframework.data.solr.core.schema.SolrPersistentEntitySchemaCreator.Feature;
import org.springframework.data.solr.server.SolrClientFactory;

/**
 * Spring Data for Apache Solr base configuration using JavaConfig.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.0
 */
@Configuration
public abstract class AbstractSolrConfiguration {

	/**
	 * {@link SolrTemplate} setup to provide {@link org.springframework.data.solr.core.SolrOperations} implementation
	 * picking up {@link #solrClientFactory}, {@link #solrConverter()} and {@link #defaultRequestMethod()}.
	 *
	 * @return
	 */
	@Bean
	public SolrTemplate solrTemplate() {
		return new SolrTemplate(solrClientFactory(), solrConverter(), defaultRequestMethod());
	}

	/**
	 * @return {@link MappingSolrConverter} picking up {@link #customConversions()} by default.
	 */
	@Bean
	public SolrConverter solrConverter() {

		MappingSolrConverter solrConverter = new MappingSolrConverter(solrMappingContext());
		solrConverter.setCustomConversions(customConversions());
		return solrConverter;
	}

	/**
	 * @return {@link SimpleSolrMappingContext} picking up {@link #solrClientFactory()} and {@link #schemaSupport()} by
	 *         default.
	 */
	@Bean
	protected MappingContext solrMappingContext() {

		return new SimpleSolrMappingContext(
				new SolrPersistentEntitySchemaCreator(solrClientFactory()).enable(schemaSupport()));
	}

	/**
	 * Define the {@link SolrClientFactory} to be used. <br />
	 * Unless you use an {@link org.apache.solr.client.solrj.embedded.EmbeddedSolrServer} simply
	 * {@code () -> new HttpSolrClient("...")} should be sufficient.
	 *
	 * @return Never {@literal null}.
	 */
	@Bean
	public abstract SolrClientFactory solrClientFactory();

	/**
	 * {@link CustomConversions} to be applied by mapping.
	 *
	 * @return CustomConversions by default. Never {@literal null}.
	 */
	protected CustomConversions customConversions() {
		return new SolrCustomConversions(Collections.emptyList());
	}

	/**
	 * Default {@link RequestMethod} to be used when sending requests via {@link SolrClient}.
	 *
	 * @return {@link RequestMethod#GET} by default. Never {@literal null}.
	 */
	protected RequestMethod defaultRequestMethod() {
		return RequestMethod.GET;
	}

	/**
	 * Define schema setup {@link Feature}s.
	 *
	 * @return empty {@link java.util.Set} by default. Never {@literal null}.
	 */
	protected Collection<Feature> schemaSupport() {
		return Collections.emptySet();
	}
}
