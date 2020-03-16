package net.bytle.db.gen.fs;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataPathAbs;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.gen.GenDataDef;
import net.bytle.db.gen.GenDataPath;

import java.nio.file.Path;

public class GenFsDataPath extends FsDataPathAbs implements FsDataPath, GenDataPath {

  public static final String EXTENSION = "--datagen.yml";

  public GenFsDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore, path);
  }

  @Override
  public GenDataDef getOrCreateDataDef() {
    if (super.relationDef==null) {
      super.relationDef = new GenDataDef(this);
    }
    return (GenDataDef) super.relationDef;
  }

  @Override
  public net.bytle.db.jdbc.SqlDataPath.Type getType() {
    return "generator";
  }


  /**
   * The name of a data generation path
   * is without the extension
   *
   * @return
   */
  @Override
  public String getName() {
    return super.getName().replace(EXTENSION, "");
  }


  @Override
  public GenFsManager getFileManager() {
    return GenFsManager.getSingletonOfFsManager();
  }
}
