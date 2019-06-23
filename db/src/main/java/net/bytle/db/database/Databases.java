package net.bytle.db.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Static function around database.
 */
public class Databases {


    public static final String BYTLE_LOCAL_SQLITE_DB_NAME = "BytleSqlite";
    /**
     * List of the database registred for this session
     */
    private static Map<String, Database> databases = new HashMap<>();

    /**
     * Initialize a database by its name.
     *
     *
     * @param name -  the database name
     * @return a database
     * <p>
     * If the product name is not implemented, it will return an ANSI implementation.
     */
    public static Database get(String name) {

        Database database = databases.get(name);
        if (database == null) {
            database = new Database(name);
            databases.put(name, database);
        }
        return database;

    }


    public static Database getDefault() {
        /**
         * %TEMP% is the same than %LocalAppData%/Temp
         * is used for user-specific items that should not roam with the user,
         * either because they only pertain to that particular machine, or because they are too large.
         */
        String tempDir = System.getenv("TEMP");

        //TODO: get bytle-db from a class
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

        return get(BYTLE_LOCAL_SQLITE_DB_NAME).setUrl(url);
    }

    /**
     * When the connection is closed, the database cache need to be removed
     *
     * @param database
     */
    public static void close(Database database) {
        databases.remove(database.getDatabaseName());
    }

    /**
     * @return the default bytle database
     */
    public static Database get() {
        return get("bytle");
    }

}
