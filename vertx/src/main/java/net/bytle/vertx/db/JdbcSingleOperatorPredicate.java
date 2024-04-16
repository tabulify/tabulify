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

  public String toSql(Integer position) {

    StringBuilder predicateBuilder = new StringBuilder();
    if (this.getOrNull()) {
      predicateBuilder.append("(");
    }
    String columnName = this.getColumn().getColumnName();
    if(this.builder.jdbcTable!=null){
      columnName = this.builder.jdbcTable.getName()+"."+columnName;
    }
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
    private JdbcTableColumn column;
    private Object value;
    private JdbcComparisonOperator operator = JdbcComparisonOperator.EQUALITY;
    private boolean orNull = false;
    private JdbcTable jdbcTable;

    public Builder setColumn(JdbcTable jdbcTable, JdbcTableColumn column, Object value) {
      this.jdbcTable = jdbcTable;
      return setColumn(column, value);
    }

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
