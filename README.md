Spring Data Solr
======================

The primary goal of the [Spring Data](http://www.springsource.org/spring-data) project is to make it easier to build Spring-powered applications that use new data access technologies such as non-relational databases, map-reduce frameworks, and cloud based data services.

The Spring Data Solr project provides integration with the [Apache Solr](http://lucene.apache.org/solr/) search engine 

Providing its own extensible ```MappingSolrConverter``` as alternative to ```DocumentObjectBinder``` Spring Data Solr handles inheritance as well as usage of custom Types such as  ```GeoLocation``` or ```DateTime```.

Getting Help
------------

* [Reference Documentation](http://docs.spring.io/spring-data/data-solr/docs/current-SNAPSHOT/reference/html/)
* [API Documentation](http://docs.spring.io/spring-data/data-solr/docs/current-SNAPSHOT/api/)
* [Spring Data Project](http://www.springsource.org/spring-data)
* [Issues](https://jira.springsource.org/browse/DATASOLR)
* [Code Analysis](https://sonar.springsource.org/dashboard/index/org.springframework.data:spring-data-solr)

If you are new to Spring as well as to Spring Data, look for information about [Spring projects](http://www.springsource.org/projects).

Quick Start
-----------

### SolrTemplate
```SolrTemplate``` is the central support class for solr operations.
 
 
### SolrRepository
A default implementation of ```SolrRepository```, aligning to the generic Repository Interfaces, is provided. Spring can do the Repository implementation for you depending on method names in the interface definition.

The ```SolrCrudRepository``` extends ```PagingAndSortingRepository``` 

```java
   public interface SolrCrudRepository<T, ID extends Serializable> extends SolrRepository<T, ID>, PagingAndSortingRepository<T, ID> {
   } 
```
    
The ```SimpleSolrRepository``` implementation uses ```MappingSolrConverter```. In order support native solrj mapping via ```DocumentObjectBinder``` fields have to be annotated with ```org.apache.solr.client.solrj.beans.Field``` or ```org.springframework.data.solr.core.mapping.Indexed```.
To enable native solrj mapping use ```SolrJConverter``` along with ```SolrTemplate```. 

```java
public interface SolrProductRepository extends SolrCrudRepository<Product, String> {
  
  //Derived Query will be "q=popularity:<popularity>&start=<page.number>&rows=<page.size>"
  Page<Product> findByPopularity(Integer popularity, Pageable page);
  
  //Will execute count prior to determine total number of elements
  //Derived Query will be "q=name:<name>*&start=0&rows=<result of count query for q=name:<name>>"
  List<Product> findByNameStartingWith(String name);
  
  //Derived Query will be "q=inStock:true&start=<page.number>&rows=<page.size>"
  Page<Product> findByAvailableTrue(Pageable page);
  
  //Derived Query will be "q=inStock:<inStock>&start=<page.number>&rows=<page.size>"
  @Query("inStock:?0")
  Page<Product> findByAvailableUsingAnnotatedQuery(boolean inStock, Pageable page);
  
  //Will execute count prior to determine total number of elements
  //Derived Query will be "q=inStock:false&start=0&rows=<result of count query for q=inStock:false>&sort=name desc"
  List<Product> findByAvailableFalseOrderByNameDesc();
  
  //Execute faceted search 
  //Query will be "q=name:<name>&facet=true&facet.field=cat&facet.limit=20&start=<page.number>&rows=<page.size>"
  @Query(value = "name:?0")
  @Facet(fields = { "cat" }, limit=20)
  FacetPage<Product> findByNameAndFacetOnCategory(String name, Pageable page);
  
  //Boosting criteria
  //Query will be "q=name:<name>^2 OR description:<description>&start=<page.number>&rows=<page.size>"
  Page<Product> findByNameOrDescription(@Boost(2) String name, String description, Pageable page);
  
  //Highlighting results
  //Query will be "q=name:(<name...>)&hl=true&hl.fl=*"
  @Highlight
  HighlightPage<Product> findByNameIn(Collection<String> name, Pageable page);
  
  //Spatial Search
  //Query will be "q=location:[<bbox.start.latitude>,<bbox.start.longitude> TO <bbox.end.latitude>,<bbox.end.longitude>]"
  Page<Product> findByLocationNear(BoundingBox bbox);
  
  //Spatial Search
  //Query will be "q={!geofilt pt=<location.latitude>,<location.longitude> sfield=location d=<distance.value>}"
  Page<Product> findByLocationWithin(GeoLocation location, Distance distance);
  
}
```

```SolrRepositoryFactory``` will create the implementation for you.

```java 
public class SolrProductSearchRepositoryFactory {
  
  @Autwired
  private SolrOperations solrOperations;
  
  public SolrProductRepository create() {
    return new SolrRepositoryFactory(this.solrOperations).getRepository(SolrProductRepository.class);
  }
  
}
```    
   
Furthermore you may provide a custom implementation for some operations.

```java
public interface SolrProductRepository extends SolrCrudRepository<Product, String>, SolrProductRepositoryCustom {
  
  @Query(fields = { "id", "name", "popularity" })
  Page<Product> findByPopularity(Integer popularity, Pageable page);
  
  List<Product> findByAuthorLike(String author);
  
}

public interface SolrProductRepositoryCustom {
  
  Page<Product> findProductsByCustomImplementation(String value, Pageable page)
  
}

public class SolrProductRepositoryImpl implements SolrProductRepositoryCustom {
  
  private SolrOperations solrTemplate;
  
  @Override
  public Page<Product> findProductsByCustomImplementation(String value, Pageable page) {
    Query query = new SimpleQuery(new SimpleStringCriteria("name:"+value)).setPageRequest(page);
    return solrTemplate.queryForPage(query, Product.class);
  }
  
  @Autowired
  public void setOperations(SolrOperations operations) {
    this.operations = operations;
  }
  
}
```

Go on and use it as shown below:

```java
@Service
public class ProductService {
  
  private SolrProductRepository repository;
  
  public void doSomething() {
    repository.deleteAll();
    
    Product product = new Product("spring-data-solr");
    product.setAuthor("Christoph Strobl");
    product.setCategory("search");
    repository.save(product);
    
    Product singleProduct = repository.findById("spring-data-solr");
    List<Product> productList = repository.findByAuthorLike("Chr");
  }
  
  @Autowired
  public void setRepository(SolrProductRepository repository) {
    this.repository = repository;
  }
}
```

### XML Namespace

You can set up repository scanning via xml configuration, which will happily create your repositories.
 
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:solr="http://www.springframework.org/schema/data/solr"
  xsi:schemaLocation="http://www.springframework.org/schema/data/solr http://www.springframework.org/schema/data/solr/spring-solr-1.0.xsd
    http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
  
  <solr:repositories base-package="com.acme.repository" />
  <solr:solr-server id="solrServer" url="http://localhost:8983/solr" />
  
  <bean id="solrTemplate" class="org.springframework.data.solr.core.SolrTemplate">
    <constructor-arg ref="solrServer" />
  </bean>
  
</beans>
```

Maven
-----

### RELEASE

```xml
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-solr</artifactId>
  <version>1.0.0.RELEASE</version>
</dependency>  
```

### Build Snapshot

```xml
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-solr</artifactId>
  <version>1.1.0.BUILD-SNAPSHOT</version>
</dependency> 

<repository>
  <id>spring-maven-snapshot</id>
  <url>http://repo.springsource.org/libs-snapshot</url>
</repository>  
```

Contributing to Spring Data
---------------------------
Please refer to [CONTRIBUTING](https://github.com/SpringSource/spring-data-solr/blob/master/CONTRIBUTING.md)
