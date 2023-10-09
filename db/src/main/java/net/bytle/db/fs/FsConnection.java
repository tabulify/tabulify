package net.bytle.db.fs;

import net.bytle.db.Tabular;
import net.bytle.db.connection.ConnectionMetadata;
import net.bytle.db.fs.binary.FsBinaryDataPath;
import net.bytle.db.fs.dir.FsDirectoryDataPath;
import net.bytle.db.noop.NoOpConnection;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.db.spi.ResourcePath;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;
import net.bytle.type.Variable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.List;

/**
 * A file data store (ie a store that is instantiated with a file system path or a uri)
 * A wrapper around {@link FileSystem}
 */
public class FsConnection extends NoOpConnection {


  private FileSystem fileSystem;


  public FsConnection(Tabular tabular, Variable name, Variable uri) {

    super(tabular, name, uri);

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

    String uri = this.getUriAsString();
    try {
      if (uri.startsWith("file:/")) {
        /**
         * There can be only one local file system
         *
         * if you try to get a file system with the URI: file:///D:/code/bytle-mono/db-gen/
         *
         * you will get
         *
         * java.lang.IllegalArgumentException: Path component should be '/'
         *
         * Ie in the URI, the path component should only be /
         *
         * hence "file:///"
         */
        fileSystem = FileSystems.getDefault();
      } else {
        fileSystem = FileSystems.newFileSystem(URI.create(uri), this.getVariablesAsKeyValueMap());
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
      if (!fileSystem.getClass().getSimpleName().equals("WindowsFileSystem")) {
        // We trow if it's not window because it's unsupported for windows
        throw e;
      }
    }
  }

  @Override
  public boolean isOpen() {
    return getFileSystem().isOpen();
  }


  @Override
  public DataPath createScriptDataPath(DataPath dataPath) {
    return new FsBinaryDataPath(this, dataPath);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<FsDataPath> select(String globPathOrName, MediaType mediaType) {
    return getDataSystem().select(getCurrentDataPath(), globPathOrName, mediaType);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<FsDataPath> select(String globPathOrName) {
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


  public URI getConnectionUri() {

    return URI.create(this.getUriAsString());

  }

  /**
   * @param path      - the nio path
   * @param mediaType - the media type
   * @return The entry point to create all file system data path
   */
  public FsDataPath getFsDataPath(Path path, MediaType mediaType) {


    FsFileManager fileManager = this
      .getDataSystem()
      .getFileManager(path, mediaType);

    Path connectionPath = this.getNioPath();
    if (path.isAbsolute()) {
      try {
        path = connectionPath.relativize(path);
      } catch (IllegalArgumentException e) {
        throw new InternalException("The path (" + path + ") is not relative path of the connection path (" + connectionPath + ")");
      }
    }
    FsDataPath dataPath = fileManager.createDataPath(this, path);

    /**
     * DataDef yaml check: Do we have also a yaml property files
     *
     * Only for files, not for directory
     */
    if (!(dataPath instanceof FsDirectoryDataPath)) {
      /**
       * Only on the local file system (ie file)
       * not on network for performance reason.
       * With HTTP, it can add up really quickly
       */
      if (path.toUri().getScheme().equals("file")) {
        Path yamlDataDef = path.resolveSibling(dataPath.getLogicalName() + DataPathAbs.DATA_DEF_SUFFIX);
        if (Files.exists(yamlDataDef)) {
          dataPath.mergeDataDefinitionFromYamlFile(yamlDataDef);
        }
      }
    }

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
  public FsDataPath getDataPath(String pathOrName, MediaType mediaType) {

    this.buildIfNotDone();

    pathOrName = FsConnectionResourcePath.toSystemPath(fileSystem, pathOrName);

    Path path;
    try {
      path = fileSystem.getPath(pathOrName);
    } catch (InvalidPathException e) {
      throw new RuntimeException("The path given is not valid. Path not valid: (" + pathOrName + "). Error returned: (" + e.getMessage() + ")", e);
    }
    return getFsDataPath(path, mediaType);

  }

  @Override
  public FsDataPath getDataPath(String pathOrName) {
    return getDataPath(pathOrName, null);
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
    return getFsDataPath(path, MediaTypes.DIR);

  }

  Path getNioPath() {
    URI uri = this.getConnectionUri();
    try {
      return Paths.get(uri).normalize();
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
