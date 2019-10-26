package net.bytle.db.database;

import net.bytle.db.DatabasesStore;
import oracle.jdbc.OracleTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Static function around database.
 */
public class Databases {


    public static final String BYTLE_LOCAL_SQLITE_DB_NAME = "BytleSqlite";
    public static final String MODULE_NAME = "BytleDb";

    /**
     * Initialize a database by its name.
     *
     * @param name -  the database name
     * @return a database
     * <p>
     * If the product name is not implemented, it will return an ANSI implementation.
     */
    public static Database of(String name) {

        return new Database(name);

    }




    public static Database getSqliteDefault() {
        /**
         * %TEMP% is the same than %LocalAppData%/Temp
         * is used for user-specific items that should not roam with the user,
         * either because they only pertain to that particular machine, or because they are too large.
         */
        String tempDir = System.getenv("TEMP");

        //TODO: of bytle-db from a class
        Path path = Paths.get(tempDir, "bytle-db").toAbsolutePath();
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Path pathDb = Paths.get(path.toString(), "defaultDb.db");

        // TODO: what if linux
        String rootWindows = "///";
        String url = "jdbc:sqlite:" + rootWindows + pathDb.toString().replace("\\", "\\\\");

        return of(BYTLE_LOCAL_SQLITE_DB_NAME).setUrl(url);

    }


    /**
     * @return the default bytle database
     */
    public static Database of() {
        return of("bytle");
    }

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




    public static Database of(String name, DatabasesStore databasesStore) {
        final Database database = databasesStore.getDatabase(name);
        if (database==null){
            return of(name);
        } else {
            return database;
        }
    }

    public static Database getDefault() {
        return of();
    }
}
