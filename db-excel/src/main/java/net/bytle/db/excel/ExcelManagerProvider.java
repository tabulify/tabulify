package net.bytle.db.excel;

import net.bytle.db.fs.FsFileManager;
import net.bytle.db.fs.FsFileManagerProvider;

import java.nio.file.Path;

public class ExcelManagerProvider extends FsFileManagerProvider {
  private ExcelManager excelManager;

  @Override
  public Boolean accept(Path path) {

    if (path.toString().toLowerCase().endsWith("xlsx")){
      return true;
    } else {
      return false;
    }

  }

  @Override
  public FsFileManager getFsFileManager() {
    if (excelManager == null){
      excelManager = new ExcelManager();
    }
    return excelManager;
  }

}
