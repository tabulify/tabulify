package net.bytle.db.database;

import net.bytle.db.DatabasesStore;

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
