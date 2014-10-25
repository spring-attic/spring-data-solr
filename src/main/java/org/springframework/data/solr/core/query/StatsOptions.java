/*
 * Copyright 2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Set of options available to get field's statistics.
 * 
 * @author Francisco Spaeth
 * @since 1.4
 */
public class StatsOptions {

	private StatsOptionsState state;

	public StatsOptions() {
		this(new StatsOptionsState());
	}

	private StatsOptions(StatsOptionsState state) {
		this.state = state;
	}

	/**
	 * Adds a field to the statistics to be requested.
	 * 
	 * @param field
	 * @return
	 */
	public FieldStatsOptions addField(Field field) {
		state.fields.add(field);
		return new FieldStatsOptions(field, state);
	}

	/**
	 * Adds a field to the statistics to be requested.
	 * 
	 * @param fieldName
	 * @return
	 */
	public FieldStatsOptions addField(String fieldName) {
		return addField(new SimpleField(fieldName));
	}

	/**
	 * @return fields to request statistics of
	 */
	public Collection<Field> getFields() {
		return Collections.unmodifiableCollection(state.fields);
	}

	/**
	 * Adds a facet on field to the statistics to be requested.
	 * 
	 * @param fieldName
	 * @return
	 */
	public StatsOptions addFacet(Field field) {
		state.facets.add(field);
		return this;
	}

	/**
	 * Adds a facet on field to the statistics to be requested.
	 * 
	 * @param fieldName
	 * @return
	 */
	public StatsOptions addFacet(String fieldName) {
		return addFacet(new SimpleField(fieldName));
	}

	/**
	 * @return the fields to facet on.
	 */
	public Collection<Field> getFacets() {
		return Collections.unmodifiableCollection(state.facets);
	}

	/**
	 * @return the selective facets to be requested.
	 */
	public Map<Field, Collection<Field>> getSelectiveFacets() {
		return Collections.unmodifiableMap(state.selectiveFacets);
	}
	
	/**
	 * Sets the distinct calculation for a given stats request.
	 * 
	 * @param calcDistinct
	 * @return
	 */
	public StatsOptions setCalcDistinct(boolean calcDistinct) {
		state.calcDistinct = calcDistinct;
		return this;
	}

	/**
	 * @return if distinct shall be calculated for the stats request
	 */
	public boolean isCalcDistinct() {
		return state.calcDistinct;
	}

	/**
	 * @return the selective distinct calculation to be requested.
	 */
	public Map<Field, Boolean> getSelectiveCalcDistincts() {
		return Collections.unmodifiableMap(state.selectiveCalcDistinct);
	}
	
	/**
	 * @param field
	 * @return if a distinct calculation shall be done selectively to the given field
	 */
	public Boolean isSelectiveCalcDistincts(Field field) {
		return state.selectiveCalcDistinct.get(field);
	}
	
	/**
	 * Set of options available to get field's statistics having a field as context.
	 * 
	 * @author Francisco Spaeth
	 * @sice 1.4
	 */
	public class FieldStatsOptions extends StatsOptions {

		private Field fieldContext;

		private FieldStatsOptions(Field fieldContext, StatsOptionsState state) {
			super(state);
			this.fieldContext = fieldContext;
		}

		/**
		 * Adds a selective facet over stats result of the field being configured.
		 * 
		 * @param field
		 * @return
		 */
		public FieldStatsOptions addSelectiveFacet(Field field) {
			if (!state.selectiveFacets.containsKey(fieldContext)) {
				state.selectiveFacets.put(fieldContext, new ArrayList<Field>());
			}
			state.selectiveFacets.get(fieldContext).add(field);
			return this;
		}

		public FieldStatsOptions addSelectiveFacet(String fieldName) {
			return addSelectiveFacet(new SimpleField(fieldName));
		}

		public FieldStatsOptions setSelectiveCalcDistinct(boolean calcDistinct) {
			state.selectiveCalcDistinct.put(fieldContext, calcDistinct);
			return this;
		}

	}

	private static class StatsOptionsState {

		private Set<Field> fields = new HashSet<Field>(1);
		private Set<Field> facets = new HashSet<Field>(0);
		private boolean calcDistinct = false;
		private Map<Field, Collection<Field>> selectiveFacets = new HashMap<Field, Collection<Field>>();
		private Map<Field, Boolean> selectiveCalcDistinct = new HashMap<Field, Boolean>();

	}

}
