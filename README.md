[![Spring Data for Apache Solr](https://spring.io/badges/spring-data-solr/ga.svg)](http://projects.spring.io/spring-data-solr/#quick-start)
[![Spring Data for Apache Solr](https://spring.io/badges/spring-data-solr/snapshot.svg)](http://projects.spring.io/spring-data-solr/#quick-start)

Spring Data for Apache Solr
===========================

The primary goal of the [Spring Data](http://projects.spring.io/spring-data) project is to make it easier to build Spring-powered applications that use new data access technologies such as non-relational databases, map-reduce frameworks, and cloud based data services.

The Spring Data for Apache Solr project provides integration with the [Apache Solr](http://lucene.apache.org/solr/) search engine 

Providing its own extensible ```MappingSolrConverter``` as alternative to ```DocumentObjectBinder``` Spring Data for Apache Solr handles inheritance as well as usage of custom Types such as  ```Point``` or ```DateTime```.

Getting Help
------------

* [Reference Documentation](http://docs.spring.io/spring-data/data-solr/docs/current/reference/html/)
* [API Documentation](http://docs.spring.io/spring-data/data-solr/docs/current/api/)
* [Spring Data Project](http://projects.spring.io/spring-data)
* [Issues](https://jira.springsource.org/browse/DATASOLR)
* [Code Analysis](https://sonar.springsource.org/dashboard/index/org.springframework.data:spring-data-solr)
* [Questions](http://stackoverflow.com/questions/tagged/spring-data-solr)

If you are new to Spring as well as to Spring Data, look for information about [Spring projects](https://spring.io/projects).

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
    
The ```SimpleSolrRepository``` implementation uses ```MappingSolrConverter```. In order support native solrj mapping via ```DocumentObjectBinder``` fields have to be annotated with ```org.apache.solr.client.solrj.beans.Field```. ```org.springframework.data.solr.core.mapping.Indexed``` can be used as substitute for ```Field``` offering additional attributes to be used eg. for index time boosting.

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
  Page<Product> findByLocationNear(Box bbox);
  
  //Spatial Search
  //Query will be "q={!geofilt pt=<location.latitude>,<location.longitude> sfield=location d=<distance.value>}"
  Page<Product> findByLocationWithin(Point location, Distance distance);
  
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
@Configuration
@EnableSolrRepositories(basePackages = { "com.acme.sorl" }), multicoreSupport = true)
public class SolrContext {
  
  private @Resource Environment env;

  @Bean
  public SolrClient solrClient() throws MalformedURLException, IllegalStateException {
    return new HttpSolrClient(env.getRequiredProperty("solr.host"));
  }

}

@Service
public class ProductService {
  
  private SolrProductRepository repository;

  @Autowired
  public ProductService(SolrProductRepository repository) {
    this.repository = repository;
  }
  
  public void doSomething() {
    repository.deleteAll();
    
    Product product = new Product("spring-data-solr");
    product.setAuthor("Christoph Strobl");
    product.setCategory("search");
    repository.save(product);
    
    Product singleProduct = repository.findById("spring-data-solr");
    List<Product> productList = repository.findByAuthorLike("Chr");
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
  xsi:schemaLocation="http://www.springframework.org/schema/data/solr http://www.springframework.org/schema/data/solr/spring-solr.xsd
    http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
  
  <solr:repositories base-package="com.acme.repository" multicoreSupport="true" />
  <solr:solr-client id="solrClient" url="http://localhost:8983/solr" />
  
</beans>
```

### Automatic Schema Population
Automatic schema population will inspect your domain types whenever the applications context is refreshed and populate new fields to your index based on the properties configuration.
This requires solr to run in [Schemaless Mode](https://cwiki.apache.org/confluence/display/solr/Schemaless+Mode).

Use `@Indexed` to provide additional details like specific solr types to use.

```java
@Configuration
@EnableSolrRepositories(schemaCreationSupport = true, multicoreSupport = true)
class Config {

  @Bean
  public SolrClient solrClient() {
    return new HttpSolrClient("http://localhost:8983/solr");
  }
}

@Document(coreName="collection1")
class Product {
  
  @Id String id;
  @Indexed(solrType="text_general") String author;
  @Indexed("cat") List<String> category;

}
```

```javascript
// curl ../solr/collection1/schema/fields -X POST -H 'Content-type:application/json'
[
  {
    "name":"id",
    "type":"string",
    "stored":true,
    "indexed":true,
    "multiValued":false
  }
  {
    "name":"author",
    "type":"text_general",
    "stored":true,
    "indexed":true,
    "multiValued":false
  }
  {
    "name":"cat",
    "type":"string",
    "stored":true,
    "indexed":true,
    "multiValued":true
  }
]
```

Maven
-----

### RELEASE

```xml
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-solr</artifactId>
  <version>${version}.RELEASE</version>
</dependency>  
```

### Build Snapshot

```xml
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-solr</artifactId>
  <version>${version}.BUILD-SNAPSHOT</version>
</dependency> 

<repository>
  <id>spring-maven-snapshot</id>
  <url>http://repo.spring.io/libs-snapshot</url>
</repository>  
```

## Contributing to Spring Data

Here are some ways for you to get involved in the community:

* Get involved with the Spring community on Stackoverflow and help out on the [spring-data-solr](http://stackoverflow.com/questions/tagged/spring-data-solr) tag by responding to questions and joining the debate.
* Create [JIRA](https://jira.spring.io/browse/DATASOLR) tickets for bugs and new features and comment and vote on the ones that you are interested in.  
* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
* Watch for upcoming articles on Spring by [subscribing](http://spring.io/blog) to spring.io.

Before we accept a non-trivial patch or pull request we will need you to [sign the Contributor License Agreement](https://cla.pivotal.io/sign/spring). Signing the contributor’s agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. If you forget to do so, you'll be reminded when you submit a pull request. Active contributors might be asked to join the core team, and given the ability to merge pull requests.

Stay in touch
-------------
Follow the project team ([@stroblchristoph](https://twitter.com/stroblchristoph), [@SpringData](https://twitter.com/springdata)) on Twitter. Releases are announced via our news feed.
