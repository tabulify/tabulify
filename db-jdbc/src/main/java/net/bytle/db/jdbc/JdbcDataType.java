package net.bytle.db.jdbc;

import net.bytle.db.model.SqlDataType;

public class JdbcDataType extends SqlDataType {

  public JdbcDataType(int typeCode) {
    super(typeCode);
  }

  public static SqlDataType of(int typeCode) {
    return new JdbcDataType(typeCode);

  }

  @Override
  public Class<?> getClazz() {
    return null;
  }

  @Override
  public int getTypeCode() {
    return 0;
  }

  @Override
  public String getTypeName() {
    return null;
  }
}
