package org.springframework.data.solr.core.query;

import org.springframework.lang.Nullable;

/**
 * Query to be used for DisMax query type.
 *
 * @author Matthew Hall
 */
public interface DisMaxQuery extends Query {

  @Nullable
  DisMaxOptions getDisMaxOptions();

  <T extends SolrDataQuery> T setDisMaxOptions(DisMaxOptions disMaxOptions);

}
