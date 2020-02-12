package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class NullSql extends SqlDataType {

  protected static final int TYPE_CODE = Types.NULL;

  @Override
  public int getTypeCode() {
    return TYPE_CODE;
  }

  @Override
  public String getTypeName() {
    return "NULL";
  }

  @Override
  public Class<?> getClazz() {
    return null;
  }


}
