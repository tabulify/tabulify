package com.tabulify.memory;

import com.tabulify.DbLoggers;
import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.connection.ConnectionMetadata;
import com.tabulify.noop.NoOpConnection;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.spi.ProcessingEngine;
import com.tabulify.exception.InternalException;
import com.tabulify.type.MediaType;
import com.tabulify.type.Strings;
import com.tabulify.type.time.Timestamp;

import java.util.List;

public class MemoryConnection extends NoOpConnection {


  private final String workingPathNamespace;
  private final MemoryDataSystem memoryDataSystem;


  public MemoryConnection(Tabular tabular, Attribute name, Attribute connectionString) {
    super(tabular, name, connectionString);
    this.memoryDataSystem = new MemoryDataSystem(this);
    this.workingPathNamespace = getUri().getPath();

  }

  public static MemoryConnection of(Tabular tabular, Attribute name, Attribute connectionString) {
    return new MemoryConnection(tabular, name, connectionString);
  }


  @Override
  public MemoryDataSystem getDataSystem() {
    return memoryDataSystem;
  }

  @Override
  public MemoryDataPath getDataPath(String pathOrName) {
    return getDataPath(pathOrName, MemoryDataPathType.LIST);
  }

  @Override
  public MemoryDataPath getDataPath(String pathOrName, MediaType mediaType) {

    if (mediaType == null) {
      mediaType = MemoryDataPathType.LIST;
    }
    return getTypedDataPath(mediaType, pathOrName);

  }

  @Override
  public String getCurrentPathCharacters() {
    return ".";
  }

  @Override
  public String getParentPathCharacters() {
    return "..";
  }

  @Override
  public String getSeparator() {
    return "/";
  }

  /**
   * In memory data path may have several implementation
   * This function permits to redirect the creation of the in-memory data resources
   *
   * @param mediaType the media type
   * @param parts     the path part
   * @return the memory data path
   */
  public MemoryDataPath getTypedDataPath(MediaType mediaType, String... parts) {
    if (parts.length == 0) {
      throw new InternalException(
        Strings.createMultiLineFromStrings("You can't create a data path without name",
          "If you don't want to specify a name, use the getRandomDataPath function").toString());
    }
    String path = String.join("/", parts);

    MemoryDataPath dataPath = getManager(mediaType).createDataPath(this, path);
    dataPath.addAttribute(DataPathAttribute.CREATION_TIME, Timestamp.createFromNowUtc().toSqlTimestamp());

    // Extension delete for the logical name
    // delete the extension (ie all parts after the first point)
    // to be in sync with the file system rules
    // when a user gives a file name
    String filename = parts[parts.length - 1];
    int endIndex = filename.indexOf('.');
    if (endIndex != -1) {
      dataPath.setLogicalName(filename.substring(0, endIndex));
    }

    return dataPath;


  }

  @Override
  public MemoryDataPath getCurrentDataPath() {
    return getDataPath(workingPathNamespace);
  }


  @Override
  public DataPath getRuntimeDataPath(DataPath dataPath, MediaType mediaType) {
    throw new UnsupportedOperationException("Scripting is not yet supported on memory structure");
  }

  @Override
  public ConnectionMetadata getMetadata() {
    return new MemoryConnectionMetadata(this);
  }

  @Override
  public ProcessingEngine getProcessingEngine() {
    throw new RuntimeException("A processing engine is not yet supported on memory structure");
  }

  @SuppressWarnings("RedundantMethodOverride")
  @Override
  public Boolean ping() {
    return true;
  }


  @Override
  public void close() {
    super.close();
  }

  public MemoryVariableManager getManager(MediaType mediaType) {

    List<MemoryVariableManagerProvider> installedProviders = MemoryVariableManagerProvider.installedProviders();
    for (MemoryVariableManagerProvider structProvider : installedProviders) {
      if (structProvider.accept(mediaType)) {
        MemoryVariableManager memoryVariableManager = structProvider.getMemoryVariableManager();
        if (memoryVariableManager == null) {
          String message = "The returned variable manager is null for the provider (" + structProvider.getClass() + ")";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new RuntimeException(message);
        } else {
          return memoryVariableManager;
        }
      }
    }
    throw new RuntimeException("The type (" + mediaType + ") has no installed provider for the memory connection.");
  }


}
