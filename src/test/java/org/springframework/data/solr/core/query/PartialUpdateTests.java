/*
 * Copyright 2012 the original author or authors.
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
import org.junit.Test;

/**
 * @author Christoph Strobl
 */
public class PartialUpdateTests {

	@Test
	public void testAddValueToField() {
		PartialUpdate update = new PartialUpdate("id", "123");
		update.addValueToField("name", "value-to-add");

		Assert.assertEquals(1, update.getUpdates().size());
		Assert.assertEquals(UpdateAction.ADD, update.getUpdates().get(0).getAction());
	}

	@Test
	public void testSetValueForField() {
		PartialUpdate update = new PartialUpdate("id", "123");
		update.setValueOfField("name", "value-to-set");

		Assert.assertEquals(1, update.getUpdates().size());
		Assert.assertEquals(UpdateAction.SET, update.getUpdates().get(0).getAction());
	}

	@Test
	public void testIncreaseValueForField() {
		PartialUpdate update = new PartialUpdate("id", "123");
		update.increaseValueOfField("popularity", 2);

		Assert.assertEquals(1, update.getUpdates().size());
		Assert.assertEquals(UpdateAction.INC, update.getUpdates().get(0).getAction());
	}

}
