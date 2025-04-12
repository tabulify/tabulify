package com.tabulify.js;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class FsJsManagerProvider extends FsFileManagerProvider {



  static private FsJsFileManager fsJsFileManager;

  public static FsJsFileManager getSingleton() {
    if (fsJsFileManager == null) {
      fsJsFileManager = new FsJsFileManager();
    }
    return fsJsFileManager;
  }

  /**
   *
   *
   */
  @Override
  public Boolean accept(MediaType mediaType) {

    return isJsFileExtensionOrMime(mediaType);
  }

  public static Boolean isJsFileExtensionOrMime(MediaType mediaType) {


    /**
     * media type may be
     * text/javascript; charset=utf-8
     */
    for (MediaType jsMediaType : FsJsDataPath.FS_FILE_EXTENSION_OR_MIME) {

      if (jsMediaType.equals(mediaType)){
        return true;
      }
    }
    /**
     * The file extension is apart to not have a `json` file
     * become a `js` javascript file
     */
    return mediaType.getSubType().equals(MediaTypes.TEXT_JAVASCRIPT.getSubType());
  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (fsJsFileManager == null) {
      fsJsFileManager = new FsJsFileManager();
    }
    return fsJsFileManager;
  }


}
