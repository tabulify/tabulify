package net.bytle.db.css;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.textfile.FsTextDataPath;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;

public class FsCssDataPath extends FsTextDataPath {



  public FsCssDataPath(FsConnection fsConnection, Path path) {
    super(fsConnection, path, MediaTypes.TEXT_CSS);
  }


  @Override
  public String getLogicalName() {
    String textFileLogicalName = super.getLogicalName();
    String prefix = ".min";
    if (textFileLogicalName.endsWith(prefix)){
      textFileLogicalName = textFileLogicalName.substring(0,textFileLogicalName.length()-prefix.length());
    }
    return textFileLogicalName;
  }

  @Override
  public MediaType getMediaType() {
    return MediaTypes.TEXT_CSS;
  }
}
