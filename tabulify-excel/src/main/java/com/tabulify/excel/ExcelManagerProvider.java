package com.tabulify.excel;


import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import net.bytle.fs.Fs;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;

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

  protected static MediaType getMediaTye(Path path) {
    switch (Fs.getExtension(path)) {
      case XLS:
        return EXCEL_XLS;
      case XLSX:
      default:
        return EXCEL_XLSX;
    }
  }

  private static final MediaType EXCEL_XLSX = new MediaType() {
    @Override
    public String getSubType() {
      return "application";
    }

    @Override
    public String getType() {
      return "xlsx";
    }

    @Override
    public boolean isContainer() {
      return false;
    }

    @Override
    public String getExtension() {
      return "xlsx";
    }
  };

  private static final MediaType EXCEL_XLS = new MediaType() {
    @Override
    public String getSubType() {
      return "application";
    }

    @Override
    public String getType() {
      return "xls";
    }

    @Override
    public boolean isContainer() {
      return false;
    }

    @Override
    public String getExtension() {
      return "xls";
    }
  };
}
