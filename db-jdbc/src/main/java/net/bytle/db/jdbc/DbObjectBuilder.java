package net.bytle.db.jdbc;

import net.bytle.cli.Log;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.Database;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.model.*;
import net.bytle.db.spi.DataPath;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * The data structure builder and parser
 *
 * Within a pattern String:
 *   * "%" means match any substring of 0 or more characters
 *   * "_" means match any one character.
 * If a search pattern argument is set to null, that argument's criterion will be dropped from the search.
 */
public class DbObjectBuilder {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;


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
//            ColumnDef columnDef = columnDefs.of(i);
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
        final JdbcDataPath dataPath = (JdbcDataPath) tableDef.getDataPath();
        Boolean done = dataPath.getDataSystem().getSqlDatabase().addPrimaryKey(tableDef);
        if (done == null || done) {
            return;
        }

        final String column_name = "COLUMN_NAME";
        final String pk_name = "PK_NAME";
        final String key_seq = "KEY_SEQ";
        List<String> pkProp = new ArrayList<>();
        pkProp.add(column_name);
        pkProp.add(pk_name);
        pkProp.add(key_seq);

        // Primary Key building
        ResultSet pkResultSet = dataPath.getDataSystem().getCurrentConnection().getMetaData().getPrimaryKeys(dataPath.getCatalog(), dataPath.getSchema().getName(), dataPath.getName());
        // Collect all the data because we don't known if they will be in order
        // and because in a recursive call, the result set may be closed
        List<Map<String, String>> pkColumns = new ArrayList<>();
        String pkName = "";
        while (pkResultSet.next()) {
            Map<String, String> pkProps = new HashMap<>();
            pkColumns.add(pkProps);
            for (String prop : pkProp) {
                pkProps.put(prop, pkResultSet.getString(prop));
            }
            pkName = pkResultSet.getString(pk_name);
        }
        pkResultSet.close();

        List<String> columns = pkColumns
                .stream()
                .sorted(Comparator.comparing(o -> Integer.valueOf(o.get(key_seq))))
                .map(s -> s.get(pk_name))
                .collect(Collectors.toList());

