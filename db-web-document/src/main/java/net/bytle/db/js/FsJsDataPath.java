package net.bytle.db.js;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.textfile.FsTextDataPath;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class FsJsDataPath extends FsTextDataPath {

  protected static Set<MediaType> FS_FILE_EXTENSION_OR_MIME = new HashSet<>();


  public FsJsDataPath(FsConnection fsConnection, Path path) {
    super(fsConnection, path);
  }


  @Override
  public String getLogicalName() {
    String textFileLogicalName = super.getLogicalName();
    String prefix = ".min";
    if (textFileLogicalName.endsWith(prefix)) {
      textFileLogicalName = textFileLogicalName.substring(0, textFileLogicalName.length() - prefix.length());
    }
    return textFileLogicalName;
  }

  @Override
  public MediaType getMediaType() {
    return MediaTypes.TEXT_JAVASCRIPT;
  }
}
