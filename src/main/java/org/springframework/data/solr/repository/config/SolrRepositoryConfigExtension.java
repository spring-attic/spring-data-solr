/*
 * Copyright 2012 - 2018 the original author or authors.
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
package org.springframework.data.solr.repository.config;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.config.BeanDefinition;
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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#registerBeansForRoot(org.springframework.beans.factory.support.BeanDefinitionRegistry, org.springframework.data.repository.config.RepositoryConfigurationSource)
	 */
	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configuration) {

		super.registerBeansForRoot(registry, configuration);

		registeCustomConversionsIfNotPresent(registry, configuration);
		registerSolrMappingContextIfNotPresent(registry, configuration);
		registerSolrConverterIfNotPresent(registry, configuration);
		registerSolrTemplateIfNotPresent(registry, configuration);

		registerIfNotAlreadyRegistered(
				() -> BeanDefinitionBuilder.genericBeanDefinition(SolrExceptionTranslator.class).getBeanDefinition(), registry,
				"solrExceptionTranslator", configuration.getSource());
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

	private static void registeCustomConversionsIfNotPresent(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configuration) {

		registerIfNotAlreadyRegistered(() -> {

			RootBeanDefinition definition = new RootBeanDefinition(SolrCustomConversions.class);

			definition.getConstructorArgumentValues().addGenericArgumentValue(Collections.emptyList());
			definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);

			return definition;

		}, registry, BeanDefinitionName.CUSTOM_CONVERSIONS.getBeanName(), configuration.getSource());
	}

	private static void registerSolrMappingContextIfNotPresent(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configuration) {

		RootBeanDefinition definition = new RootBeanDefinition(SimpleSolrMappingContext.class);
		definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
		definition.setSource(configuration.getSource());

		registerIfNotAlreadyRegistered(() -> {

			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SimpleSolrMappingContext.class);

			builder.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);

			return builder.getBeanDefinition();

		}, registry, BeanDefinitionName.SOLR_MAPPTING_CONTEXT.getBeanName(), configuration.getSource());
	}

	private static void registerSolrConverterIfNotPresent(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configuration) {

		registerIfNotAlreadyRegistered(() -> {

			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(MappingSolrConverter.class);

			builder.addConstructorArgReference(BeanDefinitionName.SOLR_MAPPTING_CONTEXT.getBeanName());
			builder.addPropertyReference("customConversions", BeanDefinitionName.CUSTOM_CONVERSIONS.getBeanName());
			builder.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);

			return builder.getBeanDefinition();

		}, registry, BeanDefinitionName.SOLR_CONVERTER.getBeanName(), configuration.getSource());
	}

	private static void registerSolrTemplateIfNotPresent(BeanDefinitionRegistry registry,
			RepositoryConfigurationSource configuration) {

		registerIfNotAlreadyRegistered(() -> {

			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SolrTemplate.class);

			builder.addConstructorArgValue(createHttpSolrClientFactory());
			builder.addConstructorArgReference(BeanDefinitionName.SOLR_CONVERTER.getBeanName());

			return builder.getBeanDefinition();

		}, registry, "solrTemplate", configuration.getSource());
	}

	private static BeanDefinition createHttpSolrClientFactory() {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(HttpSolrClientFactory.class);

		builder.addConstructorArgReference(BeanDefinitionName.SOLR_CLIENT.getBeanName());

		return builder.getBeanDefinition();
	}
}
