package net.bytle.db.gen.fs;

import net.bytle.db.fs.FsFileManagerProvider;
import net.bytle.db.fs.binary.FsBinaryFileManager;
import net.bytle.db.gen.GenDataPathType;
import net.bytle.type.MediaType;

import java.nio.file.Path;

public class GenFsManagerProvider extends FsFileManagerProvider {

  private GenFsManager genManager;


  @Override
  public Boolean accept(MediaType mediaType) {

    return mediaType.equals(GenDataPathType.DATA_GEN);

  }

  @Override
  public boolean accept(Path path) {

    return path.getFileName().toString().endsWith(GenDataPathType.DATA_GEN.getExtension());

  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (genManager == null) {
      genManager = new GenFsManager();
    }
    return genManager;
  }

}
