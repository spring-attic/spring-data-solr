/*
 * Copyright 2012 - 2017 the original author or authors.
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
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.MappingException;
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
 * Implementation of {@link SolrConverter} to read/write {@link org.apache.solr.common.SolrDocument}/
 * {@link SolrInputDocument}. <br/>
 * 
 * @author Christoph Strobl
 * @author Francisco Spaeth
 */
public class MappingSolrConverter extends SolrConverterBase
		implements SolrConverter, ApplicationContextAware, InitializingBean {

	private enum WildcardPosition {

		LEADING {

			@Override
			public boolean match(String fieldName, String candidate) {
				return StringUtils.endsWith(candidate, removeWildcard(fieldName));
			}

			@Override
			public String extractName(String fieldName, String dynamicFieldName) {
				Assert.isTrue(match(fieldName, dynamicFieldName), "dynamicFieldName must be derivated from fieldName");
				return StringUtils.removeEnd(dynamicFieldName, removeWildcard(fieldName));
			}

			@Override
			public String createName(String fieldName, String name) {
				return name + removeWildcard(fieldName);
			}
		},

		TRAILING {

			@Override
			public boolean match(String fieldName, String candidate) {
				return StringUtils.startsWith(candidate, removeWildcard(fieldName));
			}

			@Override
			public String extractName(String fieldName, String dynamicFieldName) {
				Assert.isTrue(match(fieldName, dynamicFieldName), "dynamicFieldName must be derivated from fieldName");
				return StringUtils.removeStart(dynamicFieldName, removeWildcard(fieldName));
			}

			@Override
			public String createName(String fieldName, String name) {
				return removeWildcard(fieldName) + name;
			}
		};

		public static WildcardPosition getAppropriate(String fieldName) {
			if (StringUtils.startsWith(fieldName, Criteria.WILDCARD)) {
				return WildcardPosition.LEADING;
			} else {
				return WildcardPosition.TRAILING;
			}
		}

		String removeWildcard(String fieldName) {
			return StringUtils.remove(fieldName, Criteria.WILDCARD);
		}

		public abstract boolean match(String fieldName, String candidate);

		public abstract String extractName(String fieldName, String dynamicFieldName);

		public abstract String createName(String fieldName, String name);
	}

	private final MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext;
	private final EntityInstantiators instantiators = new EntityInstantiators();

	@SuppressWarnings("unused") //
	private ApplicationContext applicationContext;

	public MappingSolrConverter(
			MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext) {
		Assert.notNull(mappingContext, "MappingContext must not be null!");

		this.mappingContext = mappingContext;
	}

	@Override
	public MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> getMappingContext() {
		return mappingContext;
	}

	@Override
	public <S, R> List<R> read(SolrDocumentList source, Class<R> type) {
		if (CollectionUtils.isEmpty(source)) {
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
		Assert.notNull(targetTypeInformation, "TargetTypeInformation must not be null!");
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
		final S instance = instantiator.createInstance(entity, parameterValueProvider);
		final PersistentPropertyAccessor accessor = new ConvertingPropertyAccessor(entity.getPropertyAccessor(instance),
				getConversionService());

		entity.doWithProperties(new PropertyHandler<SolrPersistentProperty>() {

			@Override
			public void doWithPersistentProperty(SolrPersistentProperty persistentProperty) {
				if (entity.isConstructorArgument(persistentProperty)) {
					return;
				}

				Object o = getValue(persistentProperty, source, instance);
				if (o != null) {

					if (o instanceof Collection && !persistentProperty.isCollectionLike()) {

						Collection<?> c = (Collection<?>) o;

						if (!c.isEmpty()) {

							if (c.size() == 1) {
								accessor.setProperty(persistentProperty, c.iterator().next());
							} else {
								throw new MappingException(String.format(
										"Cannot set multiple values %s read from '%s' to non collection property '%s'. Please check your mapping / schema defintion!",
										c, persistentProperty.getFieldName(), persistentProperty.getName()));
							}
						}

					} else {
						accessor.setProperty(persistentProperty, o);
					}

				}
			}
		});

		return instance;
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

		Class<? extends Object> sourceClass = source.getClass();

		if (hasCustomWriteTarget(sourceClass, SolrInputDocument.class)
				&& canConvert(sourceClass, SolrInputDocument.class)) {

			SolrInputDocument convertedDocument = convert(source, SolrInputDocument.class);
			target.putAll(convertedDocument);
		} else {

			SolrPersistentEntity<?> entity = mappingContext.getPersistentEntity(sourceClass);
			write(source, target, entity);
		}

	}

	@SuppressWarnings("rawtypes")
	protected void write(Object source, final Map target, SolrPersistentEntity<?> entity) {

		final PersistentPropertyAccessor accessor = new ConvertingPropertyAccessor(entity.getPropertyAccessor(source),
				getConversionService());

		entity.doWithProperties(new PropertyHandler<SolrPersistentProperty>() {

			@SuppressWarnings("unchecked")
			@Override
			public void doWithPersistentProperty(SolrPersistentProperty persistentProperty) {

				Object value = accessor.getProperty(persistentProperty);
				if (value == null || persistentProperty.isReadonly()) {
					return;
				}

				if (persistentProperty.containsWildcard() && !persistentProperty.isMap()) {
					throw new IllegalArgumentException("Field '" + persistentProperty.getFieldName()
							+ "' must not contain wildcards. Consider excluding Field from beeing indexed.");
				}

				Collection<SolrInputField> fields;
				if (persistentProperty.isMap() && persistentProperty.containsWildcard()) {
					fields = writeWildcardMapPropertyToTarget(target, persistentProperty, (Map<?, ?>) value);
				} else {
					fields = writeRegularPropertyToTarget(target, persistentProperty, value);
				}

				if (persistentProperty.isBoosted()) {
					for (SolrInputField field : fields) {
						field.setBoost(persistentProperty.getBoost());
					}
				}
			}
		});

		if (entity.isBoosted() && target instanceof SolrInputDocument) {
			((SolrInputDocument) target).setDocumentBoost(entity.getBoost());
		}

	}

	private Collection<SolrInputField> writeWildcardMapPropertyToTarget(Map<? super Object, ? super Object> target,
			SolrPersistentProperty persistentProperty, Map<?, ?> fieldValue) {

		TypeInformation<?> mapTypeInformation = persistentProperty.getTypeInformation().getMapValueType();
		Class<?> rawMapType = mapTypeInformation.getType();
		String fieldName = persistentProperty.getFieldName();

		Collection<SolrInputField> fields = new ArrayList<SolrInputField>();

		for (Map.Entry<?, ?> entry : fieldValue.entrySet()) {

			Object value = entry.getValue();
			String key = entry.getKey().toString();

			if (persistentProperty.isDynamicProperty()) {
				key = WildcardPosition.getAppropriate(fieldName).createName(fieldName, key);
			}

			SolrInputField field = new SolrInputField(key);

			if (value instanceof Iterable) {

				for (Object o : (Iterable<?>) value) {
					field.addValue(convertToSolrType(rawMapType, o), 1f);
				}
			} else {

				if (rawMapType.isArray()) {
					for (Object o : (Object[]) value) {
						field.addValue(convertToSolrType(rawMapType, o), 1f);
					}
				} else {
					field.addValue(convertToSolrType(rawMapType, value), 1f);
				}

			}

			target.put(key, field);
			fields.add(field);
		}

		return fields;
	}

	private Collection<SolrInputField> writeRegularPropertyToTarget(final Map<? super Object, ? super Object> target,
			SolrPersistentProperty persistentProperty, Object fieldValue) {

		SolrInputField field = new SolrInputField(persistentProperty.getFieldName());

		if (persistentProperty.isCollectionLike()) {
			Collection<?> collection = asCollection(fieldValue);
			for (Object o : collection) {
				if (o != null) {
					if(o instanceof Enum) {
						field.addValue(this.getConversionService().convert(o, String.class), 1f);
					} else {
						field.addValue(convertToSolrType(persistentProperty.getType(), o), 1f);
					}
				}
			}
		}
		else if (fieldValue instanceof Enum) {
			field.setValue(this.getConversionService().convert(fieldValue, String.class), 1f);
		}
		else {
			field.setValue(convertToSolrType(persistentProperty.getType(), fieldValue), 1f);
		}

		target.put(persistentProperty.getFieldName(), field);

		return Collections.singleton(field);

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
			if (property.isScoreProperty()) {
				return (T) readScore(value, property, parent);
			}
			return readValue(value.get(property.getFieldName()), property.getTypeInformation(), parent);
		}

		@SuppressWarnings("unchecked")
		private <T> T readScore(Map<String, ?> value, SolrPersistentProperty property, Object parent) {
			return (T) value.get("score");
		}

		@SuppressWarnings("unchecked")
		private <T> T readValue(Object value, TypeInformation<?> type, Object parent) {
			if (value == null) {
				return null;
			}

			Assert.notNull(type, "TypeInformation must not be null!");
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

		private Object readWildcard(Map<String, ?> source, SolrPersistentProperty property, Object parent) {

			WildcardPosition wildcardPosition = WildcardPosition.getAppropriate(property.getFieldName());

			if (property.isMap()) {
				return readWildcardMap(source, property, parent, wildcardPosition);
			} else if (property.isCollectionLike()) {
				return readWildcardCollectionLike(source, property, parent, wildcardPosition);
			} else {

				for (Map.Entry<String, ?> potentialMatch : source.entrySet()) {

					if (wildcardPosition.match(property.getFieldName(), potentialMatch.getKey())) {
						return getValue(property, potentialMatch.getValue(), parent);
					}
				}
			}

			return null;
		}

		private Object readWildcardCollectionLike(Map<String, ?> source, SolrPersistentProperty property, Object parent,
				WildcardPosition wildcardPosition) {

			Class<?> genericTargetType = property.getComponentType() != null ? property.getComponentType() : Object.class;

			List<Object> values = new ArrayList<Object>();

			for (Map.Entry<String, ?> potentialMatch : source.entrySet()) {

				if (!wildcardPosition.match(property.getFieldName(), potentialMatch.getKey())) {
					continue;
				}

				Object value = potentialMatch.getValue();

				if (value instanceof Iterable) {

					for (Object o : (Iterable<?>) value) {
						values.add(readValue(property, o, parent, genericTargetType));
					}
				} else {

					Object o = readValue(property, potentialMatch.getValue(), parent, genericTargetType);
					if (o instanceof Collection) {
						values.addAll((Collection<?>) o);
					} else {
						values.add(o);
					}
				}
			}

			return values.isEmpty() ? null : (property.isArray() ? values.toArray() : values);
		}

		private Object readWildcardMap(Map<String, ?> source, SolrPersistentProperty property, Object parent,
				WildcardPosition wildcardPosition) {

			TypeInformation<?> mapTypeInformation = property.getTypeInformation().getMapValueType();
			Class<?> rawMapType = mapTypeInformation.getType();

			Class<?> genericTargetType;
			if (mapTypeInformation.getTypeArguments() != null && !mapTypeInformation.getTypeArguments().isEmpty()) {
				genericTargetType = mapTypeInformation.getTypeArguments().get(0).getType();
			} else {
				genericTargetType = Object.class;
			}

			Map<String, Object> values;
			if (LinkedHashMap.class.isAssignableFrom(property.getActualType())) {
				values = new LinkedHashMap<String, Object>();
			} else {
				values = new HashMap<String, Object>();
			}

			for (Map.Entry<String, ?> potentialMatch : source.entrySet()) {

				String key = potentialMatch.getKey();

				if (!wildcardPosition.match(property.getFieldName(), key)) {
					continue;
				}

				if (property.isDynamicProperty()) {
					key = wildcardPosition.extractName(property.getFieldName(), key);
				}
				Object value = potentialMatch.getValue();

				if (value instanceof Iterable) {

					if (rawMapType.isArray() || ClassUtils.isAssignable(rawMapType, value.getClass())) {
						List<Object> nestedValues = new ArrayList<Object>();
						for (Object o : (Iterable<?>) value) {
							nestedValues.add(readValue(property, o, parent, genericTargetType));
						}
						values.put(key, (rawMapType.isArray() ? nestedValues.toArray() : nestedValues));
					} else {
						throw new IllegalArgumentException("Incompartible types found. Expected " + rawMapType + " for "
								+ property.getName() + " with name " + property.getFieldName() + ", but found " + value.getClass());
					}
				} else {

					if (rawMapType.isArray() || ClassUtils.isAssignable(rawMapType, List.class)) {
						ArrayList<Object> singletonArrayList = new ArrayList<Object>(1);
						Object read = readValue(property, value, parent, genericTargetType);
						singletonArrayList.add(read);
						values.put(key, (rawMapType.isArray() ? singletonArrayList.toArray() : singletonArrayList));

					} else {
						values.put(key, getValue(property, value, parent));
					}
				}
			}

			return values.isEmpty() ? null : values;
		}

		private Object readValue(SolrPersistentProperty property, Object o, Object parent, Class<?> target) {

			Object value = getValue(property, o, parent);
			if (value == null || target == null || target.equals(Object.class)) {
				return value;
			}

			if (canConvert(value.getClass(), target)) {
				return convert(value, target);
			}

			return value;
		}

		private Object readCollection(Collection<?> source, TypeInformation<?> type, Object parent) {
			Assert.notNull(type, "Type must not be null!");

			Class<?> collectionType = type.getType();
			if (CollectionUtils.isEmpty(source)) {
				return source;
			}

			collectionType = Collection.class.isAssignableFrom(collectionType) ? collectionType : List.class;

			Collection<Object> items;
			if (type.getType().isArray()) {
				items = new ArrayList<Object>();
			} else {
				items = CollectionFactory.createCollection(collectionType, source.size());
			}

			TypeInformation<?> componentType = type.isCollectionLike() ? type.getComponentType() : type;

			Iterator<?> it = source.iterator();
			while (it.hasNext()) {
				items.add(readValue(it.next(), componentType, parent));
			}

			return type.getType().isArray() ? convertItemsToArrayOfType(type, items) : items;
		}

		private Object convertItemsToArrayOfType(TypeInformation<?> type, Collection<Object> items) {

			Object[] newArray = (Object[]) java.lang.reflect.Array.newInstance(type.getActualType().getType(), items.size());
			Object[] itemsArray = items.toArray();
			for (int i = 0; i < itemsArray.length; i++) {
				newArray[i] = itemsArray[i];
			}
			return newArray;
		}
	}
}
