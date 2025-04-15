package com.tabulify.uri;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionBuiltIn;
import com.tabulify.fs.FsConnection;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NoPathFoundException;
import net.bytle.exception.NoPatternFoundException;
import net.bytle.type.UriEnhanced;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * A data uri is the string representation of a data path
 * <p>
 * It binds together as {@link URI} and a data URI (ie path@system)
 */
public class DataUri implements Comparable<DataUri> {


  /**
   * When a datastore or a path is specified
   * by a data uri
   */
  public static final char BLOCK_OPEN = '(';
  public static final char BLOCK_CLOSE = ')';

  /**
   * No path means current path (connection schema or directory)
   * The empty string is also used when schema, catalog are
   * not supported (ie sqlite)
   */
  public static final String CURRENT_CONNECTION_PATH = "";


  private final Connection connection;
  private final String relativePath;
  private final DataUri scriptDataUri;


  private DataUri(Connection connection, String path, DataUri scriptDataUri) {

    Objects.requireNonNull(connection, "The connection should not be null");
    this.connection = connection;
    this.relativePath = path;
    this.scriptDataUri = scriptDataUri;


  }


  public static DataUri createFromString(Tabular tabular, String spec) {

    Objects.requireNonNull(spec, "The uri spec should be not null");
    /**
     * Trim because an uri that comes from the command line may have
     * heading space
     */
    spec = spec.trim();


    // No data Uri given, means the location of the default datastore
    if (spec.isEmpty()) {
      return new DataUri(tabular.getDefaultConnection(), "", null);
    }

    // URI ?
    URI uri = null;
    try {
      uri = UriEnhanced.createFromString(spec).toUri();
    } catch (IllegalStructure e) {
      // not an uri, an URL?
    }

    /**
     * "(create_foo.sql@howto)@cd" is a valid uri without any scheme
     */
    if (uri != null && uri.getScheme() != null) {

      Path path = Paths.get(uri);

      /**
       * We choose the parent as working directory because otherwise
       * we can get a reflection when instantiating the file.
       * Because we return the path relative to it and if they
       * are the same, you got a loop in the initialization
       * if there is message that asks for its data uri
       */
      Path workingPath = path.getParent();
      if (workingPath == null) {
        workingPath = path;
      }
      URI connectionUri = workingPath.toUri();
      String name = Connection.getConnectionNameFromUri(connectionUri);
      FsConnection fsConnection = (FsConnection) tabular.getConnection(name);
      if (fsConnection == null) {
        fsConnection = (FsConnection) tabular.createRuntimeConnection(name, connectionUri.toString())
          .setDescription("Connection for the connection URI " + connectionUri);
      }

      String relativePath = workingPath.toAbsolutePath().relativize(path).toString();
      Connection connection = fsConnection;
      return (new DataUri(connection, relativePath, null));

    } else {

      // Data Uri
      DataUriString dataUriString = DataUriString.createFromString(spec);
      String connectionName = dataUriString.getConnectionName();
      Connection connection = getConnectionFromStringOrNull(tabular, connectionName);
      if (connection == null) {
        throw new RuntimeException("The connection (" + connectionName + ") given by the data uri (" + spec + ") is unknown.");
      }
      String relativePath = null;
      DataUri scriptDataUri = null;
      if (!dataUriString.isScriptSelector()) {
        relativePath = dataUriString.getPath();
      } else {
        DataUriString scriptDataUriString = dataUriString.getScriptDataUriString();
        String scriptConnectionName = scriptDataUriString.getConnectionName();
        Connection scriptConnection = getConnectionFromStringOrNull(tabular, scriptConnectionName);
        String path = scriptDataUriString.getPath();
        scriptDataUri = new DataUri(scriptConnection, path, null);
      }

      return new DataUri(connection, relativePath, scriptDataUri);

    }

  }

  /**
   * @param tabular tabular
   * @param connectionName connection name
   * @return the connection object
   */
  private static Connection getConnectionFromStringOrNull(Tabular tabular, String connectionName) {

    if (connectionName == null) {
      return tabular.getDefaultConnection();
    }
    if (connectionName.equals(ConnectionBuiltIn.SD_LOCAL_FILE_SYSTEM)) {
      return tabular.getSdConnection();
    }
    return tabular.getConnection(connectionName);

  }


  public static DataUri createFromConnectionAndPath(Connection connection, String path) {
    return new DataUri(connection, path, null);
  }

  public static DataUri createFromConnectionAndScriptUri(Connection connection, DataUri scriptUri) {
    return new DataUri(connection, null, scriptUri);
  }

  public static DataUri createFromConnection(Connection connection) {
    return createFromConnectionAndPath(connection, null);
  }

  public static DataUri createFromConnectionAndPattern(Connection connection, String pattern) {
    return createFromConnectionAndPath(connection, pattern);
  }


  public String getPath() throws NoPathFoundException {

    if (this.relativePath == null) {
      throw new NoPathFoundException("No path found for (" + this + ")");
    }
    return this.relativePath;

  }


  public String toString() {
    return this.toDataUriString().toString();
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
  public int compareTo(DataUri o) {
    return this.toString().compareTo(o.toString());
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataUri dataUri = (DataUri) o;
    return toString().equals(dataUri.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(toString());
  }


  public Connection getConnection() {
    return connection;
  }

  public String getPattern() throws NoPatternFoundException {
    try {
      return this.getPath();
    } catch (NoPathFoundException e) {
      throw new NoPatternFoundException("No pattern found for (" + this + ")");
    }
  }

  public boolean isScriptSelector() {

    return this.scriptDataUri != null;

  }

  /**
   * @return the script data uri (ie path in a data uri form (ie a script)
   */
  public DataUri getScriptUri() {
    return this.scriptDataUri;
  }

  public DataUriString toDataUriString() {
    if (!isScriptSelector()) {
      return DataUriString.create()
        .setConnection(this.connection)
        .setPath(this.relativePath);
    } else {
      return DataUriString.create()
        .setConnection(this.connection)
        .setDataUriPath(this.scriptDataUri.toDataUriString());
    }
  }


}
