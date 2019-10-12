package net.bytle.db.database;

import net.bytle.cli.Log;
import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.db.model.*;
import net.bytle.regexp.Globs;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An object with all meta information about a data store
 */
public class Database implements Comparable<Database> {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;


    // The database name
    private final String name;



    // Jdbc Url
    private String url;

    // Jdbc Driver
    private String driver;
    private String user;
    private String postStatement;
    private String password;
    private DatabasesStore databaseStore;


    Database(String name) {

        this.name = name;

    }

    
    public Database setDriver(String jdbcDriver) {
        this.driver = jdbcDriver;
        return this;
    }


    public String getDatabaseName() {
        return name;
    }

    public String getUrl() {
        return this.url;
    }

    public Database setUrl(String jdbcUrl) {
        if (this.url == null || this.url.equals(jdbcUrl)) {

            this.url = jdbcUrl;

        } else {

            throw new RuntimeException("The URL cannot be changed. It has already the value (" + this.url + ") and cannot be set to (" + jdbcUrl + ")");

        }
        return this;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Database database = (Database) o;
        return Objects.equals(name, database.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name);
    }


    public Database setUser(String user) {
        this.user = user;
        return this;
    }

    public Database setPassword(String pwd) {
        this.password = pwd;
        return this;
    }

    public Database setStatement(String connectionScriptValue) {
        this.postStatement = connectionScriptValue;
        return this;
    }



    public String getDriver() {
        return this.driver;
    }

    public String getUser() {
        return this.user;
    }

    public String getPassword() {
        return this.password;
    }

    public String getConnectionStatement() {
        return this.postStatement;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.
     *
     * <p>Finally, the implementor must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.
     *
     * <p>It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * <p>In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of
     * <i>expression</i> is negative, zero or positive.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     */
    @Override
    public int compareTo(Database o) {

            return this.getDatabaseName().compareTo(o.getDatabaseName());

    }

    public Database setDatabaseStore(DatabasesStore databasesStore) {
        this.databaseStore = databasesStore;
        return this;
    }

    public DatabasesStore getDatabaseStore() {
        return this.databaseStore;
    }


    /**
     *
     * @return the scheme of the data store
     *   * file
     *   * ...
     *
     */
    public String getScheme() {
        if(url==null){
            return DatabasesStore.LOCAL_FILE_SYSTEM;
        } else {
            return getUrl().substring(0,getUrl().indexOf(":"));
        }
    }

}
