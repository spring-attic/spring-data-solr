/*
 * Copyright 2012 - 2014 the original author or authors.
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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.data.solr.core.SolrExceptionTranslator;
import org.springframework.data.solr.repository.support.SolrRepositoryFactoryBean;
import org.w3c.dom.Element;

/**
 * {@link RepositoryConfigurationExtension} implementation to configure Solr repository configuration support,
 * evaluating the {@link EnableSolrRepositories} annotation or the equivalent XML element.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class SolrRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

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
			builder.addPropertyReference("solrOperations", attributes.getString("solrTemplateRef"));
		} else {
			builder.addPropertyReference("solrServer", attributes.getString("solrServerRef"));
		}
	}

	@Override
	public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource) {
		super.registerBeansForRoot(registry, configurationSource);

		BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(SolrExceptionTranslator.class);
		registry.registerBeanDefinition("solrExceptionTranslator", definition.getBeanDefinition());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport#postProcess(org.springframework.beans.factory.support.BeanDefinitionBuilder, org.springframework.data.repository.config.XmlRepositoryConfigurationSource)
	 */
	@Override
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {

		Element element = config.getElement();
		if (!Boolean.valueOf(element.getAttribute("multicore-support"))) {
			builder.addPropertyReference("solrOperations", element.getAttribute("solr-template-ref"));
		} else {
			builder.addPropertyReference("solrServer", element.getAttribute("solr-server-ref"));
		}
	}
}
