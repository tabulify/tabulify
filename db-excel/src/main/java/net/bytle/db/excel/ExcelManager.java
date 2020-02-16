package net.bytle.db.excel;

import net.bytle.db.fs.FsDataStore;
import net.bytle.db.fs.FsFileManager;

import java.nio.file.Path;

public class ExcelManager extends FsFileManager {


  @Override
  public ExcelDataPath createDataPath(FsDataStore fsDataStore, Path path) {

    return new ExcelDataPath(fsDataStore, path);

  }




}
