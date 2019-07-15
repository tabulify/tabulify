package net.bytle.db;

import net.bytle.crypto.Protector;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.fs.Fs;
import net.bytle.regexp.Globs;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.ini4j.Wini;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytle.db.database.Databases.MODULE_NAME;

/**
 * A database store implementation based on ini file
 * If a password is saved a passphrase should be provided
 */
public class DatabasesStore {

    /**
     * This is a passphrase used to encrypt the sample database password
     * Don't change this value
     */
    private static final String INTERNAL_PASSPHRASE = "r1zilGx22kRCUFjPGXbo";
    private static final String INTERNAL_PASSPHRASE_KEY = "bdb_internal_passphrase" ;
    private String passphrase;
    private Path path;

    public static final Path DEFAULT_STORAGE_FILE = Paths.get(Fs.getAppData(MODULE_NAME).toString(), "dsn.ini");

    /**
     * Constant
     */
    private static final String URL = "url";
    private static final String USER = "user";
    private static final String DRIVER = "driver";
    private static final String PASSWORD = "password";
    private static final String STATEMENT = "statement";


    /**
     * The ini file were database information are saved to disk
     */
    private Ini ini;


    private DatabasesStore(Path path) {

        if (path != null) {
            this.path = path;
            DbLoggers.LOGGER_DB_ENGINE.info("Opening the database store (" + path.toAbsolutePath().toString() + ")");
        } else {
            throw new RuntimeException("The path store should not be null");
        }
    }

    public static DatabasesStore of(Path path) {

        return new DatabasesStore(path);
    }

    public static DatabasesStore of() {
        return of(DEFAULT_STORAGE_FILE);
    }


    public DatabasesStore setPassphrase(String passphrase) {
        this.passphrase = passphrase;
        return this;
    }

    /**
     * @param database
     * @param internalPassphrase - to indicate that this is an built-in database and that the internal passphrase should be used - only called from this class
     * @return
     */
    private DatabasesStore save(Database database, Boolean internalPassphrase) {

        Ini ini = getIniFile();
        ini.put(database.getDatabaseName(), URL, database.getUrl());
        ini.put(database.getDatabaseName(), DRIVER, database.getDriver());
        ini.put(database.getDatabaseName(), USER, database.getUser());
        String localPassphrase;
        if (database.getPassword() != null) {
            if (this.passphrase == null) {
                if (internalPassphrase) {
                    localPassphrase = INTERNAL_PASSPHRASE;
                    ini.put(database.getDatabaseName(), INTERNAL_PASSPHRASE_KEY, true);
                } else {
                    throw new RuntimeException("A passphrase is mandatory when a password must be saved.");
                }
            } else {
                localPassphrase = this.passphrase;
            }
            String password = Protector.get(localPassphrase).encrypt(database.getPassword());
            ini.put(database.getDatabaseName(), PASSWORD, password);
        }
        ini.put(database.getDatabaseName(), STATEMENT, database.getConnectionStatement());
        flush();
        return this;
    }

    public DatabasesStore save(Database database) {

        return save(database, false);
    }


