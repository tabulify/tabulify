package net.bytle.db.gen.fs;

import net.bytle.db.fs.FsBinaryFileManager;
import net.bytle.db.fs.FsFileManagerProvider;

import java.nio.file.Path;

public class GenFsManagerProvider extends FsFileManagerProvider {
  public static final String EXTENSION = "--datagen.yml";
  private GenFsManager genManager;

  @Override
  public Boolean accept(Path path) {

    if (path.toString().toLowerCase().endsWith(EXTENSION)){
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
