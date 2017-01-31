/*
 * Copyright 2012 - 2018 the original author or authors.
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.data.solr.core.SolrExceptionTranslator;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.convert.MappingSolrConverter;
import org.springframework.data.solr.core.convert.SolrCustomConversions;
import org.springframework.data.solr.core.mapping.SimpleSolrMappingContext;
import org.springframework.data.solr.core.mapping.SolrDocument;
import org.springframework.data.solr.repository.SolrCrudRepository;
import org.springframework.data.solr.repository.SolrRepository;
import org.springframework.data.solr.repository.support.SolrRepositoryFactoryBean;
import org.springframework.data.solr.server.support.HttpSolrClientFactory;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * {@link RepositoryConfigurationExtension} implementation to configure Solr repository configuration support,
 * evaluating the {@link EnableSolrRepositories} annotation or the equivalent XML element.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class SolrRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

	enum BeanDefinitionName {
		SOLR_MAPPTING_CONTEXT("solrMappingContext"), SOLR_OPERATIONS("solrOperations"), SOLR_CLIENT(
				"solrClient"), SOLR_CONVERTER("solrConverter"), CUSTOM_CONVERSIONS("customConversions");
		String beanName;

		BeanDefinitionName(String beanName) {
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
	public String getRepositoryFactoryBeanClassName() {
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
		builder.addPropertyReference(BeanDefinitionName.SOLR_OPERATIONS.getBeanName(),
				attributes.getString("solrTemplateRef"));
		builder.addPropertyValue("schemaCreationSupport", attributes.getBoolean("schemaCreationSupport"));
		builder.addPropertyReference(BeanDefinitionName.SOLR_MAPPTING_CONTEXT.getBeanName(), "solrMappingContext");

		builder.addPropertyReference(BeanDefinitionName.SOLR_CONVERTER.getBeanName(), "solrConverter");
	}

	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {

		super.registerBeansForRoot(registry, configurationSource);

		registeCustomConversionsIfNotPresent(registry, configurationSource);
		registerSolrMappingContextIfNotPresent(registry, configurationSource);
		registerSolrConverterIfNotPresent(registry, configurationSource);
		registerSolrTemplateIfNotPresent(registry, configurationSource);

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
		builder.addPropertyReference(BeanDefinitionName.SOLR_OPERATIONS.getBeanName(),
				element.getAttribute("solr-template-ref"));

		if (StringUtils.hasText(element.getAttribute("schema-creation-support"))) {
			builder.addPropertyValue("schemaCreationSupport", element.getAttribute("schema-creation-support"));
		}
		builder.addPropertyReference(BeanDefinitionName.SOLR_MAPPTING_CONTEXT.getBeanName(), "solrMappingContext");
		builder.addPropertyReference(BeanDefinitionName.SOLR_CONVERTER.getBeanName(), "solrConverter");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#getIdentifyingAnnotations()
	 */
	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Collections.singleton(SolrDocument.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#getIdentifyingTypes()
	 */
	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Arrays.asList(SolrRepository.class, SolrCrudRepository.class);
	}

	private void registeCustomConversionsIfNotPresent(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configurationSource) {

		RootBeanDefinition definition = new RootBeanDefinition(SolrCustomConversions.class);

		definition.getConstructorArgumentValues().addGenericArgumentValue(Collections.emptyList());
		definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
		definition.setSource(configurationSource.getSource());

		registerIfNotAlreadyRegistered(definition, registry, BeanDefinitionName.CUSTOM_CONVERSIONS.getBeanName(),
				definition);
	}

	private void registerSolrMappingContextIfNotPresent(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configurationSource) {

		RootBeanDefinition definition = new RootBeanDefinition(SimpleSolrMappingContext.class);
		definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
		definition.setSource(configurationSource.getSource());

		registerIfNotAlreadyRegistered(definition, registry, BeanDefinitionName.SOLR_MAPPTING_CONTEXT.getBeanName(),
				definition);
	}

	private void registerSolrConverterIfNotPresent(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configurationSource) {

		RootBeanDefinition definition = new RootBeanDefinition(MappingSolrConverter.class);
		ConstructorArgumentValues ctorArgs = new ConstructorArgumentValues();
		ctorArgs.addIndexedArgumentValue(0,
				new RuntimeBeanReference(BeanDefinitionName.SOLR_MAPPTING_CONTEXT.getBeanName()));
		definition.setConstructorArgumentValues(ctorArgs);

		MutablePropertyValues properties = new MutablePropertyValues();
		properties.add("customConversions", new RuntimeBeanReference(BeanDefinitionName.CUSTOM_CONVERSIONS.getBeanName()));
		definition.setPropertyValues(properties);

		definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
		definition.setSource(configurationSource.getSource());

		registerIfNotAlreadyRegistered(definition, registry, BeanDefinitionName.SOLR_CONVERTER.getBeanName(), definition);
	}

	private void registerSolrTemplateIfNotPresent(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configurationSource) {

		RootBeanDefinition solrTemplateDefinition = new RootBeanDefinition(SolrTemplate.class);
		solrTemplateDefinition.setTargetType(SolrOperations.class);

		ConstructorArgumentValues ctorArgs = new ConstructorArgumentValues();

		ctorArgs.addIndexedArgumentValue(0, createHttpSolrClientFactory());
		ctorArgs.addIndexedArgumentValue(1, new RuntimeBeanReference(BeanDefinitionName.SOLR_CONVERTER.getBeanName()));

		solrTemplateDefinition.setConstructorArgumentValues(ctorArgs);

		registerIfNotAlreadyRegistered(solrTemplateDefinition, registry, "solrTemplate", solrTemplateDefinition);
	}

	private BeanDefinition createHttpSolrClientFactory() {

		GenericBeanDefinition solrClientFactory = new GenericBeanDefinition();
		solrClientFactory.setBeanClass(HttpSolrClientFactory.class);
		ConstructorArgumentValues args = new ConstructorArgumentValues();
		args.addIndexedArgumentValue(0, new RuntimeBeanReference(BeanDefinitionName.SOLR_CLIENT.getBeanName()));
		solrClientFactory.setConstructorArgumentValues(args);
		return solrClientFactory;
	}
}
