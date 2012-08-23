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
package at.pagu.soldockr.core.query.result;

import at.pagu.soldockr.core.query.Field;

/**
 * FacetEntry is returned as result of a FacetQuery holding the fieldname, value
 * and valueCount for the requested facet field
 * 
 * @author Christoph Strobl
 */
public interface FacetEntry {

  /**
   * The referenced facet field
   * @return
   */
  Field getField();

  /**
   * The nr of hits for the value
   * @return
   */
  long getValueCount();

  /**
   * The value within the field
   * @return
   */
  String getValue();

}
