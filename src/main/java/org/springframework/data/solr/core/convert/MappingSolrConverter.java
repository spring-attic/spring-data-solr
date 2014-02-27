/*
 * Copyright 2012 - 2014 the original author or authors.
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
package org.springframework.data.solr.core.convert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.CollectionFactory;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Implementation of {@link SolrConverter} to read/write {@link org.apache.solr.common.SolrDocumen}/
 * {@link SolrInputDocument}. <br/>
 * 
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public class MappingSolrConverter extends SolrConverterBase implements SolrConverter, ApplicationContextAware,
		InitializingBean {

	private enum WildcardPosition {
		LEADING, TRAILING
	}

	private final MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext;
	private final EntityInstantiators instantiators = new EntityInstantiators();

	@SuppressWarnings("unused")
	private ApplicationContext applicationContext;

	public MappingSolrConverter(MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext) {
		Assert.notNull(mappingContext);

		this.mappingContext = mappingContext;
	}

	@Override
	public MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> getMappingContext() {
		return mappingContext;
	}

	@Override
	public <S, R> List<R> read(SolrDocumentList source, Class<R> type) {
		if (source == null) {
			return Collections.emptyList();
		}

		List<R> resultList = new ArrayList<R>(source.size());
		TypeInformation<R> typeInformation = ClassTypeInformation.from(type);
		for (Map<String, ?> item : source) {
			resultList.add(read(typeInformation, item));
		}

		return resultList;
	}

	@Override
	public <R> R read(Class<R> type, Map<String, ?> source) {
		return read(ClassTypeInformation.from(type), source);
	}

	@SuppressWarnings("unchecked")
	protected <S extends Object> S read(TypeInformation<S> targetTypeInformation, Map<String, ?> source) {
		if (source == null) {
			return null;
		}
		Assert.notNull(targetTypeInformation);
		Class<S> rawType = targetTypeInformation.getType();

		// in case there's a custom conversion for the document
		if (hasCustomReadTarget(source.getClass(), rawType)) {
			return convert(source, rawType);
		}

		SolrPersistentEntity<S> entity = (SolrPersistentEntity<S>) mappingContext.getPersistentEntity(rawType);
		return read(entity, source, null);
	}

	private <S extends Object> S read(final SolrPersistentEntity<S> entity, final Map<String, ?> source, Object parent) {
		ParameterValueProvider<SolrPersistentProperty> parameterValueProvider = getParameterValueProvider(entity, source,
				parent);

		EntityInstantiator instantiator = instantiators.getInstantiatorFor(entity);
		S instance = instantiator.createInstance(entity, parameterValueProvider);

		final BeanWrapper<SolrPersistentEntity<S>, S> wrapper = BeanWrapper.create(instance, getConversionService());
		final S result = wrapper.getBean();

		entity.doWithProperties(new PropertyHandler<SolrPersistentProperty>() {

			@Override
			public void doWithPersistentProperty(SolrPersistentProperty persistentProperty) {
				if (entity.isConstructorArgument(persistentProperty)) {
					return;
				}

				Object o = getValue(persistentProperty, source, result);
				if (o != null) {
					wrapper.setProperty(persistentProperty, o);
				}

			}
		});

		return result;
	}

	protected Object getValue(SolrPersistentProperty property, Object source, Object parent) {
		SolrPropertyValueProvider provider = new SolrPropertyValueProvider(source, parent);
		return provider.getPropertyValue(property);
	}

	private ParameterValueProvider<SolrPersistentProperty> getParameterValueProvider(SolrPersistentEntity<?> entity,
			Map<String, ?> source, Object parent) {

		SolrPropertyValueProvider provider = new SolrPropertyValueProvider(source, parent);
		PersistentEntityParameterValueProvider<SolrPersistentProperty> parameterProvider = new PersistentEntityParameterValueProvider<SolrPersistentProperty>(
				entity, provider, parent);

		return parameterProvider;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void write(Object source, @SuppressWarnings("rawtypes") Map target) {
		if (source == null) {
			return;
		}

		if (hasCustomWriteTarget(source.getClass(), SolrInputDocument.class)
				&& canConvert(source.getClass(), SolrInputDocument.class)) {
			SolrInputDocument convertedDocument = convert(source, SolrInputDocument.class);
			target.putAll(convertedDocument);
			return;

		}

		TypeInformation<? extends Object> type = ClassTypeInformation.from(source.getClass());
		write(type, source, target);
	}

	protected void write(TypeInformation<?> type, Object source, @SuppressWarnings("rawtypes") Map target) {
		Assert.notNull(type);

		SolrPersistentEntity<?> entity = mappingContext.getPersistentEntity(source.getClass());
		write(source, target, entity);
	}

	@SuppressWarnings("rawtypes")
	protected void write(Object source, final Map target, SolrPersistentEntity<?> entity) {
		final BeanWrapper<SolrPersistentEntity<Object>, Object> wrapper = BeanWrapper
				.create(source, getConversionService());

		entity.doWithProperties(new PropertyHandler<SolrPersistentProperty>() {

			@SuppressWarnings({ "unchecked" })
			@Override
			public void doWithPersistentProperty(SolrPersistentProperty persistentProperty) {
				Object value = wrapper.getProperty(persistentProperty, persistentProperty.getType(), false);
				if (value == null || persistentProperty.isReadonly()) {
					return;
				}

				if (persistentProperty.containsWildcard() && !persistentProperty.isMap()) {
					throw new IllegalArgumentException("Field '" + persistentProperty.getFieldName()
							+ "' must not contain wildcards. Consider excluding Field from beeing indexed.");
				}

				Object fieldValue = value;
				if (persistentProperty.isMap() && persistentProperty.containsWildcard()) {
					TypeInformation<?> mapTypeInformation = persistentProperty.getTypeInformation().getMapValueType();
					Class<?> rawMapType = mapTypeInformation.getType();

					Map<?, ?> map = (Map<?, ?>) fieldValue;
					for (Map.Entry<?, ?> entry : map.entrySet()) {
						String mappedFieldName = entry.getKey().toString();
						SolrInputField field = new SolrInputField(mappedFieldName);
						if (entry.getValue() instanceof Iterable) {
							for (Object o : (Iterable<?>) entry.getValue()) {
								field.addValue(convertToSolrType(rawMapType, o), 1f);
							}
						} else {
							if (rawMapType.isArray()) {
								for (Object o : (Object[]) entry.getValue()) {
									field.addValue(convertToSolrType(rawMapType, o), 1f);
								}
							} else {
								field.addValue(convertToSolrType(rawMapType, entry.getValue()), 1f);
							}
						}
						target.put(mappedFieldName, field);
					}
					return;
				}

				SolrInputField field = new SolrInputField(persistentProperty.getFieldName());
				if (persistentProperty.isCollectionLike()) {
					Collection<?> collection = asCollection(fieldValue);
					for (Object o : collection) {
						if (o != null) {
							field.addValue(convertToSolrType(persistentProperty.getType(), o), 1f);
						}
					}
				} else {
					field.setValue(convertToSolrType(persistentProperty.getType(), fieldValue), 1f);
				}
				target.put(persistentProperty.getFieldName(), field);
				
				if (persistentProperty.isBoosted()) {
					field.setBoost(persistentProperty.getBoost());
				}

			}
		});

		if (entity.isBoosted()) {
			((SolrInputDocument)target).setDocumentBoost(entity.getBoost());
		}
		
	}

	private Object convertToSolrType(Class<?> type, Object value) {
		if (type == null || value == null) {
			return value;
		}

		if (isSimpleType(type)) {
			return value;
		} else if (hasCustomWriteTarget(value.getClass())) {
			Class<?> targetType = getCustomWriteTargetType(value.getClass());
			if (canConvert(value.getClass(), targetType)) {
				return convert(value, targetType);
			}
		}

		return value;
	}

	private static Collection<?> asCollection(Object source) {
		if (source instanceof Collection) {
			return (Collection<?>) source;
		}

		return source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singleton(source);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	private class SolrPropertyValueProvider implements PropertyValueProvider<SolrPersistentProperty> {

		private final Object source;
		private final Object parent;

		public SolrPropertyValueProvider(Object source, Object parent) {
			this.source = source;
			this.parent = parent;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getPropertyValue(SolrPersistentProperty property) {
			if (source instanceof Map<?, ?>) {
				return (T) readValue((Map<String, ?>) source, property, parent);
			}

			return readValue(source, property.getTypeInformation(), this.parent);
		}

		@SuppressWarnings("unchecked")
		private <T> T readValue(Map<String, ?> value, SolrPersistentProperty property, Object parent) {
			if (value == null) {
				return null;
			}
			if (property.containsWildcard()) {
				return (T) readWildcard(value, property, parent);
			}
			return readValue(value.get(property.getFieldName()), property.getTypeInformation(), parent);
		}

		@SuppressWarnings("unchecked")
		private <T> T readValue(Object value, TypeInformation<?> type, Object parent) {
			if (value == null) {
				return null;
			}

			Assert.notNull(type);
			Class<?> rawType = type.getType();
			if (hasCustomReadTarget(value.getClass(), rawType)) {
				return (T) convert(value, rawType);
			}

			Object documentValue = null;
			if (value instanceof SolrInputField) {
				documentValue = ((SolrInputField) value).getValue();
			} else {
				documentValue = value;
			}

			if (documentValue instanceof Collection) {
				return (T) readCollection((Collection<?>) documentValue, type, parent);
			} else if (canConvert(documentValue.getClass(), rawType)) {
				return (T) convert(documentValue, rawType);
			}
			return (T) documentValue;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private Object readWildcard(Map<String, ?> source, SolrPersistentProperty property, Object parent) {
			String fieldName = StringUtils.remove(property.getFieldName(), Criteria.WILDCARD);
			WildcardPosition wildcardPosition = StringUtils.startsWith(property.getFieldName(), Criteria.WILDCARD) ? WildcardPosition.LEADING
					: WildcardPosition.TRAILING;

			if (property.isMap()) {
				TypeInformation<?> mapTypeInformation = property.getTypeInformation().getMapValueType();
				Class<?> rawMapType = mapTypeInformation.getType();

				Map<String, Object> values = LinkedHashMap.class.isAssignableFrom(property.getActualType()) ? new LinkedHashMap<String, Object>()
						: new HashMap<String, Object>();
				for (Map.Entry<String, ?> potentialMatch : source.entrySet()) {
					if (isWildcardFieldNameMatch(fieldName, wildcardPosition, potentialMatch.getKey())) {
						Object value = potentialMatch.getValue();
						if (value instanceof Iterable) {
							if (rawMapType.isArray() || ClassUtils.isAssignable(rawMapType, value.getClass())) {
								List<Object> nestedValues = new ArrayList<Object>();
								for (Object o : (Iterable<?>) value) {
									nestedValues.add(getValue(property, o, parent));
								}
								values.put(potentialMatch.getKey(), (rawMapType.isArray() ? nestedValues.toArray() : nestedValues));
							} else {
								throw new IllegalArgumentException("Incompartible types found. Expected " + rawMapType + " for "
										+ property.getName() + " with name " + property.getFieldName() + ", but found " + value.getClass());
							}
						} else {
							if (rawMapType.isArray() || ClassUtils.isAssignable(rawMapType, List.class)) {
								ArrayList<Object> singletonArrayList = new ArrayList<Object>(1);
								singletonArrayList.add(getValue(property, potentialMatch.getValue(), parent));
								values.put(potentialMatch.getKey(), (rawMapType.isArray() ? singletonArrayList.toArray()
										: singletonArrayList));
							} else {
								values.put(potentialMatch.getKey(), getValue(property, potentialMatch.getValue(), parent));
							}
						}
					}
				}
				return values.isEmpty() ? null : values;
			} else if (property.isCollectionLike()) {
				List<Object> values = new ArrayList<Object>();
				for (Map.Entry<String, ?> potentialMatch : source.entrySet()) {
					if (isWildcardFieldNameMatch(fieldName, wildcardPosition, potentialMatch.getKey())) {
						Object value = potentialMatch.getValue();
						if (value instanceof Iterable) {
							for (Object o : (Iterable<?>) value) {
								values.add(getValue(property, o, parent));
							}
						} else {
							Object o = getValue(property, potentialMatch.getValue(), parent);
							if (o instanceof Collection) {
								values.addAll((Collection) o);
							} else {
								values.add(o);
							}
						}
					}
				}

				return values.isEmpty() ? null : (property.isArray() ? values.toArray() : values);
			} else {
				for (Map.Entry<String, ?> potentialMatch : source.entrySet()) {
					if (StringUtils.contains(potentialMatch.getKey(), fieldName)) {
						return getValue(property, potentialMatch.getValue(), parent);
					}
				}
			}

			return null;
		}

		private boolean isWildcardFieldNameMatch(String fieldname, WildcardPosition type, String candidate) {
			switch (type) {
			case LEADING:
				return StringUtils.endsWith(candidate, fieldname);
			case TRAILING:
				return StringUtils.startsWith(candidate, fieldname);
			}
			return false;
		}

		@SuppressWarnings("unchecked")
		private Object readCollection(Collection<?> source, TypeInformation<?> type, Object parent) {
			Assert.notNull(type);

			Class<?> collectionType = type.getType();
			if (CollectionUtils.isEmpty(source)) {
				return source;
			}

			collectionType = Collection.class.isAssignableFrom(collectionType) ? collectionType : List.class;
			Collection<Object> items = type.getType().isArray() ? new ArrayList<Object>() : CollectionFactory
					.createCollection(collectionType, source.size());
			TypeInformation<?> componentType = type.getComponentType();

			Iterator<?> it = source.iterator();
			while (it.hasNext()) {
				items.add(readValue(it.next(), componentType, parent));
			}
			return items;
		}

	}

}
