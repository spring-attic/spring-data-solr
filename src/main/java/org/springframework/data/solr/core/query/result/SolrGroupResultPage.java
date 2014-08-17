/*
 * Copyright 2012 - 2014 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.Function;
import org.springframework.data.solr.core.query.Query;
import org.springframework.util.Assert;

/**
 * Implementation for solr group result page.
 * 
 * @author Francisco Spaeth
 *
 * @param <T>
 */
public class SolrGroupResultPage<T> extends SolrResultPage<GroupResult<T>> implements GroupPage<T> {

	private static final long serialVersionUID = 1L;
	private Map<String, GroupResult<T>> resultMap;
	private Map<Object, String> objectsName;

	public SolrGroupResultPage(List<GroupResult<T>> content, Map<Object, String> objectsName) {
		super(content);
		resultMap = new HashMap<String, GroupResult<T>>();
		for (GroupResult<T> gr : content) {
			resultMap.put(gr.getName(), gr);
		}
		this.objectsName = objectsName;
	}
	
	public GroupResult<T> getGroupResult(Field field) {
		Assert.notNull(field, "group result field must not be null");
		return resultMap.get(field.getName());
	}
	
	public GroupResult<T> getGroupResult(Function function) {
		Assert.notNull(function, "group result function must not be null");
		return resultMap.get(objectsName.get(function));
	}
	
	public GroupResult<T> getGroupResult(Query query) {
		Assert.notNull(query, "group result query must not be null");
		return resultMap.get(objectsName.get(query));
	}
	
	public GroupResult<T> getGroupResult(String name) {
		Assert.notNull(name, "group result name must not be null");
		return resultMap.get(name);
	}
	
}
