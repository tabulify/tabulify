package net.bytle.db.fs;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.TableSystem;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.bytle.db.Tabular.LOCAL_FILE_SCHEME;

/**
 * A file data store (ie a store that is instantiated with a file system path or a uri)
 */
public class FsDataStore extends DataStore {


  private final FsTableSystem fsDataSystem;
  private String workingPath;

  public FsDataStore(String name, String url, FsTableSystem fsTableSystem) {

    super(name, url);
    this.fsDataSystem = fsTableSystem;

  }


  FileSystem getFileSystem() {
    return Paths.get(this.getUri()).getFileSystem();
  }

  /**
   * @param uri
   * @return a unique database name from an Uri (ie path.toUri)
   */
  public static String getDataStoreName(URI uri) {
    String databaseName;
    switch (uri.getScheme()) {
      case LOCAL_FILE_SCHEME:
        databaseName = LOCAL_FILE_SCHEME;
        break;
      default:
        databaseName = uri.getScheme() + "://" + uri.getHost();
        break;
    }
    return databaseName;
  }

  public FsDataStore setWorkingPath(String path) {
    this.workingPath = path;
    return this;
  }

  public FsDataStore setConnectionString(String connectionString) {
    super.setConnectionString(connectionString);
    return this;
  }

  @Override
  public TableSystem getDataSystem() {
    return this.fsDataSystem;
  }

  @Override
  public void close() {
    this.fsDataSystem.close();
  }

  @Override
  public boolean isOpen() {
    return false;
  }


  public URI getUri() {

    return URI.create(connectionString);

  }

  public FsDataPath getDataPath(Path path) {

    return new FsDataPath(this,path);

  }

  @Override
  public FsDataPath getDataPath(String... names) {

    // Rebuild the path
    Path currentPath = Paths.get(this.getUri());
    Path path = currentPath;
    for (String name : names) {
      path = path.resolve(name);
    }

    return getDataPath(path);

  }

  @Override
  public FsDataPath getCurrentPath() {
    Path currentPath = Paths.get(this.getUri());
    return new FsDataPath(this, currentPath);
  }

}
