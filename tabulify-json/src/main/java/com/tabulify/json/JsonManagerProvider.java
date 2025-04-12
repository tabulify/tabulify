package com.tabulify.json;

import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.fs.FsFileManagerProvider;
import net.bytle.type.MediaType;

public class JsonManagerProvider extends FsFileManagerProvider {

  static private JsonManagerFs jsonManager;

  @Override
  public Boolean accept(MediaType mediaType) {


    for (MediaType acceptedMediaType : JsonDataPath.ACCEPTED_MEDIA_TYPES) {
      if (mediaType.getSubType().equals(acceptedMediaType.getSubType())){
        return true;
      }
    }
    return false;

  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (jsonManager == null){
      jsonManager = new JsonManagerFs();
    }
    return jsonManager;
  }

}
