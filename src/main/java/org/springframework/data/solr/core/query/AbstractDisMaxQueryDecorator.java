package org.springframework.data.solr.core.query;

import org.springframework.lang.Nullable;


/**
 * General purpose {@link DisMaxQuery} decorator.
 *
 * @author Matthew Hall
 * @since 4.1.0
 */
public abstract class AbstractDisMaxQueryDecorator extends AbstractQueryDecorator implements DisMaxQuery {

  private DisMaxQuery query;

  public AbstractDisMaxQueryDecorator(DisMaxQuery query) {
    super(query);
    this.query = query;
  }

  @Nullable
  @Override
  public DisMaxOptions getDisMaxOptions() {
    return this.query.getDisMaxOptions();
  }

  public <T extends SolrDataQuery> T setDisMaxOptions(DisMaxOptions disMaxOptions) {
    return this.query.setDisMaxOptions(disMaxOptions);
  }
}
