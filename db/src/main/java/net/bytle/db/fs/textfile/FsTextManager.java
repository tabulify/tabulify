package net.bytle.db.fs.textfile;

import net.bytle.db.fs.binary.FsBinaryFileManager;
import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.FsFileManager;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;

public class FsTextManager extends FsBinaryFileManager implements FsFileManager {


  private static FsTextManager fsTextManager;

  public static FsBinaryFileManager getSingeleton() {
    if (fsTextManager == null) {
      fsTextManager = new FsTextManager();
    }
    return fsTextManager;
  }

  @Override
  public FsTextDataPath createDataPath(FsConnection fsConnection, Path path) {

    return new FsTextDataPath(fsConnection, path, MediaTypes.TEXT_PLAIN);

  }






}
