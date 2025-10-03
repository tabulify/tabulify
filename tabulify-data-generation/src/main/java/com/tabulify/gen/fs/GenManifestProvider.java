package com.tabulify.gen.fs;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.gen.GeneratorMediaType;
import net.bytle.type.MediaType;

import java.nio.file.Path;

public class GenManifestProvider extends FsFileManagerProvider {


  static final GenManifestManager GEN_MANIFEST_MANAGER = new GenManifestManager();

  @Override
  public Boolean accept(MediaType mediaType) {
    return mediaType.equals(GeneratorMediaType.DATA_GEN);
  }


  @Override
  public GenManifestManager getFsFileManager() {
    return GEN_MANIFEST_MANAGER;
  }


  public static class GenManifestManager extends FsBinaryFileManager {


    @Override
    public GenManifestDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {

      return new GenManifestDataPath(fsConnection, relativePath);

    }


  }


}
