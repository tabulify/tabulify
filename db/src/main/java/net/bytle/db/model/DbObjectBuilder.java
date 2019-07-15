package net.bytle.db.model;

import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.engine.Tables;
import net.bytle.cli.Log;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


/**
 * The data structure builder and parser
 */
public class DbObjectBuilder {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    private final Database database;


    public DbObjectBuilder(Database database) {
        this.database = database;
    }


    /**
     * A {@link TableDef} can be created manually. This operation will add the data type information to the {@link ColumnDef} from the {@link TableDef}.
     * This is needed before loading data in order to transform the data type
     *
     * @param tableDef
     * @return a new tableDef with the data type information
     * @deprecated this is done within the builder now because of the hierarchy model of the schema that gave us access to the database
     */
    public static TableDef cleanTableDef(TableDef tableDef) {

//        TableDef cleanTableDef = new TableDef.Builder(tableDef.getDatabase())
//                .name(tableDef.getName())
//                .createProperties(tableDef.getCreateProperties())
//                .build();
//
//        // Cleaning
//        // columnMetadata have only the column name as mandatory
//        // We need the data type (from JavaClass, of Default)
//        List<ColumnDef> columnDefs = tableDef.getColumnDefs();
//        List<ColumnDef> cleanColumnDefs = new ArrayList<>();
//
//        for (int i = 0; i < columnDefs.size(); i++) {
//            ColumnDef columnDef = columnDefs.get(i);
//            DataType dataType = getDataTypeOf(columnDef);
//            ColumnDef cleanColumnDef = new ColumnDef.Builder(cleanTableDef)
//                    .columnName(columnDef.getColumnName())
//                    .typeCode(dataType.getTypeCode()) // That's enough
//                    .columnTypeName(dataType.getTypeName())
//                    .clazz(columnDef.getColumnTypeJava())
//                    .precision(columnDef.getPrecision())
//                    .scale(columnDef.getScale())
//                    .build();
//            cleanColumnDefs.add(cleanColumnDef);
//        }
//        cleanTableDef.setColumns(cleanColumnDefs);

        return tableDef;
    }

    private static void buildPrimaryKey(TableDef tableDef) throws SQLException {


        // Bug in SQLite Driver - Hack
        // that doesn't return the good primary ley
        Boolean done = tableDef.getDatabase().getSqlDatabase().addPrimaryKey(tableDef);
        if (done == null || done) {
            return;
        }


        // Primary Key building
        ResultSet pkResultSet = tableDef.getDatabase().getCurrentConnection().getMetaData().getPrimaryKeys(null, tableDef.getSchema().getName(), tableDef.getName());
        // Collect all the data because we don't known if they will be in order
        // and because in a recursive call, the result set may be closed
        Map<String, Map<Integer, String>> pkMap = new HashMap<>();
        while (pkResultSet.next()) {
            Integer keySeq = pkResultSet.getInt("KEY_SEQ");
            String pkName = pkResultSet.getString("PK_NAME");
            String columnName = pkResultSet.getString("COLUMN_NAME");
            Map<Integer, String> pkCols = pkMap.get(pkName);
            if (pkCols == null) {
                pkCols = new HashMap<>();
                pkMap.put(pkName, pkCols);
            }
            pkCols.put(keySeq, columnName);
        }
        pkResultSet.close();

        // Build the primary key (only one by table)
        for (String pkName : pkMap.keySet()) {

            PrimaryKeyDef primaryKeyDef = new PrimaryKeyDef(tableDef)
                    .name(pkName);
            tableDef.addPrimaryKey(primaryKeyDef);

            Map<Integer, String> colMap = pkMap.get(pkName);
            List<Integer> integers = new ArrayList<>(colMap.keySet());
            Collections.sort(integers);
            for (Integer keySeq : integers) {
                String columnName = colMap.get(keySeq);
                ColumnDef columnDef = tableDef.getColumnOf(columnName);
                primaryKeyDef.addColumn(columnDef);
            }


        }

    }

    /**
     * The fully qualified name is the name with its schema
     * that can be used in SQL Statement
     * TODO: Move that in a SQL manager
     *
     * @param tableName
     * @param schemaName
     * @return
     */
    public String getFullyQualifiedName(String tableName, String schemaName) {
        if (schemaName == null) {
            schemaName = database.getCurrentSchema().getName();
        }
        final String statementTableName = database.getStatementTableName(tableName);

        // No schema functionality (Sqlite has a schema on database level)
        if (schemaName == null) {
            return statementTableName;
        } else {
            /**
             * Only for catalog
             * {@link DatabaseMetaData#getCatalogSeparator()}
             */
            return schemaName + "." + statementTableName;
        }


    }

