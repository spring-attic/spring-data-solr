/*
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.data.solr.repository.support;

import java.util.Optional;

import org.apache.solr.client.solrj.beans.Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.solr.core.mapping.SimpleSolrPersistentProperty;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.repository.ProductBean;
import org.springframework.data.solr.repository.query.SolrEntityInformation;
import org.springframework.data.util.ClassTypeInformation;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class MappingSolrEntityInformationTests {

	private static final String PRODUCT_BEAN_SIMPLE_NAME = "productbean";

	@Mock SolrPersistentEntity<ProductBean> persistentEntity;
	@Mock SolrPersistentEntity<ProductBeanWithAlternateFieldNameForId> persistentEntityWithAlternateFieldNameForId;
	@Mock SolrPersistentEntity<ProductBeanWithLongIdFieldType> persistentEntityWithLongIdFieldType;

	@Before
	public void setUp() {
		Mockito.when(persistentEntity.getType()).thenReturn(ProductBean.class);
		Mockito.when(persistentEntity.getSolrCoreName()).thenReturn(PRODUCT_BEAN_SIMPLE_NAME);

		Mockito.when(persistentEntityWithAlternateFieldNameForId.getType())
				.thenReturn(ProductBeanWithAlternateFieldNameForId.class);
		Mockito.when(persistentEntityWithAlternateFieldNameForId.getSolrCoreName()).thenReturn(PRODUCT_BEAN_SIMPLE_NAME);

		Mockito.when(persistentEntityWithLongIdFieldType.getType()).thenReturn(ProductBeanWithLongIdFieldType.class);
		Mockito.when(persistentEntityWithLongIdFieldType.getSolrCoreName()).thenReturn(PRODUCT_BEAN_SIMPLE_NAME);
	}

	@Test
	public void testSolrCoreRetrievalWhenNotExplicitlySet() {
		SolrEntityInformation<ProductBean, String> entityInformation = new MappingSolrEntityInformation<>(persistentEntity);
		Assert.assertEquals(PRODUCT_BEAN_SIMPLE_NAME, entityInformation.getSolrCoreName());
	}

	@Test
	public void testSolrCoreRetrievalWhenSet() {
		final String coreName = "core1";
		SolrEntityInformation<ProductBean, String> entityInformation = new MappingSolrEntityInformation<>(persistentEntity,
				coreName);
		Assert.assertEquals(coreName, entityInformation.getSolrCoreName());
	}

	@Test
	public void testIdType() throws NoSuchFieldException, SecurityException {
		Mockito.when(persistentEntity.getTypeInformation()).thenReturn(ClassTypeInformation.from(ProductBean.class));
		Mockito.when(persistentEntity.findAnnotation(Mockito.eq(AccessType.class))).thenReturn(Optional.empty());

		SimpleSolrPersistentProperty property = new SimpleSolrPersistentProperty(
				Property.of(ProductBean.class.getDeclaredField("id")), persistentEntity, new SimpleTypeHolder());
		Mockito.when(persistentEntity.getIdProperty()).thenReturn(Optional.of(property));

		SolrEntityInformation<ProductBean, String> entityInformation = new MappingSolrEntityInformation<>(persistentEntity);
		Assert.assertEquals(String.class, entityInformation.getIdType());
	}

	@Test
	public void testIdTypeWithLongIdFieldType() throws NoSuchFieldException, SecurityException {
		Mockito.when(persistentEntityWithLongIdFieldType.getTypeInformation())
				.thenReturn(ClassTypeInformation.from(ProductBeanWithLongIdFieldType.class));
		Mockito.when(persistentEntityWithLongIdFieldType.findAnnotation(Mockito.eq(AccessType.class)))
				.thenReturn(Optional.empty());

		SimpleSolrPersistentProperty property = new SimpleSolrPersistentProperty(
				Property.of(ProductBeanWithLongIdFieldType.class.getDeclaredField("id")), persistentEntityWithLongIdFieldType,
				new SimpleTypeHolder());
		Mockito.when(persistentEntityWithLongIdFieldType.getIdProperty()).thenReturn(Optional.of(property));

		SolrEntityInformation<ProductBeanWithLongIdFieldType, Long> entityInformation = new MappingSolrEntityInformation<>(
				persistentEntityWithLongIdFieldType);
		Assert.assertEquals(Long.class, entityInformation.getIdType());
	}

	@Test
	public void testGetIdAttribute() throws NoSuchFieldException, SecurityException {
		Mockito.when(persistentEntity.getTypeInformation()).thenReturn(ClassTypeInformation.from(ProductBean.class));
		Mockito.when(persistentEntity.findAnnotation(Mockito.eq(AccessType.class))).thenReturn(Optional.empty());
		SimpleSolrPersistentProperty property = new SimpleSolrPersistentProperty(
				Property.of(ProductBean.class.getDeclaredField("id")), persistentEntity, new SimpleTypeHolder());
		Mockito.when(persistentEntity.getIdProperty()).thenReturn(Optional.of(property));

		SolrEntityInformation<ProductBean, String> entityInformation = new MappingSolrEntityInformation<>(persistentEntity);
		Assert.assertEquals("id", entityInformation.getIdAttribute());
	}

	@Test
	public void testGetIdAttributeForAlternateFieldName() throws NoSuchFieldException, SecurityException {
		Mockito.when(persistentEntityWithAlternateFieldNameForId.getTypeInformation())
				.thenReturn(ClassTypeInformation.from(ProductBeanWithAlternateFieldNameForId.class));
		Mockito.when(persistentEntityWithAlternateFieldNameForId.findAnnotation(Mockito.eq(AccessType.class)))
				.thenReturn(Optional.empty());

		SimpleSolrPersistentProperty property = new SimpleSolrPersistentProperty(
				Property.of(ProductBeanWithAlternateFieldNameForId.class.getDeclaredField("productId")),
				persistentEntityWithAlternateFieldNameForId, new SimpleTypeHolder());
		Mockito.when(persistentEntityWithAlternateFieldNameForId.getIdProperty()).thenReturn(Optional.of(property));

		SolrEntityInformation<ProductBeanWithAlternateFieldNameForId, String> entityInformation = new MappingSolrEntityInformation<>(
				persistentEntityWithAlternateFieldNameForId);
		Assert.assertEquals("product_id", entityInformation.getIdAttribute());
	}

	class ProductBeanWithAlternateFieldNameForId {

		@Id @Field("product_id") private String productId;

		public String getProductId() {
			return productId;
		}

		public void setProductId(String productId) {
			this.productId = productId;
		}

	}

	class ProductBeanWithLongIdFieldType {

		@Id private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}
}
