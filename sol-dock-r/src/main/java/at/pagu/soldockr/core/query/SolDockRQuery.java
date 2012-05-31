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

/**
 * Common interface for any Query 
 *
 */
public interface SolDockRQuery {

  /**
   * Append criteria to query.
   * Criteria must not be null, nor point to a field with null value.
   * 
   * @param criteria
   * @return
   */
  <T extends SolDockRQuery> T addCriteria(Criteria criteria);

  /**
   * 
   * @return
   */
  Criteria getCriteria();

}
