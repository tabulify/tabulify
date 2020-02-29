package net.bytle.db.gen.fs;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.fs.FsRawDataPath;
import net.bytle.db.gen.GenDataDef;
import net.bytle.db.gen.GenDataPath;

import java.nio.file.Path;

public class GenFsDataPath extends FsRawDataPath implements FsDataPath, GenDataPath {

  public GenFsDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore, path);
  }

  @Override
  public GenDataDef getOrCreateDataDef() {

    return new GenDataDef(this);

  }


  /**
   * The name of a data generation path
   * is without the extension
   * @return
   */
  @Override
  public String getName() {
    return super.getName().replace(GenFsManagerProvider.EXTENSION,"");
  }



}
