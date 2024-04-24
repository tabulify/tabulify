package net.bytle.vertx.db;

public class JdbcSingleOperatorPredicate {
  private final Builder builder;

  public JdbcSingleOperatorPredicate(Builder builder) {
    this.builder = builder;
  }

  public static Builder create() {
    return new Builder();
  }

  public JdbcColumn getColumn() {
    return this.builder.column;
  }

  public JdbcComparisonOperator getComparisonOperator() {
    return this.builder.operator;
  }

  public boolean getOrNull() {
    return this.builder.orNull;
  }

  public Object getValue() {
    return this.builder.value;
  }

  public String toSql(Integer position) {

    StringBuilder predicateBuilder = new StringBuilder();
    if (this.getOrNull()) {
      predicateBuilder.append("(");
    }
    String columnName = this.builder.sqlEngine.toFullColumnName(this.getColumn());

    /**
     * Note if we need to pass the data type
     * this is done like that: $3::BIGINT for Long
     */
    predicateBuilder
      .append(columnName)
      .append(" ")
      .append(this.getComparisonOperator().toSql())
      .append(" $")
      .append(position);
    if (this.getOrNull()) {
      predicateBuilder
        .append(" or ")
        .append(columnName)
        .append(" is null)");
    }
    return predicateBuilder.toString();
  }

  public static class Builder {
    private JdbcSqlStatementEngine sqlEngine;
    private JdbcColumn column;
    private Object value;
    private JdbcComparisonOperator operator = JdbcComparisonOperator.EQUALITY;
    private boolean orNull = false;

    public Builder() {
    }

    public Builder setColumn(JdbcColumn column, Object value) {
      this.column = column;
      this.value = value;
      return this;
    }


    public Builder setOperator(JdbcComparisonOperator operator) {
      this.operator = operator;
      return this;
    }

    public Builder setOrNull(boolean orNull) {
      this.orNull = orNull;
      return this;
    }

    /**
     * The build is executed by the query, not the user
     */
    public JdbcSingleOperatorPredicate build(JdbcSqlStatementEngine sqlStatementEngine) {
      this.sqlEngine = sqlStatementEngine;
      return new JdbcSingleOperatorPredicate(this);
    }

  }
}
