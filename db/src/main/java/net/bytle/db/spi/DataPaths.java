package net.bytle.db.spi;

import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.db.Tabular;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.uri.DataUri;
import net.bytle.db.uri.Uris;
import net.bytle.regexp.Globs;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a class with static utility to construct data path
 * If you want any other operations on a data path, go to the {@link Tabulars} class
 */
public class DataPaths {


  public static DataPath of(DataUri dataUri) {

    return of(DatabasesStore.ofDefault(), dataUri);

  }

  /**
   * @param parts - The part of a path in a list format
   * @return a data path created with the default tabular system (ie memory)
   */
  public static DataPath of(String part, String... parts) {


    return Tabular.tabular().getDataPath(part, parts);


  }


  /**
   * @param dataPath
   * @param part
   * @return a child data path
   * Equivalent to the resolve function in a file system {@link Path#resolve(String)}
   */
  public static DataPath childOf(DataPath dataPath, String part) {
    return dataPath.getDataSystem().getChildOf(dataPath, part);
  }


  /**
   * A data path from a path
   *
   * @param path
   * @return a file data path
   */
  public static DataPath of(Path path) {

    Database database;
    URI uri = path.toUri();
    switch (uri.getScheme()) {
      case "file":
        String workingPath = Paths.get(".").toString();
        database = Database.of("file")
          .setConnectionString(Paths.get(".").toUri().toString())
          .setWorkingPath(workingPath);
        return of(database, path);
      default:
        // Http or https gives always absolute path
        database = Database.of(uri.getHost())
          .setConnectionString(uri.toString())
          .setWorkingPath(uri.getPath());

        Uris.getQueryAsMap(uri.getQuery()).forEach(database::addProperty);

        return of(database, path);
    }

  }

  /**
   * @param dataPath - a directory data path
   * @param name
   * @return a sibling path
   * <p>
   * Example with a data path equivalent to /foo/bar and foo as name, we get a DataPath of /foo/foo
   * Equivalent to the {@link  Path#resolveSibling(String)}
   */
  public static DataPath siblingOf(DataPath dataPath, String name) {
    List<String> pathSegments = new ArrayList<>();
    pathSegments.addAll(dataPath.getPathParts().subList(0, dataPath.getPathParts().size() - 2));
    pathSegments.add(name);
    return dataPath.getDataSystem().getDataPath(pathSegments.toArray(new String[0]));
  }


  /**
   * @param dataStore
   * @param dataUri
   * @return
   */
  public static DataPath of(Database dataStore, DataUri dataUri) {

    return getTableSystem(dataStore).getDataPath(dataUri);

  }

  /**
   * @param dataStore
   * @param path
   * @return
   */
  public static DataPath of(Database dataStore, Path path) {

    return getTableSystem(dataStore).getDataPath(path.toUri());

  }

  private static TableSystem getTableSystem(Database dataStore) {
    List<TableSystemProvider> installedProviders = TableSystemProvider.installedProviders();
    String scheme = dataStore.getScheme();
    for (TableSystemProvider tableSystemProvider : installedProviders) {
      if (tableSystemProvider.getSchemes().contains(scheme)) {
        final TableSystem tableSystem = tableSystemProvider.getTableSystem(dataStore);
        if (tableSystem == null) {
          String message = "The table system is null for the provider (" + tableSystemProvider.getClass().toString() + ")";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new RuntimeException(message);
        }
        return tableSystem;
      }
    }
    final String message = "No provider was found for the scheme (" + scheme + ") from the dataStore (" + dataStore.getDatabaseName() + ") with the Url (" + dataStore.getConnectionString() + ")";
    DbLoggers.LOGGER_DB_ENGINE.severe(message);
    throw new RuntimeException(message);
  }

  public static DataPath of(DatabasesStore databasesStore, DataUri dataUri) {

    Database dataStore;
    if (dataUri.getDataStore() == null) {
      dataStore = Databases.getDefault();
    } else {
      dataStore = databasesStore.getDatabase(dataUri.getDataStore());
    }
    return of(dataStore, dataUri);
  }

  /**
   * @param databasesStore - the dataStore database
   * @param dataUri        - a data Uri
   * @param query          - the query
   * @return a data path query
   */
  public static DataPath ofQuery(DatabasesStore databasesStore, DataUri dataUri, String query) {

    return ofQuery(of(databasesStore, dataUri), query);

  }

  /**
   * @param dataPath - a container data path that defines where the query will execute
   * @param query    - the query
   * @return a data path query
   */
  public static DataPath ofQuery(DataPath dataPath, String query) {

    return dataPath.getDataSystem().getProcessingEngine().getQuery(query);

  }

  /**
   * @param dataStore - a dataStore
   * @param query     - the query
   * @return a data path query
   */
  public static DataPath ofQuery(Database dataStore, String query) {

    DataUri dataUri = DataUri.of("." + DataUri.AT_STRING + dataStore.getDatabaseName());
    return of(dataStore, dataUri).getDataSystem().getProcessingEngine().getQuery(query);

  }

  /**
   * @param dataPath - a data path container (a directory, a schema or a catalog)
   * @return the children data paths representing sql tables, schema or files
   */
  public static List<DataPath> getChildren(DataPath dataPath) {

    if (Tabulars.isDocument(dataPath)) {
      throw new RuntimeException("The data path (" + dataPath + ") is a document, it has therefore no children");
    }
    return dataPath.getDataSystem().getChildrenDataPath(dataPath);

  }

  /**
   * @param dataPath - a data path container (a directory, a schema or a catalog)
   * @return the descendant data paths representing sql tables, schema or files
   */
  public static List<DataPath> getDescendants(DataPath dataPath) {

    if (Tabulars.isDocument(dataPath)) {
      throw new RuntimeException("The data path (" + dataPath + ") is a document, it has therefore no children");
    }
    return dataPath.getDataSystem().getDescendants(dataPath);

  }

  /**
   * @param dataPath a data path container (a directory, a schema or a catalog)
   * @param glob     a glob that filters the descendant data path returned
   * @return the descendant data paths representing sql tables, schema or files
   */
  public static List<DataPath> getDescendants(DataPath dataPath, String glob) {

    if (Tabulars.isDocument(dataPath)) {
      throw new RuntimeException("The data path (" + dataPath + ") is a document, it has therefore no children");
    }
    return dataPath.getDataSystem().getDescendants(dataPath, glob);

  }

  /**
   * @param dataPath - a parent/container dataPath
   * @param glob     -  a glob pattern
   * @return the children data path of the parent that matches the glob pattern
   */
  public static List<DataPath> getChildren(DataPath dataPath, String glob) {
    final String regex = Globs.toRegexPattern(glob);
    return getChildren(dataPath)
      .stream()
      .filter(s -> s.getName().matches(regex))
      .collect(Collectors.toList());
  }

  public static List<DataPath> select(DatabasesStore databaseStore, DataUri dataUri) {
    Database database = databaseStore.getDatabase(dataUri.getDataStore());
    DataPath dataPath = DataPaths.of(database, dataUri);
    String glob = dataUri.getPath();
    return dataPath.getDataSystem().getDescendants(dataPath, glob);
  }
}
