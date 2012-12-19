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
package org.springframework.data.solr.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.FacetPage;
import org.springframework.data.solr.core.query.result.FacetQueryEntry;
import org.springframework.data.solr.core.query.result.SimpleFacetFieldEntry;
import org.springframework.data.solr.core.query.result.SimpleFacetQueryEntry;
import org.springframework.util.Assert;

/**
 * Use Result Helper to extract various parameters from the QueryResponse and convert it into a proper Format taking
 * care of non existent and null elements with the response.
 * 
 * @author Christoph Strobl
 */
final class ResultHelper {

	private ResultHelper() {
	}

	static Map<Field, Page<FacetFieldEntry>> convertFacetQueryResponseToFacetPageMap(FacetQuery query,
			QueryResponse response) {
		Assert.notNull(query, "Cannot convert response for 'null', query");

		if (!hasFacets(query, response)) {
			return Collections.emptyMap();
		}
		Map<Field, Page<FacetFieldEntry>> facetResult = new HashMap<Field, Page<FacetFieldEntry>>();

		if (CollectionUtils.isNotEmpty(response.getFacetFields())) {
			int initalPageSize = query.getFacetOptions().getPageable().getPageSize();
			for (FacetField facetField : response.getFacetFields()) {
				if (facetField != null && StringUtils.isNotBlank(facetField.getName())) {
					Field field = new SimpleField(facetField.getName());
					if (CollectionUtils.isNotEmpty(facetField.getValues())) {
						List<FacetFieldEntry> pageEntries = new ArrayList<FacetFieldEntry>(initalPageSize);
						for (Count count : facetField.getValues()) {
							if (count != null) {
								pageEntries.add(new SimpleFacetFieldEntry(field, count.getName(), count.getCount()));
							}
						}
						facetResult.put(field, new FacetPage<FacetFieldEntry>(pageEntries, query.getFacetOptions().getPageable(),
								facetField.getValueCount()));
					} else {
						facetResult.put(field, new FacetPage<FacetFieldEntry>(Collections.<FacetFieldEntry> emptyList(), query
								.getFacetOptions().getPageable(), 0));
					}
				}
			}
		}
		return facetResult;
	}

	static List<FacetQueryEntry> convertFacetQueryResponseToFacetQueryResult(FacetQuery query, QueryResponse response) {
		Assert.notNull(query, "Cannot convert response for 'null', query");

		if (!hasFacets(query, response)) {
			return Collections.emptyList();
		}

		List<FacetQueryEntry> facetResult = new ArrayList<FacetQueryEntry>();

		if (MapUtils.isNotEmpty(response.getFacetQuery())) {
			for (Entry<String, Integer> entry : response.getFacetQuery().entrySet()) {
				facetResult.add(new SimpleFacetQueryEntry(entry.getKey(), entry.getValue()));
			}
		}
		return facetResult;
	}

	private static boolean hasFacets(FacetQuery query, QueryResponse response) {
		return query.hasFacetOptions() && response != null;
	}

}
