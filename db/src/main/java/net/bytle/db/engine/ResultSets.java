package net.bytle.db.engine;

import net.bytle.db.database.Database;
import net.bytle.db.model.RelationDef;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class ResultSets {


    public static void print(ResultSet resultSet) {

        // TODO
        // Changes with
        // https://docs.oracle.com/javase/6/docs/api/java/util/Formatter.html#format(java.lang.String,%20java.lang.Object...)
        try {

            for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {

                System.out.printf(resultSet.getMetaData().getColumnName(i) + ", ");

            }
            System.out.println();

            while (resultSet.next()) {
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    System.out.printf(resultSet.getString(i) + " ");
                }
                System.out.println();
            }

        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    /*
     * Print the type for a resultSet
     */
    public static void printDataTypeInformation(ResultSet resultSet) throws SQLException {

        ResultSetMetaData metadata = resultSet.getMetaData();

        // Header of Headers
        System.out.println("Number of columns: " + metadata.getColumnCount());
        System.out.println("Statement (if lucky): " + resultSet.getStatement().toString());

        // Headers
        System.out.println("ColumnId\t" +
                "Column Name\t" +
                "Column Label (As)\t" +
                "Type Int\t" +
                "Type Name\t" +
                "Java Class\t" +
                "Value");

        if (resultSet.getType() != ResultSet.TYPE_FORWARD_ONLY) {
            resultSet.beforeFirst();
        }
        resultSet.next();

        // Column
        for (int i = 1; i <= metadata.getColumnCount(); i++) {

            Object value;
            String classString;


            Object obj = resultSet.getObject(i);

            if (obj != null) {
                classString = obj.getClass().toString();
                value = obj;
            } else {
                classString = "null";
                value = "null";
            }


            System.out.println(i +
                    "\t" + metadata.getColumnName(i) +
                    "\t" + metadata.getColumnLabel(i) +
                    "\t" + metadata.getColumnType(i) +
                    "\t" + metadata.getColumnTypeName(i) +
                    "\t" + classString +
                    "\t" + value
            );
        }


    }

    public static void addColumns(ResultSetMetaData resultSetMetaData, RelationDef relationDef) {

        // Add the columns
        try {

            int columnCount = resultSetMetaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                relationDef.getColumnOf(resultSetMetaData.getColumnName(i))
                        .typeCode(resultSetMetaData.getColumnType(i))
                        .precision(resultSetMetaData.getPrecision(i))
                        .scale(resultSetMetaData.getScale(i));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public static void addColumns(ResultSet resultSet, RelationDef relationDef) {

        try {

            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            addColumns(resultSetMetaData, relationDef);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * All method used when starting the software
     * A resultSet is now in a {@link net.bytle.db.model.QueryDef encapsulated}
     *
     * @param resultSetMetaData
     * @param database
     * @throws SQLException
     */
    public void printDataTypeMapping(ResultSetMetaData resultSetMetaData, Database database) throws SQLException {

        // Headers
        System.out.println("ColumnId\t" +
                "Column Name\t" +
                "Column Label (As)\t" +
                "Type Int\t" +
                "Type Name\t" +
                "Target Type Name\t" +
                "Type Precision\t" +
                "Type Scale");


        // Column
        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {

            System.out.println(i +
                    "\t" + resultSetMetaData.getColumnName(i) +
                    "\t" + resultSetMetaData.getColumnLabel(i) +
                    "\t" + resultSetMetaData.getColumnType(i) +
                    "\t" + resultSetMetaData.getColumnTypeName(i) +
                    "\t" + resultSetMetaData.getPrecision(i) +
                    "\t" + resultSetMetaData.getScale(i)
            );
        }

    }

}
