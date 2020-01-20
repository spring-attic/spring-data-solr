/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * @author Christoph Strobl
 */
public class PartialUpdateTests {

	@Test
	public void testAddValueToField() {
		PartialUpdate update = new PartialUpdate("id", "123");
		update.addValueToField("name", "value-to-add");

		assertThat(update.getUpdates().size()).isEqualTo(1);
		assertThat(update.getUpdates().get(0).getAction()).isEqualTo(UpdateAction.ADD);
	}

	@Test
	public void testSetValueForField() {
		PartialUpdate update = new PartialUpdate("id", "123");
		update.setValueOfField("name", "value-to-set");

		assertThat(update.getUpdates().size()).isEqualTo(1);
		assertThat(update.getUpdates().get(0).getAction()).isEqualTo(UpdateAction.SET);
	}

	@Test
	public void testIncreaseValueForField() {
		PartialUpdate update = new PartialUpdate("id", "123");
		update.increaseValueOfField("popularity", 2);

		assertThat(update.getUpdates().size()).isEqualTo(1);
		assertThat(update.getUpdates().get(0).getAction()).isEqualTo(UpdateAction.INC);
	}

}