    /**
     * Build a table from a database
     * if no table is found, return null
     * The table get a schema but the whole schema will not be build
     *
     * @param tableName
     * @return null if no table is found
     */
    public TableDef getTableDef(String tableName, String schemaName) {

        // Table already build and in the cache ?
        String id = getId(tableName, schemaName);
        SchemaDef schemaDef = database.getSchema(schemaName);
        TableDef tableDef = new TableDef(database, tableName).setSchema(schemaDef);


        if (database.getCurrentConnection() == null) {

            return tableDef;

        } else {

            try {

                LOGGER.fine("Building the table (or view)" + id);
                String[] types = {"TABLE"};
                String schemaPattern = schemaName;
                if (schemaPattern == null) {
                    schemaPattern = database.getCurrentConnection().getSchema();
                }

                ResultSet tableResultSet = database.getCurrentConnection().getMetaData().getTables(null, schemaPattern, tableName, types);
                boolean tableExist = tableResultSet.next(); // For TYPE_FORWARD_ONLY
                if (!tableExist) {

                    tableResultSet.close();
                    return tableDef;

                } else {

                    tableDef.JdbcType(tableResultSet.getString("TABLE_TYPE"));
                    tableResultSet.close();

                    // Columns building
                    buildTableColumns(tableDef);
                    // Pk Building
                    buildPrimaryKey(tableDef);
                    // Foreign Key building
                    buildForeignKey(tableDef);
                    // Unique Key
                    buildUniqueKey(tableDef);

                    // Return the table
                    return tableDef;

                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }

    }


    /**
     * Id is build at the beginning from the name
     * and not from the object
     * <p>
     * It's used in the cache
     *
     * @param tableName
     * @param schemaName
     * @return
     */
    String getId(String tableName, String schemaName) {

        return database.getDatabaseName() + "." + getFullyQualifiedName(tableName, schemaName);

    }


    private static void buildTableColumns(TableDef tableDef) throws SQLException {

        Boolean added = tableDef.getDatabase().getSqlDatabase().addColumns(tableDef);
        if (!added) {

            ResultSet columnResultSet = tableDef.getDatabase().getCurrentConnection().getMetaData().getColumns(null, tableDef.getSchema().getName(), tableDef.getName(), null);

            while (columnResultSet.next()) {

                String isGeneratedColumn = "";
                try {
                    isGeneratedColumn = columnResultSet.getString("IS_GENERATEDCOLUMN");
                } catch (SQLException e) {
                    // Not always supported
                }

                String column_name = columnResultSet.getString("COLUMN_NAME");

                String is_autoincrement = null;
                // Not implemented by the sqliteDriver
                try {
                    is_autoincrement = columnResultSet.getString("IS_AUTOINCREMENT");
                } catch (SQLException e) {
                    LOGGER.fine("The IS_AUTOINCREMENT column seems not to be implemented. Message: " + e.getMessage());
                }

                int column_size = columnResultSet.getInt("COLUMN_SIZE");


                tableDef.getColumnOf(column_name)
                        .typeCode(columnResultSet.getInt("DATA_TYPE"))
                        .precision(column_size)
                        .scale(columnResultSet.getInt("DECIMAL_DIGITS"))
                        .isAutoincrement(is_autoincrement)
                        .isGeneratedColumn(isGeneratedColumn)
                        .setNullable(columnResultSet.getInt("NULLABLE"));

            }
            columnResultSet.close();

        }

    }

    /**
     * @param tableName
     * @param relationDef
     * @return a table def from a result set metadata
     * @throws SQLException
     */
    public TableDef getTableDef(RelationDef relationDef, String tableName, String schemaName) {

        // Table
        TableDef tableDef = database.getTable(tableName, schemaName);
        Tables.addColumns(tableDef, relationDef);

        return tableDef;
    }

    /**
     * Build Foreign Key based on
     * {@link java.sql.DatabaseMetaData#getImportedKeys(String, String, String)}
     * <p>
     * See also the counter part:
     * * Same schema
     * {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     * * Cross Schmea ?
     * {@link java.sql.DatabaseMetaData#getCrossReference(String, String, String, String, String, String)}
     *
     * @param tableDef
     */
    private void buildForeignKey(TableDef tableDef) {

        // SQLite Driver doesn't return a empty string as key name
        // for all foreigns key
        Boolean done = tableDef.getDatabase().getSqlDatabase().addForeignKey(tableDef);
        if (done == null || done) {
            return;
        }

        Database database = tableDef.getDatabase();
        // Collect all fk data
        Map<String, ForeignKeyDef> fkMap = new HashMap<>();

        // The column names of the fkresult set
        String col_fk_name = "FK_NAME";
        String col_fkcolumn_name = "FKCOLUMN_NAME";
        String col_fktable_schem = "FKTABLE_SCHEM";
        String col_fktable_name = "FKTABLE_NAME";
        //  --- Pk referenced
        String col_pkcolumn_name = "PKCOLUMN_NAME";
        String col_pktable_name = "PKTABLE_NAME";
        String col_pktable_schem = "PKTABLE_SCHEM";
        String col_pk_name = "PK_NAME";
        //  ---- Column seq for FK and PK
        String col_key_seq = "KEY_SEQ";


        List<String> resultSetColumnNames = new ArrayList<>();
        resultSetColumnNames.add(col_fk_name);
        resultSetColumnNames.add(col_fkcolumn_name);
        resultSetColumnNames.add(col_fktable_schem);
        resultSetColumnNames.add(col_fktable_name);
        resultSetColumnNames.add(col_pkcolumn_name);
        resultSetColumnNames.add(col_pktable_name);
        resultSetColumnNames.add(col_pktable_schem);
        resultSetColumnNames.add(col_pk_name);
        resultSetColumnNames.add(col_key_seq);

        // Collect the data before processing it
        // because of the recursion the data need first to be collected
        // processing the data and calling recursively the creation of an other table
        // with foreign key result in a "result set is closed" exception within the Ms Sql Driveer
        Map<String, List<Map<String, String>>> fkData = new HashMap<>();

        try (
                // ImportedKey = the primary keys imported by a table
                ResultSet fkResultSet = database.getCurrentConnection().getMetaData().getImportedKeys(null, tableDef.getSchema().getName(), tableDef.getName());
        ) {

            while (fkResultSet.next()) {

                // Put the properties for the fk
                String foreignKeyId = fkResultSet.getString(col_fk_name);
                if (foreignKeyId == null) {
                    // The foreign key name may be null
                    // It means that there is only one column
                    // The id is then the column name
                    foreignKeyId = fkResultSet.getString(col_fkcolumn_name);
                }

                Map<String, String> fkProperties = new HashMap<>();
                for (String colName : resultSetColumnNames) {
                    fkProperties.put(colName, fkResultSet.getString(colName));
                }

                // Two columns in the fk or more
                List<Map<String, String>> colProp = fkData.get(foreignKeyId);
                if (colProp == null) {
                    colProp = new ArrayList<>();
                    fkData.put(foreignKeyId, colProp);
                }
                colProp.add(fkProperties);

            }

        } catch (Exception e) {
            String s = "Error when building Foreign Key (ie imported keys) for the table " + tableDef.getFullyQualifiedName();
            LOGGER.severe(s);
            System.err.println(s);
            throw new RuntimeException(e);
        }

        for (Map.Entry<String, List<Map<String, String>>> entry : fkData.entrySet()) {

            String foreignKeyId = entry.getKey();
            List<Map<String, String>> cols = entry.getValue();

            for (Map<String, String> colProp : cols) {
                // Process the data

                // Get the foreign key (only needed if there is two columns
                ForeignKeyDef foreignKeyDef = fkMap.get(foreignKeyId);
                if (foreignKeyDef == null) {
                    foreignKeyDef = new ForeignKeyDef(tableDef)
                            .setName(foreignKeyId);
                    tableDef.addForeignKey(foreignKeyDef);
                    fkMap.put(foreignKeyId, foreignKeyDef);
                }

                // Add the foreign primary key
                if (foreignKeyDef.getForeignPrimaryKey() == null) {

                    TableDef foreignTable = getTableDef(colProp.get(col_pktable_name), colProp.get(col_pktable_schem));
                    if (tableDef.equals(foreignTable)) {
                        throw new RuntimeException("The foreign key " + foreignKeyDef.getName() + " on the table (" + foreignKeyDef.getTableDef().getFullyQualifiedName() + ") references itself and it's not supported.");
                    }
                    foreignKeyDef.setForeignPrimaryKey(foreignTable.getPrimaryKey());

                }

                // Add the inner column that is part of the foreign key definition
                String columnName = colProp.get(col_fkcolumn_name);
                ColumnDef columnByName = tableDef.getColumnOf(columnName);
                foreignKeyDef.addColumn(columnByName, Integer.parseInt(colProp.get(col_key_seq)));

            }

        }

    }

    /**
     * This function must be called after the function {@link #buildPrimaryKey(TableDef)}
     * because the getIndex function of JDBC returns also the unique index of the primary
     * key. We need then the primary key information in order to exclude it from the building
     *
     * @param tableDef
     */
    private void buildUniqueKey(TableDef tableDef) {

        // Collect all data first because we need all columns that make a unique key before
        // building the object
        Map<String, Map<Integer, String>> indexData = new HashMap<>();
        final String ordinal_position_alias = "ORDINAL_POSITION";
        final String column_name_alias = "COLUMN_NAME";
        try (
            // Oracle need to have the approximate argument to true, otherwise we get a ORA-01031: insufficient privileges
            ResultSet indexResultSet = tableDef.getDatabase().getCurrentConnection().getMetaData().getIndexInfo(null, tableDef.getSchema().getName(), tableDef.getName(), true, true);
        ) {
            while (indexResultSet.next()) {

                String index_name = indexResultSet.getString("INDEX_NAME");

                // With SQL Server we may get a line with only null values
                if (index_name == null) {
                    continue;
                }

                Map<Integer, String> indexProperties = indexData.get(index_name);
                if (indexProperties == null) {
                    indexProperties = new HashMap<>();
                    indexData.put(index_name, indexProperties);
                }
                indexProperties.put(indexResultSet.getInt(ordinal_position_alias), indexResultSet.getString(column_name_alias));

            }

        } catch (SQLException e) {
            String s = "Error when building the unique key (ie indexinfo) of the table (" + tableDef.getFullyQualifiedName() + ")";
            LOGGER.severe(s);
            System.err.println(s);
            throw new RuntimeException(e);
        }

        // Process the data
        for (String indexName : indexData.keySet()) {
            Map<Integer, String> indexProperties = indexData.get(indexName);

            // Sort the column by order
            List<Integer> positions = new ArrayList<>(indexProperties.keySet());
            List<ColumnDef> columnDefs = new ArrayList<>();
            Collections.sort(positions);
            for (Integer pos : positions) {
                ColumnDef columnDef = tableDef.getColumnOf(indexProperties.get(pos));
                columnDefs.add(columnDef);
            }

            // We don't want the unique index of the primary key
            PrimaryKeyDef primaryKeyDef = tableDef.getPrimaryKey();
            if (primaryKeyDef.getColumns().equals(columnDefs)) {
                continue;
            }

            // Construct the unique key
            UniqueKeyDef uniqueKey = tableDef.getOrCreateUniqueKey(columnDefs.toArray(new ColumnDef[columnDefs.size()]));
            uniqueKey.name(indexName);

        }


    }

    /**
     * When this method is called, it's because the schema was not yet built
     *
     * @param schemaDef
     * @return
     */
    public List<TableDef> buildSchema(SchemaDef schemaDef) {

        List<TableDef> tableDefs = new ArrayList<>();
        if (schemaDef.getDatabase().getCurrentConnection() != null) {

            String[] types = {"TABLE"};

            try {
                ResultSet tableResultSet = schemaDef.getDatabase().getCurrentConnection().getMetaData().getTables(null, schemaDef.getName(), null, types);
                List<String> tableNames = new ArrayList<>();
                while (tableResultSet.next()) {

                    tableNames.add(tableResultSet.getString("TABLE_NAME"));

                }
                tableResultSet.close();

                // getTableDef make also used of the getTables
                // and it seems that there is a cache because the result was closed
                // We do then a second loop
                for (String tableName : tableNames) {
                    TableDef tableDef = getTableDef(tableName, schemaDef.getName());
                    tableDefs.add(tableDef);
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }
        return tableDefs;

    }


    /**
     * @param database
     * @return the list of schema for this database
     */
    public List<SchemaDef> buildSchemas(Database database) {

        List<SchemaDef> schemaDefList = new ArrayList<>();
        if (database.getCurrentConnection() != null) {

            try {

                // Always NULL
                // because otherwise it's not a pattern but
                // it must match the schema name
                // We build all schemas then
                final String schemaPattern = null;
                ResultSet schemaResultSet = database.getCurrentConnection().getMetaData().getSchemas(null, schemaPattern);

                // Sqlite Driver return a NULL resultSet
                // because SQLite does not support schema ?
                if (schemaResultSet != null) {
                    while (schemaResultSet.next()) {

                        SchemaDef schema = database.getSchema(schemaResultSet.getString("TABLE_SCHEM"));
                        schemaDefList.add(schema);

                    }
                    schemaResultSet.close();
                }

            } catch (java.sql.SQLFeatureNotSupportedException e) {

                LOGGER.warning("Schemas are not supported on this database.");

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }
        return schemaDefList;
    }


    /**
     * @param query
     * @return a query def
     */
    public QueryDef getQueryDef(String query) {
        return new QueryDef(database, query);
    }

}
