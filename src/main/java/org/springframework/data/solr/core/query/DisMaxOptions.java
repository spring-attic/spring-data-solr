package org.springframework.data.solr.core.query;

/**
 * DisMax Options.
 *
 * @author Matthew Hall
 */
public class DisMaxOptions {

  private String altQuery;
  private String queryFunction;
  private String minimumMatch;
  private String boostQuery;
  private String boostFunction;
  private Integer phraseSlop;
  private Integer querySlop;
  private Double tie;
  private String phraseFunction;
  private String defaultField;


  public String getAltQuery() {
    return altQuery;
  }

  public String getQueryFunction() {
    return queryFunction;
  }

  public String getMinimumMatch() {
    return minimumMatch;
  }

  public String getBoostQuery() {
    return boostQuery;
  }

  public String getBoostFunction() {
    return boostFunction;
  }

  public Integer getPhraseSlop() {
    return phraseSlop;
  }

  public Integer getQuerySlop() {
    return querySlop;
  }

  public Double getTie() {
    return tie;
  }

  public String getPhraseFunction() {
    return phraseFunction;
  }

  public String getDefaultField() {
    return defaultField;
  }

  private DisMaxOptions(String altQuery, String queryFunction, String minimumMatch, String boostQuery,
      String boostFunction, Integer phraseSlop, Integer querySlop, Double tie, String phraseFunction,
      String defaultField) {

    this.altQuery = altQuery;
    this.queryFunction = queryFunction;
    this.minimumMatch = minimumMatch;
    this.boostQuery = boostQuery;
    this.boostFunction = boostFunction;
    this.phraseSlop = phraseSlop;
    this.querySlop = querySlop;
    this.tie = tie;
    this.phraseFunction = phraseFunction;
    this.defaultField = defaultField;
  }


  public static class Builder {

    private String altQuery;
    private String queryFunction;
    private String minimumMatch;
    private String boostQuery;
    private String boostFunction;
    private Integer phraseSlop;
    private Integer querySlop;
    private Double tie;
    private String phraseFunction;

    private String defaultField;

    public Builder() {}

    public Builder altQuery(String altQuery) {
      this.altQuery = altQuery;
      return this;
    }

    public Builder queryFunction(String queryFunction) {
      this.queryFunction = queryFunction;
      return this;
    }

    public Builder minimumMatch(String minimumMatch) {
      this.minimumMatch = minimumMatch;
      return this;
    }

    public Builder boostQuery(String boostQuery) {
      this.boostQuery = boostQuery;
      return this;
    }

    public Builder phraseSlop(Integer phraseSlop) {
      this.phraseSlop = phraseSlop;
      return this;
    }

    public Builder querySlop(Integer querySlop) {
      this.querySlop = querySlop;
      return this;
    }

    public Builder tie(Double tie) {
      this.tie = tie;
      return this;
    }

    public Builder boostFunction(String boostFunction) {
      this.boostFunction = boostFunction;
      return this;
    }

    public Builder phraseFunction(String phraseFunction) {
      this.phraseFunction = phraseFunction;
      return this;
    }

    public Builder defaultField(String defaultField) {
      this.defaultField = defaultField;
      return this;
    }

    public DisMaxOptions build() {
      return new DisMaxOptions(altQuery, queryFunction, minimumMatch, boostQuery, boostFunction,
          phraseSlop, querySlop, tie, phraseFunction, defaultField);
    }

  }
}
