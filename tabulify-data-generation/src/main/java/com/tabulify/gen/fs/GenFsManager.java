package com.tabulify.gen.fs;

import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.fs.FsConnection;

import java.nio.file.Path;

public class GenFsManager extends FsBinaryFileManager {

  static GenFsManager genFsManager;

  public static GenFsManager getSingletonOfFsManager() {
    if (genFsManager==null){
      genFsManager = new GenFsManager();
    }
    return genFsManager;
  }

  @Override
  public GenFsDataPath createDataPath(FsConnection fsConnection, Path path) {

    return new GenFsDataPath(fsConnection, path);

  }




}