    /**
     * Write the changes to the disk
     */
    private void flush() {

        try {
            getIniFile().store();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private Ini getIniFile() {
        if (ini == null) {
            reload();
        }
        return ini;
    }

    /**
     * Remove all databases metadata information that matchs on of the globPatterns
     *
     * @param globPatterns
     * @return a list of database name removed
     */
    public List<String> removeDatabases(String... globPatterns) {
        Ini ini = getIniFile();
        List<String> databases = new ArrayList<>();
        for (String globPattern : globPatterns) {
            String regexpPattern = Globs.toRegexPattern(globPattern);
            for (Profile.Section section : ini.values()) {
                if (section.getName().matches(regexpPattern)) {
                    Profile.Section deletedSection = ini.remove(section);
                    databases.add(deletedSection.getName());
                }
            }
        }
        flush();
        return databases;
    }

    /**
     * Removes all databases
     *
     * @return
     */
    public List<String> removeAllDatabases() {
        return removeDatabases("*");
    }

    /**
     * @return all databases
     */
    public List<Database> getDatabases() {
        return getDatabases("*");
    }

    /**
     * @param globPatterns
     * @return all databases that match this glob patterns
     */
    public List<Database> getDatabases(String... globPatterns) {
        List<Database> databases = new ArrayList<>();
        for (String globPattern : globPatterns) {
            String regexpPattern = Globs.toRegexPattern(globPattern);
            databases.addAll(
                    getIniFile().keySet()
                            .stream()
                            .filter(s -> s.matches(regexpPattern))
                            .map(s -> DatabaseOf(s))
                            .collect(Collectors.toList())
            );
        }
        Collections.sort(databases);
        return databases;
    }

    public List<Database> getDatabases(List<String> globPatterns) {
        return getDatabases(globPatterns.toArray(new String[0]));
    }

    public Database DatabaseOf(String name) {

        Database database = Databases.of(name);

        Wini.Section iniSection = getIniFile().get(name);
        if (iniSection != null) {
            database.setUrl(iniSection.get(URL));
            database.setUser(iniSection.get(USER));
            if (iniSection.get(PASSWORD) != null) {
                String localPassphrase;
                if (this.passphrase != null) {
                    localPassphrase = this.passphrase;
                } else {
                    final String s = iniSection.get(INTERNAL_PASSPHRASE_KEY);
                    if (s!=null){
                        if (s.equals("true")){
                            localPassphrase = INTERNAL_PASSPHRASE;
                        } else {
                            throw new RuntimeException("The internal passphrase key value (" + s + ") is unknown");
                        }
                    } else {
                        throw new RuntimeException("The database (" + database + ") has a password. A passphrase should be provided");
                    }
                }
                database.setPassword(Protector.get(localPassphrase).decrypt(iniSection.get(PASSWORD)));
            }
            database.setDriver(iniSection.get(DRIVER));
            database.setStatement(iniSection.get(STATEMENT));
        }

        return database;

    }

    /**
     * Reread the file
     */
    public DatabasesStore reload() {
        load();
        return this;
    }

    /**
     * Read the file
     */
    private void load() {
        try {
            if (!(Files.exists(this.path))) {
                Fs.createFile(this.path);
                ini = new Ini(this.path.toFile());

                // Add a Sqlite
                Path dbFile;
                // Trick to not have the user name in the output ie C:\Users\Username\...
                // The env value have a fake account
                final String bytle_db_databases_store = System.getenv("BYTLE_DB_SQLITE_PATH");
                if (bytle_db_databases_store != null) {
                    dbFile = Paths.get(bytle_db_databases_store);
                } else {
                    dbFile = Paths.get(Fs.getAppData(DbDefaultValue.LIBRARY_NAME).toAbsolutePath().toString(), DbDefaultValue.LIBRARY_NAME + ".db");
                }
                Files.createDirectories(dbFile.getParent());
                String rootWindows = "///";
                Database database = Databases.of("sqlite")
                        .setDriver("org.sqlite.JDBC")
                        .setUrl("jdbc:sqlite:" + rootWindows + dbFile.toString().replace("\\", "/"));
                save(database);

                database = Databases.of("oracle")
                        .setDriver("oracle.jdbc.OracleDriver")
                        .setUrl("jdbc:oracle:thin:@[host]:[port]/[servicename]");
                save(database);

                database = Databases.of("sqlserver")
                        .setDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver")
                        .setUrl("jdbc:sqlserver://localhost;databaseName=AdventureWorks;")
                        .setUser("sa")
                        .setPassword("TheSecret1!");
                save(database,true);

                database = Databases.of("mysql")
                        .setDriver("com.mysql.jdbc.Driver")
                        .setUrl("jdbc:mysql://[host]:[port]/[database]");
                save(database);

                // jdbc:postgresql://host:port/database?prop=value
                database = Databases.of("postgresql")
                        .setDriver("org.postgresql.Driver")
                        .setUrl("jdbc:postgresql://host:port/test?ssl=true");
                save(database);

            } else {
                ini = new Ini(this.path.toFile());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeDatabase(String name) {
        Profile.Section deletedSection = getIniFile().remove(name);
        if (deletedSection == null) {
            throw new RuntimeException("The database (" + name + ") is non existent and therefore cannot be removed.");
        }
        flush();
    }
}
