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
package at.pagu.soldockr.repository.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.pagu.soldockr.core.SolrOperations;
import at.pagu.soldockr.core.query.Query;
import at.pagu.soldockr.core.query.SimpleQuery;
import at.pagu.soldockr.core.query.SimpleStringCriteria;

public class StringBasedSolrQuery extends AbstractSolrQuery {
  
  private static final Pattern PARAMETER_PLACEHOLDER = Pattern.compile("\\?(\\d+)");

  private final String rawQueryString;
  
  public StringBasedSolrQuery(SolrQueryMethod method, SolrOperations mongoOperations) {
    this(method.getAnnotatedQuery(), method, mongoOperations);
  }
  
  public StringBasedSolrQuery(String query, SolrQueryMethod queryMethod, SolrOperations solrOperations) {
    super(solrOperations, queryMethod);
    this.rawQueryString = query;
  }

  @Override
  protected Query createQuery(SolrParameterAccessor parameterAccessor) {
    String queryString = replacePlaceholders(this.rawQueryString, parameterAccessor);
    
    return new SimpleQuery(new SimpleStringCriteria(queryString));
  }
  
  private String replacePlaceholders(String input, SolrParameterAccessor accessor) {

    Matcher matcher = PARAMETER_PLACEHOLDER.matcher(input);
    String result = input;

    while (matcher.find()) {
      String group = matcher.group();
      int index = Integer.parseInt(matcher.group(1));
      result = result.replace(group, getParameterWithIndex(accessor, index));
    }
    return result;
  }
  
  private String getParameterWithIndex(SolrParameterAccessor accessor, int index) {

    Object parameter = accessor.getBindableValue(index);

    if (parameter == null) {
      return "null";
    } 
    return parameter.toString();
  }
}
