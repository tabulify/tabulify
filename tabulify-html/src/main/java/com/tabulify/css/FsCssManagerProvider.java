package com.tabulify.css;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class FsCssManagerProvider extends FsFileManagerProvider {


  static private FsCssFileManager fsCssFileManager;

  public static FsCssFileManager getSingleton() {
    if (fsCssFileManager == null) {
      fsCssFileManager = new FsCssFileManager();
    }
    return fsCssFileManager;
  }

  /**
   */
  @Override
  public Boolean accept(MediaType mediaType) {

    return isCssFileExtensionOrMime(mediaType);

  }

  public static Boolean isCssFileExtensionOrMime(MediaType mimeType) {

    return mimeType.getSubType().equals(MediaTypes.TEXT_CSS.getSubType());

  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (fsCssFileManager == null) {
      fsCssFileManager = new FsCssFileManager();
    }
    return fsCssFileManager;
  }
}
