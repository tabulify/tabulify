package com.tabulify.jdbc;

import com.tabulify.model.ColumnDef;
import com.tabulify.spi.ProcessingEngine;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.type.Casts;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public class SqlDataProcessingEngine extends ProcessingEngine {

  private final SqlConnection jdbcDataStore;

  public SqlDataProcessingEngine(SqlConnection jdbcDataStore) {
    this.jdbcDataStore = jdbcDataStore;
  }

  @Override
  public Object getMax(ColumnDef columnDef) {


    SqlDataSystem dataSystem = jdbcDataStore.getDataSystem();
    @SuppressWarnings("SqlDialectInspection") String statementString = "select max(" + dataSystem.createQuotedName(columnDef.getColumnName()) + ") from " + ((SqlDataPath) columnDef.getRelationDef().getDataPath()).toSqlStringPath();
    try (Statement statement = this.jdbcDataStore.getCurrentConnection().createStatement()) {
      ResultSet resultSet = statement.executeQuery(statementString);
      Object returnValue = null;
      if (resultSet.next()) {
        returnValue = resultSet.getObject(1);
      }
      try {
        return Casts.cast(returnValue, columnDef.getClazz());
      } catch (CastException e) {
        throw IllegalArgumentExceptions.createFromMessage("The maximum value (" + returnValue + ") is not a valid value for the column (" + columnDef + ")", e);
      }

    } catch (SQLException e) {

      throw new RuntimeException(e);

    }

  }

  @Override
  public Object getMin(ColumnDef columnDef) {

    SqlDataSystem sqlDataSystem = (SqlDataSystem) columnDef.getRelationDef().getDataPath().getConnection().getDataSystem();
    String statementString = "select min(" + sqlDataSystem.createQuotedName(columnDef.getColumnName()) + ") from " + ((SqlDataPath) columnDef.getRelationDef().getDataPath()).toSqlStringPath();

    try (Statement statement = this.jdbcDataStore.getCurrentConnection().createStatement()) {
      ResultSet resultSet = statement.executeQuery(statementString);
      Object returnValue = null;

      if (resultSet.next()) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (columnDef.getDataType().getTypeCode()) {
          case Types.DATE:
            // In sqllite, getting a date object returns a long
            returnValue = resultSet.getDate(1);
            break;
          default:
            returnValue = resultSet.getObject(1);
            break;
        }

      }
      if (returnValue != null) {

        try {
          return Casts.cast(returnValue, columnDef.getClazz());
        } catch (CastException e) {
          throw IllegalArgumentExceptions.createFromMessage("The value (" + returnValue + ") is not a valid value for the column (" + columnDef + ")", e);
        }

      } else {
        return null;
      }

    } catch (SQLException e) {

      throw new RuntimeException(e);

    }

  }


}
