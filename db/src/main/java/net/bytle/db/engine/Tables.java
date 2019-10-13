package net.bytle.db.engine;


import net.bytle.cli.Log;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;
import net.bytle.type.Typess;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Tables {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;





    /**
     * Drpping a foreign key
     *
     * @param foreignKeyDef
     */
    public static void dropForeignKey(ForeignKeyDef foreignKeyDef) {

        /**
         * TODO: move that outside of the core
         * for now a hack
         * because Sqlite does not support alter table drop foreig keys
         */
        if (!foreignKeyDef.getTableDef().getDatabase().getDatabaseProductName().equals(Database.DB_SQLITE)) {
            final RelationDef tableDef = foreignKeyDef.getTableDef();
            final Database database = tableDef.getDatabase();
            String dropStatement = "alter table " + tableDef.getFullyQualifiedName() + " drop constraint " + foreignKeyDef.getName();
            try {

                Connection currentConnection = database.getCurrentConnection();
                Statement statement = currentConnection.createStatement();
                statement.execute(dropStatement);
                statement.close();

                LOGGER.info("Foreign Key (" + foreignKeyDef.getName() + ") deleted from the table (" + tableDef.getFullyQualifiedName() + ")");

            } catch (SQLException e) {

                System.err.println(dropStatement);
                throw new RuntimeException(e);

            }
        }


    }


    public static void createIfNotExist(TableDef tableDef, Database database) {
        if (!exists(tableDef, database)) {
            create(tableDef, database);
        }
    }



    public static void drop(TableDef tableDef, Database database) {
        drop(tableDef, database.getCurrentSchema());
    }



    public static void truncate(TableDef tableDef) {
        truncate(tableDef,tableDef.getSchema());
    }
    public static void truncate(TableDef tableDef, SchemaDef schemaDef) {

        Database database = schemaDef.getDatabase();

        if (database.getUrl() != null) {
            StringBuilder truncateTableStatement = new StringBuilder().append("delete from ");
            if (schemaDef.getName() != null) {
                truncateTableStatement
                        .append(schemaDef.getName())
                        .append(".");
            }
            truncateTableStatement.append(tableDef.getName());
            // The connection must not be clause, don't put it in the try clause below
            Connection currentConnection = database.getCurrentConnection();
            try (
                    Statement statement = currentConnection.createStatement();
            ) {

                statement.execute(truncateTableStatement.toString());
                LOGGER.info("Table " + tableDef.getFullyQualifiedName() + " truncated");

            } catch (SQLException e) {
                System.err.println(truncateTableStatement);
                throw new RuntimeException(e);
            }
        } else {

            throw new RuntimeException("Non Sql Data Source - Not implemented");

        }
    }






    public static <T> T getMin(ColumnDef<T> columnDef) {

        String columnStatement = columnDef.getColumnName();
        String statementString = "select min(" + columnStatement + ") from " + columnDef.getRelationDef().getFullyQualifiedName();
        Connection currentConnection = columnDef.getRelationDef().getDatabase().getCurrentConnection();
        try (
                Statement statement = currentConnection.createStatement();
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
            if (returnValue!=null) {

                return Typess.safeCast(returnValue,columnDef.getClazz());

            } else {
                return null;
            }

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }
    }


}

