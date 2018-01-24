/*
 * Copyright 2012 - 2018 the original author or authors.
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
package org.springframework.data.solr.core.query;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christoph Strobl
 *
 */
public class SimpleHighlightQueryTests {

	private SimpleHighlightQuery query;

	@Before
	public void setUp() {
		query = new SimpleHighlightQuery();
	}

	@Test
	public void testWithoutHighlightOptions() {
		Assert.assertNull(query.getHighlightOptions());
		Assert.assertFalse(query.hasHighlightOptions());
	}

	@Test
	public void testSetHighlightOptions() {
		query.setHighlightOptions(new HighlightOptions());
		Assert.assertNotNull(query.getHighlightOptions());
		Assert.assertTrue(query.hasHighlightOptions());
	}

	@Test
	public void testSetNullHighlightOptions() {
		query.setHighlightOptions(null);
		Assert.assertNull(query.getHighlightOptions());
		Assert.assertFalse(query.hasHighlightOptions());
	}

}
