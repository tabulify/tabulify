package com.tabulify.sqlserver;

import com.tabulify.jdbc.*;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.model.SqlTypes;
import com.tabulify.spi.DataPath;

import java.sql.Types;
import java.util.List;
import java.util.Map;

public class SqlServerDataSystem extends SqlDataSystem {

  public SqlServerDataSystem(SqlServerConnection jdbcDataStore) {
    super(jdbcDataStore);
  }

  /**
   * <a href="https://docs.microsoft.com/en-us/sql/t-sql/statements/truncate-table-transact-sql?view=sql-server-ver15">...</a>
   */
  @Override
  public void truncate(List<DataPath> dataPaths) {
    super.truncate(dataPaths);
  }

  @Override
  public Map<Integer, SqlMetaDataType> getMetaDataTypes() {
    Map<Integer, SqlMetaDataType> metaDataType = super.getMetaDataTypes();

    /**
     * Character
     */
    metaDataType.computeIfAbsent(Types.VARCHAR, SqlMetaDataType::new)
      .setDefaultPrecision(1)
      .setMaxPrecision(2147483647);

    metaDataType.computeIfAbsent(Types.CHAR, SqlMetaDataType::new)
      .setDefaultPrecision(1);

    // nvarchar is used to store json in the doc
    metaDataType.computeIfAbsent(Types.NVARCHAR, SqlMetaDataType::new)
      .setSqlName("nvarchar") // was sysname
      .setDefaultPrecision(1)
      .setMaxPrecision(2147483647);

    /**
     * https://docs.microsoft.com/en-us/sql/connect/jdbc/using-advanced-data-types
     * Clob can be seen as text that comes back as long varchar
     */
    metaDataType.computeIfAbsent(Types.CLOB, SqlMetaDataType::new)
      .setSqlName("text");
    metaDataType.computeIfAbsent(Types.LONGVARCHAR, SqlMetaDataType::new)
      .setSqlName("text");

    /**
     * Integer
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/int-bigint-smallint-and-tinyint-transact-sql
     * The driver was adding the identity word
     * https://docs.microsoft.com/en-us/sql/t-sql/statements/create-table-transact-sql-identity-property
     */
    metaDataType.computeIfAbsent(Types.INTEGER, SqlMetaDataType::new)
      .setSqlName("int");

    metaDataType.computeIfAbsent(Types.BIGINT, SqlMetaDataType::new)
      .setSqlName("bigint");

    metaDataType.computeIfAbsent(Types.SMALLINT, SqlMetaDataType::new)
      .setSqlName("smallint");

    metaDataType.computeIfAbsent(Types.TINYINT, SqlMetaDataType::new)
      .setSqlName("tinyint");

    /**
     * Numeric
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/decimal-and-numeric-transact-sql?view=sql-server-ver15
     */
    metaDataType.computeIfAbsent(Types.NUMERIC, SqlMetaDataType::new)
      .setSqlName("numeric")
      .setMaxPrecision(38)
      .setDefaultPrecision(18);

    metaDataType.computeIfAbsent(Types.DECIMAL, SqlMetaDataType::new)
      .setSqlName("decimal")
      .setMaxPrecision(38)
      .setDefaultPrecision(18);

    /**
     * Float, Double, Real
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/float-and-real-transact-sql?view=sql-server-ver15
     */
    metaDataType.computeIfAbsent(Types.FLOAT, SqlMetaDataType::new)
      .setSqlName("float")
      .setMaxPrecision(53)
      .setDefaultPrecision(53);

    /**
     * Double = float(53)
     */
    metaDataType.computeIfAbsent(Types.DOUBLE, SqlMetaDataType::new)
      .setSqlName("float")
      .setMaxPrecision(53)
      .setDefaultPrecision(53);

    /**
     *
     * datetime should become datetime2.
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/datetime-transact-sql?view=sql-server-ver15
     * <p></p>
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/datetime2-transact-sql?view=sql-server-ver15
     */
    metaDataType.computeIfAbsent(Types.TIMESTAMP, SqlMetaDataType::new)
      .setSqlName("datetime2")
      .setDefaultPrecision(7);

    /**
     * TIMESTAMP_WITH_TIMEZONE is known as datetimeoffset
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/datetimeoffset-transact-sql?view=sql-server-ver15
     */
    metaDataType.computeIfAbsent(Types.TIMESTAMP_WITH_TIMEZONE, SqlMetaDataType::new)
      .setSqlName("datetimeoffset")
      .setDefaultPrecision(7);

    metaDataType.computeIfAbsent(SqlServerTypes.DATETIMEOFFSET, i -> metaDataType.get(Types.TIMESTAMP_WITH_TIMEZONE))
      .setSqlJavaClazz(java.sql.Timestamp.class)
      .setDriverTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
      .setDefaultPrecision(7);

    metaDataType.computeIfAbsent(Types.TIME, SqlMetaDataType::new)
      .setSqlName("time")
      .setDefaultPrecision(7);

    /**
     * Boolean
     * https://docs.microsoft.com/en-us/sql/t-sql/data-types/bit-transact-sql?view=sql-server-ver15
     */
    metaDataType.computeIfAbsent(Types.BOOLEAN, i -> metaDataType.get(Types.BIT))
      .setSqlName("bit");

    /**
     * See
     * https://docs.microsoft.com/en-us/sql/connect/jdbc/using-advanced-data-types
     */
    metaDataType.computeIfAbsent(SqlTypes.JSON, SqlMetaDataType::new)
      .setSqlName("nvarchar")
      .setDriverTypeCode(Types.NVARCHAR);


    /**
     * See
     * https://docs.microsoft.com/en-us/sql/connect/jdbc/using-advanced-data-types
     */
    metaDataType.computeIfAbsent(Types.SQLXML, SqlMetaDataType::new)
      .setSqlName("xml");

    return metaDataType;

  }

