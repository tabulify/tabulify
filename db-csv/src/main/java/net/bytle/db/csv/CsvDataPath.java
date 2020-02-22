package net.bytle.db.csv;

import net.bytle.db.database.DataStore;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.textline.LineDataPath;

import java.nio.file.Path;


public class CsvDataPath extends LineDataPath {

  public CsvDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore, path);
  }


  public static CsvDataPath of(FsDataStore fsDataStore, Path path) {

    return new CsvDataPath(fsDataStore, path);

  }

  public static CsvDataPath of(Path path) {

    return new CsvDataPath(DataStore.of(path), path);

  }

  @Override
  public CsvDataDef getDataDef() {
    if (this.dataDef == null) {
      this.dataDef = new CsvDataDef(this);
    }
    return (CsvDataDef) this.dataDef;
  }

  @Override
  public CsvDataPath setDescription(String description) {
    super.setDescription(description);
    return this;
  }
}
