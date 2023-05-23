/*
 * Copyright 2012 - 2018 the original author or authors.
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
package org.springframework.data.solr.repository;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.List;

import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.SolrDocument;

/**
 * @author Christoph Strobl
 */
@SolrDocument(collection = "collection1")
@Getter
//@Setter
@ToString
public class ProductBean {

	@Id @Field("id") private String id;

	@Field("title") private List<String> title;

	@Field("name") private String name;

	@Field("description") private String description;

	private String text;

	@Field private List<String> categories;

	@Field private Float weight;

	@Field private Float price;

	@Field private Integer popularity;

	@Field("inStock") private boolean available;

	@Field("store") private String location;

	@Field("last_modified") private Date lastModified;

	@Field("content_type_s") private ContentType contentType;

	public ProductBean() {
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ProductBean other = (ProductBean) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public enum ContentType {
		TEXT, JSON, HTML
	}
}
