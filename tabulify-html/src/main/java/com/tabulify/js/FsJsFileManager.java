package com.tabulify.js;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsFileManager;
import com.tabulify.fs.binary.FsBinaryFileManager;

import java.nio.file.Path;

public class FsJsFileManager extends FsBinaryFileManager implements FsFileManager {


  private static FsJsFileManager fsJsFileManager;

  public static FsBinaryFileManager getSingeleton() {
    if (fsJsFileManager == null) {
      fsJsFileManager = new FsJsFileManager();
    }
    return fsJsFileManager;
  }

  @Override
  public FsJsDataPath createDataPath(FsConnection fsConnection, Path path) {

    return new FsJsDataPath(fsConnection, path);

  }






}
