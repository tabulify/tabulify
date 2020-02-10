package net.bytle.db.json;

import net.bytle.db.fs.FsFileManager;
import net.bytle.db.fs.FsFileManagerProvider;

import java.util.Arrays;
import java.util.List;

public class JsonManagerProvider extends FsFileManagerProvider {

  static private JsonManager jsonManager;

  @Override
  public List<String> getContentType() {
    return Arrays.asList("json","jsonl");
  }

  @Override
  public FsFileManager getFsFileManager() {
    if (jsonManager == null){
      jsonManager = new JsonManager();
    }
    return jsonManager;
  }

}
