package com.tabulify.uri;

import com.tabulify.connection.Connection;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.KeyNormalizer;

import java.net.URL;
import java.util.Objects;

/**
 * A data uri node is a string representation of a data uri, ie of a data path
 * <p>
 * A node in the data uri tree
 * (DataUriStringNode)@connection
 * <p>
 * The top tree representation is:
 * ((select.sql@cd)@db)@otherDb
 * ie a select sql file in the current directory executed against the db
 * that serves as executable content that should be executed against the `otherDb`
 * <p>
 * The tree element is the path that can be:
 * * a {@link DataUriStringNode}
 * * or a string
 * It also accepts an {@link URL}
 * <p>
 * The {@link #getPath()} is not null (empty to represent the default connection path)
 * if the {@link #getPathNode()} is empty
 */
public class DataUriStringNode implements Comparable<DataUriStringNode> {

  /**
   * The data store separator
   */
  public static final char AT_CHAR = '@';

  /**
   * When a datastore or a path is specified
   * by a data uri
   */
  public static final char BLOCK_OPEN = '(';
  public static final char BLOCK_CLOSE = ')';

  /**
   * The path of a static resource
   * If {@link #pathNode} is null, it's never null as the empty string correspond to the default path connection
   */
  private final String pathString;
  private final DataUriStringNode pathNode;
  private final KeyNormalizer connection;

  private DataUriStringNode(DataUriNodeBuilder dataUriNodeBuilder) {

    this.pathString = dataUriNodeBuilder.pathString.toString();
    if (dataUriNodeBuilder.connection.length() == 0) {
      this.connection = null;
    } else {
      try {
        this.connection = KeyNormalizer.create(dataUriNodeBuilder.connection);
      } catch (CastException e) {
        throw new IllegalArgumentException("The connection name (" + dataUriNodeBuilder.connection + ") is not valid. Error: " + e.getMessage(), e);
      }
    }
    if (dataUriNodeBuilder.pathNodeBuilder != null) {
      if (dataUriNodeBuilder.pathNode != null) {
        throw new InternalException("You cannot have a data uri builder and a node set at the same time");
      }
      this.pathNode = dataUriNodeBuilder.pathNodeBuilder.build();
    } else {
      this.pathNode = dataUriNodeBuilder.pathNode;
    }

  }

  public String getPath() {
    if (pathNode != null) {
      return pathNode.toString();
    }
    return pathString;
  }

  public static DataUriNodeBuilder builder() {
    return new DataUriNodeBuilder();
  }

  public static DataUriStringNode createFromString(String uri) throws CastException {
    if (uri == null) {
      throw new CastException("The string data uri given is null");
    }
    /**
     * Trim because an uri that comes from the command line may have
     * heading space
     */
    uri = uri.trim();
    if (uri.isEmpty()) {
      throw new CastException("The data uri should not be the empty string");
    }

    /**
     * The root
     */
    return DataUriStringParser.parse(uri);

  }

  public static DataUriStringNode createFromStringSafe(String dataUriString) {
    try {
      return DataUriStringNode.createFromString(dataUriString);
    } catch (CastException e) {
      throw new InternalException("The literal data uri string (" + dataUriString + ") is not valid. Error: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataUriStringNode dataUri = (DataUriStringNode) o;
    return toString().equals(dataUri.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(toString());
  }

  @Override
  public String toString() {
    if (pathNode != null) {
      return BLOCK_OPEN + pathNode.toString() + BLOCK_CLOSE + AT_CHAR + connection;
    }
    if (connection == null) {
      return pathString;
    }
    return pathString + AT_CHAR + connection;
  }

  public KeyNormalizer getConnection() {
    return connection;
  }

  @Override
  public int compareTo(DataUriStringNode o) {
    return this.toString().compareTo(o.toString());
  }

  public DataUriStringNode getPathNode() {
    return this.pathNode;
  }

  public boolean hasPathNode() {
    return this.pathNode != null;
  }

  public boolean isURL() {
    /**
     * Note that "(create_foo.sql@howto)@cd" is a valid uri without any scheme
     * We test therefore for http
     */
    //noinspection HttpUrlsUsage
    return this.pathString != null && this.connection == null && this.pathNode == null && (this.pathString.startsWith("https://") || this.pathString.startsWith("http://"));
  }

  public String getURL() {
    return this.pathString;
  }

  public static class DataUriNodeBuilder {
    private DataUriNodeBuilder pathNodeBuilder;
    DataUriStringNode pathNode;
    StringBuilder pathString = new StringBuilder();
    StringBuilder connection = new StringBuilder();

    public DataUriNodeBuilder setPathNodeBuilder(DataUriNodeBuilder dataUriNodePath) {
      this.pathNodeBuilder = dataUriNodePath;
      return this;
    }

    public DataUriNodeBuilder setConnection(String c) {
      connection = new StringBuilder(c);
      return this;
    }

    public DataUriNodeBuilder appendConnectionCharacter(char c) {
      connection.append(c);
      return this;
    }

    public DataUriNodeBuilder appendPathCharacter(char c) {
      pathString.append(c);
      return this;
    }


    public DataUriStringNode build() {
      return new DataUriStringNode(this);
    }

    public DataUriNodeBuilder setPathNode(DataUriStringNode dataUriString) {
      this.pathNode = dataUriString;
      return this;
    }

    public DataUriNodeBuilder setConnection(Connection sqlConnection) {
      return setConnection(sqlConnection.getName().toString());
    }

    public DataUriNodeBuilder setPath(String path) {
      if (path == null) {
        return this;
      }
      this.pathString = new StringBuilder(path);
      return this;
    }

    @Override
    public String toString() {
      return "DataUriNodeBuilder{}";
    }

    public DataUriNodeBuilder setConnection(KeyNormalizer connection) {
      return setConnection(connection.toString());
    }

    /**
     * To rename when it compiles to set path
     */
    public DataUriNodeBuilder setPattern(String glob) {
      return setPath(glob);
    }
  }
}
