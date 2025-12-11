package com.tabulify.hive;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlConnectionMetadata;
import com.tabulify.jdbc.SqlDataSystem;
import com.tabulify.model.SqlDataType;
import com.tabulify.type.KeyNormalizer;

import java.sql.Types;

public class HiveConnection extends SqlConnection {

  public HiveConnection(Tabular tabular, Attribute name, Attribute url) {
    super(tabular, name, url);
  }

  @Override
  public SqlDataSystem getDataSystem() {
    return new HiveDataSystem(this);
  }

  @Override
  public SqlConnectionMetadata getMetadata() {
    return new HiveConnectionMetadata(this);
  }


  /**
   * <a href="https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Types#LanguageManualTypes-IntegralTypes(TINYINT,SMALLINT,INT/INTEGER,BIGINT)">...</a>
   */
  @Override
  public SqlDataType getSqlDataType(KeyNormalizer typeName, int typeCode) {
    SqlDataType sqlDataType = super.getSqlDataType(typeName, typeCode);
    switch (typeCode){
      case Types.NUMERIC:
        //sqlDataType.setSqlNameSafe("DECIMAL");
        break;
      case Types.TIME:
        // Time doesn't exist, we try to make it a timestamp
        //sqlDataType.setSqlNameSafe("TIMESTAMP");
        break;
    }
    return sqlDataType;
  }
}
