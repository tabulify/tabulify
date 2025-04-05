package net.bytle.db.hive;

import net.bytle.db.Tabular;
import net.bytle.db.jdbc.SqlConnection;
import net.bytle.db.jdbc.SqlConnectionMetadata;
import net.bytle.db.jdbc.SqlDataSystem;
import net.bytle.db.model.SqlDataType;
import net.bytle.type.Variable;

import java.sql.Types;

public class HiveConnection extends SqlConnection {

  public HiveConnection(Tabular tabular, Variable name, Variable url) {
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
   * https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Types#LanguageManualTypes-IntegralTypes(TINYINT,SMALLINT,INT/INTEGER,BIGINT)
   * @param typeCode
   * @return
   */
  @Override
  public SqlDataType getSqlDataType(Integer typeCode) {
    SqlDataType sqlDataType = super.getSqlDataType(typeCode);
    switch (typeCode){
      case Types.NUMERIC:
        sqlDataType.setSqlName("DECIMAL");
        break;
      case Types.TIME:
        // Time doesn't exist, we try to make it a timestamp
        sqlDataType.setSqlName("TIMESTAMP");
        break;
    }
    return sqlDataType;
  }
}
