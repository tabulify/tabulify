package net.bytle.db.gen;

import net.bytle.db.fs.FsFileManager;
import net.bytle.db.fs.FsFileManagerProvider;

import java.nio.file.Path;

public class GenFsManagerProvider extends FsFileManagerProvider {
  private GenFsManager genManager;

  @Override
  public Boolean accept(Path path) {

    if (path.endsWith("--datagen.yml")){
      return true;
    } else {
      return false;
    }

  }

  @Override
  public FsFileManager getFsFileManager() {
    if (genManager == null){
      genManager = new GenFsManager();
    }
    return genManager;
  }

}
