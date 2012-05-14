package com.google.soldockr.repository;

import org.springframework.data.repository.CrudRepository;

public interface SolrCrudRepository<T> extends SolrRepository<T>, CrudRepository<T, String>  {

}
