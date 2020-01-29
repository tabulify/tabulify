package net.bytle.db.database;

import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.Uris;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.bytle.db.Tabular.LOCAL_FILE_SCHEME;

/**
 * A file data store (ie a store that is instantiated with a file system path or a uri)
 */
public class FileDataStore extends DataStore {

  final public static FileDataStore LOCAL_FILE_STORE = new FileDataStore("file")
    .setConnectionString(Paths.get(".").toAbsolutePath().toUri().toString())
    .setWorkingPath(Paths.get(".").toAbsolutePath().toUri().toString());

  public FileDataStore(String name) {

    super(name);

  }

  public static FileDataStore of(Path path) {
    return of(path.toUri());
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
    super.setWorkingPath(path);
    return this;
  }

  public FileDataStore setConnectionString(String connectionString) {
    super.setConnectionString(connectionString);
    return this;
  }

  private static FileDataStore of(URI uri) {

    FileDataStore fileDataStore;
    String databaseName = getDataStoreName(uri);
    switch (databaseName) {
      case LOCAL_FILE_SCHEME:
        return LOCAL_FILE_STORE;
      default:
        // Http or https gives always absolute path
        fileDataStore = new FileDataStore(databaseName)
          .setConnectionString(uri.toString())
          .setWorkingPath(uri.getPath());
        Uris.getQueryAsMap(uri.getQuery()).forEach(fileDataStore::addProperty);
        break;

    }
    return fileDataStore;
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

    return getTableSystem().getDataPath(path.toUri().getPath());

  }

}
