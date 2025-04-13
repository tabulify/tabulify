package com.tabulify.excel;


import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class ExcelManagerProvider extends FsFileManagerProvider {

  @Override
  public Boolean accept(MediaType mediaType) {
    return mediaType == MediaTypes.EXCEL_FILE;
  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    return ExcelManager.getManagerSingleton();
  }

}
