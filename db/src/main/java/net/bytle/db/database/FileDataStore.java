package net.bytle.db.database;

import net.bytle.db.uri.Uris;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A file data store (ie a store that is instantiated with a file system path or a uri)
 */
public class FileDataStore extends DataStore {



  public FileDataStore(Path path) {
    this(path.toUri());
  }

  public FileDataStore(URI uri) {

    super(uri.toString());
    this.setWorkingPath(uri.getPath());

    switch (uri.getScheme()) {
      case "file":
        String workingPath = Paths.get(".").toString();
        this
          .setConnectionString(Paths.get(".").toUri().toString())
          .setWorkingPath(workingPath);
        break;
      default:
        // Http or https gives always absolute path
        this
          .setConnectionString(uri.toString())
          .setWorkingPath(uri.getPath());

        Uris.getQueryAsMap(uri.getQuery()).forEach(this::addProperty);
        break;

    }

  }


  public URI getUri() {

    switch (getScheme()) {
      case "jdbc":
        throw new RuntimeException("Jdbc connection string cannot be casted to a URI");
      default:
        return URI.create(connectionString);
    }

  }

}
