package com.tabulify.csv;

import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

public class CsvManagerProvider extends FsFileManagerProvider {

  private CsvManagerFs csvManager;

  @Override
  public Boolean accept(MediaType mimeType) {

    // you may also have application/csv, text/csv ...
    return mimeType.getSubType().equals(MediaTypes.TEXT_CSV.getSubType());
  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (csvManager == null){
      csvManager = new CsvManagerFs();
    }
    return csvManager;
  }
}
