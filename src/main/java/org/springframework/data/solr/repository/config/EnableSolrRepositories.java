/*
 * Copyright 2012 - 2016 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.support.SolrRepositoryFactoryBean;

/**
 * Annotation to enable Solr repositories. Will scan the package of the annotated configuration class for Spring Data
 * repositories by default.
 * 
 * @author Oliver Gierke
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(SolrRepositoriesRegistrar.class)
public @interface EnableSolrRepositories {

	/**
	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
	 * {@code @EnableSolrRepositories("org.my.pkg")} instead of {@code @EnableSolrRpositories(basePackages="org.my.pkg")}.
	 */
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually exclusive with) this
	 * attribute. Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
	 * package of each class specified will be scanned. Consider creating a special no-op marker class or interface in
	 * each package that serves no purpose other than being referenced by this attribute.
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Specifies which types are eligible for component scanning. Further narrows the set of candidate components from
	 * everything in {@link #basePackages()} to everything in the base packages that matches the given filter or filters.
	 */
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 */
	Filter[] excludeFilters() default {};

	/**
	 * Returns the postfix to be used when looking up custom repository implementations. Defaults to {@literal Impl}. So
	 * for a repository named {@code PersonRepository} the corresponding implementation class will be looked up scanning
	 * for {@code PersonRepositoryImpl}.
	 * 
	 * @return
	 */
	String repositoryImplementationPostfix() default "Impl";

	/**
	 * Configures the location of where to find the Spring Data named queries properties file. Will default to
	 * {@code META-INFO/solr-named-queries.properties}.
	 * 
	 * @return
	 */
	String namedQueriesLocation() default "";

	/**
	 * Returns the key of the {@link QueryLookupStrategy} to be used for lookup queries for query methods. Defaults to
	 * {@link Key#CREATE_IF_NOT_FOUND}.
	 * 
	 * @return
	 */
	Key queryLookupStrategy() default Key.CREATE_IF_NOT_FOUND;

	/**
	 * Returns the {@link FactoryBean} class to be used for each repository instance. Defaults to
	 * {@link SolrRepositoryFactoryBean}.
	 * 
	 * @return
	 */
	Class<?> repositoryFactoryBeanClass() default SolrRepositoryFactoryBean.class;

	/**
	 * Configure the repository base class to be used to create repository proxies for this particular configuration.
	 * 
	 * @return
	 * @since 1.5
	 */
	Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

	// Solr specific configuration

	/**
	 * Configures the name of the {@link SolrTemplate} bean definition to be used to create repositories discovered
	 * through this annotation. Defaults to {@code solrTemplate}.
	 * 
	 * @return
	 */
	String solrTemplateRef() default "solrTemplate";

	/**
	 * Configures the name of the {@link org.apache.solr.client.solrj} bean definition to be used for creating templates
	 * 
	 * @since 1.1
	 */
	String solrClientRef() default "solrClient";

	/**
	 * Configures Multicore support for Solr Server
	 * 
	 * @since 1.1
	 */
	boolean multicoreSupport() default false;

	/**
	 * Configure schema creation support using the solr schema api.
	 * 
	 * @since 1.3
	 */
	boolean schemaCreationSupport() default false;

	/**
	 * Configures whether nested repository-interfaces (e.g. defined as inner classes) should be discovered by the
	 * repositories infrastructure.
	 *
	 * @since 2.1
	 */
	boolean considerNestedRepositories() default false;
}
