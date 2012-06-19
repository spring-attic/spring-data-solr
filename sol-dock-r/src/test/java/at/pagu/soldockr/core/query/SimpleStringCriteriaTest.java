package at.pagu.soldockr.core.query;

import junit.framework.Assert;

import org.junit.Test;

public class SimpleStringCriteriaTest {
  
  @Test
  public void testStringCriteria() {
    Criteria criteria = new SimpleStringCriteria("field_1:value_1 AND field_2:value_2");
    Assert.assertEquals("field_1:value_1 AND field_2:value_2", criteria.createQueryString());
  }
  
  @Test
  public void testStringCriteriaWithMoreFragments() {
    Criteria criteria = new SimpleStringCriteria("field_1:value_1 AND field_2:value_2");
    criteria = criteria.and("field_3").is("value_3");
    Assert.assertEquals("field_1:value_1 AND field_2:value_2 AND field_3:value_3", criteria.createQueryString());
  }
}
