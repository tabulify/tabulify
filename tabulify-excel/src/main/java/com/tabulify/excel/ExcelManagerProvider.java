package com.tabulify.excel;


import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class ExcelManagerProvider extends FsFileManagerProvider {


  @Override
  public Boolean accept(MediaType mediaType) {

    String subType = mediaType.getSubType();
    switch (subType) {
      case "xlsx":
      case "vnd.openxmlformats-officedocument.spreadsheetml.sheet":
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
