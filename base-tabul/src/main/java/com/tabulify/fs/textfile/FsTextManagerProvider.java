package com.tabulify.fs.textfile;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

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
   * * when no file manager reclaims the extension.
   * * when the extension is known
   * * or that we can detect a character set
   */
  @Override
  public Boolean accept(MediaType mediaType) {

    return MediaTypes.equals(mediaType, MediaTypes.TEXT_PLAIN);

  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (fsTextManager == null) {
      fsTextManager = new FsTextManager();
    }
    return fsTextManager;
  }
}
