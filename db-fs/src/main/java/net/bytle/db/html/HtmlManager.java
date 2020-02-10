package net.bytle.db.html;

import net.bytle.db.fs.struct.FsFileManager;
import net.bytle.db.fs.FsTableSystem;
import net.bytle.db.spi.DataPath;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class HtmlManager extends FsFileManager {

  private static Map<FsTableSystem, HtmlManager> htmlManagers = new HashMap<>();

  public HtmlManager(FsTableSystem fsTableSystem) {
    super(fsTableSystem);
  }

  @Override
  public void create(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public HtmlDataPath getDataPath(Path path) {
    return new HtmlDataPath(getFsTableSystem(),path);
  }


  public static FsFileManager of(FsTableSystem fsTableSystem) {
    HtmlManager htmlManager = htmlManagers.get(fsTableSystem);
    if (htmlManager == null){
      htmlManager = new HtmlManager(fsTableSystem);
      htmlManagers.put(fsTableSystem,htmlManager);
    }
    return htmlManager;
  }

}
