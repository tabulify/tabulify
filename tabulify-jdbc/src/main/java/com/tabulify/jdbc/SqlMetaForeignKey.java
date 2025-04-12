package com.tabulify.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Just an utility class to collect foreign key meta
 */
public class SqlMetaForeignKey {


  /**
   * The fields that are the id of the foreign keys
   */
  private final String name; // The constraint name (may be null)
  private final String primaryTableCatalogName;
  private final String primaryTableSchemaName;
  private final String primaryTableName;
  private final String foreignTableCatalogName;
  private final String foreignTableSchemaName;
  private final String foreignTableName;

  private Map<Short, List<String>> columns = new HashMap<>();

  public SqlMetaForeignKey(String primaryTableCatalogName, String primarySchemaName, String primaryTableName, String foreignTableCatalogName, String foreignTableSchemaName, String foreignTableName, String name) {
    this.primaryTableCatalogName = primaryTableCatalogName;
    this.primaryTableSchemaName = primarySchemaName;
    this.primaryTableName = primaryTableName;
    this.foreignTableCatalogName = foreignTableCatalogName;
    this.foreignTableSchemaName = foreignTableSchemaName;
    this.foreignTableName = foreignTableName;
    this.name = name;
  }

  /**
   * Collect the data before processing it
   * because of the build that have a recursion nature, the data need first to be collected
   * processing the data and calling recursively the creation of an other table
   * with foreign key result in a "result set is closed" exception within the Ms Sql Driver
   *
   * @param resultSet
   * @return
   * @throws SQLException
   */
  public static List<SqlMetaForeignKey> getForeignKeyMetaFromDriverResultSet(ResultSet resultSet) throws SQLException {


    Map<SqlMetaForeignKey,SqlMetaForeignKey> fkDatas = new HashMap<>();

    // The column names of the fkresult set
    while (resultSet.next()) {

      String fkName = resultSet.getString("FK_NAME");
      String fkTableColumnName = resultSet.getString("FKCOLUMN_NAME");
      String fkTableSchemaName = resultSet.getString("FKTABLE_SCHEM");
      String fkTableCatalogName = resultSet.getString("FKTABLE_CAT");
      String fkTableName = resultSet.getString("FKTABLE_NAME");
      //  --- Pk referenced
      String pkTableColumnName = resultSet.getString("PKCOLUMN_NAME");
      String pkTableName = resultSet.getString("PKTABLE_NAME");
      String pkTableSchemaName = resultSet.getString("PKTABLE_SCHEM");
      String pkTableCatalogName = resultSet.getString("PKTABLE_CAT");
      String col_pk_name = resultSet.getString("PK_NAME");
      //  ---- Column seq for FK and PK
      short col_key_seq = resultSet.getShort("KEY_SEQ");

      SqlMetaForeignKey sqlMetaForeignKey = new SqlMetaForeignKey(pkTableCatalogName, pkTableSchemaName, pkTableName, fkTableCatalogName, fkTableSchemaName, fkTableName, fkName);
      sqlMetaForeignKey = fkDatas.computeIfAbsent(sqlMetaForeignKey,sqlMetaForeignKey1 -> sqlMetaForeignKey1);
      sqlMetaForeignKey.addColumnMapping(col_key_seq,pkTableColumnName,fkTableColumnName);

    }
    return new ArrayList<>(fkDatas.values());

  }


  public SqlMetaForeignKey addColumnMapping(short seq, String pkColumn, String fkColumn) {
    List<String> columnMap = columns.computeIfAbsent(seq, ArrayList::new);
    columnMap.add(pkColumn);
    columnMap.add(fkColumn);
    return this;
  }

  public String getPrimaryTableName() {
    return this.primaryTableName;
  }

  public List<String> getForeignKeyColumns() {
    return this.columns.keySet()
      .stream()
      .sorted()
      .map(l -> this.columns.get(l).get(1))
      .collect(Collectors.toList());

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SqlMetaForeignKey that = (SqlMetaForeignKey) o;
    return Objects.equals(name, that.name) &&
      Objects.equals(primaryTableCatalogName, that.primaryTableCatalogName) &&
      Objects.equals(primaryTableSchemaName, that.primaryTableSchemaName) &&
      primaryTableName.equals(that.primaryTableName) &&
      Objects.equals(foreignTableCatalogName, that.foreignTableCatalogName) &&
      Objects.equals(foreignTableSchemaName, that.foreignTableSchemaName) &&
      foreignTableName.equals(that.foreignTableName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, primaryTableCatalogName, primaryTableSchemaName, primaryTableName, foreignTableCatalogName, foreignTableSchemaName, foreignTableName);
  }

  public String getPrimaryTableCatalogName() {
    return this.primaryTableCatalogName;
  }

  public String getPrimaryTableSchemaName() {
    return this.primaryTableSchemaName;
  }

  public String getName() {
    return this.name;
  }

  public String getForeignTableCatalogName() {
    return this.foreignTableCatalogName;
  }

  public String getForeignTableSchemaName() {
    return this.foreignTableSchemaName;
  }

  public String getForeignTableName() {
    return this.foreignTableName;
  }
}