        tableDef.primaryKeyOf(columns.toArray(new String[0]))
                .setName(pkName);


    }


    /**
     * Build a table from a database
     * if no table is found, return null
     * The table of a schema but the whole schema will not be build
     *
     * @param tableDef
     * @return null if no table is found
     */
    public static TableDef getTableDef(TableDef tableDef) {


        try {
            JdbcDataPath jdbcDataPath = (JdbcDataPath) tableDef.getDataPath();
            LOGGER.fine("Building the table structure for the data path (" + jdbcDataPath + ")");

            String[] types = {"TABLE"};

            final JdbcDataPath schemaPath = jdbcDataPath.getSchema();
            String schema = null;
            if (schemaPath!=null) {
                schema = schemaPath.getName();
            }
            String catalog = jdbcDataPath.getCatalog();
            String tableName = jdbcDataPath.getName();

            ResultSet tableResultSet = jdbcDataPath.getDataSystem().getCurrentConnection().getMetaData().getTables(catalog, schema, tableName, types);
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


    private static void buildTableColumns(TableDef tableDef) throws SQLException {

        final JdbcDataPath dataPath = (JdbcDataPath) tableDef.getDataPath();
        Boolean added = dataPath.getDataSystem().getSqlDatabase().addColumns(tableDef);
        if (!added) {

            ResultSet columnResultSet = dataPath.getDataSystem().getCurrentConnection().getMetaData().getColumns(dataPath.getCatalog(), dataPath.getSchema().getName(), dataPath.getName(), null);

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


                final int sqlTypeCode = columnResultSet.getInt("DATA_TYPE");

                DataTypeJdbc dataType = DataTypesJdbc.of(sqlTypeCode);
                tableDef.getColumnOf(column_name, dataType.getClass())
                        .typeCode(sqlTypeCode)
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
    private static void buildForeignKey(TableDef tableDef) {

        // SQLite Driver doesn't return a empty string as key name
        // for all foreigns key
        final JdbcDataPath dataPath = (JdbcDataPath) tableDef.getDataPath();
        Boolean done = dataPath.getDataSystem().getSqlDatabase().addForeignKey(tableDef);
        if (done == null || done) {
            return;
        }

        Database database = dataPath.getDataSystem().getDatabase();
        // Collect all fk data
        Map<String, ForeignKeyDef> fkMap = new HashMap<>();

        // The column names of the fkresult set
        String col_fk_name = "FK_NAME";
        String col_fkcolumn_name = "FKCOLUMN_NAME";
        String col_fktable_schem = "FKTABLE_SCHEM";
        String col_fktable_cat = "FKTABLE_CAT";
        String col_fktable_name = "FKTABLE_NAME";
        //  --- Pk referenced
        String col_pkcolumn_name = "PKCOLUMN_NAME";
        String col_pktable_name = "PKTABLE_NAME";
        String col_pktable_schem = "PKTABLE_SCHEM";
        String col_pktable_cat = "PKTABLE_CAT";
        String col_pk_name = "PK_NAME";
        //  ---- Column seq for FK and PK
        String col_key_seq = "KEY_SEQ";


        List<String> resultSetColumnNames = new ArrayList<>();
        resultSetColumnNames.add(col_fk_name);
        resultSetColumnNames.add(col_fkcolumn_name);
        resultSetColumnNames.add(col_fktable_schem);
        resultSetColumnNames.add(col_fktable_cat);
        resultSetColumnNames.add(col_fktable_name);
        resultSetColumnNames.add(col_pkcolumn_name);
        resultSetColumnNames.add(col_pktable_name);
        resultSetColumnNames.add(col_pktable_schem);
        resultSetColumnNames.add(col_pktable_cat);
        resultSetColumnNames.add(col_pk_name);
        resultSetColumnNames.add(col_key_seq);

        // Collect the data before processing it
        // because of the recursion the data need first to be collected
        // processing the data and calling recursively the creation of an other table
        // with foreign key result in a "result set is closed" exception within the Ms Sql Driver

        // Just to hold the data a list of all fk columns values
        List<Map<String, String>> fkDatas = new ArrayList<>();

        try (
                // ImportedKey = the primary keys imported by a table
                ResultSet fkResultSet = dataPath.getDataSystem().getCurrentConnection().getMetaData().getImportedKeys(dataPath.getCatalog(), dataPath.getSchema().getName(), dataPath.getName());
        ) {

            while (fkResultSet.next()) {

                // The foreign key name may be null
                Map<String, String> fkProperties = resultSetColumnNames
                        .stream()
                        .collect(Collectors.toMap(s -> s, s -> {
                            try {
                                return fkResultSet.getString(s);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }));
                fkDatas.add(fkProperties);

            }

        } catch (Exception e) {
            String s = "Error when building Foreign Key (ie imported keys) for the table " + dataPath;
            LOGGER.severe(s);
            System.err.println(s);
            throw new RuntimeException(e);
        }

        // How much foreign key (ie how much foreign key tables)
        List<JdbcDataPath> foreignTableNames = fkDatas.stream()
                .map(s -> JdbcDataPath.of(dataPath.getDataSystem(), s.get(col_pktable_cat), s.get(col_pktable_schem), s.get(col_pktable_name)))
                .collect(Collectors.toList());


        for (JdbcDataPath foreignTable : foreignTableNames) {
            Map<Integer, String> cols = new HashMap<>();
            String fk_name = "";
            for (Map<String, String> fkData : fkDatas) {
                if (fkData.get(col_pktable_name).equals(foreignTable)) {
                    cols.put(Integer.valueOf(fkData.get(col_key_seq)), fkData.get(col_fkcolumn_name));
                    fk_name = fkData.get(col_fk_name);
                }
            }
            List<String> columns = cols.keySet().stream()
                    .sorted()
                    .map(s -> cols.get(s))
                    .collect(Collectors.toList());

            final PrimaryKeyDef primaryKey = foreignTable.getDataDef().getPrimaryKey();
            if (primaryKey == null) {
                throw new RuntimeException("The foreign table (" + foreignTable + ") has no primary key");
            }
            tableDef
                    .foreignKeyOf(primaryKey, columns)
                    .setName(fk_name);
        }

    }

    /**
     * This function must be called after the function {@link #buildPrimaryKey(TableDef)}
     * because the getIndex function of JDBC returns also the unique index of the primary
     * key. We need then the primary key information in order to exclude it from the building
     *
     * @param metaDataDef
     */
    private static void buildUniqueKey(TableDef metaDataDef) {

        // Collect all data first because we need all columns that make a unique key before
        // building the object
        Map<String, Map<Integer, String>> indexData = new HashMap<>();
        final String ordinal_position_alias = "ORDINAL_POSITION";
        final String column_name_alias = "COLUMN_NAME";
        final JdbcDataPath dataPath = (JdbcDataPath) metaDataDef.getDataPath();
        final String schema = dataPath.getSchema() != null ? dataPath.getSchema().getName() : null;
        try (
                // Oracle need to have the approximate argument to true, otherwise we of a ORA-01031: insufficient privileges
                ResultSet indexResultSet = dataPath.getDataSystem().getCurrentConnection().getMetaData().getIndexInfo(dataPath.getCatalog(), schema, dataPath.getName(), true, true);
        ) {
            while (indexResultSet.next()) {

                String index_name = indexResultSet.getString("INDEX_NAME");

                // With SQL Server we may of a line with only null values
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
            String s = "Error when building the unique key (ie indexinfo) of the table (" + dataPath + ")";
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
                ColumnDef columnDef = metaDataDef.getColumnDef(indexProperties.get(pos));
                columnDefs.add(columnDef);
            }

            // We don't want the unique index of the primary key
            PrimaryKeyDef primaryKeyDef = metaDataDef.getPrimaryKey();
            if (primaryKeyDef.getColumns().equals(columnDefs)) {
                continue;
            }

            // Construct the unique key
            UniqueKeyDef uniqueKey = metaDataDef.getOrCreateUniqueKey(columnDefs.toArray(new ColumnDef[columnDefs.size()]));
            uniqueKey.name(indexName);

        }


    }


    public static List<DataPath> getChildrenDataPath(JdbcDataPath jdbcDataPath) {

        List<DataPath> jdbcDataPaths = new ArrayList<>();
        try {

            String[] types = {"TABLE"};
            String schema = jdbcDataPath.getSchema() != null ? jdbcDataPath.getSchema().getName():null;
            String catalog = jdbcDataPath.getCatalog();
            String tableName = null;

            ResultSet tableResultSet = jdbcDataPath.getDataSystem().getCurrentConnection().getMetaData().getTables(catalog, schema, tableName, types);
            while (tableResultSet.next()) {
                JdbcDataPath childDataPath = jdbcDataPath.getDataSystem().getDataPath(catalog, schema, tableResultSet.getString("TABLE_NAME"));
                jdbcDataPaths.add(childDataPath);
            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return jdbcDataPaths;

    }

}
