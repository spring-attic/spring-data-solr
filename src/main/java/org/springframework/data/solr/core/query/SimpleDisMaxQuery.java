package org.springframework.data.solr.core.query;

import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

/**
 * Trivial implementation of {@link DisMaxQuery}
 *
 * @author Matthew Hall
 */
public class SimpleDisMaxQuery extends SimpleQuery implements DisMaxQuery {

  private DisMaxOptions disMaxOptions;

  public SimpleDisMaxQuery(Criteria criteria) {
    this(criteria, null);
  }

  public SimpleDisMaxQuery(Criteria criteria, @Nullable Pageable pageable) {
    super(criteria, pageable);
  }

  @Nullable
  @Override
  public DisMaxOptions getDisMaxOptions() {
    return this.disMaxOptions;
  }

  @SuppressWarnings("unchecked")
  public <T extends SolrDataQuery> T setDisMaxOptions(DisMaxOptions disMaxOptions) {
    this.disMaxOptions = disMaxOptions;
    return (T) this;
  }
}