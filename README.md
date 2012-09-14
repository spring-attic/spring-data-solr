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

The SolrCrudRepository extends CrudRepository 

    public interface SolrCrudRepository<T, ID extends Serializable> extends
        SolrRepository<T, ID> {
    } 
    
The SimpleSolrRepository implementation uses SolrJ converters for entity transformation and therefore requires fields to be annotated with org.apache.solr.client.solrj.beans.Field.

    public interface SolrProductRepository extends SolrCrudRepository<Product, String> {

        //Derived Query will be "q=popularity:<popularity>&start=<page.number>&rows=<page.size>"
        Page<Product> findByPopularity(Integer popularity, Pageable page);

        //Will execute count before to determine total number of elements
        //Derived Query will be "q=name:<name>*&start=0&rows=<result of count query for q=name:<value>>"
        List<Product> findByNameStartingWith(String name);

        //Derived Query will be "q=inStock:true&start=<page.number>&rows=<page.size>"
        Page<Product> findByAvailableTrue(Pageable page);
  
        @Query("inStock:false")
        Page<Product> findByAvailableFalseUsingAnnotatedQuery(Pageable page);
    }

 SolrRepositoryFactory will create the implementation for you.
 
    public class SolrProductSearchRepositoryFactory {

        @Autwired
        private SolrOperations solrOperations;
  
        public SolrProductRepository create() {
  	        return new SolrRepositoryFactory(this.solrOperations).getRepository(SolrProductRepository.class);
        }
  
    }
    
Furthermore you may provide a custom implementation for some operations.

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
            return solrTemplate.executeListQuery(new SimpleQuery(new SimpleStringCriteria("name:"+value)).setPageRequest(page), Product.class);
        }

    }
    
    public interface SolrProductRepository extends CustomSolrRepository, SolrCrudRepository<Product, String> {
    	
    	Page<Product> findByPopularity(Integer popularity, Pageable page);
    	
    }
    
    public class CustomSolrProductSearchRepositoryFactory {

        @Autwired
        private SolrOperations solrOperations;
  
        public SolrProductRepository create() {
  	        return new SolrRepositoryFactory(this.solrOperations).getRepository(SolrProductRepository.class, CustomSolrRepositoryImpl(this.solrOperations));
        }
  
    }

Contributing to Spring Data
---------------------------

Here are some ways for you to get involved in the community:

* Get involved with the Spring community on the Spring Community Forums.  Please help out on the [forum](http://forum.springsource.org/forumdisplay.php?f=80) by responding to questions and joining the debate.
* Create [JIRA](https://jira.springsource.org/browse/DATASOLR) tickets for bugs and new features and comment and vote on the ones that you are interested in.  
* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
* Watch for upcoming articles on Spring by [subscribing](http://www.springsource.org/node/feed) to springframework.org

Before we accept a non-trivial patch or pull request we will need you to sign the [contributor's agreement](https://support.springsource.com/spring_committer_signup).  Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do.  Active contributors might be asked to join the core team, and given the ability to merge pull requests. 