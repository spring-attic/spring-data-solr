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
package at.pagu.soldockr.core.query;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import at.pagu.soldockr.core.query.Criteria;
import at.pagu.soldockr.core.query.FacetOptions;
import at.pagu.soldockr.core.query.Field;
import at.pagu.soldockr.core.query.Query;
import at.pagu.soldockr.core.query.SimpleField;
import at.pagu.soldockr.core.query.SimpleQuery;

public class SimpleQueryTest {

  @Test(expected = IllegalArgumentException.class)
  public void testAddNullCriteria() {
    new SimpleQuery().addCriteria(null);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testAddCriteriaWithNullField() {
    new SimpleQuery().addCriteria(new Criteria());
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testAddCriteriaWithEmptyFieldname() {
    new SimpleQuery().addCriteria(new Criteria(new SimpleField("")));
  }

  @Test
  public void testAddCriteria() {
    Criteria criteria1 = new Criteria("field_1");
    Criteria criteria2 = new Criteria("field_2");
    Query query = new SimpleQuery().addCriteria(criteria1).addCriteria(criteria2);
    
    Assert.assertEquals(criteria1, query.getCriteria());
    Assert.assertEquals(2, query.getCriteria().getCriteriaChain().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddProjectionNullField() {
    new SimpleQuery().addProjectionOnField((Field) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddProjectionNullFieldName() {
    new SimpleQuery().addProjectionOnField(new SimpleField(StringUtils.EMPTY));
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testAddProjection() {
    Query query = new SimpleQuery().addProjectionOnField(new SimpleField("field_1")).addProjectionOnField(new SimpleField("field_2"));
    Assert.assertEquals(2, ((List) query.getProjectionOnFields()).size());
  }
  
  @Test
  public void testSetPageRequest() {
    SimpleQuery query = new SimpleQuery();
    Assert.assertEquals(SimpleQuery.DEFAULT_PAGE, query.getPageRequest());
    
    Pageable alteredPage = new PageRequest(0, 20);
    
    query.setPageRequest(alteredPage);
    Assert.assertEquals(alteredPage, query.getPageRequest());
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testSetPageRequestWithNullValue() {
    new SimpleQuery().setPageRequest(null);
  }
  
  @Test
  public void testAddFacetOptions() {
    FacetOptions facetOptions = new FacetOptions("field_1", "field_2");
    SimpleQuery query = new SimpleQuery().setFacetOptions(facetOptions);
    Assert.assertEquals(facetOptions, query.getFacetOptions());
  }
  
  @Test
  public void testAddFacetOptionsWithNullValue() {
    SimpleQuery query = new SimpleQuery().setFacetOptions(null);
    Assert.assertNull(query.getFacetOptions());
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testAddFacetOptionsWithoutFacetFields() {
    new SimpleQuery().setFacetOptions(new FacetOptions());
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testAddGroupByNullField() {
    new SimpleQuery().addGroupByField((Field) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddGroupByNullFieldName() {
    new SimpleQuery().addGroupByField(new SimpleField(StringUtils.EMPTY));
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testAddGroupBy() {
    Query query = new SimpleQuery().addGroupByField(new SimpleField("field_1")).addGroupByField(new SimpleField("field_2"));
    Assert.assertEquals(2, ((List) query.getGroupByFields()).size());
  }

}
