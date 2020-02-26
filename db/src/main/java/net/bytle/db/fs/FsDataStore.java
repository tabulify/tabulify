package net.bytle.db.fs;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.spi.ProcessingEngine;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A file data store (ie a store that is instantiated with a file system path or a uri)
 */
public class FsDataStore extends DataStore {

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
   */
  static final FsDataStore LOCAL_FILE_SYSTEM = new FsDataStore("file", Paths.get(".").toAbsolutePath().toString());
  private final FileSystem fileSystem;


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
    return this.fileSystem;
  }


  public FsDataStore setConnectionString(String connectionString) {
    super.setConnectionString(connectionString);
    return this;
  }

  @Override
  public FsDataSystem getDataSystem() {
    return FsDataSystem.of();
  }

  @Override
  public void close() {
    FsDataSystem.of();
  }

  @Override
  public boolean isOpen() {
    return false;
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


  public URI getUri() {

    return Paths.get(connectionString).toUri();

  }

  public FsDataPath getDataPath(Path path) {

    return this.getDataSystem().getFileManager(path).createDataPath(this,path);

  }

  @Override
  public FsDataPath getDataPath(String... names) {

    // Rebuild the path
    Path currentPath = Paths.get(connectionString);
    Path path = currentPath;
    for (String name : names) {
      path = path.resolve(name);
    }

    return getDataPath(path);

  }

  @Override
  public FsDataPath getCurrentDataPath() {
    Path currentPath = Paths.get(connectionString);
    return new FsRawDataPath(this, currentPath);
  }

}
