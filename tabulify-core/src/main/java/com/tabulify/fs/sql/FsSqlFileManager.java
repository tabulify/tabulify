package com.tabulify.fs.sql;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsFileManager;
import com.tabulify.fs.binary.FsBinaryFileManager;
import net.bytle.type.MediaType;

import java.nio.file.Path;

public class FsSqlFileManager extends FsBinaryFileManager implements FsFileManager {


  private static FsSqlFileManager fsJsFileManager;

  public static FsSqlFileManager getSingleton() {
    if (fsJsFileManager == null) {
      fsJsFileManager = new FsSqlFileManager();
    }
    return fsJsFileManager;
  }

  @Override
  public FsSqlDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {

    return new FsSqlDataPath(fsConnection, relativePath);

  }






}
