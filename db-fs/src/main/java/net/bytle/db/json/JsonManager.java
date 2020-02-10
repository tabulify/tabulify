package net.bytle.db.json;

import net.bytle.db.fs.struct.FsFileManager;
import net.bytle.db.fs.FsTableSystem;
import net.bytle.db.spi.DataPath;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JsonManager extends FsFileManager {

  private static Map<FsTableSystem,JsonManager> jsonManagers = new HashMap<>();

  public JsonManager(FsTableSystem fsTableSystem) {
    super(fsTableSystem);
  }

  @Override
  public void create(DataPath dataPath) {
    throw new RuntimeException("To do");
  }

  @Override
  public JsonDataPath getDataPath(Path path) {
    return new JsonDataPath(getFsTableSystem(),path);
  }

  public static FsFileManager of(FsTableSystem fsTableSystem) {
    JsonManager jsonManager = jsonManagers.get(fsTableSystem);
    if (jsonManager == null){
      jsonManager = new JsonManager(fsTableSystem);
      jsonManagers.put(fsTableSystem,jsonManager);
    }
    return jsonManager;
  }
}
