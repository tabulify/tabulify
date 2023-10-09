package net.bytle.db.excel;

import net.bytle.db.fs.FsFileManagerProvider;
import net.bytle.db.fs.binary.FsBinaryFileManager;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class ExcelManagerProvider extends FsFileManagerProvider {
  private ExcelManager excelManager;

  @Override
  public Boolean accept(MediaType mediaType) {

    return mediaType == MediaTypes.EXCEL_FILE;

  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (excelManager == null){
      excelManager = new ExcelManager();
    }
    return excelManager;
  }

}
