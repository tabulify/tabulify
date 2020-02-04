package net.bytle.db.uri;

import net.bytle.fs.Fs;

import java.nio.file.Path;
import java.util.List;

public class QueryUri {


  private final DataUri dataUri;

  public QueryUri(String dataUri) {
    this.dataUri = DataUri.of(dataUri);
  }

  public static QueryUri of(String dataUri) {
    return new QueryUri(dataUri);
  }

  public List<Path> getFilePaths() {
    return Fs.getFilesByGlob(dataUri.getPath());
  }




}
