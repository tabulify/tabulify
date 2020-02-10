package net.bytle.db.json;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsTableSystem;

import java.nio.file.Path;

public class JsonDataPath extends FsDataPath {

  public JsonDataPath(FsTableSystem fsTableSystem, Path path) {
    super(fsTableSystem, path);
  }

  @Override
  public JsonDataDef getDataDef() {
    if (this.dataDef == null) {
      this.dataDef = new JsonDataDef(this);
    }
    return (JsonDataDef) this.dataDef;
  }
}
