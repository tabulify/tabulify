package net.bytle.db.fs.dir;

import net.bytle.db.fs.FsFileManagerProvider;
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

  /**
   *
   *
   */
  @Override
  public Boolean accept(MediaType mediaType) {

    return mediaType.getSubType().equals(MediaTypes.DIR.getSubType());

  }

  @Override
  public FsDirectoryManager getFsFileManager() {
    if (fsDirectoryManager == null) {
      fsDirectoryManager = new FsDirectoryManager();
    }
    return fsDirectoryManager;
  }
}
