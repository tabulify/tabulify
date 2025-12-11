package com.tabulify.xml;

import com.tabulify.fs.FsFileManagerProvider;
import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

public class XmlManagerProvider extends FsFileManagerProvider {

  static private XmlManagerFs xmlManagerFs;

  @Override
  public Boolean accept(MediaType mediaType) {

    return MediaTypes.TEXT_XML.getSubType().equals(mediaType.getSubType());

  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (xmlManagerFs == null) {
      xmlManagerFs = new XmlManagerFs();
    }
    return xmlManagerFs;
  }

}
