/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.solr.core.query.result;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Francisco Spaeth
 * @author Christoph Strobl
 */
public class SimpleStatsResultTests {

	@Test // DATASOLR-160
	public void testGetMinDouble() {

		SimpleStatsResult stats = new SimpleStatsResult();
		stats.setMin(13);

		Assert.assertEquals("13", stats.getMinAsString());
		Assert.assertEquals(Double.valueOf(13), stats.getMinAsDouble());
		Assert.assertNull(null, stats.getMinAsDate());
	}

	@Test // DATASOLR-160
	public void testGetMinDate() {

		SimpleStatsResult stats = new SimpleStatsResult();
		Date date = new Date();
		stats.setMin(date);

		Assert.assertEquals(date.toString(), stats.getMinAsString());
		Assert.assertNull(stats.getMinAsDouble());
		Assert.assertEquals(date, stats.getMinAsDate());
	}

	@Test // DATASOLR-160
	public void testGetMaxDouble() {

		SimpleStatsResult stats = new SimpleStatsResult();
		stats.setMax(13);

		Assert.assertEquals("13", stats.getMaxAsString());
		Assert.assertEquals(Double.valueOf(13), stats.getMaxAsDouble());
		Assert.assertNull(null, stats.getMaxAsDate());
	}

	@Test // DATASOLR-160
	public void testGetMaxDate() {

		SimpleStatsResult stats = new SimpleStatsResult();
		Date date = new Date();
		stats.setMax(date);

		Assert.assertEquals(date.toString(), stats.getMaxAsString());
		Assert.assertNull(stats.getMaxAsDouble());
		Assert.assertEquals(date, stats.getMaxAsDate());
	}

}
