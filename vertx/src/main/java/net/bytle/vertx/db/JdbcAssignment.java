package net.bytle.vertx.db;

/**
 * An assignment can be:
 * * an expression (realm_list = realm_list + 1)
 * * or a raw value (realm_list = 1)
 */
public class JdbcAssignment {

  public static JdbcAssignment createExpression(String expression) {
    return new JdbcAssignment(expression,AssignmentType.EXPRESSION);
  }

  public Object getValue() {
    return this.value;
  }

  public boolean isValue() {
    return this.type ==AssignmentType.VALUE ;
  }

  public String getExpression() {
    return this.value.toString();
  }

  enum AssignmentType {
    VALUE,
    EXPRESSION
  }

  private final Object value;
  private final AssignmentType type;

  JdbcAssignment(Object value, AssignmentType type) {
    this.value = value;
    this.type = type;
  }

  public static JdbcAssignment createValue(Object value) {
    return new JdbcAssignment(value,AssignmentType.VALUE);
  }


}
