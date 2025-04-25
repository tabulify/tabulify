package com.tabulify.gen.fs;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.gen.GenDataPathType;
import net.bytle.type.MediaType;

import java.nio.file.Path;

public class GenFsManagerProvider extends FsFileManagerProvider {

  private GenFsManager genManager;


  @Override
  public Boolean accept(MediaType mediaType) {

    return mediaType.equals(GenDataPathType.DATA_GEN);

  }


  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (genManager == null) {
      genManager = new GenFsManager();
    }
    return genManager;
  }

}
