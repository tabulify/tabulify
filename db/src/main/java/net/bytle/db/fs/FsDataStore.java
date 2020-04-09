package net.bytle.db.fs;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.spi.ProcessingEngine;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A file data store (ie a store that is instantiated with a file system path or a uri)
 */
public class FsDataStore extends DataStore {

  /**
   * Create a data store from a path
   * This is a convenient way to create a local file system or a http data store
   * from a simple path
   *
   * @param path
   * @return
   */
  public static FsDataStore of(Path path) {
    FsDataStore fsDataStore;
    if (path.toUri().getScheme().equals("file")){
      fsDataStore = FsDataStore.getLocalFileSystem();
    } else {
      FileSystem fileSystem = path.getFileSystem();
      fsDataStore = new FsDataStore(fileSystem.toString(), path.toUri().toString(), fileSystem);
    }
    return fsDataStore;
  }

  /**
   * Accessible via {@link #getLocalFileSystem()}
   * A sort of wrapper around {@link FileSystems#getDefault()}
   */
  static final FsDataStore LOCAL_FILE_SYSTEM = new FsDataStore("file", Paths.get(".").toAbsolutePath().toString());
  private FileSystem fileSystem;


  public FsDataStore(String name, String url) {

    super(name, url);
    fileSystem = Paths.get(url).getFileSystem();

  }

  public FsDataStore(String name, String url, FileSystem fileSystem) {

    super(name, url);
    this.fileSystem = fileSystem;

  }

  public static FsDataStore getLocalFileSystem() {
    return LOCAL_FILE_SYSTEM;
  }


  FileSystem getFileSystem() {
    if (this.fileSystem==null){
      try {
        fileSystem = FileSystems.newFileSystem(URI.create(getConnectionString()), getProperties());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return this.fileSystem;
  }


  public FsDataStore setConnectionString(String connectionString) {
    super.setConnectionString(connectionString);
    return this;
  }

  @Override
  public FsDataSystem getDataSystem() {
    return new FsDataSystem(this);
  }

  @Override
  public void close() {
    try {
      getFileSystem().close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedOperationException e){
      if (getFileSystem().getClass().getSimpleName().equals("WindowsFileSystem")){
        // yes it's unsupported
      } else {
        throw e;
      }
    }
  }

  @Override
  public boolean isOpen() {
    return getFileSystem().isOpen();
  }

  @Override
  public DataPathAbs getQueryDataPath(String query) {
    throw new RuntimeException("Query is not yet supported on file");
  }

  @Override
  public Integer getMaxWriterConnection() {
    return 1;
  }

  @Override
  public ProcessingEngine getProcessingEngine() {
    throw new RuntimeException("A processing engine is not yet supported on file");
  }

  @Override
  public <T> T getObject(Object object, Class<T> clazz) {
    throw new RuntimeException("Not yet supported");
  }


  public URI getUri() {

    return Paths.get(connectionString).toUri();

  }

  /**
   *
   * @param path
   * @return
   * The entry point to create all file system data path
   */
  public FsDataPath getFsDataPath(Path path) {

    return this.getDataSystem().getFileManager(path).createDataPath(this,path);

  }

  @Override
  public FsDataPath getDefaultDataPath(String... names) {

    // Rebuild the path
    Path currentPath = Paths.get(connectionString);
    Path path = currentPath;
    for (String name : names) {
      path = path.resolve(name);
    }

    return getFsDataPath(path);

  }

  @Override
  public DataPath getTypedDataPath(String type, String... parts) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public FsDataPath getCurrentDataPath() {
    return getDefaultDataPath(connectionString);
  }

}
