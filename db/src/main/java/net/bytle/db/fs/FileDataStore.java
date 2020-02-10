package net.bytle.db.fs;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;

import java.net.URI;
import java.nio.file.Path;

import static net.bytle.db.Tabular.LOCAL_FILE_SCHEME;

/**
 * A file data store (ie a store that is instantiated with a file system path or a uri)
 */
public class FileDataStore extends DataStore {


  private final FsTableSystem fsDataSystem;
  private String workingPath;

  public FileDataStore(String name, String url, FsTableSystem fsTableSystem) {

    super(name, url);
    this.fsDataSystem = fsTableSystem;
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

  public FileDataStore setWorkingPath(String path) {
    this.workingPath = path;
    return this;
  }

  public FileDataStore setConnectionString(String connectionString) {
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

    switch (getScheme()) {
      case "jdbc":
        throw new RuntimeException("Jdbc connection string cannot be casted to a URI");
      default:
        return URI.create(connectionString);
    }

  }

  public DataPath getDataPath(Path path) {

    return this.fsDataSystem.getDataPath(path.toString());

  }

}
