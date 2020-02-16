package net.bytle.db.excel;

import net.bytle.db.fs.FsRawDataPath;
import net.bytle.db.fs.FsDataStore;

import java.nio.file.Path;

public class ExcelDataPath extends FsRawDataPath {

  public ExcelDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore, path);
  }

  @Override
  public ExcelDataDef getDataDef() {
    return new ExcelDataDef(this);
  }

}
