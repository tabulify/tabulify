package com.tabulify.excel;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.binary.FsBinaryDataPath;
import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.type.MediaType;

import java.nio.file.Path;


public class ExcelManager extends FsBinaryFileManager {

  private static ExcelManager excelManager;

  public static ExcelManager getManagerSingleton() {
    if (excelManager == null) {
      excelManager = new ExcelManager();
    }
    return excelManager;
  }

  @Override
  public FsBinaryDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {
    return new ExcelDataPath(fsConnection, relativePath);
  }

  @Override
  public void create(FsDataPath fsDataPath) {

    ((ExcelDataPath) fsDataPath).createFile();
  }
}
