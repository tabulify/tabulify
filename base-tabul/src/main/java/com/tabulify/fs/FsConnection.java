package com.tabulify.fs;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.connection.ConnectionAttributeEnum;
import com.tabulify.connection.ConnectionMetadata;
import com.tabulify.noop.NoOpConnection;
import com.tabulify.spi.DataDefManifest;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.ProcessingEngine;
import com.tabulify.spi.ResourcePath;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;
import com.tabulify.type.UriEnhanced;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A file data store (ie a store that is instantiated with a file system path or an uri)
 * A wrapper around {@link FileSystem}
 */
public class FsConnection extends NoOpConnection {


  /**
   * The file system (local, sftp, s3, ...)
   */
  private FileSystem fileSystem;


  public FsConnection(Tabular tabular, Attribute name, Attribute uri) {

    super(tabular, name, uri);
    this.addAttributesFromEnumAttributeClass(FsConnectionAttribute.class);

  }

  @Override
  public List<Class<? extends ConnectionAttributeEnum>> getAttributeEnums() {
    List<Class<? extends ConnectionAttributeEnum>> list = new ArrayList<>(super.getAttributeEnums());
    list.add(FsConnectionAttribute.class);
    return list;
  }

  protected FileSystem getFileSystem() {
    if (this.fileSystem != null) {
      buildIfNotDone();
    }
    return fileSystem;
  }


  /**
   * We need to build late to pass
   * all properties (ie user, password, ...)
   */
  private void buildIfNotDone() {

    if (this.fileSystem != null) {
      return;
    }

    UriEnhanced uri = this.getUri();
    try {
      if (uri.getScheme().equals("file")) {
        /**
         * There can be only one local file system
         * <p>
         * if you try to get a file system with the URI: file:///D:/code/bytle-mono/db-gen/
         * <p>
         * you will get
         * <p>
         * java.lang.IllegalArgumentException: Path component should be '/'
         * <p>
         * Ie in the URI, the path component should only be /
         * <p>
         * hence "file:///"
         */
        fileSystem = FileSystems.getDefault();
      } else {

        try {
          fileSystem = FileSystems.newFileSystem(uri.toUri(), this.getConnectionProperties());
        } catch (IOException e) {
          throw new RuntimeException(e);
        } catch (FileSystemAlreadyExistsException e) {
          fileSystem = FileSystems.getFileSystem(uri.toUri());
        }

      }

    } catch (Exception e) {
      throw new RuntimeException("Unable to create the file system for the URI (" + uri + "). Message: " + e.getMessage(), e);
    }

  }


  @Override
  public FsDataSystem getDataSystem() {
    return new FsDataSystem(this);
  }

  @Override
  public void close() {
    FileSystem fileSystem = getFileSystem();
    if (fileSystem == null) {
      return;
    }
    try {
      fileSystem.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedOperationException e) {

      /**
       * We may receive the UnsupportedOperationException
       * <p>
       * Example Linux
       * Exception in thread "main" java.lang.UnsupportedOperationException
       *         at java.base/sun.nio.fs.UnixFileSystem.close(Unknown Source)
       *         at com.tabulify.db.fs.FsConnection.close(FsConnection.java:99)
       *         at com.tabulify.db.Tabular.close(Tabular.java:573)
       *         at com.tabulify.db.tabli.Tabli.main(Tabli.java:481)
       */
      Set<String> invalidValues = Set.of("WindowsFileSystem", "LinuxFileSystem");
      String fileSystemName = fileSystem.getClass().getSimpleName();
      if (!invalidValues.contains(fileSystemName)) {
        // We throw
        throw new UnsupportedOperationException("Error for file system: " + fileSystemName, e);
      }

    }
  }

  @Override
  public boolean isOpen() {
    return getFileSystem().isOpen();
  }


