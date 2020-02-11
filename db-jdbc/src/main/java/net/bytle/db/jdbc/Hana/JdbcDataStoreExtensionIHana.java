package net.bytle.db.jdbc.Hana;

import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.jdbc.*;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.PrimaryKeyDef;
import net.bytle.db.model.UniqueKeyDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gerard on 11-01-2016.
 */
public class JdbcDataStoreExtensionIHana extends JdbcDataStoreExtension {


  private static Map<Integer, DataTypeDatabase> dataTypeDatabaseSet = new HashMap<>();

  static {
    dataTypeDatabaseSet.put(HanaDbVarcharType.TYPE_CODE, new HanaDbVarcharType());
  }

  public JdbcDataStoreExtensionIHana(JdbcDataStore jdbcDataStore) {
    super(jdbcDataStore);
  }


  @Override
  public DataTypeDatabase dataTypeOf(Integer typeCode) {
    return dataTypeDatabaseSet.get(typeCode);
  }

  /**
   * Returns statement to create the table
   *
   * @param jdbcDataPath
   * @return
   */
  @Override
  public List<String> getCreateTableStatements(JdbcDataPath jdbcDataPath) {


    List<String> statements = new ArrayList<>();

    String statement = "create ";

    // String tableType = tableDef.getCreateProperties().getProperty("after_create");
    //        if (tableType != null){
    //            statement += tableType;
    //        }
    statement += " table " + jdbcDataPath.getName() + " (\n"
      + DbDdl.getCreateTableStatementColumnsDefinition(jdbcDataPath)
      + "\n)";

    statements.add(statement);
    final PrimaryKeyDef primaryKey = jdbcDataPath.getDataDef().getPrimaryKey();
    if (primaryKey != null) {
      statements.add(DbDdl.getAlterTablePrimaryKeyStatement(jdbcDataPath));
    }

    for (ForeignKeyDef foreignKeyDef : jdbcDataPath.getDataDef().getForeignKeys()) {
      statements.add(DbDdl.getAlterTableForeignKeyStatement(foreignKeyDef));
    }

    for (UniqueKeyDef uniqueKeyDef : jdbcDataPath.getDataDef().getUniqueKeys()) {
      statements.add(DbDdl.getAlterTableUniqueKeyStatement(uniqueKeyDef));
    }

    return statements;

  }


  @Override
  public Object getLoadObject(int targetColumnType, Object sourceObject) {
    return null;
  }

  @Override
  public String getNormativeSchemaObjectName(String objectName) {
    return null;
  }

  @Override
  public Integer getMaxWriterConnection() {
    return null;
  }

  @Override
  public String getTruncateStatement(JdbcDataPath dataPath) {
    StringBuilder truncateStatementBuilder = new StringBuilder().append("truncate from ");
    truncateStatementBuilder.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
    return truncateStatementBuilder.toString();
  }

}
