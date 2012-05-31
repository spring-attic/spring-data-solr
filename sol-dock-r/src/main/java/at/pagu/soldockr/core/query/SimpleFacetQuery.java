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

import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

public class SimpleFacetQuery extends SimpleQuery implements FacetQuery {

  private FacetOptions facetOptions;

  public SimpleFacetQuery() {
    super();
  }

  public SimpleFacetQuery(Criteria criteria) {
    this(criteria, null);
  }

  public SimpleFacetQuery(Criteria criteria, Pageable pageable) {
    super(criteria, pageable);
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <T extends SolDockRQuery> T setFacetOptions(FacetOptions facetOptions) {
    if (facetOptions != null) {
      Assert.isTrue(facetOptions.hasFields(), "Cannot set facet options having no fields.");
    }
    this.facetOptions = facetOptions;
    return (T) this;
  }

  @Override
  public FacetOptions getFacetOptions() {
    return this.facetOptions;
  }

  @Override
  public boolean hasFacetOptions() {
    return this.getFacetOptions() != null;
  }

}
