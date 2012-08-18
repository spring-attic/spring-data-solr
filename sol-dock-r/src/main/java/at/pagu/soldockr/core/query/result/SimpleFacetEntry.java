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
import at.pagu.soldockr.core.query.SimpleField;

/**
 * The most trivial implementation of FacetEntry
 *
 */
public class SimpleFacetEntry implements FacetEntry {

  private final Field field;
  private final long count;
  private final String value;

  public SimpleFacetEntry(String fieldname, String value, long count) {
    this(new SimpleField(fieldname), value, count);
  }

  public SimpleFacetEntry(Field field, String value, long count) {
    this.field = field;
    this.value = value;
    this.count = count;
  }

  @Override
  public final Field getField() {
    return this.field;
  }

  @Override
  public final long getValueCount() {
    return this.count;
  }

  @Override
  public String getValue() {
    return this.value;
  }

  @Override
  public String toString() {
    return "SimpleFacetEntry [field=" + field + ", count=" + count + ", value=" + value + "]";
  }

}
