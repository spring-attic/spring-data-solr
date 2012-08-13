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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.data.convert.DefaultTypeMapper;
import org.springframework.data.convert.SimpleTypeInformationMapper;
import org.springframework.data.convert.TypeAliasAccessor;
import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;

public class SimpleSolrTypeMapper extends DefaultTypeMapper<Object> implements SolrTypeMapper {

  public SimpleSolrTypeMapper() {
    super(new SolrObjectTypeAliasAccessor());
  }

  public SimpleSolrTypeMapper(MappingContext<? extends PersistentEntity<?, ?>, ?> mappingContext) {
    super(new SolrObjectTypeAliasAccessor(), mappingContext, Arrays.asList(SimpleTypeInformationMapper.INSTANCE));
  }

  public SimpleSolrTypeMapper(List<? extends TypeInformationMapper> mappers) {
    super(new SolrObjectTypeAliasAccessor(), mappers);
  }

  public static final class SolrObjectTypeAliasAccessor implements TypeAliasAccessor<Object> {

    public Object readAliasFrom(Object source) {

      if (source instanceof Collection) {
        return null;
      }

      return source;
    }

    public void writeTypeTo(Object sink, Object alias) {
      // TODO: check if DO nothing is ok here
    }
  }
}
