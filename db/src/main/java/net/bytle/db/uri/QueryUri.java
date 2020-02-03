package net.bytle.db.uri;

import java.nio.file.Files;
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
    Files.newDirectoryStream(dataUri.getPath(),)
  }


}
