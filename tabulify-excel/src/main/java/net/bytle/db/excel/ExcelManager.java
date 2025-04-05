package net.bytle.db.excel;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.binary.FsBinaryFileManager;

import java.nio.file.Path;

public class ExcelManager extends FsBinaryFileManager {


  @Override
  public ExcelDataPath createDataPath(FsConnection fsConnection, Path path) {

    return new ExcelDataPath(fsConnection, path);

  }




}
