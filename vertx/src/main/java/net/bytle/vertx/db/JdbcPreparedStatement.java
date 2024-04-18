package net.bytle.vertx.db;

import java.util.List;

public class JdbcPreparedStatement {
  private final List<Object> bindingValues;
  private final String preparedStatement;

  public JdbcPreparedStatement(String preparedStatement, List<Object> bindingValues) {
    this.preparedStatement = preparedStatement;
    this.bindingValues = bindingValues;
  }

  public List<Object> getBindingValues() {
    return bindingValues;
  }

  public String getPreparedSql() {
    return preparedStatement;
  }

  @Override
  public String toString() {
    return "Prepared Statement: \nbindingValues=" + bindingValues +
      "\nSql\n=" + preparedStatement ;
  }
}
