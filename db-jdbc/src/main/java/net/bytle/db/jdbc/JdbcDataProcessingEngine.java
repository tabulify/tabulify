package net.bytle.db.jdbc;

import net.bytle.db.model.ColumnDef;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.type.Typess;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public class JdbcDataProcessingEngine extends ProcessingEngine {

    private final SqlDataStore jdbcDataStore;

    public JdbcDataProcessingEngine(SqlDataStore jdbcDataStore) {
        this.jdbcDataStore = jdbcDataStore;
    }

    @Override
    public <T> T getMax(ColumnDef<T> columnDef) {

        String columnStatement = columnDef.getColumnName();

        String statementString = "select max(" + columnStatement + ") from " + JdbcDataSystemSql.getFullyQualifiedSqlName(columnDef.getDataDef().getDataPath());
        try (
          Statement statement = this.jdbcDataStore.getCurrentConnection().createStatement();
          ResultSet resultSet = statement.executeQuery(statementString);
        ) {
            Object returnValue = null;
            if (resultSet.next()) {
                returnValue = resultSet.getObject(1);
            }
            return (T) returnValue;

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

    }

    @Override
    public <T> T getMin(ColumnDef<T> columnDef) {

        String columnStatement = columnDef.getColumnName();
        String statementString = "select min(" + columnStatement + ") from " + JdbcDataSystemSql.getFullyQualifiedSqlName(columnDef.getDataDef().getDataPath());

        try (
          Statement statement = this.jdbcDataStore.getCurrentConnection().createStatement();
          ResultSet resultSet = statement.executeQuery(statementString);
        ) {
            Object returnValue = null;

            if (resultSet.next()) {
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

                return Typess.safeCast(returnValue, columnDef.getClazz());

            } else {
                return null;
            }

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

    }



}
