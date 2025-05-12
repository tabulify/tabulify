package com.tabulify.oracle;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlConnectionAttributeEnum;
import com.tabulify.jdbc.SqlDataSystem;
import com.tabulify.model.SqlDataType;
import oracle.jdbc.OracleTypes;

/**
 *
 */
public class OracleConnection extends SqlConnection {

  public OracleConnection(Tabular tabular, Attribute name, Attribute url) {

    super(tabular, name, url);

    // Oracle requires quote on every name if you set them
    // ie "cat_id" and not cat_id
    // If you name a schema object using a quoted identifier, then you must use the double quotation marks whenever you refer to that object.
    Attribute nameQuoteEnabled = this.getAttribute(SqlConnectionAttributeEnum.NAME_QUOTING_ENABLED);
    if (nameQuoteEnabled.getOrigin() == Origin.DEFAULT) {
      nameQuoteEnabled.setPlainValue(false);
    }

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
