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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.solr.VersionUtil;
import org.springframework.data.solr.core.geo.GeoConverters.Point3DToStringConverter;
import org.springframework.data.solr.core.geo.GeoConverters.StringToPointConverter;
import org.springframework.data.solr.core.mapping.SolrSimpleTypes;
import org.springframework.util.Assert;

/**
 * CustomConversions holds basically a list of {@link Converter} that can be used for mapping objects to (
 * {@link WritingConverter}) and from ({@link ReadingConverter}) solr representation.
 * 
 * @author Christoph Strobl
 * @author Rias A. Sherzad
 */
public class CustomConversions {

	private final Set<Class<?>> customSimpleTypes;
	private final List<Object> converters;
	private final Set<ConvertiblePair> readingPairs;
	private final Set<ConvertiblePair> writingPairs;
	private SimpleTypeHolder simpleTypeHolder;

	private ConcurrentMap<ConvertiblePair, Class<?>> cache = new ConcurrentHashMap<ConvertiblePair, Class<?>>(36, 0.9f, 1);

	/**
	 * Create new instance
	 */
	public CustomConversions() {
		this(new ArrayList<Object>());
	}

	/**
	 * Create new instance registering given converters
	 * 
	 * @param converters
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public CustomConversions(List converters) {
		this.converters = (converters != null ? new ArrayList<Object>(converters) : new ArrayList<Object>());
		this.readingPairs = new HashSet<ConvertiblePair>();
		this.writingPairs = new HashSet<ConvertiblePair>();
		this.customSimpleTypes = new HashSet<Class<?>>();

		this.simpleTypeHolder = new SimpleTypeHolder(customSimpleTypes, SolrSimpleTypes.HOLDER);

		this.converters.add(StringToPointConverter.INSTANCE);
		this.converters.add(Point3DToStringConverter.INSTANCE);
		this.converters.add(new SolrjConverters.UpdateToSolrInputDocumentConverter());

		// Register Joda-Time converters only if Joda-Time was found in the classpath.
		if (VersionUtil.isJodaTimeAvailable()) {
			this.converters.add(DateTimeConverters.DateToJodaDateTimeConverter.INSTANCE);
			this.converters.add(DateTimeConverters.JodaDateTimeToDateConverter.INSTANCE);
			this.converters.add(DateTimeConverters.DateToLocalDateTimeConverter.INSTANCE);
			this.converters.add(DateTimeConverters.JodaLocalDateTimeToDateConverter.INSTANCE);
		}

		for (Object converter : this.converters) {
			registerConversion(converter);
		}
	}

	/**
	 * Register custom converters within given {@link GenericConversionService}
	 * 
	 * @param conversionService must not be null
	 */
	public void registerConvertersIn(GenericConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null!");

		for (Object converter : converters) {
			if (converter instanceof Converter) {
				conversionService.addConverter((Converter<?, ?>) converter);
			} else if (converter instanceof ConverterFactory) {
				conversionService.addConverterFactory((ConverterFactory<?, ?>) converter);
			} else if (converter instanceof GenericConverter) {
				conversionService.addConverter((GenericConverter) converter);
			} else {
				throw new IllegalArgumentException("Given object '" + converter
						+ "' expected to be a Converter, ConverterFactory or GenericeConverter!");
			}
		}
	}

	/**
	 * @param clazz
	 * @return true if given class is considered a simple type
	 */
	public boolean isSimpleType(Class<?> clazz) {
		return simpleTypeHolder.isSimpleType(clazz);
	}

	/**
	 * find most recent write target for given class
	 * 
	 * @param source must not be null
	 * @return
	 */
	public Class<?> getCustomWriteTarget(Class<?> source) {
		return getCustomWriteTarget(source, null);
	}

	/**
	 * find most recent write target for given source and targetType
	 * 
	 * @param sourceType
	 * @param targetType
	 * @return
	 */
	public Class<?> getCustomWriteTarget(Class<?> sourceType, Class<?> targetType) {
		Assert.notNull(sourceType, "SourceType must not be null!");
		return getCustomTarget(sourceType, targetType, writingPairs);
	}

