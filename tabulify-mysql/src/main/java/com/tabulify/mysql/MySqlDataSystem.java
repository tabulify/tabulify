package com.tabulify.mysql;

import com.tabulify.jdbc.*;
import com.tabulify.model.*;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DropTruncateAttribute;
import com.tabulify.transfer.TransferSourceTargetOrder;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoSchemaException;
import net.bytle.type.Casts;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MySqlDataSystem extends SqlDataSystem {


  public static final Integer VARCHAR_MAX_LENGTH_PLANET_SCALE = 16383;


  /**
   * 10 digits:YYYY-MM-DD
   */
  public static final int DATE_MAX_PRECISION = 10;


  /**
   * 22 comes from MySQL metadata reporting for the maximum number
   * of characters needed to represent a DOUBLE value
   */
  public static final int DOUBLE_MAX_PRECISION = 22;
  /**
   * 308 comes from MySQL metadata reporting for the maximum number
   * of characters needed to represent a DOUBLE value
   * Not sure, tired
   */
  public static final int DOUBLE_MAX_SCALE = 308;


  public MySqlDataSystem(SqlConnection sqlConnection) {
    super(sqlConnection);
  }

  @Override
  public void dataTypeBuildingMain(SqlDataTypeManager sqlManager) {
    super.dataTypeBuildingMain(sqlManager);


    if (this.getConnection().isPlanetScale()) {
      sqlManager.getTypeBuilder(MySqlVendorTypes.VARCHAR)
        .setMaxPrecision(MySqlDataSystem.VARCHAR_MAX_LENGTH_PLANET_SCALE);
    }

    // json was missing from the driver names
    // https://dev.mysql.com/doc/refman/8.4/en/json.html
    // CREATE TABLE t1 (jdoc JSON);
    sqlManager.createTypeBuilder(MySqlVendorTypes.JSON);

    // Max precision of the driver is 65535
    // This is the limit of a row
    // Way too much and not supported with a
    // `create table ( `name` varchar(65535))`
    // Error: Row size too large. The maximum row size for the used table type, not counting BLOBs, is 65535. This includes storage overhead, check the manual. You have to change some columns to TEXT or BLOBs
    sqlManager.getTypeBuilder(MySqlVendorTypes.VARCHAR)
      .setMaxPrecision(MySqlVendorTypes.VARCHAR.getMaxPrecision());

    // Max and min scale from the driver is 308 and -308
    // not 30 and 0
    sqlManager.getTypeBuilder(MySqlVendorTypes.DECIMAL)
      .setMaximumScale(MySqlVendorTypes.DECIMAL.getMaximumScale())
      .setMinimumScale(0)
    ;

    // precision is 26 for the driver, not 6
    sqlManager.getTypeBuilder(MySqlVendorTypes.TIMESTAMP)
      .setMaxPrecision(MySqlVendorTypes.TIMESTAMP.getMaxPrecision());
    sqlManager.getTypeBuilder(MySqlVendorTypes.DATETIME)
      .setMaxPrecision(MySqlVendorTypes.DATETIME.getMaxPrecision());




  }

  @Override
  public MySqlConnection getConnection() {
    return (MySqlConnection) super.getConnection();
  }

  /**
   * MySql does not allow to not set the precision
   */
  @Override
  protected String createDataTypeStatement(ColumnDef<?> columnDef) {


    SqlDataType<?> targetSqlType = this.sqlConnection.getSqlDataTypeFromSourceColumn(columnDef);
    int precision = columnDef.getPrecision();
    String sqlName = targetSqlType.toKeyNormalizer().toSqlCase();

    int targetTypeCode = targetSqlType.getVendorTypeNumber();
    switch (targetTypeCode) {
//      case Types.NVARCHAR:
//      case Types.NCHAR:
//      case Types.CHAR:
//      case Types.VARCHAR:
//
//        if (defaultPrecision == 0) {
//          throw new RuntimeException("The default precision is null for the data type (" + targetSqlType + ") and this is not allowed for a data type with precision");
//        }
//        if (precision == 0) {
//          precision = defaultPrecision;
//        }
//        return sqlName + "(" + precision + ")";
      case Types.TIMESTAMP:
      case Types.TIMESTAMP_WITH_TIMEZONE:
        // to avoid Invalid default value for timestamp field
        // we add null default null
        // https://stackoverflow.com/questions/9192027/invalid-default-value-for-create-date-timestamp-field
        SqlDataTypeNullable nullable = targetSqlType.getNullable();
        if (nullable != SqlDataTypeNullable.NO_NULL) {
          if (precision != 0) {
            sqlName = sqlName + "(" + precision + ")";
          }
          return sqlName + " null default null";
        }
        break;
    }
    return super.createDataTypeStatement(columnDef);

  }


  @Override
  public List<SqlMetaForeignKey> getMetaForeignKeys(SqlDataPath dataPath) {

    MySqlConnection sqlConnection = (MySqlConnection) (dataPath.getConnection());
    if (sqlConnection.isPlanetScale()) {
      SqlLog.LOGGER_DB_JDBC.warning("PlanetScale does not support the retrieval of imported keys (ie foreign keys). No Foreign keys added to " + dataPath);
      return new ArrayList<>();
    }
    return super.getMetaForeignKeys(dataPath);
  }


  @Override
  protected String createUpsertStatementUtilityOnConflict(TransferSourceTargetOrder transferSourceTarget) {
    List<UniqueKeyDef> targetUniqueKeysFoundInSourceColumns = getTargetUniqueKeysFoundInSourceColumns(transferSourceTarget);

    List<ColumnDef<?>> sourceNonUniqueColumnsForTarget = transferSourceTarget.getSourceNonUniqueColumnsForTarget();
    if (targetUniqueKeysFoundInSourceColumns.isEmpty() || sourceNonUniqueColumnsForTarget.isEmpty()) {
      return "";
    }

    /**
     * Build the statement
     */
    return "on duplicate key update " +
      sourceNonUniqueColumnsForTarget
        .stream()
        .map(c -> this.createQuotedName(c.getColumnName()) + " = values(" + this.createQuotedName(c.getColumnName()) + ")")
        .collect(Collectors.joining(", "));

  }

  /**
   * Unfortunately, the MySql driver does not return the correct precision for timestamp
   * so we need to read the information_schema and take over
   */
  @Override
  public List<SqlMetaColumn> getMetaColumns(SqlDataPath dataPath) {

    Connection currentConnection = dataPath.getConnection().getCurrentJdbcConnection();

    List<SqlMetaColumn> sqlMetaColumns = new ArrayList<>();
    String schemaName;
    try {
      schemaName = dataPath.getSchema().getName();
    } catch (NoSchemaException e) {
      try {
        schemaName = currentConnection.getSchema();
      } catch (SQLException ex) {
        // should not occur
        throw new RuntimeException("No schema found for the MySql connection. We can't retrieve the columns of the table " + dataPath, ex);
      }
    }


    String sql = "SELECT *\n" +
      "FROM information_schema.columns\n" +
      "WHERE (table_schema=? and table_name = ?)\n" +
      "order by ordinal_position";

    // Set the parameter values
    PreparedStatement preparedStatement;
    try {
      preparedStatement = currentConnection.prepareStatement(sql);
      preparedStatement.setString(1, schemaName);
      preparedStatement.setString(2, dataPath.getName());
    } catch (SQLException e) {
      // should not occur
      throw new RuntimeException("Internal Error in prepared statement", e);
    }

    try (
      ResultSet columnResultSet = preparedStatement.executeQuery()
    ) {
      while (columnResultSet.next()) {

        SqlMetaColumn sqlMetaColumn = SqlMetaColumn.createOf(columnResultSet.getString("COLUMN_NAME"));
        sqlMetaColumns.add(sqlMetaColumn);


        String sqlName = columnResultSet.getString("DATA_TYPE").toLowerCase();

        int scale = columnResultSet.getInt("NUMERIC_SCALE");
        Integer position = columnResultSet.getInt("ORDINAL_POSITION");
        Boolean isNullable = columnResultSet.getBoolean("IS_NULLABLE");

        /**
         * Precision
         * MySql has the precision spawn on  multiple column by type
         * We make the assumption that they are all null except the good one
         * We don't check the type
         * <p></p>
         * Fix point data type
         * We use getObject to see if the value is null
         * {@link ResultSet#getInt(int)} returns 0, and it's a little bit weird
         */
        Integer precision = columnResultSet.getObject("NUMERIC_PRECISION", Integer.class);
        if (precision == null) {
          /**
           * String
           */
          precision = columnResultSet.getObject("CHARACTER_MAXIMUM_LENGTH", Integer.class);
        }
        if (precision == null) {
          /**
           * Character
           */
          precision = columnResultSet.getObject("CHARACTER_OCTET_LENGTH", Integer.class);
        }
        if (precision == null) {
          /**
           * Date
           */
          precision = columnResultSet.getObject("DATETIME_PRECISION", Integer.class);
        }
        if (precision == null) {
          precision = 0;
        }
        sqlMetaColumn
          .setColumnSize(precision)
          .setTypeName(sqlName)
          .setDecimalDigits(scale)
          .setPosition(position)
          .setIsNullable(isNullable);
      }

    } catch (SQLException e) {
      throw new RuntimeException("Error while retrieving the metadata columns definition of the table " + dataPath, e);
    }
    return sqlMetaColumns;

  }

  /**
   * Create a drop statement for a {@link Constraint}
   * <a href="https://dev.mysql.com/doc/refman/8.4/en/alter-table.html">...</a>
   */
  protected String dropConstraintStatement(Constraint constraint) {
    StringBuilder dropConstraintStatement = new StringBuilder();
    dropConstraintStatement.append("alter ");
    SqlDataPath table = (SqlDataPath) constraint.getRelationDef().getDataPath();
    SqlMediaType type = table.getMediaType();
    //noinspection SwitchStatementWithTooFewBranches
    switch (type) {
      case TABLE:
        dropConstraintStatement.append("table ");
        break;
      default:
        throw new RuntimeException("The drop of foreign key on the table type (" + type + ") is not implemented");
    }
    dropConstraintStatement
      .append(table.toSqlStringPath())
      .append(" drop");
    ConstraintType constraintType = constraint.getConstraintType();
    switch (constraintType) {
      case FOREIGN_KEY:
        dropConstraintStatement.append(" FOREIGN KEY ")
          .append(createQuotedName(constraint.getName()));
        break;
      case PRIMARY_KEY:
        // no name is needed
        dropConstraintStatement
          .append(" PRIMARY KEY");
        break;
      case UNIQUE_KEY:
        dropConstraintStatement
          .append(" UNIQUE KEY ")
          .append(createQuotedName(constraint.getName()));
        break;
      default:
        throw new InternalException("The drop of the constraint type " + constraintType + " is not yet implemented");
    }

    return dropConstraintStatement.toString();

  }

  @Override
  protected List<String> createDropStatement(List<SqlDataPath> sqlDataPaths, Set<DropTruncateAttribute> dropAttributes) {
    SqlMediaType enumObjectType = sqlDataPaths.get(0).getMediaType();
    return SqlDropStatement.builder()
      .setType(enumObjectType)
      .setIsCascadeSupported(false)
      .setIfExistsSupported(true)
      .setMultipleSqlObjectSupported(false)
      .build()
      .getStatements(sqlDataPaths, dropAttributes);
  }

  @Override
  public Long getSize(DataPath dataPath) {
    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
    /**
     * Table only for now
     */
    if (!
      (
        sqlDataPath.getMediaType() == SqlMediaType.TABLE ||
          sqlDataPath.getMediaType() == SqlMediaType.SYSTEM_TABLE
      )) {
      return -1L;
    }

    SqlRequest sqlRequest = SqlRequest.builder()
      .setSql(this.getConnection(), "SELECT\n" +
        "    data_length + index_length\n" +
        "FROM information_schema.TABLES\n" +
        "WHERE table_schema = ?\n" +
        "  AND table_name = ?")
      .build()
      .addParameter(
        SqlParameter
          .builder()
          .setValue(sqlDataPath.getSchemaSafe().getName()
          )
      )
      .addParameter(
        SqlParameter
          .builder()
          .setValue(sqlDataPath.getName()
          )
      );
    List<List<?>> records = sqlRequest.execute().getRecords();
    if (records.isEmpty()) {
      // may not exist
      return -1L;
    }
    /**
     * {@link SqlDataTypeAnsi#BIGINT} ie Long
     */
    Object sizeAsObject = records.get(0).get(0);
    try {
      return Casts.cast(sizeAsObject, Long.class);
    } catch (CastException e) {
      throw new InternalException("The returned size of the resource (" + dataPath + ") could not be cast to a long. Error:" + e.getMessage(), e);
    }
  }

  @Override
  public Set<SqlDataTypeVendor> getSqlDataTypeVendors() {
    return Set.of(MySqlVendorTypes.values());
  }

  @Override
  public SqlTypeKeyUniqueIdentifier getSqlTypeKeyUniqueIdentifier() {
    return SqlTypeKeyUniqueIdentifier.NAME_ONLY;
  }

  @Override
  public String createUpsertMergeStatementWithParameters(TransferSourceTargetOrder transferSourceTarget) {
    return createUpsertStatementUtilityValuesPartBefore(transferSourceTarget) +
      createInsertStatementUtilityValuesClauseGenerator(transferSourceTarget, true, false) +
      createUpsertStatementUtilityValuesPartAfter(transferSourceTarget);
  }

  /**
   * Create upsert from values statement
   */
  @Override
  public String createUpsertMergeStatementWithPrintfExpressions(TransferSourceTargetOrder transferSourceTarget) {
    return createUpsertStatementUtilityValuesPartBefore(transferSourceTarget) +
      createInsertStatementUtilityValuesClauseGenerator(transferSourceTarget, false, false) +
      createUpsertStatementUtilityValuesPartAfter(transferSourceTarget);
  }

}
