package net.bytle.db.gen.fs;

import net.bytle.db.fs.binary.FsBinaryFileManager;
import net.bytle.db.fs.FsConnection;

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
