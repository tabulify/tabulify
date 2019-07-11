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

    public DatabasesStore save(Database database) {

        Ini ini = getIniFile();
        ini.put(database.getDatabaseName(), URL, database.getUrl());
        ini.put(database.getDatabaseName(), DRIVER, database.getDriver());
        ini.put(database.getDatabaseName(), USER, database.getUser());
        if (database.getPassword() != null) {
            if (passphrase == null) {
                throw new RuntimeException("A passphrase is mandatory when a password must be saved.");
            }
            String password = Protector.get(passphrase).encrypt(database.getPassword());
            ini.put(database.getDatabaseName(), PASSWORD, password);
        }
        ini.put(database.getDatabaseName(), STATEMENT, database.getConnectionStatement());
        flush();
        return this;
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
        if (!(Files.exists(this.path))) {
            Fs.createFile(this.path);
        }
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

    public Database DatabaseOf(String name) {

        Database database = Databases.of(name);

        Wini.Section iniSection = getIniFile().get(name);
        if (iniSection != null) {
            database.setUrl(iniSection.get(URL));
            database.setUser(iniSection.get(USER));
            if (iniSection.get(PASSWORD) != null) {
                if (passphrase != null) {
                    database.setPassword(Protector.get(passphrase).decrypt(iniSection.get(PASSWORD)));
                } else {
                    throw new RuntimeException("The database (" + database + ") has a password. A passphrase should be provided");
                }
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
            ini = new Ini(this.path.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
