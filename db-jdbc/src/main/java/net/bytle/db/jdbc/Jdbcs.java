package net.bytle.db.jdbc;

import net.bytle.db.model.ForeignKeyDef;
import net.bytle.regexp.Globs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Static method
 *
 * Note for later:
 * String regexpPattern = Globs.toRegexPattern(globPattern);
 */
public class Jdbcs {

    /**
     * When this method is called, it's because the schema was not yet built
     *
     * @param jdbcDataPath - representing a schema path (ie a schema)
     * @return
     */
    public static List<JdbcDataPath> getTables(JdbcDataPath jdbcDataPath) {

        List<JdbcDataPath> jdbcDataPaths = new ArrayList<>();


        String[] types = {"TABLE"};

        try {
            ResultSet tableResultSet = jdbcDataPath.getDataSystem().getCurrentConnection().getMetaData().getTables(jdbcDataPath.getCatalog(), jdbcDataPath.getSchema().getName(), null, types);
            List<String> objectNames = new ArrayList<>();
            while (tableResultSet.next()) {

                objectNames.add(tableResultSet.getString("TABLE_NAME"));

            }
            tableResultSet.close();

            // getRelationDef make also used of the getTables
            // and it seems that there is a cache because the result was closed
            // We do then a second loop
            for (String objectName : objectNames) {
                jdbcDataPaths.add(JdbcDataPath.of(jdbcDataPath.getDataSystem(), jdbcDataPath.getCatalog(), jdbcDataPath.getSchema().getName(), objectName));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Collections.sort(jdbcDataPaths);
        return jdbcDataPaths;

    }

    /**
     * @param jdbcDataPath - a catalog or a schema pattern (should be a uri...)
     * @return the list of schema for this database
     */
    public static List<JdbcDataPath> getSchemas(JdbcDataPath jdbcDataPath) {

        List<JdbcDataPath> jdbcDataPaths = new ArrayList<>();


        try {

            // Always NULL
            // because otherwise it's not a pattern but
            // it must match the schema name
            // We build all schemas then
            final String schemaPattern = null;
            ResultSet schemaResultSet = jdbcDataPath.getDataSystem().getCurrentConnection().getMetaData().getSchemas(jdbcDataPath.getCatalog(), schemaPattern);

            // Sqlite Driver return a NULL resultSet
            // because SQLite does not support schema ?
            if (schemaResultSet != null) {
                while (schemaResultSet.next()) {

                    jdbcDataPaths.add(JdbcDataPath.of(jdbcDataPath.getDataSystem(),jdbcDataPath.getCatalog(),schemaResultSet.getString("TABLE_SCHEM"),null));

                }
                schemaResultSet.close();
            }

        } catch (java.sql.SQLFeatureNotSupportedException e) {

            JdbcDataSystemLog.LOGGER_DB_JDBC.warning("Schemas are not supported on this database.");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return jdbcDataPaths;
    }


    /**
     * Retrieve the relationship (ie foreigns key and external key) of tables
     *
     * @param jdbcDataPath - the name of a table or a glob pattern
     * @return
     */
    public List<ForeignKeyDef> getForeignKeys(JdbcDataPath jdbcDataPath) {
        Set<ForeignKeyDef> foreignKeys = new HashSet<>();
        String regexpPattern = Globs.toRegexPattern(jdbcDataPath.getName());
        for (JdbcDataPath dataPath : getTables(jdbcDataPath.getSchema())) {
            if (dataPath.getName().matches(regexpPattern)) {
                foreignKeys.addAll(dataPath.getDataDef().getForeignKeys());
            }
            for (ForeignKeyDef foreignKeyDef: dataPath.getDataDef().getForeignKeys()){
                if (foreignKeyDef.getForeignPrimaryKey().getRelationDef().getDataPath().getName().matches(regexpPattern)){
                    foreignKeys.add(foreignKeyDef);
                }
            }
        }
        return new ArrayList<>(foreignKeys);
    }


}
