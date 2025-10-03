package com.tabulify.uri;

import com.tabulify.connection.Connection;
import net.bytle.exception.NoPathFoundException;
import net.bytle.regexp.Glob;

import java.util.Objects;

/**
 * A data uri node is the equivalent of {@link DataUriStringNode}
 * but with real connection object
 * It's a node because it can be a tree of runtime data uri
 */
public class DataUriNode implements Comparable<DataUriNode> {


  /**
   * No path means current path (connection schema or directory)
   * The empty string is also used when schema, catalog are
   * not supported (ie sqlite)
   */
  public static final String CURRENT_CONNECTION_PATH = "";


  private final Connection connection;
  private final String pathString;
  private final DataUriNode pathDataUriNode;


  DataUriNode(Connection connection, String pathString, DataUriNode pathDataUriNode) {

    Objects.requireNonNull(connection, "The connection should not be null");
    if (pathString == null && pathDataUriNode == null) {
      // Empty path string is the default
      pathString = CURRENT_CONNECTION_PATH;
    }
    this.connection = connection;
    this.pathString = pathString;
    this.pathDataUriNode = pathDataUriNode;


  }

  /**
   * protected because the user should use
   * * an instance of {@link DataUriBuilder}
   * * or {@link com.tabulify.Tabular#createDataUri(DataUriStringNode)}
   * This builder is used only by {@link DataUriBuilder} to build a data uri
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * @deprecated for the {@link #builder()}
   */
  public static DataUriNode createFromConnectionAndPath(Connection connection, String path) {

    return new DataUriNode(connection, path, null);
  }

  /**
   * @deprecated for the {@link #builder()}
   */
  public static DataUriNode createFromConnectionAndScriptUri(Connection connection, DataUriNode scriptUri) {
    return new DataUriNode(connection, null, scriptUri);
  }

  /**
   * @deprecated for the {@link #builder()}
   */
  public static DataUriNode createFromConnection(Connection connection) {
    return createFromConnectionAndPath(connection, null);
  }

  /**
   * @deprecated for the {@link #builder()}
   */
  public static DataUriNode createFromConnectionAndTemplate(Connection connection, String template) {
    return createFromConnectionAndPath(connection, template);
  }


  public String getPath() throws NoPathFoundException {

    if (this.pathString == null || this.pathString.isEmpty()) {
      throw new NoPathFoundException("No path found for (" + this + ")");
    }
    return this.pathString;

  }


  public String toString() {
    return this.toDataUriStringNode().toString();
  }


  @Override
  public int compareTo(DataUriNode o) {
    return this.toString().compareTo(o.toString());
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataUriNode dataUri = (DataUriNode) o;
    return toString().equals(dataUri.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(toString());
  }


  public Connection getConnection() {
    return connection;
  }


  public boolean isRuntimeSelector() {

    return this.pathDataUriNode != null;

  }

  /**
   * @return the script data uri (ie path in a data uri form (ie a script)
   */
  public DataUriNode getDataUri() {
    return this.pathDataUriNode;
  }

  public DataUriStringNode toDataUriStringNode() {
    if (!isRuntimeSelector()) {
      return DataUriStringNode
        .builder()
        .setConnection(this.connection.toString())
        .setPath(this.pathString)
        .build();
    }
    return DataUriStringNode.builder()
      .setConnection(this.connection.toString())
      .setPathNode(this.pathDataUriNode.toDataUriStringNode())
      .build();

  }


  /**
   * @return if this data uri is a glob pattern
   */
  public boolean isGlobPattern() {
    try {
      if (Glob.createOf(this.getPath()).containsGlobWildCard()) {
        return true;
      }
    } catch (NoPathFoundException e) {
      // no glob pattern
    }
    if (this.pathDataUriNode != null) {
      return this.pathDataUriNode.isGlobPattern();
    }
    return false;
  }


  public static class Builder {
    private Connection connection;
    private String path;
    private DataUriNode pathDataUriNode;

    public Builder setConnection(Connection connection) {
      this.connection = connection;
      return this;
    }

    public Builder setPath(String path) {
      this.path = path;
      return this;
    }

    public DataUriNode build() {
      return new DataUriNode(connection, path, pathDataUriNode);
    }

    public Builder setPathDataUri(DataUriNode dataUri) {
      this.pathDataUriNode = dataUri;
      return this;
    }
  }
}
