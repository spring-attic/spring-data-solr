Spring Data Solr
======================

The primary goal of the [Spring Data](http://www.springsource.org/spring-data) project is to make it easier to build Spring-powered applications that use new data access technologies such as non-relational databases, map-reduce frameworks, and cloud based data services.

The Spring Data Solr project provides integration with the [Apache Solr](http://lucene.apache.org/solr/) search engine. 

Getting Help
------------

Help is currently under construction but on its way.

If you are new to Spring as well as to Spring Data, look for information about [Spring projects](http://www.springsource.org/projects). 


Quick Start
-----------

### SolrTemplate
SolrTemplate is the central support class for solr operations.
 
 
### SolrRepository
A default implementation of SolrRepository, aligning to the generic Repository Interfaces, is provided. Spring can do the Repository implementation for you depending on method names in the interface definition.

The SolrCrudRepository extends PagingAndSortingRepository 

```java
    public interface SolrCrudRepository<T, ID extends Serializable> extends SolrRepository<T, ID>, PagingAndSortingRepository<T, ID> {
    } 
```
    
The SimpleSolrRepository implementation uses SolrJ converters for entity transformation and therefore requires fields to be annotated with org.apache.solr.client.solrj.beans.Field.

```java
    public interface SolrProductRepository extends SolrCrudRepository<Product, String> {

        //Derived Query will be "q=popularity:<popularity>&start=<page.number>&rows=<page.size>"
        Page<Product> findByPopularity(Integer popularity, Pageable page);

        //Will execute count prior to determine total number of elements
        //Derived Query will be "q=name:<name>*&start=0&rows=<result of count query for q=name:<name>>"
        List<Product> findByNameStartingWith(String name);

        //Derived Query will be "q=inStock:true&start=<page.number>&rows=<page.size>"
        Page<Product> findByAvailableTrue(Pageable page);
  
        @Query("inStock:false")
        Page<Product> findByAvailableFalseUsingAnnotatedQuery(Pageable page);
        
        //Will execute count prior to determine total number of elements
        //Derived Query will be q=inStock:false&start=0&rows=<result of count query for q=inStock:false>&sort=name desc
        List<ProductBean> findByAvailableFalseOrderByNameDesc();
    }
```

 SolrRepositoryFactory will create the implementation for you.

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
    public interface CustomSolrRepository {

        Page<Product> findProductsByCustomImplementation(String value, Pageable page);
	
    }

    public class CustomSolrRepositoryImpl implements CustomSolrRepository {
	
        private SolrOperations solrTemplate;
	
        public CustomSolrRepositoryImpl(SolrOperations solrTemplate) {
            super();
            this.solrTemplate = solrTemplate;
        }

        @Override
        public Page<Product> findProductsByCustomImplementation(String value, Pageable page) {
            Query query = new SimpleQuery(new SimpleStringCriteria("name:"+value)).setPageRequest(page);
            return solrTemplate.executeListQuery(query, Product.class);
        }

    }
    
    public interface SolrProductRepository extends CustomSolrRepository, SolrCrudRepository<Product, String> {
    	
    	Page<Product> findByPopularity(Integer popularity, Pageable page);
    	
    }
    
    public class CustomSolrProductSearchRepositoryFactory {

        @Autwired
        private SolrOperations solrOperations;
  
        public SolrProductRepository create() {
  	        return new SolrRepositoryFactory(this.solrOperations)
  	            .getRepository(SolrProductRepository.class, new CustomSolrRepositoryImpl(this.solrOperations));
        }
  
    }
```

Contributing to Spring Data
---------------------------
Please refer to [CONTRIBUTING](https://github.com/SpringSource/spring-data-solr/blob/master/CONTRIBUTING.md)
