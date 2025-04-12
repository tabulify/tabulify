package com.tabulify.xml;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextManager;

import java.nio.file.Path;

public class XmlManagerFs extends FsTextManager {


  @Override
  public XmlDataPath createDataPath(FsConnection fsConnection, Path path) {

    return new XmlDataPath(fsConnection, path);

  }


}
