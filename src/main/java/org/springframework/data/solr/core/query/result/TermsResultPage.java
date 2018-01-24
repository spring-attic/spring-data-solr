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
package org.springframework.data.solr.core.query.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.solr.core.query.Field;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 */
public class TermsResultPage implements TermsPage {

	private Map<StringPageKey, List<TermsFieldEntry>> termsMap = new LinkedHashMap<>(1);

	public final void addTermsResult(List<TermsFieldEntry> entries, Field field) {
		Assert.notNull(field, "Cannot add terms for 'null' field.");
		this.termsMap.put(new StringPageKey(field.getName()), entries);
	}

	public final void addTermsResult(List<TermsFieldEntry> entries, String fieldname) {
		Assert.notNull(fieldname, "Cannot add terms for 'null' field.");
		this.termsMap.put(new StringPageKey(fieldname), entries);
	}

	public void addAllTerms(Map<String, List<TermsFieldEntry>> pageMap) {
		for (Map.Entry<String, List<TermsFieldEntry>> entry : pageMap.entrySet()) {
			addTermsResult(entry.getValue(), entry.getKey());
		}
	}

	@Override
	public Iterable<TermsFieldEntry> getTermsForField(String fieldname) {
		Iterable<TermsFieldEntry> terms = this.termsMap.get(new StringPageKey(fieldname));
		return terms != null ? terms : Collections.<TermsFieldEntry> emptyList();
	}

	public Iterable<TermsFieldEntry> getTerms(Field field) {
		Assert.notNull(field, "Field cannot be null.");
		return getTermsForField(field.getName());
	}

	public Collection<Field> getTermsFields() {
		if (this.termsMap.isEmpty()) {
			return Collections.emptyList();
		}

		List<Field> fields = new ArrayList<>(this.termsMap.size());
		for (StringPageKey pageKey : this.termsMap.keySet()) {
			fields.add(new SimpleField(pageKey.getKey()));
		}
		return fields;
	}

	@Override
	public Iterator<TermsFieldEntry> iterator() {
		return getContent().iterator();
	}

	@Override
	public List<TermsFieldEntry> getContent() {
		List<TermsFieldEntry> values = new ArrayList<>();
		for (List<TermsFieldEntry> entries : termsMap.values()) {
			values.addAll(entries);
		}
		return values;
	}

	@Override
	public boolean hasContent() {
		return !termsMap.isEmpty();
	}

}
