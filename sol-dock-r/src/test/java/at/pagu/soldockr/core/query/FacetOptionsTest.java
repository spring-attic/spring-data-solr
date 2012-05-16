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

import junit.framework.Assert;

import org.junit.Test;

import at.pagu.soldockr.core.query.FacetOptions;
import at.pagu.soldockr.core.query.Field;
import at.pagu.soldockr.core.query.SimpleField;
import at.pagu.soldockr.core.query.FacetOptions.FacetSort;

public class FacetOptionsTest {

  @Test
  public void testFacetOptionsEmptyConstructor() {
    FacetOptions options = new FacetOptions();
    Assert.assertFalse(options.hasFields());
  }

  @Test
  public void testFacetOptionsConstructorSingleField() {
    FacetOptions options = new FacetOptions(new SimpleField("field_1"));
    Assert.assertTrue(options.hasFields());
    Assert.assertEquals(1, options.getFacetOnFields().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFacetOptionsConstructorSingleNullValueField() {
    new FacetOptions((SimpleField) null);
  }

  @Test
  public void testFacetOptionsConstructorSingleFieldname() {
    FacetOptions options = new FacetOptions("field_1");
    Assert.assertTrue(options.hasFields());
    Assert.assertEquals(1, options.getFacetOnFields().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFacetOptionsConstructorSingleNullValueFieldname() {
    new FacetOptions((String) null);
  }

  @Test
  public void addFacetOnField() {
    FacetOptions options = new FacetOptions();
    options.addFacetOnField(new SimpleField("field_1"));
    options.addFacetOnField(new SimpleField("field_2"));

    Assert.assertEquals(2, options.getFacetOnFields().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void addFacetOnFieldNullValue() {
    new FacetOptions().addFacetOnField((Field) null);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void addFacetOnFieldWithoutFieldname() {
    new FacetOptions().addFacetOnField(new SimpleField(""));
  }
  
  @Test
  public void testSetFacetSort() {
    FacetOptions options = new FacetOptions(); 
    Assert.assertNotNull(options.getFacetSort());
    Assert.assertEquals(FacetOptions.DEFAULT_FACET_SORT, options.getFacetSort());
    
    options.setFacetSort(FacetSort.INDEX);
    Assert.assertEquals(FacetSort.INDEX, options.getFacetSort());
  }

  @Test(expected=IllegalArgumentException.class)
  public void testSetFacetSortWithNullValue() {
    new FacetOptions().setFacetSort(null);
  }
  
  @Test
  public void testSetFacetLimit() {
    FacetOptions options = new FacetOptions(); 
    Assert.assertEquals(FacetOptions.DEFAULT_FACET_LIMIT, options.getFacetLimit());
    
    options.setFacetLimit(20);
    Assert.assertEquals(20, options.getFacetLimit());
    
    options.setFacetLimit(-1);
    Assert.assertEquals(1, options.getFacetLimit());
  }
  
  @Test
  public void testSetFacetMinCount() {
    FacetOptions options = new FacetOptions(); 
    Assert.assertEquals(FacetOptions.DEFAULT_FACET_MIN_COUNT, options.getFacetMinCount());
    
    options.setFacetMinCount(20);
    Assert.assertEquals(20, options.getFacetMinCount());
    
    options.setFacetMinCount(-1);
    Assert.assertEquals(0, options.getFacetMinCount());
  }
  
}
