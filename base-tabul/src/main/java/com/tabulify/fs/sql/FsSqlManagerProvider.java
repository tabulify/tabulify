package com.tabulify.fs.sql;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.exception.CastException;
import com.tabulify.type.MediaType;

public class FsSqlManagerProvider extends FsFileManagerProvider {

  static private FsSqlFileManager sqlFileManager;

  /**
   *
   */
  @Override
  public Boolean accept(MediaType mediaType) {

    try {
      FsSqlMediaType.cast(mediaType);
      return true;
    } catch (CastException e) {
      return false;
    }

  }


  @Override
  public FsSqlFileManager getFsFileManager() {
    if (sqlFileManager == null) {
      sqlFileManager = new FsSqlFileManager();
    }
    return sqlFileManager;
  }
}
