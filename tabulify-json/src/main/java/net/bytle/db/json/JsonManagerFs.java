package net.bytle.db.json;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.textfile.FsTextManager;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotAbsoluteException;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;

public class JsonManagerFs extends FsTextManager {


  @Override
  public JsonDataPath createDataPath(FsConnection fsConnection, Path path) {

    try {
      Path absolute = fsConnection.getDataSystem().toAbsolutePath(path);
      return new JsonDataPath(fsConnection, absolute, MediaTypes.createFromPath(absolute));
    } catch (NotAbsoluteException e) {
      throw new InternalException("It should not happen as the path is absolute");
    }

  }


}
