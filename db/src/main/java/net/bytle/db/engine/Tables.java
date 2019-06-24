package net.bytle.db.engine;


import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.model.*;
import net.bytle.db.stream.*;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class Tables {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_ENGINE;


    public static Integer getMaxIntegerValue(ColumnDef columnDef) {

        String columnStatement = columnDef.getColumnName();
        String statementString = "select max(" + columnStatement + ") from " + columnDef.getRelationDef().getFullyQualifiedName();

        try {

            Connection currentConnection = columnDef.getRelationDef().getDatabase().getCurrentConnection();
            Statement statement = currentConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(statementString);
            Integer returnValue = null;
            if (resultSet.next()) {
                returnValue = resultSet.getInt(1);
            }
            statement.close();
            return returnValue;

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }


    }

    /**
     * Return the number of rows
     *
     * @param tableDef
     * @return
     */
    public static Integer getSize(TableDef tableDef) {

        String statementString = "select count(1) from " + tableDef.getFullyQualifiedName();
        try {
            Connection currentConnection = tableDef.getDatabase().getCurrentConnection();
            if (currentConnection == null) {
                throw new RuntimeException("The database " + tableDef.getDatabase() + " seems to have no connections (Is this a relational database supporting JDBC ?)");
            }
            Statement statement = currentConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(statementString);
            Integer returnValue = null;
            if (resultSet.next()) {
                returnValue = resultSet.getInt(1);
            }
            statement.close();
            return returnValue;

        } catch (SQLException e) {
            System.err.println(statementString);
            throw new RuntimeException(e);
        }

    }


    public static Date getMinDateValue(ColumnDef columnDef) {
        String statementString = "select min(" + columnDef.getColumnName() + ") from " + columnDef.getRelationDef().getFullyQualifiedName();
        try {
            Connection currentConnection = columnDef.getRelationDef().getDatabase().getCurrentConnection();
            Statement statement = currentConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(statementString);
            Date returnValue = null;
            if (resultSet.next()) {
                returnValue = resultSet.getDate(1);
            }
            statement.close();
            return returnValue;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static void printRowSizeBySchema(SchemaDef schemaDef) {

        for (TableDef tableDef : schemaDef.getTables()) {
            System.out.println(tableDef.getSchema().getName() + "\t" + tableDef.getName() + "\t" + getSize(tableDef));
        }


    }

    /**
     * @param tableDef
     * @param database
     * @return if the table exist in the underlying database (actually the letter case is important)
     * <p>
     * The structure of the table is not checked
     *
     * TODO: the table may exist in a non-relational database
     */
    public synchronized static boolean exists(TableDef tableDef, Database database) {

        // Tables exists ?
        final Connection currentConnection = database.getCurrentConnection();
        if (currentConnection == null) {
            throw new RuntimeException("The database (" + database + ") has no connection (no URL ?). We then cannot check the existence of a table.");
        }
        Boolean tableExist;
        try {
            String[] types = {"TABLE"};
            ResultSet tableResultSet;
            String schemaName = null;
            SchemaDef schema = database.getCurrentSchema();
            if (schema != null) {
                schemaName = schema.getName();
            }

            tableResultSet = currentConnection.getMetaData().getTables(null, schemaName, tableDef.getName(), types);
            tableExist = tableResultSet.next(); // For TYPE_FORWARD_ONLY
            tableResultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return tableExist;

    }

    /**
     * @param tableDef
     * @return if the table exist in the underlying database (actually the letter case is important)
     * <p>
     * The structure of the table is not checked
     */
    public static boolean exists(TableDef tableDef) {
        return exists(tableDef, tableDef.getDatabase());
    }

    public static String getMaxStringValue(ColumnDef columnDef) {

        String columnStatement = columnDef.getColumnName();

        String statementString = "select max(" + columnStatement + ") from " + columnDef.getRelationDef().getFullyQualifiedName();

        try {

            Connection currentConnection = columnDef.getRelationDef().getDatabase().getCurrentConnection();
            Statement statement = currentConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(statementString);
            String returnValue = null;
            if (resultSet.next()) {
                returnValue = resultSet.getString(1);
            }
            statement.close();
            return returnValue;

        } catch (SQLException e) {

            throw new RuntimeException(e);

        }

    }

    /**
     * Create all tables if they don't exist
     * taking into account the foreign key constraints
     * <p>
     * To throw an exception if the table already exist, see @{link {@link #create(List)}}
     *
     * @param tables
     */
    public static void createIfNotExist(List<TableDef> tables) {

        Dag dag = Dag.get(tables);
        tables = dag.getCreateOrderedTables();
        for (TableDef tableDef : tables) {
            if (!Tables.exists(tableDef)) {
                Tables.create(tableDef);
            } else {
                throw new RuntimeException("Table (" + tableDef.getFullyQualifiedName() + ") already exist");
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

        // Return it back (The schema and the name may be not the same, we return a new object)
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
    public static void dropIfExist(List<TableDef> tableDefs) {

        for (TableDef tableDef : Dag.get(tableDefs).getDropOrderedTables()) {

            if (exists(tableDef)) {
                drop(tableDef);
            } else {
                Tables.dropCache(tableDef);
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

    /**
     * Drop one or more tables
     * <p>
     * If the table is a foreign table, the child constraint will
     * prevent the table to be dropped if the child table is not given.
     * <p>
     *
     *
     * @param tableDefs - The tables to drop
     */
    public static void drop(List<TableDef> tableDefs) {

        Dag dag = Dag.get(tableDefs);

        for (TableDef tableDef : dag.getDropOrderedTables()) {

            Tables.dropCache(tableDef);

            Database database = tableDef.getDatabase();

            if (database.getUrl() != null) {
                String dropTableStatement = "drop table " + tableDef.getFullyQualifiedName();
                try {

                    Connection currentConnection = database.getCurrentConnection();
                    Statement statement = currentConnection.createStatement();
                    statement.execute(dropTableStatement);
                    statement.close();


                    LOGGER.info("Table " + tableDef.getFullyQualifiedName() + " dropped");

                } catch (SQLException e) {
                    System.err.println(dropTableStatement);
                    throw new RuntimeException(e);
                }
            } else {

                StorageManager.drop(tableDef);

            }

        }
    }


    public static void dropCache(TableDef tableDef) {

        // Cache
        // Schema Cache Drop
        tableDef.getSchema().dropTableFromCache(tableDef);
        // Database Table Cache
        tableDef.getDatabase().getObjectBuilder().dropTableFromCache(tableDef);

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
            try {

                Connection currentConnection = database.getCurrentConnection();
                Statement statement = currentConnection.createStatement();
                statement.execute(deleteStatement);
                statement.close();

                LOGGER.info("Table " + tableDef.getFullyQualifiedName() + " deleted");

            } catch (SQLException e) {
                System.err.println(deleteStatement);
                throw new RuntimeException(e);
            }
        } else {
            StorageManager.delete(tableDef);
        }

    }

    public static void dropIfExist(TableDef... tableDefs) {

        Tables.dropIfExist(Arrays.asList(tableDefs));

    }

    public static void dropForeignKey(ForeignKeyDef foreignKeyDef) {

        final TableDef tableDef = foreignKeyDef.getTableDef();
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

    /**
     * Empty all caches
     *
     * @param database
     */
    public static void emptyAllCache(Database database) {

        // Cache
        // Schema Cache Drop
        database.getCurrentSchema().dropCache();
        // Database Table Cache
        database.getObjectBuilder().dropCache();

    }

    /**
     * @param tableName - the name of the table
     * @return a tableDef in the default bytle database (namespace)
     * <p>
     * The database was get with the function @{link Databases.get}
     */
    public static TableDef get(String tableName) {
        Database database = Databases.get();
        return database.getTable(tableName);
    }

    /**
     * The table will be created in the database of the tableDef
     * If you want to create the table in another database
     * use the function @{link {@link #create(TableDef, Database)}}
     *
     * @param tableDef - a tableDef definition
     */
    public static void create(TableDef tableDef) {
        create(tableDef, tableDef.getDatabase());
    }


    public static InsertStream getTableInsertStream(TableDef tableDef) {

        if (tableDef.getDatabase().getUrl() == null) {
            return MemoryInsertStream.get(tableDef);
        } else {
            return SqlInsertStream.get(tableDef);
        }

    }

    public static MemorySelectStream getTableOutputStream(TableDef tableDef) {
        return MemorySelectStream.get(tableDef);
    }

    /**
     * Print the data of a table
     *
     * @param tableDef
     */
    public static void print(TableDef tableDef) {
        MemorySelectStream tableOutputStream = Tables.getTableOutputStream(tableDef);
        Streams.print(tableOutputStream);
        tableOutputStream.close();
    }


    /**
     * Add the columns to the targetDef from the sourceDef
     *
     * @param targetDef
     * @param sourceDef
     */
    public static void addColumns(TableDef targetDef, RelationDef sourceDef) {

        // Add the columns
        int columnCount = sourceDef.getColumnDefs().size();
        for (int i = 0; i < columnCount; i++) {
            ColumnDef columnDef = sourceDef.getColumnDef(i);
            targetDef.getColumnOf(columnDef.getColumnName())
                    .typeCode(columnDef.getDataType().getTypeCode())
                    .precision(columnDef.getPrecision())
                    .scale(columnDef.getScale());
        }

    }


    public static void printColumns(TableDef tableDef) {
        TableDef tableStructure = Tables.get("structure")
                .addColumn("#")
                .addColumn("Colum Name")
                .addColumn("Data Type")
                .addColumn("Key")
                .addColumn("Not Null")
                .addColumn("Default")
                .addColumn("Auto Increment")
                .addColumn("Description");

        InsertStream insertStream = Tables.getTableInsertStream(tableStructure);
        int i = 0;
        for (ColumnDef columnDef : tableDef.getColumnDefs()) {
            i++;
            insertStream.insert(
                    i,
                    columnDef.getColumnName(),
                    columnDef.getDataType().getTypeName(),
                    (tableDef.getPrimaryKey().getColumns().contains(columnDef) ? "x" : ""),
                    (columnDef.getNullable() == 0 ? "x" : ""),
                    columnDef.getDefault(),
                    columnDef.getIsAutoincrement(),
                    columnDef.getDescription()

            );
        }
        insertStream.close();

        Tables.print(tableStructure);
        Tables.drop(tableStructure);
    }
}
