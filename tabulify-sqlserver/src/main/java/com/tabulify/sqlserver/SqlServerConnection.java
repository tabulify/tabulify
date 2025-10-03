package com.tabulify.sqlserver;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.model.SqlDataTypeAnsi;
import microsoft.sql.DateTimeOffset;
import net.bytle.exception.CastException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gerard on 28-11-2015.
 * <p>
 * Tracing
 * <a href="https://docs.microsoft.com/en-us/sql/connect/jdbc/tracing-driver-operation?view=sql-server-2017">...</a>
 * Logger logger = Logger.getLogger("com.microsoft.sqlserver.jdbc");
 * logger.setLevel(Level.FINE);
 */
public class SqlServerConnection extends SqlConnection {


  public SqlServerConnection(Tabular tabular, Attribute name, Attribute url) {
    super(tabular, name, url);
  }

  @Override
  public Map<String, Object> getDefaultNativeDriverAttributes() {
    // Sql Server
    // https://docs.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties?view=sql-server-2017
    //https://docs.microsoft.com/en-us/sql/t-sql/functions/context-info-transact-sql?view=sql-server-2017
    Map<String, Object> properties = new HashMap<>();
    properties.put("applicationName", getTabular().toPublicName(getTabular().getName() + "-" + this.getName()));
    return properties;
  }

  @Override
  public SqlServerDataSystem getDataSystem() {
    return new SqlServerDataSystem(this);
  }


  @Override
  public SqlDataType<?> getSqlDataTypeFromSourceColumn(ColumnDef<?> columnDef) {

    switch (columnDef.getDataType().getAnsiType()) {
      case BOOLEAN:
        /**
         * Supported as bit 1
         * https://learn.microsoft.com/en-us/sql/t-sql/data-types/bit-transact-sql
         */
        return this.getSqlDataType(SqlDataTypeAnsi.BIT);
      case CLOB:
        /**
         * Should be a varchar(max) yeah ...
         * https://learn.microsoft.com/en-us/sql/connect/jdbc/using-advanced-data-types?view=sql-server-ver17#blob-and-clob-and-nclob-data-types
         */
        return this.getSqlDataType(SqlDataTypeAnsi.LONG_CHARACTER_VARYING);
      case TIME_WITH_TIME_ZONE:
        return this.getSqlDataType(SqlDataTypeAnsi.TIME);
      case TIMESTAMP_WITH_TIME_ZONE:
        return this.getSqlDataType(SqlServerTypes.DATETIMEOFFSET);
      /**
       * SQL know only 2 floating point, real and float(p)
       * Float known only 2 p (24 or 53)
       * If 1<=n<=24, n is treated as 24. If 25<=n<=53, n is treated as 53.
       * https://learn.microsoft.com/en-us/sql/t-sql/data-types/float-and-real-transact-sql
       * We convert to real for float < 24 so that's clear
       */
      case FLOAT:
        int precision = columnDef.getPrecision();
        if (precision != 0 && precision <= 24) {
          return this.getSqlDataType(SqlDataTypeAnsi.REAL);
        }
        return this.getSqlDataType(SqlDataTypeAnsi.FLOAT);
    }

    return super.getSqlDataTypeFromSourceColumn(columnDef);
  }

  @Override
  public Object toSqlObject(Object sourceObject, SqlDataType<?> targetColumnType) throws CastException {
    if (targetColumnType.getValueClass().equals(DateTimeOffset.class)) {
      if (sourceObject instanceof OffsetDateTime) {
        return DateTimeOffset.valueOf((OffsetDateTime) sourceObject);
      }
      throw new CastException("The source value is not a OffsetDateTime object (ie timestamp with time zone) but a " + sourceObject.getClass().getName() + ". We could not convert it to the Microsoft DateTimeOffset.");
    }
    return super.toSqlObject(sourceObject, targetColumnType);
  }

}
