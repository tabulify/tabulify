package com.tabulify.fs.sql;

import com.tabulify.fs.FsFileManagerProvider;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class FsSqlManagerProvider extends FsFileManagerProvider {

  static private FsSqlFileManager sqlFileManager;

  /**
   */
  @Override
  public Boolean accept(MediaType mediaType) {

    return mediaType.getSubType().equals(MediaTypes.TEXT_SQL.getSubType());
  }


  @Override
  public FsSqlFileManager getFsFileManager() {
    if (sqlFileManager == null) {
      sqlFileManager = new FsSqlFileManager();
    }
    return sqlFileManager;
  }
}
