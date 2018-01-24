/*
 * Copyright 2012-2017 the original author or authors.
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
package org.springframework.data.solr;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Version;
import org.springframework.data.solr.core.mapping.SolrDocument;

/**
 * @author Christoph Strobl
 */
@SolrDocument(solrCoreName = "collection1")
public class ExampleSolrBean {

	@Field private String id;

	@Field private String name;

	@Field("cat") private List<String> category;

	@Field private float price;

	@Field private boolean inStock;

	@Field private Integer popularity;

	@Field("last_modified") private Date lastModified;

	@Field private String store;

	@Field("manu_id_s") private String manufacturerId;

	@Field private Float distance;

	@Version @Field("_version_") private Long version;

	public ExampleSolrBean() {
		this.category = new ArrayList<>();
	}

	public ExampleSolrBean(String id, String name, String category) {
		this();
		this.id = id;
		this.name = name;
		this.category.add(category);
	}

	public ExampleSolrBean(String id, String name, String category, float price, boolean inStock) {
		this(id, name, category);
		this.price = price;
		this.inStock = inStock;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getCategory() {
		return category;
	}

	public void setCategory(List<String> category) {
		this.category = category;
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public boolean isInStock() {
		return inStock;
	}

	public void setInStock(boolean inStock) {
		this.inStock = inStock;
	}

	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}

	public Integer getPopularity() {
		return popularity;
	}

	public void setPopularity(Integer popularity) {
		this.popularity = popularity;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public String getStore() {
		return store;
	}

	public void setStore(String store) {
		this.store = store;
	}

	public String getManufacturerId() {
		return manufacturerId;
	}

	public void setManufacturerId(String manufacturerId) {
		this.manufacturerId = manufacturerId;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public Float getDistance() {
		return distance;
	}

	public void setDistance(Float distance) {
		this.distance = distance;
	}

	@Override
	public String toString() {
		return "ExampleSolrBean [id=" + id + ", name=" + name + "]";
	}

}
