package com.tabulify.json;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import net.bytle.exception.CastException;
import net.bytle.type.MediaType;

public class JsonManagerProvider extends FsFileManagerProvider {

  static private JsonManagerFs jsonManager;

  @Override
  public Boolean accept(MediaType mediaType) {

    try {
      JsonMediaType.cast(mediaType);
      return true;
    } catch (CastException e) {
      return false;
    }


  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (jsonManager == null) {
      jsonManager = new JsonManagerFs();
    }
    return jsonManager;
  }

}
