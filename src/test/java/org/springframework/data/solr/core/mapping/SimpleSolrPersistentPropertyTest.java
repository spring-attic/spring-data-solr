/*
 * Copyright 2014-2015 the original author or authors.
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
package org.springframework.data.solr.core.mapping;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.solr.repository.Score;
import org.springframework.data.util.TypeInformation;

/**
 * @author Francisco Spaeth
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleSolrPersistentPropertyTest {

	private @Mock PersistentEntity<BeanWithScore, SolrPersistentProperty> owner;
	private @Mock SimpleTypeHolder simpleTypeHolder;
	private @Mock TypeInformation<BeanWithScore> typeInformation;

	/**
	 * @see DATASOLR-210
	 */
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void scoredPropertyShouldBeReadOnlyAndNotWritable() throws NoSuchFieldException, SecurityException,
			IntrospectionException {

		Field field = BeanWithScore.class.getDeclaredField("myScoreProperty");
		PropertyDescriptor propertyDescriptor = new PropertyDescriptor("myScoreProperty", BeanWithScore.class, null, null);

		when(owner.getType()).thenReturn((Class) BeanWithScore.class);
		when(owner.getTypeInformation()).thenReturn(typeInformation);
		when(typeInformation.getProperty("myScoreProperty")).thenReturn((TypeInformation) typeInformation);

		SimpleSolrPersistentProperty property = new SimpleSolrPersistentProperty(field, propertyDescriptor, owner,
				simpleTypeHolder);

		assertTrue(property.isScoreProperty());
		assertFalse(property.isWritable());
	}

	static class BeanWithScore {

		@Score Float myScoreProperty;

		public Float getMyScoreProperty() {
			return myScoreProperty;
		}
	}

}
