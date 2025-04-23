package com.tabulify.mysql;

import com.tabulify.jdbc.*;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.model.UniqueKeyDef;
import com.tabulify.transfer.TransferSourceTarget;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.sql.DatabaseMetaData.typeNoNulls;

public class MySqlDataSystem extends SqlDataSystem {


  /**
   * VARCHAR column that uses the utf8 character set can be declared to be a maximum of 21,844 characters.
   * https://dev.mysql.com/doc/refman/5.6/en/string-type-syntax.html
   */
  public static final Integer VARCHAR_MAX_LENGTH = 21844;
  public static final Integer VARCHAR_MAX_LENGTH_PSCALE = 16383;

  /**
   * 256 - 1
   * https://dev.mysql.com/doc/refman/5.7/en/char.html
   */
  public static final Integer CHAR_MAX_LENGTH = 255;
  private static final Integer CHAR_MAX_LENGTH_PSCALE = CHAR_MAX_LENGTH;


  public MySqlDataSystem(SqlConnection sqlConnection) {
    super(sqlConnection);
  }

  @Override
  public Map<Integer, SqlMetaDataType> getMetaDataTypes() {

    Map<Integer, SqlMetaDataType> sqlMetaDataType = super.getMetaDataTypes();

    Integer maxVarcharPrecision = MySqlDataSystem.VARCHAR_MAX_LENGTH;
    if (this.getConnection().isPlanetScale()) {
      maxVarcharPrecision = MySqlDataSystem.VARCHAR_MAX_LENGTH_PSCALE;
    }

    Integer maxCharPrecision = MySqlDataSystem.CHAR_MAX_LENGTH;
    if (this.getConnection().isPlanetScale()) {
      maxCharPrecision = MySqlDataSystem.CHAR_MAX_LENGTH_PSCALE;
    }
    /**
     * All text are unicode, no distinction between National data type
     * and normal data type
     * nvarchar = varchar
     * nchar = char
     * https://dev.mysql.com/doc/refman/5.6/en/string-type-syntax.html
     */
    sqlMetaDataType.computeIfAbsent(Types.NVARCHAR, SqlMetaDataType::new)
      .setSqlName("varchar")
      .setMaxPrecision(maxVarcharPrecision);
    sqlMetaDataType.computeIfAbsent(Types.NCHAR, SqlMetaDataType::new)
      .setSqlName("char")
      .setMaxPrecision(maxCharPrecision);

    /**
     * For whatever reason, VARCHAR in the driver is TINYTEXT (max 255)
     */
    sqlMetaDataType.computeIfAbsent(Types.VARCHAR, SqlMetaDataType::new)
      .setSqlName("varchar")
      .setMaxPrecision(maxVarcharPrecision);

    /**
     * For whatever reason, CHAR in the driver is SET (https://dev.mysql.com/doc/refman/5.7/en/set.html)
     */
    sqlMetaDataType.computeIfAbsent(Types.CHAR, SqlMetaDataType::new)
      .setSqlName("char")
      .setMaxPrecision(maxCharPrecision);

    /**
     * https://dev.mysql.com/doc/refman/5.7/en/blob.html
     */
    sqlMetaDataType.computeIfAbsent(Types.CLOB, SqlMetaDataType::new)
      .setSqlName("text");

    /**
     * https://dev.mysql.com/doc/refman/5.7/en/fixed-point-types.html
     */
    int maxPrecision = 65; // max digits
    int defaultPrecision = 10; // when no precision is given
    int maximumScale = 30;
    int minimumScale = 0;
    sqlMetaDataType.computeIfAbsent(Types.DECIMAL, SqlMetaDataType::new)
      .setSqlName("decimal")
      .setMaxPrecision(maxPrecision)
      .setMaximumScale(maximumScale)
      .setMinimumScale(minimumScale)
      .setDefaultPrecision(defaultPrecision);

    /**
     * numeric = decimal as stated here:
     * https://dev.mysql.com/doc/refman/5.7/en/fixed-point-types.html
     */
    sqlMetaDataType.computeIfAbsent(Types.NUMERIC, SqlMetaDataType::new)
      .setSqlName("decimal")
      .setMaxPrecision(maxPrecision)
      .setMaximumScale(maximumScale)
      .setMinimumScale(minimumScale)
      .setDefaultPrecision(defaultPrecision)
      .setAutoIncrement(false);

    /**
     * Types.DATE is by default a year(4) ....
     * https://dev.mysql.com/doc/refman/5.7/en/fixed-point-types.html
     */
    sqlMetaDataType.computeIfAbsent(Types.DATE, SqlMetaDataType::new)
      .setSqlName("date")
      .setAutoIncrement(false);

    /**
     * Default precision to zero so that we don't get it in the `create` statement
     * By default, null
     */
    sqlMetaDataType.computeIfAbsent(Types.TIME, SqlMetaDataType::new)
      .setDefaultPrecision(0);

    /**
     * Timezone does not exist
     * MySQL converts TIMESTAMP values from the current time zone to UTC for storage, and back from UTC to the current time zone for retrieval.
     * https://dev.mysql.com/doc/refman/8.4/en/datetime.html
     */
    SqlMetaDataType timestamp = sqlMetaDataType.get(Types.TIMESTAMP);
    sqlMetaDataType.put(Types.TIMESTAMP_WITH_TIMEZONE, timestamp);
    SqlMetaDataType time = sqlMetaDataType.get(Types.TIME);
    sqlMetaDataType.put(Types.TIME_WITH_TIMEZONE, time);

    return sqlMetaDataType;

  }

  @Override
  public MySqlConnection getConnection() {
    return (MySqlConnection) super.getConnection();
  }

  /**
   * MySql does not allow to not set the precision
   */
  @Override
  protected String createDataTypeStatement(ColumnDef columnDef) {


    SqlDataType targetSqlType = this.sqlConnection.getSqlDataTypeFromSourceDataType(columnDef.getDataType());
    Integer defaultPrecision = targetSqlType.getDefaultPrecision();
    Integer precision = columnDef.getPrecision();
    String sqlName = targetSqlType.getSqlName();

    int targetTypeCode = targetSqlType.getTypeCode();
    switch (targetTypeCode) {
      case Types.NVARCHAR:
      case Types.NCHAR:
      case Types.CHAR:
      case Types.VARCHAR:

        if (defaultPrecision == null) {
          throw new RuntimeException("The default precision is null for the data type (" + targetSqlType + ") and this is not allowed for a data type with precision");
        }
        if (precision == null) {
          precision = defaultPrecision;
        }
        return sqlName + "(" + precision + ")";
      case Types.TIMESTAMP:
      case Types.TIMESTAMP_WITH_TIMEZONE:
        // to avoid Invalid default value for timestamp field
        // we add null default null
        // https://stackoverflow.com/questions/9192027/invalid-default-value-for-create-date-timestamp-field
        Short nullable = targetSqlType.getNullable();
        if (nullable != typeNoNulls) {
          if (precision != null && precision != 0) {
            sqlName = sqlName + "(" + precision + ")";
          }
          return sqlName + " null default null";
        }

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
  protected String createUpsertStatementUtilityOnConflict(TransferSourceTarget transferSourceTarget) {
    List<UniqueKeyDef> targetUniqueKeysFoundInSourceColumns = getTargetUniqueKeysFoundInSourceColumns(transferSourceTarget);

    List<ColumnDef> sourceNonUniqueColumnsForTarget = transferSourceTarget.getSourceNonUniqueColumnsForTarget();
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


}