  @Override
  protected String createDataTypeStatement(ColumnDef columnDef) {
    SqlDataType dataType = columnDef.getDataType();

    Integer precision = columnDef.getPrecision();
    switch (dataType.getTargetTypeCode()) {
      case Types.TIMESTAMP_WITH_TIMEZONE:
        if (!(precision == null || precision.equals(dataType.getDefaultPrecision()))) {
          return dataType.getSqlName() + "(" + precision + ")";
        } else {
          return dataType.getSqlName();
        }
      case Types.VARCHAR:
      case Types.NVARCHAR:
        /**
         * The default for varchar is 1 when there is no position
         * Which means that we got a lot of problem.
         *
         * We change that it to make it max and output `max` when the precision is the max
         *
         * This has also the effect that JSON takes also the max
         */
        StringBuilder typeStatement = new StringBuilder();
        typeStatement.append(dataType.getSqlName());

        typeStatement.append("(");
        if (precision == null || dataType.getMaxPrecision() != null && columnDef.getPrecision().equals(dataType.getMaxPrecision())) {
          typeStatement.append("max");
        } else {
          typeStatement.append(columnDef.getPrecision());
        }
        typeStatement.append(")");

        return typeStatement.toString();
      default:
        return super.createDataTypeStatement(columnDef);
    }

  }


  @Override
  public List<SqlMetaColumn> getMetaColumns(SqlDataPath dataPath) {
    List<SqlMetaColumn> metaColumns = super.getMetaColumns(dataPath);

    metaColumns.forEach(c -> {
        if (c.getTypeCode().equals(SqlServerTypes.DATETIMEOFFSET)) {
          c.setPrecision(c.getScale());
          c.setScale(null);
        }
        /**
         * The diver is return {@link Types.LONGNVARCHAR}
         */
        if (c.getTypeName().equals("xml")) {
          c.setTypeCode(Types.SQLXML);
        }
      }
    );
    return metaColumns;
  }


  @Override
  protected String createDropTableStatement(SqlDataPath sqlDataPath) {
    if (sqlDataPath.getMediaType() == SqlDataPathType.VIEW) {
      // 'DROP VIEW' does not allow specifying the database name as a prefix to the object name.
      return "drop view " + sqlDataPath.toSqlStringPath(2);

    } else {
      return super.createDropTableStatement(sqlDataPath);
    }
  }


}
