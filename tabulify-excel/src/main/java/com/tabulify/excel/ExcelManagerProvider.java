package com.tabulify.excel;


import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class ExcelManagerProvider extends FsFileManagerProvider {


  public static final String XLSX = "xlsx";
  public static final String XLS = "xls";

  @Override
  public Boolean accept(MediaType mediaType) {

    String subType = mediaType.getSubType();
    if (subType.equals("vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
      return true;
    }

    switch (mediaType.getExtension()) {
      case XLSX:
      case XLS:
        return true;
      default:
        return false;
    }

  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    return ExcelManager.getManagerSingleton();
  }

}
