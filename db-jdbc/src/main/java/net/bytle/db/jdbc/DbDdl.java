package net.bytle.db.jdbc;

import net.bytle.db.database.DataStore;
import net.bytle.db.model.*;
import net.bytle.db.spi.DataPath;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class DbDdl {

  private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());


  /**
   * Return create statements (inclusive primary key, foreign key and unique key)
   *
   * @param dataPath The table Name in the create statement
   */
  public static List<String> getCreateTableStatements(DataPath dataPath) {

    JdbcDataPath jdbcDataPath = (JdbcDataPath) dataPath;

    List<String> statements = new ArrayList<>();
    StringBuilder createTableStatement = new StringBuilder()
      .append("create table ");
    final String schemaName = jdbcDataPath.getSchema().getName();
    if (schemaName != null) {
      createTableStatement.append(schemaName).append(".");
    }
    createTableStatement
      .append(jdbcDataPath.getName())
      .append(" (\n")
      .append(getCreateTableStatementColumnsDefinition(dataPath))
      .append(" )\n");
    statements.add(createTableStatement.toString());

    // Primary Key
    final PrimaryKeyDef primaryKey = dataPath.getDataDef().getPrimaryKey();
    if (primaryKey != null) {
      if (primaryKey.getColumns().size() != 0) {
        String createPrimaryKeyStatement = getAlterTablePrimaryKeyStatement(jdbcDataPath);
        if (createPrimaryKeyStatement != null) {
          statements.add(createPrimaryKeyStatement);
        }
      }
    }

    // Foreign key
    for (ForeignKeyDef foreignKeyDef : jdbcDataPath.getDataDef().getForeignKeys()) {
      String createForeignKeyStatement = getAlterTableForeignKeyStatement(foreignKeyDef);
      if (createForeignKeyStatement != null) {
        statements.add(createForeignKeyStatement);
      }
    }

    // Unique key
    for (UniqueKeyDef uniqueKeyDef : jdbcDataPath.getDataDef().getUniqueKeys()) {
      String createUniqueKeyStatement = getAlterTableUniqueKeyStatement(uniqueKeyDef);
      statements.add(createUniqueKeyStatement);
    }

    return statements;

  }


  /**
   * @param dataPath : The target schema
   * @return the column string part of a create statement
   */
  public static String getCreateTableStatementColumnsDefinition(DataPath dataPath) {


    StringBuilder statementColumnPart = new StringBuilder();
    RelationDef dataDef = dataPath.getDataDef();
    for (int i = 0; i < dataDef.getColumnsSize(); i++) {

      try {

        ColumnDef columnDef = dataDef.getColumnDef(i);
        // Add it to the columns statement
        statementColumnPart.append(getColumnStatementForCreateTable(columnDef));

      } catch (Exception e) {

        throw new RuntimeException(e + "\nException: The Column Statement build until now is:\n" + statementColumnPart, e);

      }

      // Is it the end ...
      if (i != dataDef.getColumnsSize() - 1) {
        statementColumnPart.append(",\n");
      } else {
        statementColumnPart.append("\n");
      }

    }

    return statementColumnPart.toString();

  }

  /**
   * A blank statement in the form "columnName datatype(scale, precision)"
   * The constraint such as NOT NULL unique may change between database
   * Example Sqlite has the primary key statement before NOT NULL
   *
   * @param columnDef : The source column
   * @return
   */
  public static String getColumnStatementForCreateTable(ColumnDef columnDef) {

    // Target data type from source data type is lost
    // It should be handle when the path is changing of database
    SqlDataType targetSqlDataType = columnDef.getDataType();

    // Always passed to create the statement
    Integer precision = columnDef.getPrecision();
    Integer maxPrecision = targetSqlDataType.getMaxPrecision();
    if (precision == null) {
      precision = maxPrecision;
    }
    if (precision != null && maxPrecision !=null && precision > maxPrecision){
      DataStore dataStore = columnDef.getDataDef().getDataPath().getDataStore();
      String message = "The precision (" + precision + ") of the column (" + columnDef + ") is greater than the maximum allowed ("+maxPrecision+") for the datastore (" + dataStore.getName() + ")";
      if (dataStore.isStrict()) {
        throw new RuntimeException(message);
      } else {
        LOGGER.warning(message);
        precision = maxPrecision;
      }
    }
    Integer scale = columnDef.getScale();
    if (scale == null) {
      scale = targetSqlDataType.getMaximumScale();
    }

    String dataTypeCreateStatement = null;


    if (targetSqlDataType.getTypeCode() == Types.DATE || targetSqlDataType.getTypeCode() == Types.TIME) {

      dataTypeCreateStatement = targetSqlDataType.getTypeNames().get(0);

    } else {

      dataTypeCreateStatement = getCreateDataTypeStatement(targetSqlDataType.getTypeNames().get(0), precision, scale);

    }

    // NOT NULL
    String notNullStatement = "";
    if (!columnDef.getNullable()) {
      // Hack because hive is read only, it does not support Not Null
      if (!((JdbcDataPath) columnDef.getDataDef().getDataPath()).getDataStore().getProductName().equals(JdbcDataStore.DB_HIVE)) {
        notNullStatement = " NOT NULL";
      }
    }

    // Hack for Hive
    String encloseString = "\"";
    if (((JdbcDataPath) columnDef.getDataDef().getDataPath()).getDataStore().getProductName().equals(JdbcDataStore.DB_HIVE)) {
      encloseString = "`";
    }

    // Number as columnName is not possible in Oracle
    // Just with two double quote
    return encloseString + columnDef.getColumnName() + encloseString + " " + dataTypeCreateStatement + notNullStatement;
  }




  static String getCreateDataTypeStatement(String columnTypeName, Integer precision, Integer scale) {

    String dataTypeCreateStatement = columnTypeName;
    if (precision != null && precision > 0) {
      dataTypeCreateStatement += "(" + precision;
      if (scale != null && scale != 0) {
        dataTypeCreateStatement += "," + scale;
      }
      dataTypeCreateStatement += ")";
    }
    return dataTypeCreateStatement;

  }

  /**
   * @param uniqueKeyDef - The source unique key def
   * @return an alter table unique statement
   */
  public static String getAlterTableUniqueKeyStatement(UniqueKeyDef uniqueKeyDef) {

    String statement = "ALTER TABLE " + JdbcDataSystemSql.getFullyQualifiedSqlName(uniqueKeyDef.getRelationDef().getDataPath()) + " ADD ";

    // The serie of columns definitions (col1, col2,...)
    final List<ColumnDef> columns = uniqueKeyDef.getColumns();
    List<String> columnNames = new ArrayList<>();
    for (ColumnDef columnDef : columns) {
      columnNames.add(columnDef.getColumnName());
    }
    final String columnDefStatement = String.join(",", columnNames.toArray(new String[columnNames.size()]));

    // The final statement that presence of the name
    final String name = uniqueKeyDef.getName();
    if (name == null) {
      statement = statement + "UNIQUE (" + columnDefStatement + ") ";
    } else {
      statement = statement + "CONSTRAINT " + name + " UNIQUE (" + columnDefStatement + ") ";
    }

    return statement;

  }

  /**
   * @param foreignKeyDef - The source foreign key
   * @return a alter table foreign key statement
   */
  public static String getAlterTableForeignKeyStatement(ForeignKeyDef foreignKeyDef) {

    JdbcDataStore jdbcDataSystem = (JdbcDataStore) foreignKeyDef.getTableDef().getDataPath().getDataStore();

    // Constraint are supported from 2.1
    // https://issues.apache.org/jira/browse/HIVE-13290
    if (jdbcDataSystem.getProductName().equals(JdbcDataStore.DB_HIVE)) {
      if (jdbcDataSystem.getDatabaseMajorVersion() < 2) {
        return null;
      } else {
        if (jdbcDataSystem.getDatabaseMinorVersion() < 1) {
          return null;
        }
      }
    }
    StringBuilder statement = new StringBuilder().append("ALTER TABLE ");
    statement.append(JdbcDataSystemSql.getFullyQualifiedSqlName(foreignKeyDef.getTableDef().getDataPath()))
      .append(" ADD ")
      .append("CONSTRAINT ")
      .append(foreignKeyDef.getName())
      .append(" FOREIGN KEY (");
    final List<ColumnDef> nativeColumns = foreignKeyDef.getChildColumns();
    for (int i = 0; i < nativeColumns.size(); i++) {
      statement.append(nativeColumns.get(i).getColumnName());
      if (i != nativeColumns.size() - 1) {
        statement.append(", ");
      }
    }
    statement.append(") ");


    final DataPath foreignDataPath = foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath();
    statement
      .append("REFERENCES ")
      .append(JdbcDataSystemSql.getFullyQualifiedSqlName(foreignDataPath))
      .append(" (");
    List<ColumnDef> foreignColumns = foreignDataPath.getDataDef().getPrimaryKey().getColumns();
    for (int i = 0; i < foreignColumns.size(); i++) {
      statement.append(foreignColumns.get(i).getColumnName());
      if (i != foreignColumns.size() - 1) {
        statement.append(", ");
      }
    }
    statement.append(")");

    return statement.toString();


  }

  public static String getAlterTablePrimaryKeyStatement(JdbcDataPath jdbcDataPath) {

    final PrimaryKeyDef primaryKey = jdbcDataPath.getDataDef().getPrimaryKey();
    List<ColumnDef> columns = primaryKey.getColumns();
    int size = columns.size();
    if (size == 0) {
      return null;
    }

    // TODO: Move to Hive
    // Constraint are supported from 2.1
    // https://issues.apache.org/jira/browse/HIVE-13290
    final JdbcDataStore dataStore = jdbcDataPath.getDataStore();
    if (dataStore.getProductName().equals(JdbcDataStore.DB_HIVE)) {
      if (dataStore.getDatabaseMajorVersion() < 2) {
        return null;
      } else {
        if (dataStore.getDatabaseMinorVersion() < 1) {
          return null;
        }
      }
    }

    StringBuilder statement = new StringBuilder().append("ALTER TABLE ");
    statement
      .append(JdbcDataSystemSql.getFullyQualifiedSqlName(jdbcDataPath))
      .append(" ADD ");
    if (primaryKey.getName() != null) {
      statement
        .append("CONSTRAINT ")
        .append(primaryKey.getName())
        .append(" ");
    }
    List<String> columnNames = new ArrayList<>();
    for (ColumnDef columnDef : columns) {
      columnNames.add(columnDef.getColumnName());
    }
    statement
      .append("PRIMARY KEY  (")
      .append(String.join(", ", columnNames))
      .append(")");

    return statement.toString();
  }


  public static void truncateTable(JdbcDataPath dataPath) {

    try {
      String dropTableStatement = "truncate table " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath);
      dataPath.getDataStore().getCurrentConnection().createStatement().execute(dropTableStatement);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static void deleteAllRecordsTable(JdbcDataPath dataPath) {

    try {
      String dropTableStatement = "delete from " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath);
      dataPath.getDataStore().getCurrentConnection().createStatement().execute(dropTableStatement);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static void dropForeignKey(ForeignKeyDef foreignKeyDef) {
    try {
      final JdbcDataPath dataPath = (JdbcDataPath) foreignKeyDef.getTableDef().getDataPath();
      String dropTableStatement = "ALTER TABLE " + JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath)
        + " DROP CONSTRAINT " + foreignKeyDef.getName();
      dataPath.getDataStore().getCurrentConnection().createStatement().execute(dropTableStatement);
      LOGGER.info("Foreign Key: " + foreignKeyDef.getName() + " on the table " + dataPath.toString() + " were deleted.");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

}
