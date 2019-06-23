package net.bytle.db.loader;

import oracle.jdbc.OracleTypes;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Created by gerard on 22-03-2017.
 */
public class Databases {

    /**
     * Return an object to be set in a prepared statement (for instance)
     * Example: if you want to load a double in an Oracle BINARY_DOUBLE, you need to cast it first as a
     * oracle.sql.BINARY_DOUBLE
     *
     * @param targetConnection the target connection
     * @param targetColumnType the target column type
     * @param sourceObject     the java object to be loaded
     * @return
     */
    public static Object castLoadObjectIfNecessary(Connection targetConnection, int targetColumnType, Object sourceObject) {

        String databaseProductName;
        try {
            databaseProductName = targetConnection.getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (databaseProductName.equals("Oracle")) {

            if (targetColumnType == OracleTypes.BINARY_DOUBLE && sourceObject instanceof Double) {
                return new oracle.sql.BINARY_DOUBLE((Double) sourceObject);
            } else if (targetColumnType == OracleTypes.BINARY_FLOAT && sourceObject instanceof Float) {
                return new oracle.sql.BINARY_FLOAT((Float) sourceObject);
            } else {
                return sourceObject;
            }

        } else {
            return sourceObject;
        }

    }

    /**
     * Create an insert statement from a result set
     * @param targetTableName
     * @param sourceResultSet
     * @return the insert statement
     * @throws SQLException
     */
    public static String getInsertStatement(String targetTableName, ResultSet sourceResultSet) throws SQLException {

        ResultSetMetaData metaData = sourceResultSet.getMetaData();
        String insertStatement = "INSERT INTO " + targetTableName + " (";
        String insertStatementBindVariable = "";

        // Loop to create the statement
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            insertStatement += "\""+metaData.getColumnName(i) + "\", ";
            insertStatementBindVariable += "?, ";
        }

        // Suppress the last comma
        insertStatement = insertStatement.substring(0, insertStatement.length() - 2);
        insertStatementBindVariable = insertStatementBindVariable.substring(0, insertStatementBindVariable.length() - 2);

        insertStatement += ") VALUES (" + insertStatementBindVariable + ")";

        return insertStatement;

    }

    public static void printResultSet(ResultSet resultSet){
        try {
            if (resultSet!=null) {
                final int columnCount = resultSet.getMetaData().getColumnCount();
                while (resultSet.next()) {
                    for(int i = 0; i < columnCount; i++){
                        System.out.print(resultSet.getString(i+1));
                        if (i!=columnCount-1) {
                            System.out.print(",");
                        }
                    }
                    System.out.print(System.lineSeparator());
                }
                System.out.flush();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Print the headers (column name)
     * @param resultSet
     */
    public static void printColumnNames(ResultSet resultSet){
        try {
            ResultSetMetaData metaData = resultSet.getMetaData();

            int columnCount = metaData.getColumnCount();

            for (int i = 0; i < columnCount; i++) {
                System.out.print(metaData.getColumnName(i+1));
                if (i!=columnCount-1) {
                    System.out.print(",");
                }
            }
            System.out.print(System.lineSeparator());
            System.out.flush();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
