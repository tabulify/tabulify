package net.bytle.db.excel;

import net.bytle.db.fs.FsBinaryDataPath;
import net.bytle.db.fs.FsDataStore;

import java.nio.file.Path;

public class ExcelDataPath extends FsBinaryDataPath {

  public ExcelDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore, path);
  }

  @Override
  public ExcelDataDef getOrCreateDataDef() {
    return new ExcelDataDef(this);
  }

}
