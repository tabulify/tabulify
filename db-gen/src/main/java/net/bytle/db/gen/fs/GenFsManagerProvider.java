package net.bytle.db.gen.fs;

import net.bytle.db.fs.FsBinaryFileManager;
import net.bytle.db.fs.FsFileManagerProvider;

import java.nio.file.Path;

public class GenFsManagerProvider extends FsFileManagerProvider {

  private GenFsManager genManager;

  @Override
  public Boolean accept(Path path) {

    if (path.toString().toLowerCase().endsWith(GenFsDataPath.EXTENSION)){
      return true;
    } else {
      return false;
    }

  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (genManager == null){
      genManager = new GenFsManager();
    }
    return genManager;
  }

}
