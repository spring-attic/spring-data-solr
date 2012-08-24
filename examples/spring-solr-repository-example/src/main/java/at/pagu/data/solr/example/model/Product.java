/*
 * Copyright (C) 2012 j73x73r@gmail.com.
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
package at.pagu.data.solr.example.model;

import java.util.List;

import org.apache.solr.client.solrj.beans.Field;

/**
 * @author Christoph Strobl
 */
public class Product implements SearchableProduct {

  @Field(ID_FIELD)
  private String id;

  @Field(NAME_FIELD)
  private String name;

  @Field(CATEGORY_FIELD)
  private List<String> categories;

  @Field(WEIGHT_FIELD)
  private Float weight;

  @Field(PRICE_FIELD)
  private Float price;

  @Field(POPULARITY_FIELD)
  private Integer popularity;

  @Field(AVAILABLE_FIELD)
  private boolean available;

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

  public List<String> getCategories() {
    return categories;
  }

  public void setCategories(List<String> categories) {
    this.categories = categories;
  }

  public Float getWeight() {
    return weight;
  }

  public void setWeight(Float weight) {
    this.weight = weight;
  }

  public Float getPrice() {
    return price;
  }

  public void setPrice(Float price) {
    this.price = price;
  }

  public Integer getPopularity() {
    return popularity;
  }

  public void setPopularity(Integer popularity) {
    this.popularity = popularity;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  @Override
  public String toString() {
    return "Product [id=" + id + ", name=" + name + ", categories=" + categories + ", weight=" + weight + ", price=" + price + ", popularity=" + popularity + ", available=" + available + "]";
  }

}
