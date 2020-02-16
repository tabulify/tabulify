package net.bytle.db.gen;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.fs.FsRawDataPath;

import java.nio.file.Path;

public class GenFsDataPath extends FsRawDataPath implements FsDataPath, GenDataPath {

  public GenFsDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore, path);
  }

  @Override
  public GenDataDef getDataDef() {
    return new GenDataDef(this);
  }

}
