package net.bytle.db.uri;

import net.bytle.fs.Fs;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryUri {


  private final DataUri dataUri;

  public QueryUri(String dataUri) {
    this.dataUri = DataUri.of(dataUri);
  }

  public static QueryUri of(String dataUri) {
    return new QueryUri(dataUri);
  }

  Map<String, String> getQueries(){
    Map<String, String> queries = new HashMap<>();
    List<Path> paths = Fs.getFilesByGlob(dataUri.getPath());
    for (Path path:paths){
      String query = Fs.getFileContent(path);
      queries.put(path.toString(),query);
    }
    return queries;

  }


}