  @Override
  public DataPath getRuntimeDataPath(DataPath dataPath, MediaType mediaType) {
    FsFileManager fileManager = this.getDataSystem().getFileManager(dataPath.getMediaType());
    return fileManager.createRuntimeDataPath(this, (FsDataPath) dataPath);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<DataPath> select(String globPathOrName, MediaType mediaType) {
    return getDataSystem().select(getCurrentDataPath(), globPathOrName, mediaType);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<DataPath> select(String globPathOrName) {
    return select(globPathOrName, null);
  }

  @Override
  public ConnectionMetadata getMetadata() {
    return new FsConnectionMetadata(this);
  }

  @Override
  public ProcessingEngine getProcessingEngine() {
    throw new RuntimeException("A processing engine is not yet supported on file");
  }

  @Override
  public <T> T getObject(Object valueObject, Class<T> clazz) {
    try {
      return Casts.cast(valueObject, clazz);
    } catch (CastException e) {
      throw new RuntimeException("The value (" + valueObject + ") could not be cast to " + clazz.getSimpleName() + " for the connection (" + this + ")", e);
    }
  }

  @Override
  public ResourcePath createStringPath(String pathOrName, String... names) {
    return FsConnectionResourcePath.createOf(this.getCurrentDataPath().getAbsoluteNioPath(), pathOrName, names);
  }


  /**
   * @param path      - the nio relative path
   * @param mediaType - the media type
   * @return The entry point to create all file system data path
   */
  public DataPath getFsDataPath(Path path, MediaType mediaType) {


    FsDataSystem dataSystem = this.getDataSystem();

    if (mediaType == null) {
      mediaType = dataSystem.determineMediaType(path);
    }
    FsFileManager fileManager = dataSystem.getFileManager(mediaType);

    Path connectionPath = this.getNioPath();
    if (path.isAbsolute()) {
      try {
        path = connectionPath.relativize(path);
      } catch (IllegalArgumentException e) {
        throw new InternalException("The path (" + path + ") is not relative path of the connection path (" + connectionPath + ")");
      }
    }
    DataPath dataPath = fileManager.createDataPath(this, path, mediaType);

    /**
     * DataDef yaml check: Do we have also a yaml property files
     * <p>
     * Only for files, not for directory
     */
    DataDefManifest.mergeDataDef(dataPath, mediaType);

    return dataPath;

  }

  /**
   * A wrapper around {@link FileSystem#getPath(String, String...)}
   *
   * @param pathOrName - the first (the path string if the second argument names is null, otherwise a name part of the path string)
   * @param mediaType  - the media type
   * @return the data path
   */
  @Override
  public DataPath getDataPath(String pathOrName, MediaType mediaType) {

    this.buildIfNotDone();

    pathOrName = FsConnectionResourcePath.toSystemPath(fileSystem, pathOrName);

    Path path;
    try {
      path = fileSystem.getPath(pathOrName);
    } catch (InvalidPathException e) {
      throw new IllegalArgumentException("The path given is not valid. Path not valid: (" + pathOrName + "). Error returned: (" + e.getMessage() + ")", e);
    }
    if (mediaType == null) {
      mediaType = this.getDataSystem().determineMediaType(path);
    }
    return getFsDataPath(path, mediaType);

  }

  @Override
  public DataPath getDataPath(String pathOrName) {
    return getDataPath(pathOrName, (MediaType) null);
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
    return getFileSystem().getSeparator();
  }


  /**
   * The current path of the connection
   *
   * @return the data path
   */
  @Override
  public FsDataPath getCurrentDataPath() {

    /**
     * Default value
     */

    Path path = this.getNioPath();

    /**
     * We set that the current data path is a directory
     * (Which should be by default any way)
     * Because otherwise when using the HTTP file system,
     * the function would have to make a call each time
     * This way, this is no more necessary and there is a huge
     * gain on time performance (36 to 1 sec)
     */
    return (FsDataPath) getFsDataPath(path, MediaTypes.DIR);

  }

  /**
   * @return the absolute path of the connection
   */
  public Path getNioPath() {
    URI uri = this.getUri().toUri();
    try {
      return Paths.get(uri).normalize().toAbsolutePath();
    } catch (Exception e) {
      /**
       * To get context for
       * {@link FileSystemNotFoundException} and other runtime error
       */
      throw new RuntimeException("Error for the URI (" + uri + ")", e);
    }
  }

  /**
   * All file system shares the {@link Files common API}  to move, copy files.
   * They then share the same service id
   *
   * @return the type of system
   */
  @Override
  public String getServiceId() {
    return "fileSystem";
  }


}
