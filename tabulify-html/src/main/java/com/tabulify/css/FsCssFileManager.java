package com.tabulify.css;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsFileManager;
import com.tabulify.fs.binary.FsBinaryFileManager;

import java.nio.file.Path;

public class FsCssFileManager extends FsBinaryFileManager implements FsFileManager {


  private static FsCssFileManager fsCssFileManager;

  public static FsBinaryFileManager getSingeleton() {
    if (fsCssFileManager == null) {
      fsCssFileManager = new FsCssFileManager();
    }
    return fsCssFileManager;
  }

  @Override
  public FsCssDataPath createDataPath(FsConnection fsConnection, Path path) {

    return new FsCssDataPath(fsConnection, path);

  }






}
