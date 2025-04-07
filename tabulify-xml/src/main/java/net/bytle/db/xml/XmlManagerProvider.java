package net.bytle.db.xml;

import net.bytle.db.fs.FsFileManagerProvider;
import net.bytle.db.fs.binary.FsBinaryFileManager;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

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
