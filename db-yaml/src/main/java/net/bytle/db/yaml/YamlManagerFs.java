package net.bytle.db.yaml;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.textfile.FsTextManager;

import java.nio.file.Path;

public class YamlManagerFs extends FsTextManager {


  @Override
  public YamlDataPath createDataPath(FsConnection fsConnection, Path path) {

    return new YamlDataPath(fsConnection, path);

  }


}
