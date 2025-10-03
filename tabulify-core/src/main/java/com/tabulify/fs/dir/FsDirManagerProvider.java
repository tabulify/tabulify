package com.tabulify.fs.dir;

import com.tabulify.fs.FsFileManagerProvider;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class FsDirManagerProvider extends FsFileManagerProvider {


  static private FsDirectoryManager fsDirectoryManager;

  public static FsDirectoryManager getSingleton() {
    if (fsDirectoryManager == null) {
      fsDirectoryManager = new FsDirectoryManager();
    }
    return fsDirectoryManager;
  }

  @Override
  public Boolean accept(MediaType mediaType) {
    return MediaTypes.equals(mediaType, MediaTypes.DIR);
  }

  @Override
  public FsDirectoryManager getFsFileManager() {
    return getSingleton();
  }
}
