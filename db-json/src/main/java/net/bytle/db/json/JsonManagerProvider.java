package net.bytle.db.json;

import net.bytle.db.fs.FsBinaryFileManager;
import net.bytle.db.fs.FsFileManagerProvider;

import java.nio.file.Path;

public class JsonManagerProvider extends FsFileManagerProvider {

  static private JsonManager jsonManager;

  @Override
  public Boolean accept(Path path) {

    return path.toString().toLowerCase().endsWith("json") || path.toString().toLowerCase().endsWith("jsonl");

  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (jsonManager == null){
      jsonManager = new JsonManager();
    }
    return jsonManager;
  }

}
