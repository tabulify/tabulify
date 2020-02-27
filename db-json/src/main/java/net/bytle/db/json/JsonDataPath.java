package net.bytle.db.json;

import net.bytle.db.fs.FsDataStore;
import net.bytle.db.fs.FsRawDataPath;

import java.nio.file.Path;

public class JsonDataPath extends FsRawDataPath {

  public JsonDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore, path);
  }

  @Override
  public JsonDataDef getOrCreateDataDef() {
    if (this.relationDef == null) {
      this.relationDef = new JsonDataDef(this);
    }
    return (JsonDataDef) this.relationDef;
  }
}
