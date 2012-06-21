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

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.SpELAwareParameterValueProvider;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

import at.pagu.soldockr.SolrServerFactory;
import at.pagu.soldockr.core.mapping.SolrPersistentEntity;
import at.pagu.soldockr.core.mapping.SolrPersistentProperty;

public class MappingSolrConverter implements SolrConverter, ApplicationContextAware, InitializingBean {

  protected final MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext;
  protected final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
  protected ApplicationContext applicationContext;
  protected SolrServerFactory solrServerFactory;
  protected SolrTypeMapper typeMapper;

  public MappingSolrConverter(SolrServerFactory solrServerFactory,
      MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext) {

    Assert.notNull(solrServerFactory);
    Assert.notNull(mappingContext);
    
    this.solrServerFactory = solrServerFactory;
    this.mappingContext = mappingContext;
    this.typeMapper = new SimpleSolrTypeMapper(mappingContext);
  }

  @Override
  public MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> getMappingContext() {
    return mappingContext;
  }

  @Override
  public ConversionService getConversionService() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <R> R read(Class<R> type, Object source) {
    //TODO: return read(ClassTypeInformation.from(type), source);
    throw new NotImplementedException();
    
  }
  
  protected <S extends Object> S read(TypeInformation<S> type, Object object) {
    TypeInformation<? extends S> typeToUse = typeMapper.readType(object, type);
    Class<? extends S> rawType = typeToUse.getType();
    
    SolrPersistentEntity<S> persistentEntity = (SolrPersistentEntity<S>) mappingContext
        .getPersistentEntity(typeToUse);
    
    
    return read(persistentEntity, object);
  }
  
  private <S extends Object> S read(final SolrPersistentEntity<S> entity, final Object dbo) {

    final StandardEvaluationContext spelCtx = new StandardEvaluationContext(dbo);
    spelCtx.addPropertyAccessor(new MapAccessor());

    if (applicationContext != null) {
      spelCtx.setBeanResolver(new BeanFactoryResolver(applicationContext));
    }

    
    ParameterValueProvider provider = new SpELAwareParameterValueProvider(spelExpressionParser, spelCtx);

    final BeanWrapper<SolrPersistentEntity<S>, S> wrapper = BeanWrapper.create(entity, provider, getConversionService());

    entity.doWithProperties(new PropertyHandler<SolrPersistentProperty>() {
      public void doWithPersistentProperty(SolrPersistentProperty prop) {

        Object obj = getValueInternal(prop, dbo, spelCtx, prop.getSpelExpression());
        wrapper.setProperty(prop, obj, true);
      }
    });

    entity.doWithAssociations(new AssociationHandler<SolrPersistentProperty>() {
      public void doWithAssociation(Association<SolrPersistentProperty> association) {
        SolrPersistentProperty inverseProp = association.getInverse();
        Object obj = getValueInternal(inverseProp, dbo, spelCtx, inverseProp.getSpelExpression());
        try {
          wrapper.setProperty(inverseProp, obj);
        } catch (IllegalAccessException e) {
          throw new MappingException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
          throw new MappingException(e.getMessage(), e);
        }
      }
    });

    return wrapper.getBean();
  }
  
  @SuppressWarnings("unchecked")
  protected Object getValueInternal(SolrPersistentProperty prop, Object dbo, StandardEvaluationContext ctx,
      String spelExpr) {

    Object o;
    if (null != spelExpr) {
      Expression x = spelExpressionParser.parseExpression(spelExpr);
      o = x.getValue(ctx);
    } else {

      //FIXME: handle value 
      throw new NotImplementedException();


    }
    return null;
  }

  

  @Override
  public void write(Object source, Object sink) {
    // TODO Auto-generated method stub

  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  public void afterPropertiesSet() {
    // TODO: initializeConverters();
  }

}
