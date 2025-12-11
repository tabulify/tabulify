package com.tabulify.uri;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.fs.FsConnection;
import com.tabulify.exception.CastException;
import com.tabulify.exception.IllegalStructure;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.UriEnhanced;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tabulify.connection.ConnectionBuiltIn.MD_LOCAL_FILE_SYSTEM;

/**
 * A class to build a {@link DataUriNode}
 */
public class DataUriBuilder {

  private final DataUriBuilderBuilder builder;

  public DataUriBuilder(DataUriBuilderBuilder dataUriBuilderBuilder) {
    this.builder = dataUriBuilderBuilder;
  }

  public static DataUriBuilderBuilder builder(Tabular tabular) {
    return new DataUriBuilderBuilder(tabular);
  }

  /**
   * @param dataUriString - {@link DataUriStringNode}
   */
  public DataUriNode apply(String dataUriString) {
    Objects.requireNonNull(dataUriString, "The data uri string should be not null");

    /**
     * Trim because an uri that comes from the command line may have
     * heading space
     */
    dataUriString = dataUriString.trim();


    // No data Uri given, means the location of the default datastore
    if (dataUriString.isEmpty()) {
      return new DataUriNode(this.builder.tabular.getDefaultConnection(), "", null);
    }

    // Data Uri
    DataUriStringNode dataUriStringNode;
    try {
      dataUriStringNode = DataUriStringNode.createFromString(dataUriString);
    } catch (CastException e) {
      throw new IllegalArgumentException("The string " + dataUriString + " is not valid data uri format");
    }

    return apply(dataUriStringNode);


  }

  public DataUriNode apply(DataUriStringNode dataUriStringNode) {

    // URI ?
    if (dataUriStringNode.isURL()) {
      String url = dataUriStringNode.getURL();
      try {
        return apply(UriEnhanced.createFromString(dataUriStringNode.getURL()).toUri());
      } catch (IllegalStructure e) {
        throw new IllegalArgumentException("The data uri (" + url + ") is not a valid URL. Error: " + e.getMessage(), e);
      }
    }


    return this.getDataUriNode(dataUriStringNode);

  }

  /**
   * Recursive function to build a data uri node
   */
  private DataUriNode getDataUriNode(DataUriStringNode dataUriStringNode) {


    DataUriNode.Builder dataUriNodeBuilder = DataUriNode.builder();

    /**
     * Connection
     */
    KeyNormalizer connectionName = dataUriStringNode.getConnection();
    Connection connection;
    if (connectionName == null) {
      connection = builder.tabular.getDefaultConnection();
    } else {
      connection = getConnectionFromStringOrNull(connectionName);
      if (connection == null) {
        throw this.unknownConnection(connectionName, dataUriStringNode);
      }
    }
    dataUriNodeBuilder.setConnection(connection);

    /**
     * Path
     */
    if (!dataUriStringNode.hasPathNode()) {

      dataUriNodeBuilder.setPath(dataUriStringNode.getPath());

    } else {

      DataUriNode dataUriNode = this.getDataUriNode(dataUriStringNode.getPathNode());
      dataUriNodeBuilder.setPathDataUri(dataUriNode);

    }

    return dataUriNodeBuilder.build();


  }

  private IllegalArgumentException unknownConnection(KeyNormalizer connectionName, DataUriStringNode dataUriString) {
    String connectionsExpected = builder.tabular.getConnections().stream().map(Connection::getName).map(KeyNormalizer::toString).collect(Collectors.joining(", "));
    throw new IllegalArgumentException("The connection (" + connectionName + ") given by the data uri (" + dataUriString + ") is unknown.\nWe were expecting one of: " + connectionsExpected);
  }

  public DataUriNode apply(URI uri) {
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
    KeyNormalizer name = Connection.getConnectionNameFromUri(connectionUri);
    FsConnection fsConnection = (FsConnection) this.builder.tabular.getConnection(name);
    if (fsConnection == null) {
      fsConnection = (FsConnection) this.builder.tabular.createRuntimeConnection(connectionUri.toString(), name)
        .setComment("Connection for the connection URI " + connectionUri);
    }

    String relativePath = workingPath.toAbsolutePath().relativize(path).toString();
    Connection connection = fsConnection;
    return (new DataUriNode(connection, relativePath, null));

  }


  public static class DataUriBuilderBuilder {

    private final Tabular tabular;
    private final Map<KeyNormalizer, Connection> connections = new HashMap<>();

    public DataUriBuilderBuilder(Tabular tabular) {
      this.tabular = tabular;
    }


    public DataUriBuilderBuilder addConnection(KeyNormalizer connectionName, Connection connection) {
      this.connections.put(connectionName, connection);
      return this;
    }

    public DataUriBuilder build() {

      return new DataUriBuilder(this);
    }

    public DataUriBuilderBuilder addConnection(Connection connection) {
      this.connections.put(connection.getName(), connection);
      return this;
    }

    public DataUriBuilderBuilder addManifestDirectoryConnection(Path parent) {
      FsConnection sdConnection = tabular.createRuntimeConnectionFromLocalPath(MD_LOCAL_FILE_SYSTEM, parent);
      addConnection(MD_LOCAL_FILE_SYSTEM, sdConnection);
      return this;
    }

  }

  /**
   * @param connectionName connection name
   * @return the connection object
   */
  private Connection getConnectionFromStringOrNull(KeyNormalizer connectionName) {

    if (connectionName == null) {
      return builder.tabular.getDefaultConnection();
    }
    if (this.builder.connections.containsKey(connectionName)) {
      return this.builder.connections.get(connectionName);
    }
    return builder.tabular.getConnection(connectionName);

  }
}
