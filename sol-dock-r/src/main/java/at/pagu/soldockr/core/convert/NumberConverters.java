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

import org.springframework.core.convert.converter.Converter;

public final class NumberConverters {

  public enum NumberConverter implements Converter<Number, String> {
    INSTANCE;

    @Override
    public String convert(Number source) {
      if(source == null) {
        return null;
      }
      
      if(source.doubleValue() < 0d) {
        return "\\" + source.toString();
      }
      return source.toString();
    }
    
  }
  
}
