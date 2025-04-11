package net.bytle.db.css;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.FsFileManager;
import net.bytle.db.fs.binary.FsBinaryFileManager;

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
