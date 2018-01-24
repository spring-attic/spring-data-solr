/*
 * Copyright 2012 - 2016 the original author or authors.
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
package org.springframework.data.solr.core.mapping;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.data.annotation.Reference;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.solr.repository.Score;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

/**
 * Solr specific {@link PersistentEntity} implementation holding eg. name of solr core.
 * 
 * @param <T>
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public class SimpleSolrPersistentEntity<T> extends BasicPersistentEntity<T, SolrPersistentProperty>
		implements SolrPersistentEntity<T>, ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSolrPersistentEntity.class);

	private final TypeInformation<T> typeInformation;
	private final StandardEvaluationContext context;
	private String solrCoreName;
	private Float boost;

	public SimpleSolrPersistentEntity(TypeInformation<T> typeInformation) {

		super(typeInformation);
		this.context = new StandardEvaluationContext();
		this.typeInformation = typeInformation;
		this.solrCoreName = derivateSolrCoreName();
		this.boost = derivateDocumentBoost();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		context.addPropertyAccessor(new BeanFactoryAccessor());
		context.setBeanResolver(new BeanFactoryResolver(applicationContext));
		context.setRootObject(applicationContext);
	}

	private String derivateSolrCoreName() {

		String derivativeSolrCoreName = this.typeInformation.getType().getSimpleName().toLowerCase(Locale.ENGLISH);
		SolrDocument solrDocument = findAnnotation(SolrDocument.class);
		if (solrDocument != null) {
			if (StringUtils.hasText(solrDocument.solrCoreName())) {
				derivativeSolrCoreName = solrDocument.solrCoreName();
			}
		}
		return derivativeSolrCoreName;
	}

	private Float derivateDocumentBoost() {

		SolrDocument solrDocument = findAnnotation(SolrDocument.class);
		if (solrDocument != null && !Float.isNaN(solrDocument.boost())) {
			return solrDocument.boost();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentEntity#getSolrCoreName()
	 */
	@Override
	public String getSolrCoreName() {
		return this.solrCoreName;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentEntity#isBoosted()
	 */
	@Override
	public boolean isBoosted() {
		return boost != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentEntity#getBoost()
	 */
	@Override
	public Float getBoost() {
		return boost;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentEntity#hasScoreProperty()
	 */
	@Override
	public boolean hasScoreProperty() {
		return getScoreProperty() != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.mapping.SolrPersistentEntity#getScoreProperty()
	 */
	@Override
	public SolrPersistentProperty getScoreProperty() {
		return getPersistentProperty(Score.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.BasicPersistentEntity#verify()
	 */
	@Override
	public void verify() {

		super.verify();
		verifyScoreFieldUniqueness();
		verifyDynamicPropertyMapping();
		verifyAssociations();
	}

	private void verifyScoreFieldUniqueness() {
		doWithProperties(new ScoreFieldUniquenessHandler());
	}

	private void verifyDynamicPropertyMapping() {
		doWithProperties(DynamicFieldMappingHandler.INSTANCE);
	}

	private void verifyAssociations() {
		doWithProperties(AssociationFieldMappingHandler.INSTANCE);
	}

	/**
	 * Handler to inspect {@link SolrPersistentProperty} instances and check that max one can be mapped as {@link Score}
	 * property.
	 * 
	 * @author Christpoh Strobl
	 * @since 1.4
	 */
	private static class ScoreFieldUniquenessHandler implements PropertyHandler<SolrPersistentProperty> {

		private static final String AMBIGUOUS_FIELD_MAPPING = "Ambiguous score field mapping detected! Both %s and %s marked as target for score value. Disambiguate using @Score annotation!";
		private SolrPersistentProperty scoreProperty;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.PropertyHandler#doWithPersistentProperty(org.springframework.data.mapping.PersistentProperty)
		 */
		public void doWithPersistentProperty(SolrPersistentProperty persistentProperty) {
			assertUniqueness(persistentProperty);
		}

		private void assertUniqueness(SolrPersistentProperty property) {

			if (property.isScoreProperty()) {

				if (scoreProperty != null) {
					throw new MappingException(
							String.format(AMBIGUOUS_FIELD_MAPPING, property.getFieldName(), scoreProperty.getFieldName()));
				}

				scoreProperty = property;
			}
		}
	}

	/**
	 * Handler to inspect {@link SolrPersistentProperty} instances and check usage of {@link Dynamic}.
	 * 
	 * @author Christoph Strobl
	 * @since 1.5
	 */
	private static enum DynamicFieldMappingHandler implements PropertyHandler<SolrPersistentProperty> {

		INSTANCE;

		private static final String DYNAMIC_PROPERTY_NOT_A_MAP = "Invalid mapping information for property '%s' with mapped name '%s'. @Dynamic can only be applied on Map based types!";
		private static final String DYNAMIC_PROPERTY_NOT_CONTAINING_WILDCARD = "Invalid mapping information for property '%s' with mapped name '%s'. Dynamic property needs to specify wildcard.";

		@Override
		public void doWithPersistentProperty(SolrPersistentProperty property) {

			if (property.isDynamicProperty()) {

				if (!property.isMap()) {
					throw new MappingException(
							String.format(DYNAMIC_PROPERTY_NOT_A_MAP, property.getName(), property.getFieldName()));
				}

				if (!property.containsWildcard()) {
					throw new MappingException(
							String.format(DYNAMIC_PROPERTY_NOT_CONTAINING_WILDCARD, property.getName(), property.getFieldName()));
				}
			}
		}
	}

	/**
	 * Handler to inspect {@link SolrPersistentProperty} instances and check usage of {@link Dynamic}.
	 *
	 * @author Christoph Strobl
	 * @since 2.1
	 */
	private static enum AssociationFieldMappingHandler implements PropertyHandler<SolrPersistentProperty> {

		INSTANCE;

		@Override
		public void doWithPersistentProperty(SolrPersistentProperty property) {

			if (property.isAnnotationPresent(Reference.class)) {

				LOGGER.warn(
						"Associations via @Reference are not supported and will be ignored by Spring Data for Apache Solr. Please check property '%s' in %s",
						property.getName(), property.getOwner().getName());
			}
		}
	}

}
