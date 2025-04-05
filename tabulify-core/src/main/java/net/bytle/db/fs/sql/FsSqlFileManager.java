package net.bytle.db.fs.sql;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.FsFileManager;
import net.bytle.db.fs.binary.FsBinaryFileManager;

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
  public FsSqlDataPath createDataPath(FsConnection fsConnection, Path path) {

    return new FsSqlDataPath(fsConnection, path);

  }






}
