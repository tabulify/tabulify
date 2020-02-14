package net.bytle.db.csv;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataStore;

import java.nio.file.Path;


public class CsvDataPath extends FsDataPath {

  public CsvDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore, path);
  }


  public static CsvDataPath of(FsDataStore fsDataStore, Path path) {

    return new CsvDataPath(fsDataStore, path);

  }

  public static CsvDataPath of(Path path) {

    return new CsvDataPath(FsDataStore.getLocalFileSystem(), path);

  }

  @Override
  public CsvDataDef getDataDef() {
    if (this.dataDef == null) {
      this.dataDef = new CsvDataDef(this);
    }
    return (CsvDataDef) this.dataDef;
  }

}
