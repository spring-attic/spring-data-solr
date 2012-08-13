package at.pagu.soldockr.repository;

import java.util.List;

import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Id;

public class ProductBean {

  @Id
  @Field("id")
  private String id;

  @Field("title")
  private String title;

  @Field("name")
  private String name;

  @Field("textGeneral")
  private String text;

  @Field
  private List<String> categories;

  @Field
  private Float weight;

  @Field
  private Float price;

  @Field
  private Integer popularity;

  @Field("inStock")
  private boolean available;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
