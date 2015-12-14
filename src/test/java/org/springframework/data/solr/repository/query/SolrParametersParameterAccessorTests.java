/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.data.solr.repository.query;

import java.lang.reflect.Method;
import java.util.List;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.solr.repository.Boost;
import org.springframework.data.solr.repository.ProductBean;

/**
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrParametersParameterAccessorTests {

	private RepositoryMetadata metadata = AbstractRepositoryMetadata.getMetadata(Repo1.class);

	@Mock private SolrEntityInformationCreator entityInformationCreatorMock;

	@Test
	public void testGetBoost() throws Exception {
		SolrQueryMethod queryMethod = findByNameAndParams(Repo1.class, "findBySingleBoostedValue", String.class);
		SolrParametersParameterAccessor accessor = new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "value1" });

		Assert.assertEquals("value1", accessor.getBindableValue(0));
		Assert.assertEquals(2.0f, accessor.getBoost(0), 0.0f);
	}

	@Test
	public void testGetBoostMultipleObjects() throws Exception {
		SolrQueryMethod queryMethod = findByNameAndParams(Repo1.class, "findByMultipleBoostedValues", String.class,
				Integer.class);
		SolrParametersParameterAccessor accessor = new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "value1", Integer.valueOf(1000) });

		Assert.assertEquals("value1", accessor.getBindableValue(0));
		Assert.assertEquals(2.0f, accessor.getBoost(0), 0.0f);
		Assert.assertEquals(Integer.valueOf(1000), accessor.getBindableValue(1));
		Assert.assertEquals(10.0f, accessor.getBoost(1), 0.0f);
	}

	@Test
	public void testGetBoostNotAllArgsHavingBoostValues() throws Exception {
		SolrQueryMethod queryMethod = findByNameAndParams(Repo1.class, "findByMultipleValuesVariousBoost", String.class,
				Integer.class);
		SolrParametersParameterAccessor accessor = new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "value1", Integer.valueOf(1000) });

		Assert.assertEquals("value1", accessor.getBindableValue(0));
		Assert.assertEquals(Float.NaN, accessor.getBoost(0), 0.0f);
		Assert.assertEquals(Integer.valueOf(1000), accessor.getBindableValue(1));
		Assert.assertEquals(10.0f, accessor.getBoost(1), 0.0f);
	}

	@Test
	public void testInterator() throws Exception {
		SolrQueryMethod queryMethod = findByNameAndParams(Repo1.class, "findByMultipleValuesVariousBoost", String.class,
				Integer.class);
		SolrParametersParameterAccessor accessor = new SolrParametersParameterAccessor(queryMethod,
				new Object[] { "value1", Integer.valueOf(1000) });

		for (Object bindableParameter : accessor) {
			Assert.assertThat(bindableParameter, IsInstanceOf.instanceOf(BindableSolrParameter.class));
		}
	}

	@Test
	public void testNoArgs() throws Exception {
		SolrQueryMethod queryMethod = findByNameAndParams(Repo1.class, "findByNoArguments");
		SolrParametersParameterAccessor accessor = new SolrParametersParameterAccessor(queryMethod, new Object[] {});

		Assert.assertFalse(accessor.iterator().hasNext());
	}

	private SolrQueryMethod findByNameAndParams(Class<?> clazz, String methodName, Class<?>... params)
			throws SecurityException, NoSuchMethodException {
		Method method = clazz.getMethod(methodName, params);
		return new SolrQueryMethod(method, metadata, new SpelAwareProxyProjectionFactory(), entityInformationCreatorMock);
	}

	public interface Repo1 extends Repository<ProductBean, String> {

		List<ProductBean> findBySingleBoostedValue(@Boost(2) String value);

		List<ProductBean> findByMultipleBoostedValues(@Boost(2) String value1, @Boost(10) Integer value2);

		List<ProductBean> findByMultipleValuesVariousBoost(String value1, @Boost(10) Integer value2);

		List<ProductBean> findByNoArguments();
	}
}
