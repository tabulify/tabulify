package net.bytle.xml;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.textfile.FsTextManager;

import java.nio.file.Path;

public class XmlManagerFs extends FsTextManager {


  @Override
  public XmlDataPath createDataPath(FsConnection fsConnection, Path path) {

    return new XmlDataPath(fsConnection, path);

  }


}
