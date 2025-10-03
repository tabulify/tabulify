package com.tabulify.xml;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextManager;
import net.bytle.type.MediaType;

import java.nio.file.Path;

public class XmlManagerFs extends FsTextManager {


  @Override
  public XmlDataPath createDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {

    return new XmlDataPath(fsConnection, relativePath);

  }


}
