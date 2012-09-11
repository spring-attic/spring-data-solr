/*
 * Copyright (C) 2012 sol-dock-r authors.
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
package at.pagu.soldockr.repository;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import at.pagu.soldockr.AbstractITestWithEmbeddedSolrServer;
import at.pagu.soldockr.ExampleSolrBean;
import at.pagu.soldockr.core.SolrTemplate;

/**
 * @author Christoph Strobl
 */
public class ITestSimpleSolrRepository extends AbstractITestWithEmbeddedSolrServer {
  
  private ExampleSolrBeanRepository repository;
  
  @Before
  public void setUp() {
    repository = new ExampleSolrBeanRepository();
    repository.setSolrOperations(new SolrTemplate(solrServer, null));
  }
  
  @Test
  public void testBeanLifecyle() {
    ExampleSolrBean toInsert = createDefaultExampleBean();
    ExampleSolrBean savedBean = repository.save(toInsert);
    
    Assert.assertSame(toInsert, savedBean);
    
    Assert.assertTrue(repository.exists(savedBean.getId()));
    
    ExampleSolrBean retrieved = repository.findOne(savedBean.getId());
    Assert.assertNotNull(retrieved);
    Assert.assertEquals(savedBean, retrieved);
    
    Assert.assertEquals(1, repository.count());
    
    Assert.assertTrue(repository.exists(savedBean.getId()));
    
    repository.delete(savedBean);
    
    Assert.assertEquals(0, repository.count());
    retrieved = repository.findOne(savedBean.getId());
    Assert.assertNull(retrieved);
  }
  
  @Test
  public void testListFunctions() {
    int objectCount = 100;
    List<ExampleSolrBean> toInsert = new ArrayList<ExampleSolrBean>(objectCount);
    for(int i = 0; i<100; i++) {
      toInsert.add(createExampleBeanWithId(Integer.toString(i)));
    }
    
    repository.save(toInsert);
    
    Assert.assertEquals(objectCount, repository.count());
    
    int counter = 0;
    for(ExampleSolrBean retrievedBean : repository.findAll()) {
      Assert.assertEquals(toInsert.get(counter), retrievedBean);
      counter++;
      if(counter > objectCount) {
        Assert.fail("More beans return than added!");
      }
    }
    
    repository.delete(toInsert.get(0));
    Assert.assertEquals(99, repository.count());
    
    repository.deleteAll();
    
    Assert.assertEquals(0, repository.count());
  }

}