	Class<?> getCustomTarget(Class<?> sourceType, Class<?> expectedTargetType, Iterable<ConvertiblePair> pairs) {
		Assert.notNull(sourceType, "SourceType must not be null!");
		Assert.notNull(pairs, "Pairs of ConvertiblePairs must not be null!");

		ConvertiblePair expectedTypePair = new ConvertiblePair(sourceType, expectedTargetType != null ? expectedTargetType
				: Any.class);

		if (cache.containsKey(expectedTypePair)) {
			Class<?> cachedTargetType = cache.get(expectedTypePair);
			return cachedTargetType != Any.class ? cachedTargetType : null;
		}

		for (ConvertiblePair typePair : pairs) {
			if (typePair.getSourceType().isAssignableFrom(sourceType)) {
				Class<?> targetType = typePair.getTargetType();
				if (expectedTargetType == null || targetType.isAssignableFrom(expectedTargetType)) {
					cache.putIfAbsent(expectedTypePair, targetType);
					return targetType;
				}
			}
		}

		cache.putIfAbsent(expectedTypePair, Any.class);
		return null;
	}

	/**
	 * check if custom read target available for given types
	 * 
	 * @param sourceType
	 * @param targetType
	 * @return true if custom converter registered for source/target type
	 */
	public boolean hasCustomReadTarget(Class<?> sourceType, Class<?> targetType) {
		Assert.notNull(sourceType, "SourceType must not be null!");
		Assert.notNull(targetType, "TargetType must not be null!");

		return getCustomReadTarget(sourceType, targetType) != null;
	}

	/**
	 * check if custom write target available for given types
	 * 
	 * @param sourceType
	 * @param targetType
	 * @return
	 */
	public boolean hasCustomWriteTarget(Class<?> sourceType, Class<?> targetType) {
		return getCustomTarget(sourceType, targetType, writingPairs) != null;
	}

	private Class<?> getCustomReadTarget(Class<?> sourceType, Class<?> targetType) {
		return getCustomTarget(sourceType, targetType, readingPairs);
	}

	private void registerConversion(Object converter) {
		Class<?> type = converter.getClass();
		boolean isWriting = type.isAnnotationPresent(WritingConverter.class);
		boolean isReading = type.isAnnotationPresent(ReadingConverter.class);

		if (!isReading && !isWriting) {
			isReading = true;
			isWriting = true;
		}

		if (converter instanceof GenericConverter) {
			GenericConverter genericConverter = (GenericConverter) converter;
			for (ConvertiblePair pair : genericConverter.getConvertibleTypes()) {
				register(new ConvertibleContext(pair, isReading, isWriting));
			}
		} else if (converter instanceof Converter) {
			Class<?>[] arguments = GenericTypeResolver.resolveTypeArguments(converter.getClass(), Converter.class);
			register(new ConvertibleContext(arguments[0], arguments[1], isReading, isWriting));
		} else {
			throw new IllegalArgumentException("Unsupported Converter type! Expected either GenericConverter if Converter.");
		}
	}

	private void register(ConvertibleContext context) {
		ConvertiblePair pair = context.getConvertible();
		if (context.isReading()) {
			readingPairs.add(pair);
		}
		if (context.isWriting()) {
			writingPairs.add(pair);
			customSimpleTypes.add(pair.getSourceType());
		}
	}

	/**
	 * ConvertibleContext is a holder for {@link ConvertiblePair} and read/write information
	 * 
	 * @author Christoph Strobl
	 */
	static class ConvertibleContext {

		private final ConvertiblePair convertible;
		private final boolean reading;
		private final boolean writing;

		/**
		 * Create new instance
		 * 
		 * @param convertible
		 * @param isReading
		 * @param isWriting
		 */
		public ConvertibleContext(ConvertiblePair convertible, boolean isReading, boolean isWriting) {
			Assert.notNull(convertible, "ConvertiblePair must not be null!");
			this.convertible = convertible;
			this.reading = isReading;
			this.writing = isWriting;
		}

		/**
		 * Create new instance wrapping source/target into {@link ConvertiblePair}
		 * 
		 * @param source
		 * @param target
		 * @param isReading
		 * @param isWriting
		 */
		public ConvertibleContext(Class<?> source, Class<?> target, boolean isReading, boolean isWriting) {
			this(new ConvertiblePair(source, target), isReading, isWriting);
		}

		public ConvertiblePair getConvertible() {
			return convertible;
		}

		public boolean isReading() {
			return reading;
		}

		public boolean isWriting() {
			return writing;
		}

	}

	/**
	 * Simple placeholder as {@link ConvertiblePair} will not allow null values
	 * 
	 * @author Christoph Strobl
	 */
	private static class Any {

	}
}
