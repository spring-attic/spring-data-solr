/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.solr.core.query.result;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Offset.offset;

import java.util.Date;

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

		assertThat(stats.getMinAsString()).isEqualTo("13");
		assertThat(stats.getMinAsDouble()).isEqualTo(Double.valueOf(13));
		assertThat(stats.getMinAsDate()).as("null").isNull();
	}

	@Test // DATASOLR-160
	public void testGetMinDate() {

		SimpleStatsResult stats = new SimpleStatsResult();
		Date date = new Date();
		stats.setMin(date);

		assertThat(stats.getMinAsString()).isEqualTo(date.toString());
		assertThat(stats.getMinAsDouble()).isNull();
		assertThat(stats.getMinAsDate()).isEqualTo(date);
	}

	@Test // DATASOLR-160
	public void testGetMaxDouble() {

		SimpleStatsResult stats = new SimpleStatsResult();
		stats.setMax(13);

		assertThat(stats.getMaxAsString()).isEqualTo("13");
		assertThat(stats.getMaxAsDouble()).isEqualTo(Double.valueOf(13));
		assertThat(stats.getMaxAsDate()).as("null").isNull();
	}

	@Test // DATASOLR-160
	public void testGetMaxDate() {

		SimpleStatsResult stats = new SimpleStatsResult();
		Date date = new Date();
		stats.setMax(date);

		assertThat(stats.getMaxAsString()).isEqualTo(date.toString());
		assertThat(stats.getMaxAsDouble()).isNull();
		assertThat(stats.getMaxAsDate()).isEqualTo(date);
	}

	@Test // DATASOLR-404
	public void testMeanDate() {

		SimpleStatsResult stats = new SimpleStatsResult();
		Date date = new Date();
		stats.setMean(date);

		assertThat(stats.getMeanAsDate()).isEqualTo(date);
	}

	@Test // DATASOLR-404
	public void testMeanNumber() {

		SimpleStatsResult stats = new SimpleStatsResult();
		stats.setMean(1L);

		assertThat(stats.getMeanAsDouble()).isCloseTo(1D, offset(0D));
	}

	@Test // DATASOLR-404
	public void testMeanDateWhenNoDate() {

		SimpleStatsResult stats = new SimpleStatsResult();
		stats.setMean("o_O");

		assertThat(stats.getMeanAsDate()).isNull();
	}
}
