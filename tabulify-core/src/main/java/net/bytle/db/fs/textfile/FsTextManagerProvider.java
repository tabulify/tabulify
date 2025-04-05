package net.bytle.db.fs.textfile;

import net.bytle.db.fs.FsFileManagerProvider;
import net.bytle.db.fs.binary.FsBinaryFileManager;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class FsTextManagerProvider extends FsFileManagerProvider {



  static private FsTextManager fsTextManager;

  public static FsTextManager getSingleton() {
    if (fsTextManager == null) {
      fsTextManager = new FsTextManager();
    }
    return fsTextManager;
  }

  /**
   * This is the default manager
   *   * when no file manager reclaims the extension.
   *   * when the extension is known
   *   * or that we can detect a character set
   *
   *
   */
  @Override
  public Boolean accept(MediaType mediaType) {

    return mediaType.toString().equals(MediaTypes.TEXT_PLAIN.toString());

  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (fsTextManager == null) {
      fsTextManager = new FsTextManager();
    }
    return fsTextManager;
  }
}
