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
package at.pagu.soldockr.core.convert;

import java.util.Map;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import at.pagu.soldockr.SolrServerFactory;
import at.pagu.soldockr.core.mapping.SimpleSolrMappingContext;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingSolrConverterTest {

  private MappingSolrConverter converter;
  private SimpleSolrMappingContext mappingContext;

  @Mock
  private SolrServerFactory solrServerFactoryMock;

  @Mock
  private SolrServer solrServerMock;

  @Mock
  private ApplicationContext applicationContextMock;

  @Before
  public void setUp() {
    mappingContext = new SimpleSolrMappingContext();
    mappingContext.setApplicationContext(applicationContextMock);

    Mockito.when(solrServerFactoryMock.getSolrServer()).thenReturn(solrServerMock);
    Mockito.when(solrServerMock.getBinder()).thenReturn(new DocumentObjectBinder());

    converter = new MappingSolrConverter(solrServerFactoryMock, mappingContext);
    converter.afterPropertiesSet();
  }

  @Test
  public void testWrite() {
    ConvertableBean convertable = new ConvertableBean("j73x73r", 1979);
    SolrInputDocument solrDocument = new SolrInputDocument();
    converter.write(convertable, solrDocument);

    Assert.assertEquals(convertable.getStringProperty(), solrDocument.getFieldValue("stringProperty"));
    Assert.assertEquals(convertable.getIntProperty(), solrDocument.getFieldValue("intProperty"));
  }

  @Test
  public void testRead() {
    SolrDocument document = new SolrDocument();
    document.addField("stringProperty", "christoph");
    document.addField("intProperty", 32);

    ConvertableBean convertable = converter.read(ConvertableBean.class, (Map<String, Object>) document);

    Assert.assertEquals(document.getFieldValue("stringProperty"), convertable.getStringProperty());
    Assert.assertEquals(document.getFieldValue("intProperty"), convertable.getIntProperty());
  }

  public static class ConvertableBean {

    @Field
    String stringProperty;

    @Field
    Integer intProperty;

    public ConvertableBean() {}

    public ConvertableBean(String stringProperty, Integer intProperty) {
      super();
      this.stringProperty = stringProperty;
      this.intProperty = intProperty;
    }

    String getStringProperty() {
      return stringProperty;
    }

    void setStringProperty(String stringProperty) {
      this.stringProperty = stringProperty;
    }

    Integer getIntProperty() {
      return intProperty;
    }

    void setIntProperty(Integer intProperty) {
      this.intProperty = intProperty;
    }

  }

}
