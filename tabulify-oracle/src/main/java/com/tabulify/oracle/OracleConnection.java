package com.tabulify.oracle;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlDataSystem;
import com.tabulify.model.SqlDataType;
import oracle.jdbc.OracleTypes;

/**
 *
 */
public class OracleConnection extends SqlConnection {

  public OracleConnection(Tabular tabular, Attribute name, Attribute url) {
    super(tabular, name, url);
  }

  @Override
  public SqlDataSystem getDataSystem() {
    return new OracleSystem(this);
  }




  @Override
  public Object toSqlObject(Object sourceObject, SqlDataType targetColumnType) {

    if (targetColumnType.getTypeCode() == OracleTypes.BINARY_DOUBLE && sourceObject instanceof Double) {
      return new oracle.sql.BINARY_DOUBLE((Double) sourceObject);
    }
    if (targetColumnType.getTypeCode() == OracleTypes.BINARY_FLOAT && sourceObject instanceof Float) {
      return new oracle.sql.BINARY_FLOAT((Float) sourceObject);
    }
    return sourceObject;

  }

  @Override
  public OracleConnectionMetadata getMetadata() {
    return new OracleConnectionMetadata(this);
  }



}
