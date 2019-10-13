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
     * Create all tables taking into account the foreign key constraints
     * <p>
     * This function will throw an exception if the table already exist.
     * To not throw an exception, see the funciton @{link {@link #createIfNotExist(List)}}
     *
     * @param tables
     */
    public static void create(List<TableDef> tables, Database database) {

        Dag dag = Dag.get(tables);
        tables = dag.getCreateOrderedTables();
        for (TableDef tableDef : tables) {

            if (database == null) {
                Tables.create(tableDef);
            } else {
                Tables.create(tableDef, database);
            }

        }

    }

    /**
     * Create all tables taking into account the foreign key constraints
     * <p>
     * This function will throw an exception if the table already exist.
     * To not throw an exception, see the funciton @{link {@link #createIfNotExist(List)}}
     *
     * @param tables
     */
    public static void create(List<TableDef> tables) {

        create(tables, null);

    }

    /**
     * Alias function that create the table in the default schema of the database
     * call the function @{link {@link #create(TableDef, SchemaDef)}}
     *
     * @param tableDef
     * @param database - the target database
     */
    public static void create(TableDef tableDef, Database database) {
        create(tableDef, database.getCurrentSchema());
    }

    public static TableDef create(TableDef tableDef, String tableName) {
        return create(tableDef, tableDef.getSchema(), tableName);
    }

    /**
     * Create the table in a new schema
     *
     * @param tableDef
     * @param schemaDef - the target schema
     */
    public static void create(TableDef tableDef, SchemaDef schemaDef) {

        create(tableDef, schemaDef, tableDef.getName());

    }

    /**
     * Create the table in a new schema with a new name
     *
     * @param tableDef
     * @param schemaDef - the target schema
     * @param tableName - the table name
     */
    public static TableDef create(TableDef tableDef, SchemaDef schemaDef, String tableName) {

        // Check that the foreign tables exist
        for (ForeignKeyDef foreignKeyDef : tableDef.getForeignKeys()) {
            TableDef foreignTable = foreignKeyDef.getForeignPrimaryKey().getTableDef();
            if (!exists(foreignTable, schemaDef)) {
                throw new RuntimeException("The foreign table (" + foreignTable + ") does not exist");
            }
        }

        // Standard SQL
        List<String> createTableStatements = DbDdl.getCreateTableStatements(tableDef, schemaDef, tableName);
        for (String createTableStatement : createTableStatements) {
            try {
                final Connection currentConnection = schemaDef.getDatabase()
                        .getCurrentConnection();
                if (currentConnection == null) {
                    throw new RuntimeException("Cannot create the table (" + tableDef.getName() + ") because the database have a null connection. Are you sure it's a SQL database (" + schemaDef.getName() + ") where you want to create the table");
                }

                Statement statement = currentConnection.createStatement();
                statement.execute(createTableStatement);
                statement.close();

            } catch (SQLException e) {
                System.err.println(createTableStatement);
                throw new RuntimeException(e);
            }
        }
        LOGGER.info("Table " + tableDef.getFullyQualifiedName() + " created in the schema (" + schemaDef + ")");

        return schemaDef.getTableOf(tableName);

    }


    /**
     * Create the table in the database if it doesn't exist
     *
     * @param tableDef
     */
    public static void createIfNotExist(TableDef tableDef) {

        if (!exists(tableDef)) {

            create(tableDef);

        } else {

            LOGGER.fine("The table (" + tableDef.getName() + ") already exist.");

        }

    }

    /**
     * Drop the table from the database if exist
     * and drop the table from the cache
     *
     * @param tableDefs
     */
    public static void dropIfExists(List<TableDef> tableDefs) {

        for (TableDef tableDef : Dag.get(tableDefs).getDropOrderedTables()) {
            if (exists(tableDef)) {
                drop(tableDef);
            }
        }

    }


    /**
     * Call @{link {@link #drop(List)}}
     *
     * @param tableDefs
     */
    public static void drop(TableDef... tableDefs) {

        drop(Arrays.asList(tableDefs));

    }

    public static void drop(TableDef tableDef, SchemaDef schemaDef) {
        Database database = schemaDef.getDatabase();

        if (database.getUrl() != null) {
            StringBuilder dropTableStatement = new StringBuilder().append("drop table '");
            if (schemaDef.getName() != null) {
                dropTableStatement
                        .append(schemaDef.getName())
                        .append(".");
            }
            dropTableStatement.append(tableDef.getName())
                    .append("'");
            // The connection must not be clause, don't put it in the try clause below
            Connection currentConnection = database.getCurrentConnection();
            try (
                    Statement statement = currentConnection.createStatement();
            ) {

                statement.execute(dropTableStatement.toString());
                LOGGER.info("Table " + tableDef.getFullyQualifiedName() + " dropped");

            } catch (SQLException e) {
                System.err.println(dropTableStatement);
                throw new RuntimeException(e);
            }
        } else {

            throw new RuntimeException("Non Sql Data Source - Not implemented");

        }
    }

    /**
     * Drop one or more tables
     * <p>
     * If the table is a foreign table, the child constraint will
     * prevent the table to be dropped if the child table is not given.
     * <p>
     *
     * @param tableDefs - The tables to drop
     */
    public static void drop(List<TableDef> tableDefs) {

        Dag dag = Dag.get(tableDefs);

        for (TableDef tableDef : dag.getDropOrderedTables()) {

            drop(tableDef, tableDef.getSchema());

        }
    }


    /**
     * Suppress all rows of the table
     *
     * @param tableDef - the tableDef where to suppress all rows
     */
    public static void delete(TableDef tableDef) {

        Database database = tableDef.getDatabase();
        if (database.getUrl() != null) {
            String deleteStatement = "delete from " + tableDef.getFullyQualifiedName();
            Connection currentConnection = database.getCurrentConnection();
            try (
                    Statement statement = currentConnection.createStatement();
            ) {
                statement.execute(deleteStatement);
                // Without commit, the database is locked for sqlite
                currentConnection.commit();
                LOGGER.info("Table " + tableDef.getFullyQualifiedName() + " deleted");
            } catch (SQLException e) {
                System.err.println(deleteStatement);
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Non Sql Data Source - Not implemented");
        }

    }

    public static void dropIfExists(TableDef... tableDefs) {

        Tables.dropIfExists(Arrays.asList(tableDefs));

    }

    public static void dropIfExists(TableDef tableDef, SchemaDef schemaDef) {


        if (exists(tableDef, schemaDef)) {
            drop(tableDef, schemaDef);
        }


    }

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

    public static List<String> getNames(List<TableDef> tables) {

        return tables.stream().map(s -> s.getName()).collect(Collectors.toList());

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

