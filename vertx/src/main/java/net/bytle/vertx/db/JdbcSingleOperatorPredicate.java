package net.bytle.vertx.db;

public class JdbcSingleOperatorPredicate {
  private final Builder builder;

  public JdbcSingleOperatorPredicate(Builder builder) {
    this.builder = builder;
  }

  public static Builder builder() {
    return new Builder();
  }

  public JdbcTableColumn getColumn() {
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

  public static class Builder {
    private JdbcTableColumn column;
    private Object value;
    private JdbcComparisonOperator operator = JdbcComparisonOperator.EQUALITY;
    private boolean orNull = false;

    public Builder setColumn(JdbcTableColumn column, Object value) {
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

    public JdbcSingleOperatorPredicate build() {
      return new JdbcSingleOperatorPredicate(this);
    }
  }
}
