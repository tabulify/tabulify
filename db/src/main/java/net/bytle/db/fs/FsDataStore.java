package net.bytle.db.fs;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.ProcessingEngine;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A file data store (ie a store that is instantiated with a file system path or a uri)
 */
public class FsDataStore extends DataStore {


  public static final FsDataStore LOCAL_FILE_SYSTEM = new FsDataStore("file", Paths.get(".").toAbsolutePath().toString());


  public FsDataStore(String name, String url) {

    super(name, url);

  }


  FileSystem getFileSystem() {
    return Paths.get(this.getUri()).getFileSystem();
  }


  public FsDataStore setConnectionString(String connectionString) {
    super.setConnectionString(connectionString);
    return this;
  }

  @Override
  public FsTableSystem getDataSystem() {
    return FsTableSystem.of();
  }

  @Override
  public void close() {
    FsTableSystem.of();
  }

  @Override
  public boolean isOpen() {
    return false;
  }

  @Override
  public DataPath getQueryDataPath(String query) {
    throw new RuntimeException("Query is not yet supported on file");
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
    return new FsDataPath(this, currentPath);
  }

}
