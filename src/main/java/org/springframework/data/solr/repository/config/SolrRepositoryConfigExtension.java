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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.data.solr.core.SolrExceptionTranslator;
import org.springframework.data.solr.core.convert.CustomConversions;
import org.springframework.data.solr.core.convert.MappingSolrConverter;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.mapping.SolrDocument;
import org.springframework.data.solr.repository.SolrCrudRepository;
import org.springframework.data.solr.repository.SolrRepository;
import org.springframework.data.solr.repository.support.SolrRepositoryFactoryBean;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * {@link RepositoryConfigurationExtension} implementation to configure Solr repository configuration support,
 * evaluating the {@link EnableSolrRepositories} annotation or the equivalent XML element.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class SolrRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

	enum BeanDefinition {
		SOLR_MAPPTING_CONTEXT("solrMappingContext"), SOLR_OPERATIONS("solrOperations"), SOLR_CLIENT(
				"solrClient"), SOLR_CONVERTER("solrConverter"), CUSTOM_CONVERSIONS("customConversions");
		String beanName;

		private BeanDefinition(String beanName) {
			this.beanName = beanName;
		}

		public String getBeanName() {
			return beanName;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtension#getRepositoryFactoryClassName()
	 */
	@Override
	public String getRepositoryFactoryClassName() {
		return SolrRepositoryFactoryBean.class.getName();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#getModulePrefix()
	 */
	@Override
	protected String getModulePrefix() {
		return "solr";
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {

		AnnotationAttributes attributes = config.getAttributes();
		if (!attributes.getBoolean("multicoreSupport")) {
			builder.addPropertyReference(BeanDefinition.SOLR_OPERATIONS.getBeanName(),
					attributes.getString("solrTemplateRef"));
		} else {
			builder.addPropertyReference(BeanDefinition.SOLR_CLIENT.getBeanName(), attributes.getString("solrClientRef"));
		}
		builder.addPropertyValue("schemaCreationSupport", attributes.getBoolean("schemaCreationSupport"));
		builder.addPropertyReference(BeanDefinition.SOLR_MAPPTING_CONTEXT.getBeanName(), "solrMappingContext");

		builder.addPropertyReference(BeanDefinition.SOLR_CONVERTER.getBeanName(), "solrConverter");
	}

	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {

		super.registerBeansForRoot(registry, configurationSource);

		registeCustomConversionsIfNotPresent(registry, configurationSource);
		registerSolrMappingContextIfNotPresent(registry, configurationSource);
		registerSolrConverterIfNotPresent(registry, configurationSource);

		registerIfNotAlreadyRegistered(
				BeanDefinitionBuilder.genericBeanDefinition(SolrExceptionTranslator.class).getBeanDefinition(), registry,
				"solrExceptionTranslator", configurationSource);

	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.XmlRepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {

		Element element = config.getElement();
		if (!Boolean.valueOf(element.getAttribute("multicore-support"))) {
			builder.addPropertyReference(BeanDefinition.SOLR_OPERATIONS.getBeanName(),
					element.getAttribute("solr-template-ref"));
		} else {
			builder.addPropertyReference(BeanDefinition.SOLR_CLIENT.getBeanName(), element.getAttribute("solr-client-ref"));
		}
		if (StringUtils.hasText(element.getAttribute("schema-creation-support"))) {
			builder.addPropertyValue("schemaCreationSupport", element.getAttribute("schema-creation-support"));
		}
		builder.addPropertyReference(BeanDefinition.SOLR_MAPPTING_CONTEXT.getBeanName(), "solrMappingContext");
		builder.addPropertyReference(BeanDefinition.SOLR_CONVERTER.getBeanName(), "solrConverter");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#getIdentifyingAnnotations()
	 */
	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Collections.<Class<? extends Annotation>> singleton(SolrDocument.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#getIdentifyingTypes()
	 */
	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Arrays.<Class<?>> asList(SolrRepository.class, SolrCrudRepository.class);
	}

	private void registeCustomConversionsIfNotPresent(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configurationSource) {

		RootBeanDefinition definition = new RootBeanDefinition(CustomConversions.class);
		definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
		definition.setSource(configurationSource.getSource());

		registerIfNotAlreadyRegistered(definition, registry, BeanDefinition.CUSTOM_CONVERSIONS.getBeanName(), definition);
	}

	private void registerSolrMappingContextIfNotPresent(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configurationSource) {

		RootBeanDefinition definition = new RootBeanDefinition(SimpleSolrMappingContext.class);
		definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
		definition.setSource(configurationSource.getSource());

		registerIfNotAlreadyRegistered(definition, registry, BeanDefinition.SOLR_MAPPTING_CONTEXT.getBeanName(),
				definition);
	}

	private void registerSolrConverterIfNotPresent(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configurationSource) {

		RootBeanDefinition definition = new RootBeanDefinition(MappingSolrConverter.class);
		ConstructorArgumentValues ctorArgs = new ConstructorArgumentValues();
		ctorArgs.addIndexedArgumentValue(0, new RuntimeBeanReference(BeanDefinition.SOLR_MAPPTING_CONTEXT.getBeanName()));
		definition.setConstructorArgumentValues(ctorArgs);

		MutablePropertyValues properties = new MutablePropertyValues();
		properties.add("customConversions", new RuntimeBeanReference(BeanDefinition.CUSTOM_CONVERSIONS.getBeanName()));
		definition.setPropertyValues(properties);

		definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
		definition.setSource(configurationSource.getSource());

		registerIfNotAlreadyRegistered(definition, registry, BeanDefinition.SOLR_CONVERTER.getBeanName(), definition);
	}
}
